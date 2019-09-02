package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

import org.apache.flink.runtime.jobgraph.IntermediateDataSet;
import org.apache.flink.runtime.jobgraph.JobEdge;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;

import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import java.util.HashMap;


/**
 * A mapping between a JobGraph's JobVertex objects and the devices that they
 * have been assigned to.
 */
public class FlinkExecutionGraph {

    private final JobGraph jobGraph;
    private final JobVertex[] jobVertices;

    /**
     * An ArrayList of ScheduledJobVertex objects, in the same order as their
     * corresponding JobVertex objects in this.jobVertices.
     */
    private final ArrayList<ScheduledJobVertex> scheduledJobVertices;

    /**
     * An ArrayList that includes all Layers in this FlinkExecutionGraph, in
     * order.
     */
    private final ArrayList<Layer> layers;

    /**
     *
     */
    private final Map<String, Double> objectiveCosts;

    // --------------------------------------------------------------------------------------------

    public FlinkExecutionGraph(JobGraph jobGraph, JobVertex[] jobVertices) {
        this.jobGraph = jobGraph;
        this.jobVertices = jobVertices;
        this.layers = new ArrayList<>();
        this.objectiveCosts = new HashMap<>();

        // Construct all ScheduledJobVertex objects.
        // Create a temporary mapping of this.jobVertices for multiple fast 
        // inverse lookups while searching for each JobVertex's children.
        final Map<JobVertex, Integer> mirroredJobVertices = new HashMap<>();
        for (int i = 0; i < this.jobVertices.length; i++) {
            mirroredJobVertices.put(this.jobVertices[i], i);
        }
        // Then, find the children for each JobVertex and construct the
        // corresponding ScheduledJobVertex object.
        this.scheduledJobVertices = new ArrayList<>(jobVertices.length);
        for (int i = 0; i < this.jobVertices.length; i++) {
            ArrayList<Integer> children = new ArrayList<>();
            for (IntermediateDataSet intermediateDataSet : this.jobVertices[i].getProducedDataSets()) {
                for (JobEdge jobEdge : intermediateDataSet.getConsumers()) {
                    children.add(mirroredJobVertices.get(jobEdge.getTarget()));
                }
            }
            this.scheduledJobVertices.add(i, new ScheduledJobVertex(i, this.jobVertices[i], children));
        }
    }

    public ArrayList<ScheduledJobVertex> getScheduledJobVertices() {
        return this.scheduledJobVertices;
    }

    public ArrayList<Layer> getLayers() {
        return this.layers;
    }

    public Layer getLayer(int index) {
        return this.layers.get(index);
    }

    public Map<String, Double> getObjectiveCosts() {
        return this.objectiveCosts;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Construct and populate this FlinkExecutionGraph's Layer objects.
     */
    public void constructLayers() {
        constructLayersBFS();
    }

    /**
     *
     */
    public void assignResource(int jobVertexIndex, HwResource assignedResource) {
        this.scheduledJobVertices.get(jobVertexIndex).setAssignedResource(assignedResource);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Find and return the root vertices for the JobGraph related to this
     * FlinkExecutionGraph.
     *
     * ~ O(V)
     */
    private List<Integer> findRootJobVertices() {
        if (this.jobVertices.length == 0) {
            return java.util.Collections.emptyList();
        }

        final List<Integer> roots = new ArrayList<>(1);
        for (int i = 0; i < this.jobVertices.length; i++) {
            if (this.jobVertices[i].hasNoConnectedInputs()) {
                roots.add(i);
            }
        }
        return roots;
    }

    /**
     * Makes sure that field {@code ArrayList<Layer> layers} is properly
     * initialized and already accommodates the given capacity of (possibly
     * empty) Layer objects.
     */
    private void ensureLayersCapacity(int cap) {
        if (this.layers.size() >= cap) {
            return;
        }

        this.layers.ensureCapacity(cap);
        for (int i = this.layers.size(); i < cap; ++i) {
            this.layers.add(i, new Layer());
        }
    }

    /**
     * Construct the Layer objects through a BFS-like traversal of the graph.
     *
     * It should be ~ O(V+E), since it visits each node once every time that
     * the latter is found anywhere in the adjacency list.
     *
     * TODO(ckatsak): Special handling for case (this.jobVertices.length == 0)?
     *                Not necessary for now.
     */
    private void constructLayersBFS() {
        // Construct a queue for the BFS-like traversal.
        final Queue<Integer> q = new LinkedList<Integer>();

        // Construct the first Layer, i.e. JobGraph's "root" JobVertex objects.
        final Layer rootLayer = new Layer();
        for (int rootJobVertexIndex : this.findRootJobVertices()) {
            rootLayer.addScheduledJobVertex(this.scheduledJobVertices.get(rootJobVertexIndex));
            //this.scheduledJobVertices.get(rootJobVertexIndex).setLayer(0);  // initialized to 0 by default anyway
            q.add(rootJobVertexIndex);
        }
        this.layers.add(rootLayer);

        // Construct the next Layers via a BFS-like traversal using a Queue.
        while (!q.isEmpty()) {
            // Pop the next JobVertex index and retrieve the corresponding ScheduledJobVertex and its layer.
            int parentIndex = q.remove();
            ScheduledJobVertex parentScheduledJobVertex = this.scheduledJobVertices.get(parentIndex);
            int parentLayer = parentScheduledJobVertex.getLayer();

            // Then, loop through its children:
            for (int childIndex : parentScheduledJobVertex.getChildren()) {
                // Retrieve child's corresponding ScheduledJobVertex and its layer.
                ScheduledJobVertex childScheduledJobVertex = this.scheduledJobVertices.get(childIndex);
                int childLayer = childScheduledJobVertex.getLayer();
                // If this parent imposes a higher layer than the one already set, then change it.
                if (parentLayer + 1 > childLayer) {
                    ensureLayersCapacity(parentLayer + 2);                                            // make sure layers is big enough
                    this.layers.get(childLayer).removeScheduledJobVertex(childScheduledJobVertex);    // modify old Layer object
                    this.layers.get(parentLayer + 1).addScheduledJobVertex(childScheduledJobVertex);  // modify new Layer object
                    childScheduledJobVertex.setLayer(parentLayer + 1);                                // modify child's layer index
                }
            }

            // Last, append all parent's children to the queue to be (possibly re-)examined.
            q.addAll(parentScheduledJobVertex.getChildren());
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * FIXME(ckatsak)
     */
    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
