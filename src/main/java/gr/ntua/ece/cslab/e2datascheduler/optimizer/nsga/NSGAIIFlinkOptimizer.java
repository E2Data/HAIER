package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.ClusterNode;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.YarnCluster;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.Objective;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer;

import org.apache.flink.runtime.jobgraph.JobGraph;

import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

import java.util.logging.Logger;
import java.util.*;


/**
 * Implementation of the NSGAII optimizer
 */
public class NSGAIIFlinkOptimizer implements Optimizer {

    private static final Logger logger = Logger.getLogger(NSGAIIFlinkOptimizer.class.getCanonicalName());

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");

    private int maxParetoPlans = Integer.parseInt(resourceBundle.getString("optimizer.maxParetoPlans"));
    private int numGenerations = Integer.parseInt(resourceBundle.getString("optimizer.maxGenerations"));

    /**
     * A list of the available devices to assign the tasks (JobVertices) on.
     */
    private List<HwResource> devices;

    // --------------------------------------------------------------------------------------------

    public NSGAIIFlinkOptimizer() {
        devices = new ArrayList<>();
    }

    public int getMaxParetoPlans() {
        return this.maxParetoPlans;
    }

    public void setMaxParetoPlans(int maxParetoPlans) {
        this.maxParetoPlans = maxParetoPlans;
    }

    public int getNumGenerations() {
        return this.numGenerations;
    }

    public void setNumGenerations(int numGenerations) {
        this.numGenerations = numGenerations;
    }

    public List<HwResource> getDevices() {
        return this.devices;
    }

    public void setDevices(List<HwResource> devices) {
        this.devices = devices;
    }

    // --------------------------------------------------------------------------------------------

    public FlinkExecutionGraph optimize(final JobGraph flinkJobGraph, final OptimizationPolicy policy, final Model mlModel) {
        // On each optimization request, we obtain a fresh view of the
        // available cluster resources.
        YarnCluster cluster = YarnCluster.getInstance();
        if (cluster != null) {
            devices.clear();

            for (ClusterNode node : cluster.getNodes()) {
                logger.info("Yarn reported node: " + node);
                ArrayList<HwResource>  availableResources = node.getAvailableResources();
                for (HwResource r : availableResources) {
                    logger.info("\tYarn reported resource: " + r);
                    devices.add(r);
                }
            }
        } //FIXME: what is the expected behavior if I cannot get fresh status from the cluster?
/*
        //  vv  FIXME(ckatsak)  vv
        // Yarn workaround -- hard-coding devices
        HwResource r = new HwResource();
        r.setHost("silver1");
        r.setName("cpu");
        devices.add(r);
        r = new HwResource();
        r.setHost("gold1");
        r.setName("gtx");
        devices.add(r);
        r = new HwResource();
        r.setHost("gold2");
        r.setName("tesla");
        devices.add(r);
        //  ^^  FIXME(ckatsak)  ^^
*/


        // Construct the problem.
        final NSGAIIFlinkPlanning problem = new NSGAIIFlinkPlanning(devices, mlModel, flinkJobGraph, policy);

        // Run NSGA-II.
        final NondominatedPopulation result = new Executor()
                .withProblem(problem)
                .withAlgorithm("NSGAII")
                .withProperty("populationSize", maxParetoPlans)
                .withMaxEvaluations(numGenerations)
                .run();

        // Delegate the final selection of the FlinkExecutionGraph to return,
        // to the provided OptimizationPolicy object.
        final List<FlinkExecutionGraph> paretoFlinkExecutionGraphs = new ArrayList<>(maxParetoPlans);
        for (Solution solution : result) {
            paretoFlinkExecutionGraphs.add(problem.solutionGraphs.get(solution));
        }
        return policy.pickFlinkExecutionGraph(paretoFlinkExecutionGraphs);
    }

}
