package gr.ntua.ece.cslab.e2datascheduler.beans.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ScheduledGraphNode;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Layer represents a set of ScheduledGraphNodes that may be executed
 * simultaneously.
 */
public class Layer {

    private ArrayList<ScheduledGraphNode> nodes;

    private double duration;


    public ArrayList<ScheduledGraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<ScheduledGraphNode> nodes) {
        this.nodes = nodes;
    }

    public boolean addNode(ScheduledGraphNode node) {
        return nodes.add(node);
    }

    public boolean removeNode(ScheduledGraphNode node) {
        return nodes.remove(node);
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }


    /**
     * Constructs and returns a mapping between each HwResource that has been
     * assigned in this Layer, and a list of ScheduledGraphNodes that have been
     * assigned to it.
     * This mapping can be useful to differentiate the logic behind cost
     * evaluation of some objective (e.g. execution time) when some of the
     * tasks (i.e. nodes) have to be serialized according to the current
     * assignment.
     */
    public Map<HwResource, ArrayList<ScheduledGraphNode>> getColocations() {
        Map<HwResource, ArrayList<ScheduledGraphNode>> ret =
            new HashMap<HwResource, ArrayList<ScheduledGraphNode>>();

        for (ScheduledGraphNode node : nodes) {
            HwResource assignedResource = node.getAssignedResource();
            if (!ret.containsKey(assignedResource)) {
                ret.put(assignedResource, new ArrayList<ScheduledGraphNode>());
            }
            ret.get(assignedResource).add(node);
        }

        return ret;
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
