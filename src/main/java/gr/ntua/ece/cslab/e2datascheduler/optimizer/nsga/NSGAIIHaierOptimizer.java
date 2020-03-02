package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.ClusterNode;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.YarnCluster;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer;

import org.apache.flink.runtime.jobgraph.JobGraph;

import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

import java.util.logging.Logger;
import java.util.*;


/**
 * Implementation of an E2Data HAIER's optimizer that is based on the use of the NSGAII genetic algorithm and
 * Machine Learning models for the evaluation of each of its generations.
 */
public class NSGAIIHaierOptimizer implements Optimizer {

    private static final Logger logger = Logger.getLogger(NSGAIIHaierOptimizer.class.getCanonicalName());

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");

    // TODO(ckatsak): These could possibly be defined by the user through the GUI?
    private int maxParetoPlans = Integer.parseInt(resourceBundle.getString("optimizer.maxParetoPlans"));
    private int numGenerations = Integer.parseInt(resourceBundle.getString("optimizer.maxGenerations"));

    /**
     * A list of the available {@link HwResource}s in the cluster, to assign the
     * tasks ({@link org.apache.flink.runtime.jobgraph.JobVertex} objects) on.
     */
    private List<HwResource> devices;

    // --------------------------------------------------------------------------------------------

    /**
     * Implementation of an E2Data HAIER's optimizer that is based on the use of the NSGAII genetic algorithm and
     * Machine Learning models for the evaluation of each of its generations.
     */
    public NSGAIIHaierOptimizer() {
        this.devices = new ArrayList<>();
    }

    public int getMaxParetoPlans() {
        return this.maxParetoPlans;
    }

    public void setMaxParetoPlans(final int maxParetoPlans) {
        this.maxParetoPlans = maxParetoPlans;
    }

    public int getNumGenerations() {
        return this.numGenerations;
    }

    public void setNumGenerations(final int numGenerations) {
        this.numGenerations = numGenerations;
    }

    public List<HwResource> getDevices() {
        return this.devices;
    }

    public void setDevices(final List<HwResource> devices) {
        this.devices = devices;
    }

    // --------------------------------------------------------------------------------------------

    public HaierExecutionGraph optimize(
            final JobGraph flinkJobGraph,
            final OptimizationPolicy policy,
            final Model mlModel) {
        // On each optimization request, obtain a fresh view of the available cluster resources.
        final YarnCluster cluster = YarnCluster.getInstance();
        if (cluster != null) {
            devices.clear();

            for (ClusterNode node : cluster.getNodes()) {
                logger.info("Yarn reported node: " + node);
                final ArrayList<HwResource>  availableResources = node.getAvailableResources();
                for (HwResource r : availableResources) {
                    logger.info("\tYarn reported resource: " + r);
                    devices.add(r);
                }
            }
        } //FIXME(gmytil): what is the expected behavior if I cannot get fresh status from the cluster?
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
        final NSGAIIHaierPlanning problem = new NSGAIIHaierPlanning(devices, mlModel, flinkJobGraph, policy);

        // Run NSGA-II.
        final NondominatedPopulation result = new Executor()
                .withProblem(problem)
                .withAlgorithm("NSGAII")
                .withProperty("populationSize", maxParetoPlans)
                .withMaxEvaluations(numGenerations)
                .run();

        // Delegate the final selection of the HaierExecutionGraph to return,
        // to the provided OptimizationPolicy object.
        final List<HaierExecutionGraph> paretoHaierExecutionGraphs = new ArrayList<>(maxParetoPlans);
        for (Solution solution : result) {
            paretoHaierExecutionGraphs.add(problem.solutionGraphs.get(solution));
        }
        return policy.pickHaierExecutionGraph(paretoHaierExecutionGraphs);
    }

}
