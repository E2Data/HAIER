package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.Objective;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation.ExhaustiveEvaluation;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation.LayeredEvaluation;

import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import java.util.logging.Logger;

/**
 * Class that implements the main functionalities of the NSGAII algorithm, i.e.,
 * the 'newSolution' and 'evaluate' methods
 */
public class NSGAIIFlinkPlanning extends AbstractProblem {

    private static final Logger logger = Logger.getLogger(NSGAIIFlinkPlanning.class.getCanonicalName());

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static String timeEvalAlgorithm = resourceBundle.getString("optimizer.evalAlgorithm").toLowerCase();

    /**
     * The Flink JobGraph that represents the tasks, and must be scheduled on
     * the available devices in the cluster.
     */
    private final JobGraph jobGraph;

    /**
     * A list (with stable order) of the JobVertex objects included in the
     * Flink JobGraph of this problem.
     */
    private final JobVertex[] jobVertices;

    /**
     * A mapping between objective names and their corresponding integer
     * identifier.
     *
     * E.g.: { "execTime" : 0,  "powerCons" : 1,  ... } .
     *
     * This mapping is required because MOEA handles objectives as integers.
     */
    final Map<String, Integer> objectives;

    /**
     * A list of the devices in the cluster that are available for this
     * problem.
     */
    private final List<HwResource> devices;

    /**
     * The Model that is being consulted for the cost of a task on a particular
     * device.
     */
    private final Model mlModel;

    /**
     * It stores all found solutions.
     */
    final Map<Solution, FlinkExecutionGraph> solutionGraphs;

    /**
     * timeEvaluator is a GOF Strategy handle for the implementation of
     * the execution time evaluation algorithms on a Flink JobGraph.
     */
    final TimeEvaluationAlgorithm timeEvaluator;

    // -------------------------------------------------------------------------------------------

    public NSGAIIFlinkPlanning(List<HwResource> devices, Model mlModel, JobGraph jobGraph, OptimizationPolicy policy){
        super(jobGraph.getNumberOfVertices(), policy.getNumberOfObjectives());
        this.devices = new ArrayList<HwResource>();
        for (HwResource r : devices) {
            this.devices.add(r);
        }
        this.mlModel = mlModel;
        this.jobGraph = jobGraph;
        this.jobVertices = initializeJobVertices(jobGraph);
        this.solutionGraphs = new HashMap<>();

        this.objectives = new HashMap<>(policy.getNumberOfObjectives(), 1.0f);
        int objectiveIndex = 0;
        for (Objective obj : policy.getObjectives()) {
            this.objectives.put(obj.getName(), objectiveIndex);
            objectiveIndex++;
        }

        switch (timeEvalAlgorithm) {
            case "layered":
                this.timeEvaluator = new LayeredEvaluation(this.mlModel);
                break;
            case "exhaustive":
            default:
                this.timeEvaluator = new ExhaustiveEvaluation();
                break;
        }
    }

    /**
     * Auxiliary method to initialize the array of JobVertex objects.
     *
     * First, it attempts to retrieve them topologically sorted; if that does
     * not work well, it falls back to random order.
     */
    private JobVertex[] initializeJobVertices(final JobGraph jobGraph) {
        try {
            return jobGraph.getVerticesSortedTopologicallyFromSources().toArray(new JobVertex[jobGraph.getNumberOfVertices()]);
        } catch (Exception e) {
            logger.severe("OOPS!\n" + e.getMessage() + "\n");
            e.printStackTrace();
            return jobGraph.getVerticesAsArray();
        }
    }

    // -------------------------------------------------------------------------------------------

    /**
     * This method is automatically invoked by the MOEA framework at each
     * generation/iteration of the genetic algorithm.
     * @return a candidate solution for the optimization problem
     */
    @Override
    public Solution newSolution() {
        final Solution solution = new Solution(this.jobVertices.length, this.objectives.size());

        for (int jobVertexIndex = 0; jobVertexIndex < this.jobVertices.length; jobVertexIndex++) {
            solution.setVariable(jobVertexIndex, EncodingUtils.newInt(0, this.devices.size() - 1));
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
        final FlinkExecutionGraph flinkExecutionGraph = constructFlinkExecutionGraph(EncodingUtils.getInt(solution));
        final Map<String, Double> objectiveCosts = flinkExecutionGraph.getObjectiveCosts();
        this.solutionGraphs.put(solution, flinkExecutionGraph);

        // TODO(ckatsak): For now, objectives are identified via String objects.
        //                This should probably change. Maybe Enums + Visitor ?
        for (String objective : this.objectives.keySet()) {
            double costEstimation = Double.NEGATIVE_INFINITY;
            switch (objective) {
                case "execTime":
                    costEstimation = this.timeEvaluator.calculateExecutionTime(flinkExecutionGraph);
                    break;
                case "powerCons":
                    costEstimation = this.calculatePowerConsumption(flinkExecutionGraph);
                    break;
                default:
                    // FIXME(ckatsak): This should be unreachable; yet, it depends on the input
                    //  incoming from the network. For now, just log it and skip its evaluation.
                    logger.warning("Unknown objective: '" + objective + "'\n");
                    break;
            }
            objectiveCosts.put(objective, costEstimation);
        }

        for (Map.Entry<String, Integer> objective : this.objectives.entrySet()) {
            solution.setObjective(objective.getValue(), objectiveCosts.get(objective.getKey()));
        }
    }

    /**
     * Construct the FlinkExecutionGraph imposed by the the given solution's
     * plan.
     */
    //FIXME: (gmytil) What happens if a resource assigned by the plan is already in use?
    private FlinkExecutionGraph constructFlinkExecutionGraph(final int[] plan) {
        assert plan.length == this.jobVertices.length : "plan.length != jobVertices.length";

        /* Construct the FlinkExecutionGraph. */
        final FlinkExecutionGraph flinkExecutionGraph = new FlinkExecutionGraph(this.jobGraph, this.jobVertices);

        /* Annotate each ScheduledJobVertex with its assigned hardware resource
         * according to the current plan. */
        for (int jobVertexIndex = 0; jobVertexIndex < plan.length; jobVertexIndex++) {
            flinkExecutionGraph.assignResource(jobVertexIndex, this.devices.get(plan[jobVertexIndex]));
        }

        /* Construct Layer objects and annotate the graph. */
        //flinkExecutionGraph.timeEvaluatorInitialization();
        timeEvaluator.initialization(flinkExecutionGraph);

        return flinkExecutionGraph;
    }

    /**
     * Calculate the total power consumption for the given FlinkExecutionGraph.
     */
    private double calculatePowerConsumption(final FlinkExecutionGraph flinkExecutionGraph) {
        double totalConsumption = 0.0d;

        for (ScheduledJobVertex scheduledJobVertex : flinkExecutionGraph.getScheduledJobVertices()) {
            // XXX(ckatsak): Two versions: one using the FeatureExtractor and another
            // one passing the source code to the Model, as per @kbitsak 's preference.
            totalConsumption += this.mlModel.predict("powerCons",
                                                     scheduledJobVertex.getAssignedResource(),
                                                     scheduledJobVertex.getSourceCode());
            //totalConsumption += this.mlModel.predict("powerCons", scheduledJobVertex.getAssignedResource(),
            //        FeatureExtractor.extract(scheduledJobVertex.getSourceCode()));
        }

        return totalConsumption;
    }

}
