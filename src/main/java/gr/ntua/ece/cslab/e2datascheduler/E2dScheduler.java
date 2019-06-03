package gr.ntua.ece.cslab.e2datascheduler;

import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.JobGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.ModelLibrary;
import gr.ntua.ece.cslab.e2datascheduler.ml.impl.DummyModel;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.NSGAIIOptimizer;

import java.util.NoSuchElementException;
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
                    Model dummyModel = new DummyModel();
                    dummyModel.load(modelPath);
                    models.put(modelName, dummyModel);
                    break;
                }
                //TODO: Add 'case' clauses for supported models
                default: break;
            }
        }
        optimizer = new NSGAIIOptimizer();
    }

    public static E2dScheduler getInstance(){
        if(scheduler == null){
            scheduler = new E2dScheduler();
        }
        return scheduler;
    }

    public ExecutionGraph schedule(JobGraph indexedGraph, OptimizationPolicy policy) {
        Model selectedModel = null;
        if(policy.getMlModel() == null || !models.containsKey(policy.getMlModel())){
            selectedModel = models.get(DEFAULT_MODEL);
        }else{
            selectedModel = models.get(policy.getMlModel());
        }

        ExecutionGraph result = optimizer.optimize(indexedGraph, policy, selectedModel);
        return result;
    }
}
