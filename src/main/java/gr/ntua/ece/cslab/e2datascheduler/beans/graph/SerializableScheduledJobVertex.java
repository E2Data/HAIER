package gr.ntua.ece.cslab.e2datascheduler.beans.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

import org.apache.flink.runtime.jobgraph.JobVertexID;

//import com.google.gson.GsonBuilder;

public class SerializableScheduledJobVertex {

    private JobVertexID id;
    private HwResource assignedResource;
    private JobVertexID[] children;

    public JobVertexID getId() {
        return this.id;
    }

    public void setId(JobVertexID id) {
        this.id = id;
    }

    public HwResource getAssignedResource() {
        return this.assignedResource;
    }

    public void setAssignedResource(HwResource assignedResource) {
        this.assignedResource = assignedResource;
    }

    public JobVertexID[] getChildren() {
        return this.children;
    }

    public void setChildren(JobVertexID[] children) {
        this.children = children;
    }

//    @Override
//    public String toString() {
//        return new GsonBuilder().create().toJson(this);
//    }

}
