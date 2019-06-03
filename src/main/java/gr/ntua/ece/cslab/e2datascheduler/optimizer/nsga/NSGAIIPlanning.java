package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.GraphNode;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.JobGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.Layer;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ScheduledGraphNode;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
//import gr.ntua.ece.cslab.e2datascheduler.ml.util.FeatureExtractor;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: NSGA planning should not be static. The structures on which NSGA operates should be regular objects
/**
 * Class that implements the main functionalities of the NSGAII algorithm, i.e.,
 * the 'newSolution' and 'evaluate' methods
 */
public class NSGAIIPlanning extends AbstractProblem {

    /**
     * The input JobGraph.
     */
    public static JobGraph tasks;
    //public static Map<Integer, GraphNode> tasks;

    /**
     * A mapping between objective names and their corresponding integer
     * identifier.
     * E.g.: { "execTime" : 0,  "powerCons" : 1,  ... } .
     * This mapping is required because MOEA handles objectives as integers.
     */
    public static Map<String, Integer> objectives;

    /**
     * A list of the available devices in the cluster.
     */
    public static List<HwResource> devices;

    /**
     * The Model that is being consulted for the cost of a task on a particular
     * device.
     */
    public static Model mlModel;

    public static Map<Solution, ExecutionGraph> solutionGraphs;


    public NSGAIIPlanning(){
        super(tasks.size(), objectives.size());
    }

    /**
     * This method is automatically invoked by the MOEA framework at each
     * generation/iteration of the genetic algorithm.
     * @return a candidate solution for the optimization problem
     */
    @Override
    public Solution newSolution() {
        Solution solution = new Solution(tasks.size(), objectives.size());

        for (int taskID = 0; taskID < tasks.size(); ++taskID) {
            solution.setVariable(taskID, EncodingUtils.newInt(0, devices.size() - 1));
        }

        return solution;
    }

    /**
     * Method that evaluates how good or bad a candidate solution is.
     * The result of the evaluation is stored in the
     * {@link org.moeaframework.core.Solution} object itself.
     * @param solution A candidate solution produced by the MOEA framework.
     */
    @Override
    public void evaluate(Solution solution) {
        ExecutionGraph executionGraph = constructExecutionGraph(EncodingUtils.getInt(solution));
        Map<String, Double> objectiveCosts = executionGraph.getObjectiveCosts();
        solutionGraphs.put(solution, executionGraph);

        // FIXME(ckatsak): For now, objectives are identified via String objects.
        // This should probably change. Maybe Enums + Visitor ?
        for (String objective : objectives.keySet()) {
            switch (objective) {
                case "execTime":
                    objectiveCosts.put(objective, calculateExecutionTime(executionGraph));
                    break;
                case "powerCons":
                    objectiveCosts.put(objective, calculatePowerConsumption(executionGraph));
                    break;
                default:
                    // FIXME(ckatsak): Should be unreachable; yet... will it be?
                    // For now, just log it and skip its evaluation. <-- broken FIXME!
                    System.err.printf("[NSGAIIPlanning] Unknown objective: '%s'", objective);
                    break;
            }
        }

        for (Map.Entry<String, Integer> objective : objectives.entrySet()) {
            solution.setObjective(objective.getValue(), objectiveCosts.get(objective.getKey()));
        }
    }

