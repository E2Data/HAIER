package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.ClusterNode;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.YarnCluster;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ToyJobGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.Objective;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer;

import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

import java.util.*;


/**
 * Implementation of the NSGAII optimizer
 */
public class NSGAIIOptimizer implements Optimizer {

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");

    private int maxParetoPlans = Integer.parseInt(resourceBundle.getString("optimizer.maxParetoPlans"));
    private int numGenerations = Integer.parseInt(resourceBundle.getString("optimizer.maxGenerations"));

    /**
     * A list of the available devices to assign the tasks on.
     */
    private List<HwResource> devices;

    /**
     * It stores all solutions found for the most recent call to method {@code
     * optimize}.
     * Declaring it local to method {@code optimize} would suffice, but it is
     * declared as a (package-scoped) class field to facilitate plotting during
     * testing.
     */
    Map<Solution, ExecutionGraph> solutionGraphs;

    public NSGAIIOptimizer(){
        devices = new ArrayList<>();
    }

    public int getMaxParetoPlans() {
        return maxParetoPlans;
    }

    public void setMaxParetoPlans(int maxParetoPlans) {
        this.maxParetoPlans = maxParetoPlans;
    }

    public int getNumGenerations() {
        return numGenerations;
    }

    public void setNumGenerations(int numGenerations) {
        this.numGenerations = numGenerations;
    }

    public List<HwResource> getDevices() {
        return devices;
    }

    public void setDevices(List<HwResource> devices) {
        this.devices = devices;
    }


    public ExecutionGraph optimize(ToyJobGraph graph, OptimizationPolicy policy, Model mlModel) {
        // Begin by initializing problem-independent parameters for NSGA-II.

        // Then, properly initialize problem-specific parameters for NSGA-II execution.

        // (gmytil) each time we get an optimization request, we have to obtain a fresh view
        // of the available cluster resources.
/*
		YarnCluster cluster = YarnCluster.getInstance();
		if(cluster != null) {
		    devices.clear();

		    for (ClusterNode node : cluster.getNodes()) {
			ArrayList<HwResource>  availableResources = node.getAvailableResources();
			for (HwResource r : availableResources) {
			    devices.add(r);
			}

		    }
		} //FIXME: what is the expected behavior if I cannot get fresh status from the cluster?
*/
        //  vv  FIXME(ckatsak)  vv
        // Yarn workaround -- hard-coding devices
        HwResource r = new HwResource();
        r.setHost("silver1");
        devices.add(r);
        r = new HwResource();
        r.setHost("gold1");
        devices.add(r);
        r = new HwResource();
        r.setHost("gold2");
        devices.add(r);
        //  ^^  FIXME(ckatsak)  ^^

        NSGAIIPlanning.devices = devices;
        NSGAIIPlanning.mlModel = mlModel;

        //Map<Solution, ExecutionGraph> solutionGraphs = new HashMap<>();
        this.solutionGraphs = new HashMap<>();
        NSGAIIPlanning.solutionGraphs = solutionGraphs;

        NSGAIIPlanning.objectives = new HashMap<String, Integer>();
        int objectiveId = 0;
        for(Objective obj : policy.getObjectives()){
            NSGAIIPlanning.objectives.put(obj.getName(), objectiveId);
            objectiveId++;
        }

        NSGAIIPlanning.tasks = graph;

        // Run NSGA-II.
        NondominatedPopulation result = new Executor()
                .withProblemClass(NSGAIIPlanning.class)
                .withAlgorithm("NSGAII")
                .withProperty("populationSize", maxParetoPlans)
                .withMaxEvaluations(numGenerations)
                .run();

        // Delegate the final selection of the ExecutionGraph to return, to the
        // provided OptimizationPolicy object.
        List<ExecutionGraph> paretoExecutionGraphs = new ArrayList<>(maxParetoPlans);
        for (Solution solution : result) {
            paretoExecutionGraphs.add(solutionGraphs.get(solution));
        }
        return policy.pickExecutionGraph(paretoExecutionGraphs);
    }

}
