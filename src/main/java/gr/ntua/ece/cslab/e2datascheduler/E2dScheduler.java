package gr.ntua.ece.cslab.e2datascheduler;

import gr.ntua.ece.cslab.e2datascheduler.beans.gui.CandidatePlan;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.ModelLibrary;
import gr.ntua.ece.cslab.e2datascheduler.ml.impl.DemoModel;
import gr.ntua.ece.cslab.e2datascheduler.ml.impl.DummyCSLabModel;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Parameters;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.NSGAIIHaierOptimizer;
import gr.ntua.ece.cslab.e2datascheduler.util.HaierLogHandler;
import gr.ntua.ece.cslab.e2datascheduler.util.SelectionQueue;

import org.apache.flink.runtime.jobgraph.JobGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton of E2Data scheduler.
 */
public class E2dScheduler {

    private static final Logger logger = Logger.getLogger(E2dScheduler.class.getCanonicalName());

    public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String DEFAULT_MODEL = resourceBundle.getString("default.model");

    /**
     * The existence of a GUI component is configured at compile-time, thus deciding whether to account for a GUI
     * user's decision for picking the final execution plan or not.
     */
    private static final boolean GUI_ENABLED = Boolean.parseBoolean(resourceBundle.getString("gui.enabled"));


    private static E2dScheduler scheduler;

    private final Optimizer optimizer;
    private final ModelLibrary models;

    /**
     * planQueues maps each {@link org.apache.flink.api.common.JobID} (in its hex format) to the
     * {@link SelectionQueue} that contains its execution plans that form the Pareto frontier.
     */
    private final ConcurrentMap<String, SelectionQueue<CandidatePlan>> planQueues;


    // --------------------------------------------------------------------------------------------


    private E2dScheduler() {
        Logger.getLogger("").setLevel(Level.FINEST);
        Handler consoleHandler = new ConsoleHandler();
        Handler haierHandler = HaierLogHandler.getHandler();
        consoleHandler.setLevel(Level.FINEST);
        haierHandler.setLevel(Level.FINEST);
        Logger.getLogger("").addHandler(consoleHandler);
        Logger.getLogger("").addHandler(haierHandler);

        logger.info("JVM Runtime Information:\n" + JVMRuntimeInfo());

        this.models = new ModelLibrary();

        final String[] modelsWithPaths = resourceBundle.getString("supported.models").split(",");
        for (String m : modelsWithPaths) {
            final String[] mp = m.split("=");
            final String modelName = mp[0];
            final String modelPath = mp[1];
            switch (modelName) {
                case "linearRegression":
                    //FIXME: (gmytil) For now, I instantiate a dummy model. In the regular case, I will load one
                    // from disk with kbitsak's code
                    final Model dummyCsLabModel = new DummyCSLabModel();
                    dummyCsLabModel.load(modelPath);
                    this.models.put(modelName, dummyCsLabModel);
                    break;
                case "demo":
                    final Model demoModel = new DemoModel();
                    demoModel.load(modelPath);
                    this.models.put(modelName, demoModel);
                    break;
                //TODO: Add 'case' clauses for supported models
                default:
                    break;
            }
        }

        this.planQueues = new ConcurrentHashMap<>();
        this.optimizer = new NSGAIIHaierOptimizer();
    }

    public static E2dScheduler getInstance() {
        if (scheduler == null) {
            scheduler = new E2dScheduler();
        }
        return scheduler;
    }


    // --------------------------------------------------------------------------------------------


    /**
     * A scheduling result produced by HAIER. If it is not "empty" (i.e., none of the JobVertex objects could be
     * offloaded to one of the heterogeneous architectures supported by E2Data), it contains the optimized
     * {@link HaierExecutionGraph} that has been selected among the execution plans that were generated.
     */
    public static class SchedulingResult {
        private final boolean empty;
        private final HaierExecutionGraph result;

