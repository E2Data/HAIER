package gr.ntua.ece.cslab.e2datascheduler.beans.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;


public class ScheduledGraphNode extends GraphNode {

    /**
     * The device that the corresponding GraphNode was assigned to by an Optimizer.
     */
    private HwResource assignedResource;

    /**
     * List of parent nodes.
     */
    private ArrayList<ScheduledGraphNode> parents;

    /**
     * 
     */
    private int layer;

    /**
     * Retrieve the assigned device.
     */
    public HwResource getAssignedResource() {
        return assignedResource;
    }

    /**
     * Used by an Optimizer to set the assigned device.
     */
    public void setAssignedResource(HwResource assignedResource) {
        this.assignedResource = assignedResource;
    }

    /**
     * Retrieve the parent ScheduledGraphNodes.
     */
    @JsonIgnore
    public ArrayList<ScheduledGraphNode> getParents() {
        return parents;
    }

    @JsonIgnore
    public void setParents(ArrayList<ScheduledGraphNode> parents) {
        this.parents = parents;
    }

    /**
     * Add another ScheduledGraphNode to the parents of this ScheduledGraphNode.
     */
    public void addParent(ScheduledGraphNode scheduledGraphNode) {
        parents.add(scheduledGraphNode);
    }

    @JsonIgnore
    public int getLayer() {
        return layer;
    }

    @JsonIgnore
    public void setLayer(int layer) {
        this.layer = layer;
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
