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
 * Implementation of an E2Data HAIER's optimizer that is based on the use of the NSGAII genetic algorithm and
 * Machine Learning models for the evaluation of each of its generations.
 */
public class NSGAIIHaierOptimizer implements Optimizer {

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
    private List<HwResource> devices;

    // --------------------------------------------------------------------------------------------

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
        logger.info("Retrieving this.parameters.maxParetoPlans (= " + this.parameters.getMaxParetoPlans() + ") and " +
                "this.parameters.numGenerations (= " + this.parameters.getNumGenerations() + ").");
        return this.parameters;
    }

    /**
     * Set new values for the NSGA-II parameters in a synchronized way with respect to concurrent getters.
     *
     * @param parameters contains the new values of the NSGA-II parameters to be set; must be an object of the
     *                   {@link NSGAIIParameters} class.
     */
    @Override
    public synchronized void configure(final Parameters parameters) {
        this.parameters = (NSGAIIParameters) parameters;
        logger.info("Just set this.parameters.maxParetoPlans to " + this.parameters.getMaxParetoPlans() +
                " and this.parameters.numGenerations to " + this.parameters.getNumGenerations() + ".");
    }

    public List<HwResource> getDevices() {
        return this.devices;
    }

    public void setDevices(final List<HwResource> devices) {
        this.devices = devices;
    }

    // --------------------------------------------------------------------------------------------


    // FIXME(ckatsak): When GUI is in, the NSGAIIParameters are read by E2dScheduler to allocate a BlockingQueue.
    //                 Therefore, a (currently unhandled) race condition would occur if the NSGAIIParameters were
    //                 changed between the allocation of the BlockingQueue in E2dScheduler.schedule() and the
    //                 configuration of the NSGA-II Executor in here.
    //                 Alternatively, we could use `synchronized` here; however this would effectively serialize
    //                 the process of all scheduling requests that HAIER handles.
    public List<HaierExecutionGraph> optimize(
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

        // Lock on specific problem parameters.
        final NSGAIIParameters problemParams = (NSGAIIParameters) this.retrieveConfiguration(); // synchronized access

        // Run NSGA-II.
        final NondominatedPopulation result = new Executor()
                .withProblem(problem)
                .withAlgorithm("NSGAII")
                .withProperty("populationSize", problemParams.getMaxParetoPlans())
                .withMaxEvaluations(problemParams.getNumGenerations())
                .run();

        // Delegate the final selection of the HaierExecutionGraph to return,
        // to the provided OptimizationPolicy object.
        final List<HaierExecutionGraph> paretoHaierExecutionGraphs = new ArrayList<>(problemParams.getMaxParetoPlans());
        for (Solution solution : result) {
            paretoHaierExecutionGraphs.add(problem.solutionGraphs.get(solution));
        }
        logger.fine("Pareto frontier (" + paretoHaierExecutionGraphs.size() + " solutions): " +
                paretoHaierExecutionGraphs);
        return paretoHaierExecutionGraphs;
    }

}