        /**
         * Create an "empty" scheduling result, i.e., one in which none of the
         * {@link org.apache.flink.runtime.jobgraph.JobVertex} objects for a given {@link JobGraph} could be
         * offloaded to an accelerator at all.
         */
        public SchedulingResult() {
            this.empty = true;
            this.result = null;
        }

        /**
         * Create a "non-empty" scheduling result that contains a {@link HaierExecutionGraph}. An "empty" scheduling
         * result signifies that no {@link org.apache.flink.runtime.jobgraph.JobVertex} objects for a given
         * {@link JobGraph} could be offloaded to an accelerator at all.
         *
         * @param haierExecutionGraph the scheduling result, or {@code null} to signify an "empty" result.
         */
        public SchedulingResult(final HaierExecutionGraph haierExecutionGraph) {
            this.empty = false;
            this.result = haierExecutionGraph;
        }

        /**
         * @return {@code true} if the scheduling result is "empty"; {@code false} otherwise
         */
        public boolean isEmpty() {
            return this.empty;
        }

        /**
         * @return the result {@link HaierExecutionGraph} or {@code null} if the result is "empty"
         *
         * FIXME(ckatsak): Is it possible to return null for a "non-empty" result (e.g., in the case of an
         *                 internal error)? Until this is thoroughly reviewed, always use isEmpty() before this.
         */
        public HaierExecutionGraph getResult() {
            return this.result;
        }
    }

    /**
     * TODO(ckatsak): Documentation
     *
     * @param jobGraph
     * @param policy
     * @return
     */
    public SchedulingResult schedule(final JobGraph jobGraph, final OptimizationPolicy policy) {
        // If the given JobGraph does not contain any JobVertex that can be offloaded to an accelerator at all,
        // return fast.
        if (!HaierExecutionGraph.containsAnyComputationalJobVertex(jobGraph)) {
            logger.info("Early exiting because none of the JobVertex objects can be offloaded to accelerators.");
            return new SchedulingResult();
        }

        // If GUI is enabled, register the JobID to the SelectionQueue,
        // to respond to GET /e2data/nsga2/{jobId}/plans appropriately.
        if (GUI_ENABLED) {
            final String jobId = jobGraph.getJobID().toString();
            logger.info("Creating new SelectionQueue for job " + jobId);
            this.planQueues.put(jobId, new SelectionQueue<>());
        }

        final Model selectedModel;
        if (policy.getMlModel() == null || !this.models.containsKey(policy.getMlModel())) {
            selectedModel = this.models.get(DEFAULT_MODEL);
            //selectedModel = new DemoModel();
        } else {
            selectedModel = this.models.get(policy.getMlModel());
        }

        //return new NSGAIIHaierOptimizer().optimize(jobGraph, policy, selectedModel);
        //return this.optimizer.optimize(jobGraph, policy, selectedModel);
        final List<HaierExecutionGraph> paretoHaierExecutionGraphs =
                this.optimizer.optimize(jobGraph, policy, selectedModel);
        return new SchedulingResult(this.pickPlan(jobGraph, policy, paretoHaierExecutionGraphs));
    }

    /**
     * Pick one of the available execution plans among those in the Pareto frontier calculated by the {@link Optimizer},
     * based either on user's choice (through the GUI) or on the applied {@link OptimizationPolicy}.
     *
     * @param jobGraph Flink's {@link JobGraph} in question
     * @param policy enabled {@link OptimizationPolicy}
     * @param paretoHaierExecutionGraphs the set of execution plans in the Pareto frontier, calculated by the
     *                                   {@link Optimizer}
     * @return the selected execution plan
     */
    private HaierExecutionGraph pickPlan(
            final JobGraph jobGraph,
            final OptimizationPolicy policy,
            final List<HaierExecutionGraph> paretoHaierExecutionGraphs) {
        if (GUI_ENABLED) {
            return this.retrieveFromGUI(jobGraph.getJobID().toString(), paretoHaierExecutionGraphs);
        } else {
            return policy.pickHaierExecutionGraph(paretoHaierExecutionGraphs);
        }
    }

