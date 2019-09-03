package gr.ntua.ece.cslab.e2datascheduler.beans.graph;

import com.google.gson.GsonBuilder;

public class JSONableFlinkExecutionGraph {

    private JSONableScheduledJobVertex[] scheduledJobVertices;

    public JSONableScheduledJobVertex[] getScheduledJobVertices() {
        return this.scheduledJobVertices;
    }

    public void setJSONableScheduledJobVertices(JSONableScheduledJobVertex[] scheduledJobVertices) {
        this.scheduledJobVertices = scheduledJobVertices;
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
