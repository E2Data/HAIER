package gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy;

import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.List;

/*

  ///////////// Sample policy json //////////////////////


  {"policy": {"objectives": [
                                {"name":"execTime", "targetFunction":"MIN", "combineFunction":"MAX"},
                                {"name":"powerCons", "targetFunction":"MIN", "combineFunction":"SUM"}
                            ]
             }
  }


  ///////////////////////////////////////////////////////

 */

/**
 * Class that describes the optimization policy for a specific job
 */
public class OptimizationPolicy {

    private Objective[] objectives;
    //FIXME: For the moment, the JSON parsing of the OptimizationPolicy does not support the existence of the mlModel
    private String mlModel; // the user can select what ML model will be used for the cost estimation of her job.
                            // If no model is specified, the default one is used.

    //TODO: (gmytil) place more info related to the policy. For example, indicator on how to choose among pareto


    public Objective[] getObjectives() {
        return objectives;
    }

    public void setObjectives(Objective[] objectives) {
        this.objectives = objectives;
    }

    public int getNumberOfObjectives() {
        return objectives.length;
    }

    public String getMlModel() {
        return mlModel;
    }

    /**
     * Provided a set of Pareto-optimal solutions for the objectives in hand by
     * an Optimizer, this method picks which FlinkExecutionGraph to finally
     * respond with.
     */
    public FlinkExecutionGraph pickFlinkExecutionGraph(List<FlinkExecutionGraph> paretoFlinkExecutionGraphs) {
        // FIXME(ckatsak): THIS IS A STUB
        return paretoFlinkExecutionGraphs.get(0);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Deserializes an optimization policy JSON
     * @param policyJSON
     * @return an {@link OptimizationPolicy} object
     */
    public static OptimizationPolicy parseJSON(String policyJSON){
        JsonElement jRootElement = new JsonParser().parse(policyJSON);
        JsonElement policyObject = jRootElement.getAsJsonObject().get("policy");
        JsonElement objectivesJson = policyObject.getAsJsonObject().get("objectives");

        Gson gson = new Gson();
        Objective[] objList = gson.fromJson(objectivesJson, Objective[].class);

        OptimizationPolicy optPolicy = new OptimizationPolicy();
        optPolicy.setObjectives(objList);

        return optPolicy;
    }



    //FIXME: (gmytil) main is only for quick 'n dirty testing
    public static void main(String[] args){
        String policy = "{\"policy\": {\"objectives\": [\n" +
                "                                {\"name\":\"execTime\", \"targetFunction\":\"MIN\", \"combineFunction\":\"MAX\"},\n" +
                "                                {\"name\":\"powerCons\", \"targetFunction\":\"MIN\", \"combineFunction\":\"SUM\"}\n" +
                "                             ]\n" +
                "              }\n" +
                "  }";

        OptimizationPolicy optPolicy = parseJSON(policy);

        for(Objective obj : optPolicy.getObjectives()){
            System.out.println(obj.getName());
        }
    }

}
