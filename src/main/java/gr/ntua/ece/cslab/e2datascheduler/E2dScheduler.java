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

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String DEFAULT_MODEL = resourceBundle.getString("default.model");


    private static E2dScheduler scheduler;

    private Optimizer optimizer;
    private ModelLibrary models;

    private E2dScheduler(){
        models = new ModelLibrary();

        String[] modelsWithPaths = resourceBundle.getString("supported.models").split(",");
        for(String m : modelsWithPaths){
            String[] mp = m.split("=");
            String modelName = mp[0];
            String modelPath = mp[1];
            switch (modelName){
                case "linearRegression": {
                    //FIXME: (gmytil) For now, I instantiate a dummy model. In the regular case, I will load one
                    // from disk with kbitsak's code
                    Model dummyCsLabModel = new DummyCSLabModel();
                    dummyCsLabModel.load(modelPath);
                    models.put(modelName, dummyCsLabModel);
                    break;
                }
                case "demo": {
                    Model demoModel = new DemoModel();
                    demoModel.load(modelPath);
                    models.put(modelName, demoModel);
                    break;
                }
                //TODO: Add 'case' clauses for supported models
                default: break;
            }
        }

        optimizer = new NSGAIIHaierOptimizer();
    }

    public static E2dScheduler getInstance(){
        if(scheduler == null){
            scheduler = new E2dScheduler();
        }
        return scheduler;
    }

    public HaierExecutionGraph schedule(JobGraph jobGraph, OptimizationPolicy policy) {
        Model selectedModel;
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
        return this.optimizer.retrieveConfiguration();
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
