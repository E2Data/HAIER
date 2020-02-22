package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Layer represents a set of ScheduledJobVertex that may be executed
 * simultaneously.
 */
public class Layer {

    private final ArrayList<ScheduledJobVertex> scheduledJobVertices;

    private double duration;

    // -------------------------------------------------------------------------------------------

    public Layer() {
        this.scheduledJobVertices = new ArrayList<>();
    }

    public ArrayList<ScheduledJobVertex> getScheduledJobVertices() {
        return this.scheduledJobVertices;
    }

    public boolean addScheduledJobVertex(ScheduledJobVertex scheduledJobVertex) {
        return this.scheduledJobVertices.add(scheduledJobVertex);
    }

    public boolean removeScheduledJobVertex(ScheduledJobVertex scheduledJobVertex) {
        return this.scheduledJobVertices.remove(scheduledJobVertex);
    }

    public double getDuration() {
        return this.duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    // -------------------------------------------------------------------------------------------

    /**
     * Constructs and returns a mapping between each HwResource that has been
     * assigned in this Layer, and a list of ScheduledJobVertex that have been
     * assigned to it.
     *
     * This mapping can be useful to differentiate the logic behind cost
     * evaluation of some objective (e.g. execution time) when some of the
     * JobVertex objects (i.e. tasks) have to be serialized according to the
     * current assignment.
     */
    public Map<HwResource, ArrayList<ScheduledJobVertex>> getColocations() {
        final Map<HwResource, ArrayList<ScheduledJobVertex>> ret =
            new HashMap<HwResource, ArrayList<ScheduledJobVertex>>();

        for (ScheduledJobVertex scheduledJobVertex : this.scheduledJobVertices) {
            HwResource assignedResource = scheduledJobVertex.getAssignedResource();
            if (!ret.containsKey(assignedResource)) {
                ret.put(assignedResource, new ArrayList<ScheduledJobVertex>());
            }
            ret.get(assignedResource).add(scheduledJobVertex);
        }

        return ret;
    }

    // -------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
