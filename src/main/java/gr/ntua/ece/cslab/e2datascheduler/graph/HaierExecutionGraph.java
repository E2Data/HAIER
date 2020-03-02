package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.JSONableHaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.JSONableScheduledJobVertex;

import org.apache.flink.runtime.jobgraph.IntermediateDataSet;
import org.apache.flink.runtime.jobgraph.JobEdge;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.runtime.jobgraph.JobVertexID;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A mapping between an Apache FLink's {@link JobGraph}'s {@link JobVertex} objects and the {@link HwResource}s
 * that they have been assigned to.
 */
public class HaierExecutionGraph {

    private final JobGraph jobGraph;
    private final JobVertex[] jobVertices;

    /**
     * An {@link ArrayList} of {@link ScheduledJobVertex} objects, in the same order as their
     * corresponding {@link JobVertex} objects in field {@code this.jobVertices}.
     */
    private final ArrayList<ScheduledJobVertex> scheduledJobVertices;

    /**
     * TODO(ckatsak): Documentation
     */
    private final Map<String, Double> objectiveCosts;

    // --------------------------------------------------------------------------------------------

    /**
     * A mapping between an Apache Flink's {@link JobGraph}'s {@link JobVertex} objects and the {@link HwResource}s
     * that they have been assigned to.
     *
     * @param jobGraph The {@link JobGraph} that this {@link HaierExecutionGraph} is based upon.
     * @param jobVertices An array of the {@link JobVertex} objects to be included in this {@link HaierExecutionGraph}.
     */
    public HaierExecutionGraph(final JobGraph jobGraph, final JobVertex[] jobVertices) {
        this.jobGraph = jobGraph;
        this.jobVertices = jobVertices;
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

    public JobVertex[] getJobVertices() {
        return this.jobVertices;
    }

    public ArrayList<ScheduledJobVertex> getScheduledJobVertices() {
        return this.scheduledJobVertices;
    }

    public Map<String, Double> getObjectiveCosts() {
        return this.objectiveCosts;
    }

    /**
     * Assign the given {@link HwResource} to the {@link ScheduledJobVertex} with the given (internal to HAIER) ID.
     *
     * @param jobVertexIndex The given (internal to HAIER) ID of the {@link ScheduledJobVertex}.
     * @param assignedResource The given {@link HwResource}.
     */
    public void assignResource(final int jobVertexIndex, final HwResource assignedResource) {
        this.scheduledJobVertices.get(jobVertexIndex).setAssignedResource(assignedResource);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Find and return the root vertices for the JobGraph related to this HaierExecutionGraph.
     *
     * ~ O(V)
     */
    public List<Integer> findRootJobVertices() {
        if (this.jobVertices.length == 0) {
            return java.util.Collections.emptyList();
        }

        final List<Integer> roots = new ArrayList<>();
        for (int i = 0; i < this.jobVertices.length; i++) {
            if (this.jobVertices[i].hasNoConnectedInputs()) {
                roots.add(i);
            }
        }
        return roots;
    }

    /**
     * FIXME(ckatsak): Probably not needed anymore.
     *
     * @param scheduledJobVertex
     * @return
     */
    public Set<Integer> getSubDAGInclusive(final ScheduledJobVertex scheduledJobVertex) {
        final Set<Integer> ret = new HashSet<>();
        auxSubDAGInclusiveRecursive(scheduledJobVertex, ret);
        return ret;
    }

    /**
     * FIXME(ckatsak): Probably not needed anymore.
     *
     * @param scheduledJobVertex
     * @param subDAG
     */
    private void auxSubDAGInclusiveRecursive(final ScheduledJobVertex scheduledJobVertex, final Set<Integer> subDAG) {
        subDAG.add(scheduledJobVertex.getJobVertexIndex());
        for (Integer childVertex : scheduledJobVertex.getChildren()) {
            auxSubDAGInclusiveRecursive(this.scheduledJobVertices.get(childVertex), subDAG);
        }
    }

    // --------------------------------------------------------------------------------------------

    private static final List<String> NON_COMPUTATIONAL_OPERATOR_NAMES = new ArrayList<String>() {{
        add("DataSource");
        add("DataSink");
        add("Sync");
        add("PartialSolution");
    }};

    /**
     * Returns {@code true} if the given {@link JobVertex} represents a task that can be offloaded to some
     * heterogeneous architecture supported by E2Data; {@code false} otherwise.
     *
     * @param jobVertex The given {@link JobVertex}.
     * @return {@code true} if it can be offloaded; {@code false} otherwise.
     */
    public static boolean isComputational(final JobVertex jobVertex) {
        for (String name : HaierExecutionGraph.NON_COMPUTATIONAL_OPERATOR_NAMES) {
            if (jobVertex.getName().startsWith(name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the given {@link ScheduledJobVertex} represents a task that can be offloaded to some
     * heterogeneous architecture supported by E2Data; {@code false} otherwise.
     *
     * @param scheduledJobVertex The given {@link ScheduledJobVertex}.
     * @return {@code true} if it can be offloaded; {@code false} otherwise.
     */
    public static boolean isComputational(final ScheduledJobVertex scheduledJobVertex) {
        return HaierExecutionGraph.isComputational(scheduledJobVertex.getJobVertex());
    }

    /**
     * @return a JSON-formatted {@link JSONableHaierExecutionGraph}.
     */
    @Override
    public String toString() {
        final List<Integer> schedulableIndices = new ArrayList<>();
        outer:
        for (int i = 0; i < this.jobVertices.length; i++) {
            for (String name : HaierExecutionGraph.NON_COMPUTATIONAL_OPERATOR_NAMES) {
                if (this.jobVertices[i].getName().startsWith(name)) {
                    continue outer;
                }
            }
            schedulableIndices.add(i);
        }

        final List<JSONableScheduledJobVertex> vs = new ArrayList<>(schedulableIndices.size());
        for (int i : schedulableIndices) {
            final JSONableScheduledJobVertex v = new JSONableScheduledJobVertex();
            v.setId(this.jobVertices[i].getID());
            v.setAssignedResource(this.scheduledJobVertices.get(i).getAssignedResource());

            final List<Integer> childrenIndices = this.scheduledJobVertices.get(i).getChildren();
            final JobVertexID[] childrenIDs = new JobVertexID[childrenIndices.size()];
            for (int j = 0; j < childrenIndices.size(); j++) {
                childrenIDs[j] = this.jobVertices[childrenIndices.get(j)].getID();
            }
            v.setChildren(childrenIDs);
            vs.add(v);
        }

        final JSONableHaierExecutionGraph jheg = new JSONableHaierExecutionGraph();
        jheg.setJSONableScheduledJobVertices(vs.toArray(new JSONableScheduledJobVertex[schedulableIndices.size()]));

        return new GsonBuilder().setPrettyPrinting().create().toJson(jheg);
    }

}