    /**
     * Construct the ExecutionGraph imposed by the the given solution's plan.
     */
    //FIXME: What happens if a resource assigned by the plan is already in use?
    private ExecutionGraph constructExecutionGraph(int[] plan) {
        //FIXME: assertions good for testing but not for prod code. Maybe create appropriate exception?
        assert plan.length == tasks.size() : "plan.length != tasks.size()";

        /* Allocate memory for the ExecutionGraph, and begin initializing it. */
        ExecutionGraph executionGraph = new ExecutionGraph();
        executionGraph.setExecutionGraph(new ArrayList<ScheduledGraphNode>(plan.length));
        executionGraph.setObjectiveCosts(new HashMap<String, Double>());
        executionGraph.setRoots(tasks.getRoots());
        executionGraph.setIndexedGraph(tasks.getIndexedGraph());

        /* Allocate memory for each node, initialize and start annotating each
         * with its assigned device according to the current plan. */
        ArrayList<GraphNode> graphNodes = tasks.getGraph();
        for (int i = 0; i < plan.length; ++i) {
            GraphNode gn = graphNodes.get(i);
            //FIXME: assertions good for testing but not for prod code. Maybe create appropriate exception?
            assert i == gn.getId() : "plan's index != GraphNode.getId()";
            ScheduledGraphNode sgn = new ScheduledGraphNode();
            sgn.setId(i);
            sgn.setChildren(gn.getChildren());
            sgn.setSourceCode(gn.getSourceCode());
            sgn.setAssignedResource(devices.get(plan[i]));
            executionGraph.add(i, sgn);
        }

        /* Construct Layer objects and annotate the graph. */
        executionGraph.constructLayers();

        return executionGraph;
    }

    /**
     * Calculate the total execution time for the given ExecutionGraph.
     */
    private double calculateExecutionTime(ExecutionGraph executionGraph) {
        double totalDuration = 0.0d;

        for (Layer layer : executionGraph.getLayers()) {
            // Group the tasks based on the device they have been assigned to.
            Map<HwResource, ArrayList<ScheduledGraphNode>> dev2nodes = layer.getColocations();

            // Initialize an auxiliary array to store the execution time for each device.
            double[] deviceExecTime = new double[dev2nodes.size()];
            // Loop through the tasks assigned on each device to calculate the
            // total execution time per device. Then, store this in the array.
            int i = 0;
            for (ArrayList<ScheduledGraphNode> deviceTasks : dev2nodes.values()) {
                deviceExecTime[i++] = sumDurations(deviceTasks);
            }

            // Once the execution time on each device is calculated for all
            // devices, current Layer's total execution time is defined as the
            // maximum of these values, since execution on different devices
            // takes place in parallel.
            double layerDuration = 0.0d;
            for (i = 0; i < deviceExecTime.length; ++i) {
                if (deviceExecTime[i] > layerDuration) {
                    layerDuration = deviceExecTime[i];
                }
            }

            // Store it in Layer too. FIXME(ckatsak): not really needed for now
            layer.setDuration(layerDuration);
            // Sum it to the ExecutionGraph's total execution time.
            totalDuration += layerDuration;
        }

        return totalDuration;
    }

    /**
     * Given an List of ScheduledGraphNode, it returns the sum of the estimated
     * execution durations, using the Model's predictions for each task.
     */
    private static double sumDurations(List<ScheduledGraphNode> tasks) {
        double sum = 0.0d;

        for (ScheduledGraphNode task : tasks) {
            // XXX(ckatsak): Two versions: one using the FeatureExtractor and another
            // one passing the source code to the Model, as per @kbitsak 's preference.
            sum += mlModel.predict("execTime", task.getAssignedResource(), task.getSourceCode());
            //sum += mlModel.predict("execTime", task.getAssignedResource(),
            //        FeatureExtractor.extract(task.getSourceCode()));
        }

        return sum;
    }

    /**
     * Calculate the total power consumption for the given ExecutionGraph.
     */
    private double calculatePowerConsumption(ExecutionGraph executionGraph) {
        double totalConsumption = 0.0d;

        for (ScheduledGraphNode task : executionGraph.getExecutionGraph()) {
            // XXX(ckatsak): Two versions: one using the FeatureExtractor and another
            // one passing the source code to the Model, as per @kbitsak 's preference.
            totalConsumption += mlModel.predict("powerCons", task.getAssignedResource(),
                    task.getSourceCode());
            //totalConsumption += mlModel.predict("powerCons", task.getAssignedResource(),
            //        FeatureExtractor.extract(task.getSourceCode()));
        }

        return totalConsumption;
    }

}
