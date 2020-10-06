package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.ClusterNode;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.YarnCluster;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Parameters;

import org.apache.flink.runtime.jobgraph.JobGraph;

import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;


/**
 * Implementation of an E2Data HAIER's optimizer that is based on the use of the NSGA0II genetic algorithm and
 * Machine Learning models for the evaluation of each of its generations.
 */
public final class NSGAIIHaierOptimizer implements Optimizer {

    private static final Logger logger = Logger.getLogger(NSGAIIHaierOptimizer.class.getCanonicalName());

    public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");

    /**
     * Configuration parameters for the NSGA-II genetic algorithm. This attribute should *always* be accessed through
     * its getter and setter methods to avoid race conditions.
     */
    private NSGAIIParameters parameters;

    /**
     * A list of the available {@link HwResource}s in the cluster, to assign the
     * tasks ({@link org.apache.flink.runtime.jobgraph.JobVertex} objects) on.
     */
    private final List<HwResource> devices;


    // --------------------------------------------------------------------------------------------


    /**
     * Getter method for the list of the available {@link HwResource}s in the cluster.
     *
     * @return the list of available {@link HwResource}s in the cluster.
     */
    public synchronized List<HwResource> getDevices() {
        return this.devices;
    }

    /**
     * Implementation of an E2Data HAIER's optimizer that is based on the use of the NSGA-II genetic algorithm and
     * Machine Learning models for the evaluation of each of its generations.
     */
    public NSGAIIHaierOptimizer() {
        this.devices = new ArrayList<>();
        this.parameters = new NSGAIIParameters(
                Integer.parseInt(resourceBundle.getString("optimizer.maxParetoPlans")),
                Integer.parseInt(resourceBundle.getString("optimizer.maxGenerations"))
        );
    }

    /**
     * Retrieve the values of NSGAIIHaierOptimizer's current configuration parameters in a synchronized way with
     * respect to concurrent setters.
     *
     * @return the values of NSGAIIHaierOptimizer's current configuration parameters; an object that can be safely
     *         casted to {@link NSGAIIParameters}.
     */
    @Override
    public synchronized Parameters retrieveConfiguration() {
        logger.fine("Retrieving this.parameters.maxParetoPlans (= " + this.parameters.getMaxParetoPlans() + ") and " +
                "this.parameters.numGenerations (= " + this.parameters.getNumGenerations() + ").");
        return this.parameters;
    }

    /**
     * Set new values for the NSGA-II parameters in a synchronized way with respect to concurrent getters.
     *
     * @param parameters contains the new values of the NSGA-II parameters to be set; must be an instance of the
     *                   {@link NSGAIIParameters} class.
     */
    @Override
    public synchronized void configure(final Parameters parameters) {
        this.parameters = (NSGAIIParameters) parameters;
        logger.fine("Just set this.parameters.maxParetoPlans to " + this.parameters.getMaxParetoPlans() +
                " and this.parameters.numGenerations to " + this.parameters.getNumGenerations() + ".");
    }


    // --------------------------------------------------------------------------------------------


    // NOTE(ckatsak): This method has to be synchronized because multiple handler threads may run it concurrently,
    //                thus messing with one another's available cluster hardware resources.
    //                Therefore, the process of each scheduling request that HAIER handles is effectively serialized.
    public synchronized List<HaierExecutionGraph> optimize(
            final JobGraph flinkJobGraph,
            final OptimizationPolicy policy,
            final Model mlModel) {
        // On each optimization request, obtain a fresh view of the available cluster resources.
        this.retrieveAvailableHardwareResources();  // FIXME(ckatsak): Currently intolerant to Yarn failures.
        logger.info("Cluster hardware resources available for this allocation: " + this.devices);

        // Lock on specific problem parameters.
        final NSGAIIParameters problemParams = (NSGAIIParameters) this.retrieveConfiguration(); // synchronized access

        // Construct the problem.
        final NSGAIIHaierPlanning problem = new NSGAIIHaierPlanning(devices, mlModel, flinkJobGraph, policy);

        // Run NSGA-II.
        final NondominatedPopulation result = new Executor()
                .withProblem(problem)
                .withAlgorithm("NSGAII")
                .withProperty("populationSize", problemParams.getMaxParetoPlans())
                .withMaxEvaluations(problemParams.getNumGenerations())
                .run();

        // Return the list of the available HaierExecutionGraphs (i.e., execution plans).
        String str = "Enumerating all (" + result.size() + ") final solution(s):\n";
        final List<HaierExecutionGraph> paretoHaierExecutionGraphs = new ArrayList<>(problemParams.getMaxParetoPlans());
        for (Solution solution : result) {
            paretoHaierExecutionGraphs.add(problem.solutionGraphs.get(solution));
            // TODO(ckatsak): Logging for exactly *2* objectives, hardcoded for now:
            str += "- Solution (" + solution.getObjective(0) + ", " + solution.getObjective(1) + ")\n";
        }
        str += problem.abortedPlans + " out of " + problem.totalGeneratedPlans +
                " generated execution plans were aborted due to misalignment with co-location constraints.";
        logger.finest(str);
        logger.finer("Pareto frontier (" + paretoHaierExecutionGraphs.size() + " solutions): " +
                paretoHaierExecutionGraphs);
        return paretoHaierExecutionGraphs;
    }

    /**
     * Obtain a fresh view of the available cluster resources from Yarn.
     *
     * FIXME(gmytil): what is the expected behavior if I cannot get fresh status from the cluster?
     */
    private void retrieveAvailableHardwareResources() {
        final YarnCluster cluster = YarnCluster.getInstance();
        if (null == cluster) {
            logger.severe("Could not retrieve a fresh view of the cluster from Yarn!");
            // FIXME(ckatsak): Throw an exception here, to be caught by the HTTP handler.
            return;
        }
        logger.info("Successfully retrieved a fresh view of the cluster from Yarn.");

        devices.clear();
        for (ClusterNode node : cluster.getNodes()) {
            logger.fine("Yarn detected cluster node: " + node);
            final ArrayList<HwResource>  availableResources = node.getAvailableResources();
            for (HwResource r : availableResources) {
                logger.finer("\tYarn detected available hardware resource: " + r);
                // If multiple GPUs or FPGAs of the same model are present, they should be represented as distinct
                // hardware resource objects; so we allocate distinct HwResource instances for each of them.
                if (r.getName().startsWith("yarn.io/gpu") || r.getName().startsWith("yarn.io/fpga")) {
                    // FIXME(ckatsak): This distinction of concrete resources of the same type performed by HAIER
                    //                 should not be necessary when YARN's integration with OpenCL has been completed.
                    for (int i = 0; i < r.getValue(); i++) {
                        logger.finer("\t\tCreating distinct hardware resource no" + (i + 1));
                        devices.add(new HwResource(r, i));
                    }
                } else {
                    devices.add(r);
                }
            }
        }
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
    }

}
