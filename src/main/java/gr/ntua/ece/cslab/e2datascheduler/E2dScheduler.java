package gr.ntua.ece.cslab.e2datascheduler;

import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.ModelLibrary;
import gr.ntua.ece.cslab.e2datascheduler.ml.impl.DemoModel;
import gr.ntua.ece.cslab.e2datascheduler.ml.impl.DummyCSLabModel;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Parameters;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.NSGAIIHaierOptimizer;

import org.apache.flink.runtime.jobgraph.JobGraph;

import java.util.ResourceBundle;

/**
 * Singleton of E2Data scheduler
 */
public class E2dScheduler {

    public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String DEFAULT_MODEL = resourceBundle.getString("default.model");


    private static E2dScheduler scheduler;

    private final Optimizer optimizer;
    private final ModelLibrary models;

    private E2dScheduler() {
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

        this.optimizer = new NSGAIIHaierOptimizer();
    }

    public static E2dScheduler getInstance() {
        if (scheduler == null) {
            scheduler = new E2dScheduler();
        }
        return scheduler;
    }

    public HaierExecutionGraph schedule(final JobGraph jobGraph, final OptimizationPolicy policy) {
        final Model selectedModel;
        if (policy.getMlModel() == null || !this.models.containsKey(policy.getMlModel())) {
            selectedModel = this.models.get(DEFAULT_MODEL);
            //selectedModel = new DemoModel();
        } else {
            selectedModel = this.models.get(policy.getMlModel());
        }

        return this.optimizer.optimize(jobGraph, policy, selectedModel);
        //return new NSGAIIHaierOptimizer().optimize(jobGraph, policy, selectedModel);
    }

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
}
