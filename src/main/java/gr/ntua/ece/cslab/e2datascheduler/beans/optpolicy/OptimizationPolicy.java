package gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy;

import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.List;


/**
 * Class that describes the optimization policy for a specific job.
 *
 * Sample JSON-formatted policy:
 *
 * {@code
 *    { "policy": { "objectives": [
 *                                      { "name":"execTime", "targetFunction":"MIN", "combineFunction":"MAX" },
 *                                      { "name":"powerCons", "targetFunction":"MIN", "combineFunction":"SUM" }
 *                                ]
 *                }
 *    }
 * }
 */
public class OptimizationPolicy {

    private Objective[] objectives;
    //FIXME(gmytil): For the moment, the JSON parsing of the OptimizationPolicy does not support the existence of
    //               the mlModel
    private String mlModel; // the user can select what ML model will be used for the cost estimation of her job.
                            // If no model is specified, the default one is used.

    //TODO(gmytil): place more info related to the policy. For example, indicator on how to choose among pareto


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
     * Given a set of Pareto-optimal solutions (normally produced by a HAIER
     * {@link gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer}) for the multi-objective optimization
     * problem at hand, this method picks which {@link HaierExecutionGraph} should be selected.
     *
     * @param paretoHaierExecutionGraphs A list of all {@link HaierExecutionGraph}s that represent Pareto-optimal
     *                                   solutions.
     * @return The {@link HaierExecutionGraph} that represents the selected Pareto-optimal solution.
     */
    public HaierExecutionGraph pickHaierExecutionGraph(final List<HaierExecutionGraph> paretoHaierExecutionGraphs) {
        // FIXME(ckatsak): THIS IS A STUB
        return paretoHaierExecutionGraphs.get(0);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Deserializes a JSON-formatted {@link OptimizationPolicy} object.
     *
     * @param policyJSON The input JSON-formatted policy as a {@link String}.
     * @return The {@link OptimizationPolicy} object.
     */
    public static OptimizationPolicy parseJSON(final String policyJSON){
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
