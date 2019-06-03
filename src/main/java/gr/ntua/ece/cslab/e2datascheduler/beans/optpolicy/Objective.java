package gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy;

import com.google.gson.Gson;

/**
 * Description of a policy objective. An objective is part of the user's policy
 * and is expected to be provided as input in JSON format.
 */
public class Objective {

    public enum OptimizationFunc {MIN, MAX} //FIXME: (gmytil) take care of jsons with invalid functions
    public enum CombineFunc {SUM, MAX}

    private String name;
    private OptimizationFunc targetFunction;
    private CombineFunc combineFunction;

    public String getName() {
        return name;
    }

    public OptimizationFunc getTargetFunction() {
        return targetFunction;
    }


    public CombineFunc getCombineFunction() {
        return combineFunction;
    }


    //FIXME: (gmytil) main is only for quick 'n dirty testing
    public static void main(String[] args){
        String validObj = "{\"name\":\"execTime\", \"targetFunction\":\"MIN\", \"combineFunction\":\"MAX\"}";

        Gson gson = new Gson();
        Objective obj1 = gson.fromJson(validObj, Objective.class);
        System.out.println("Objective name = " + obj1.getName());
        if(obj1.getTargetFunction().equals(OptimizationFunc.MIN)){
            System.out.println("All good");
        }
    }


}
