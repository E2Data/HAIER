package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalJobVertex;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Layer represents a set of task vertices (i.e. {@link ComputationalJobVertex} objects)
 * that may be executed simultaneously on different {@link HwResource}s.
 */
public class Layer {

    /**
     * The tasks (i.e. {@link ComputationalJobVertex} objects) that belong to this {@link Layer}.
     */
    private final ArrayList<ComputationalJobVertex> computationalJobVertices;

    /**
     * The total execution time of this {@link Layer}, according to this execution time evaluation algorithm.
     */
    private double duration;

    // -------------------------------------------------------------------------------------------

    /**
     * Layer represents a set of task vertices (i.e. {@link ComputationalJobVertex} objects)
     * that may be executed simultaneously on different {@link HwResource}s.
     */
    public Layer() {
        this.computationalJobVertices = new ArrayList<>();
    }

    /**
     * Add the given {@link ComputationalJobVertex} to this {@link Layer}.
     *
     * @param computationalJobVertex The {@link ComputationalJobVertex} to add to this {@link Layer}.
     * @return The return value of the associated {@link ArrayList}'s {@code add()} method.
     */
    public boolean addComputationalJobVertex(final ComputationalJobVertex computationalJobVertex) {
        return this.computationalJobVertices.add(computationalJobVertex);
    }

    /**
     * Remove the given {@link ComputationalJobVertex} from this {@link Layer}.
     *
     * @param computationalJobVertex The {@link ComputationalJobVertex} to remove from this {@link Layer}.
     * @return The return value of the associated {@link ArrayList}'s {@code remove()} method.
     */
    public boolean removeComputationalJobVertex(final ComputationalJobVertex computationalJobVertex) {
        return this.computationalJobVertices.remove(computationalJobVertex);
    }

    /**
     * Retrieve the total execution time of this {@link Layer}.
     *
     * @return The total execution time of this {@link Layer}.
     */
    public double getDuration() {
        return this.duration;
    }

    /**
     * Set the total execution time of this {@link Layer}.
     *
     * @param duration The total execution time of this {@link Layer}.
     */
    public void setDuration(final double duration) {
        this.duration = duration;
    }

    // -------------------------------------------------------------------------------------------

    /**
     * Constructs and returns a mapping between each {@link HwResource} that has been assigned in this {@link Layer},
     * and a list of {@link ComputationalJobVertex} that have been assigned to it.
     *
     * This mapping can be useful to differentiate the logic behind cost evaluation of some objective (e.g. execution
     * time) when some of the tasks (i.e. JobVertex objects) have to be serialized according to the current assignment.
     *
     * @return The mapping between each {@link HwResource} that has been assigned in this {@link Layer} and the list of
     * {@link ComputationalJobVertex} that have been assigned to it.
     */
    public Map<HwResource, ArrayList<ComputationalJobVertex>> getColocations() {
        final Map<HwResource, ArrayList<ComputationalJobVertex>> ret = new HashMap<>();

        for (ComputationalJobVertex computationalJobVertex : this.computationalJobVertices) {
            final HwResource assignedResource = computationalJobVertex.getAssignedResource();
            if (!ret.containsKey(assignedResource)) {
                ret.put(assignedResource, new ArrayList<ComputationalJobVertex>());
            }
            ret.get(assignedResource).add(computationalJobVertex);
        }

        return ret;
    }

    // -------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
