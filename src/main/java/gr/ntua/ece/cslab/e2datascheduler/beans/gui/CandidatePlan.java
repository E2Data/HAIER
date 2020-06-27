package gr.ntua.ece.cslab.e2datascheduler.beans.gui;

import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class CandidatePlan {

    @JsonProperty("plan_id")
    private int planID;

    @JsonProperty("exec_time")
    private double execTime;

    @JsonProperty("power_cons")
    private double powerCons;

    private CandidatePlan(){}

    public CandidatePlan(final int planID, final HaierExecutionGraph haierExecutionGraph) {
        this.planID = planID;
        final Map<String, Double> objectiveCosts = haierExecutionGraph.getObjectiveCosts();
        this.execTime = objectiveCosts.get("execTime");
        this.powerCons = objectiveCosts.get("powerCons");
    }

    public int getPlanID() {
        return this.planID;
    }

    public void setPlanID(int planID) {
        this.planID = planID;
    }

    public double getExecTime() {
        return this.execTime;
    }

    public void setExecTime(final double execTime) {
        this.execTime = execTime;
    }

    public double getPowerCons() {
        return this.powerCons;
    }

    public void setPowerCons(final double powerCons) {
        this.powerCons = powerCons;
    }
}