    /**
     * Retrieve the final execution plan that has been selected in the GUI.
     *
     * @param jobId the {@link org.apache.flink.api.common.JobID} associated with Flink's {@link JobGraph} in question
     * @param paretoHaierExecutionGraphs the set of execution plans in the Pareto frontier, calculated by the
     *                                   {@link Optimizer}
     * @return the selected execution plan
     */
    private HaierExecutionGraph retrieveFromGUI(
            final String jobId,
            final List<HaierExecutionGraph> paretoHaierExecutionGraphs) {
        // Make sure that the SelectionQueue for this Job has already been allocated.
        final SelectionQueue<CandidatePlan> plansQueue = this.planQueues.get(jobId);
        if (null == plansQueue) {
            return null;
        }
        // NOTE(ckatsak): The following assertion held true when BlockingQueue<CandidatePlan> was
        // used instead of the SelectionQueue; see NSGAIIOptimizer.optimize() (the FIXME comment).
        //assert plansQueue.size() >= paretoHaierExecutionGraphs.size() :
        //        "Unhandled race condition detected: plansQueue.size() < paretoHaierExecutionGraphs.size()";

        // Prepare a serialized representation of all candidate execution plans of the Pareto frontier.
        final List<CandidatePlan> candidatePlans = new ArrayList<>(paretoHaierExecutionGraphs.size());
        for (int i = 0; i < paretoHaierExecutionGraphs.size(); i++) {
            candidatePlans.add(i, new CandidatePlan(i, paretoHaierExecutionGraphs.get(i)));
        }
        plansQueue.submitOptions(candidatePlans);

        CandidatePlan choice = null;
        while (null == choice) {
            logger.info("Attempting to retrieve user's choice through the GUI...");
            try {
                choice = plansQueue.retrieveChoice(3000);
            } catch (InterruptedException e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
            }
        }
        if (choice.getPlanID() >= paretoHaierExecutionGraphs.size()) {
            logger.severe("choice.planID = " + choice.getPlanID() + " >= ParetoFrontier.length = " +
                    paretoHaierExecutionGraphs.size());
            return null;
        }
        return paretoHaierExecutionGraphs.get(choice.getPlanID());
    }


    // --------------------------------------------------------------------------------------------


    /**
     * Retrieve the values of Optimizer's current configuration parameters.
     *
     * @return the values of Optimizer's current configuration parameters.
     */
    public Parameters retrieveConfiguration() {
        return this.optimizer.retrieveConfiguration(); // synchronized access
    }

    /**
     * Configure HAIER's Optimizer with the given Parameters.
     *
     * @param parameters is an optimizer-dependent object containing configuration parameters.
     */
    public void configureOptimizer(final Parameters parameters) {
        this.optimizer.configure(parameters); // synchronized access
    }

    /**
     * Get a reference to the {@link SelectionQueue} associated with the given
     * {@link org.apache.flink.api.common.JobID}.
     *
     * @param jobId the {@link org.apache.flink.api.common.JobID} associated with Flink's {@link JobGraph} in question
     * @return the {@link SelectionQueue} associated with Flink's {@link JobGraph} in question
     */
    public SelectionQueue<CandidatePlan> getSelectionQueue(final String jobId) {
        return this.planQueues.get(jobId);
    }


    //---------------------------------------------------------------------------------------------


    /**
     * Retrieve information about the current JVM runtime.
     *
     * @return a {@link String} containing information about the current JVM runtime
     */
    public static String JVMRuntimeInfo() {
        String ret = "";
        ret += System.getProperty("java.runtime.name") + "\n";
        ret += System.getProperty("java.vm.vendor") + " " + System.getProperty("java.runtime.version") + "\n";
        ret += "java.home = " + System.getProperty("java.home") + "\n";
        return ret;
    }

}
