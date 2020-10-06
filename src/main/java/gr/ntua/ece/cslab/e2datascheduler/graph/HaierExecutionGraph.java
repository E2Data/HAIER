package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.SerializableScheduledJobVertex;

import org.apache.flink.runtime.jobgraph.IntermediateDataSet;
import org.apache.flink.runtime.jobgraph.JobEdge;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.jobmanager.scheduler.CoLocationGroup;

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


    // --------------------------------------------------------------------------------------------


    /**
     * Check whether the co-location constraints of this {@link HaierExecutionGraph} are being respected.
     *
     * @return {@code true} if the co-location constraints are being respected; {@code false} otherwise
     */
    public boolean checkCoLocationConstraints() {
        // Group all ScheduledJobVertices of the HaierExecutionGraph by their CoLocationGroup.
        final Map<CoLocationGroup, List<ScheduledJobVertex>> verticesPerCoLocationGroup = new HashMap<>();
        for (int jobVertexIndex = 0; jobVertexIndex < this.getJobVertices().length; jobVertexIndex++) {
            final JobVertex jobVertex = this.getJobVertices()[jobVertexIndex];
            final CoLocationGroup coLocationGroup = jobVertex.getCoLocationGroup();
            // If there is no CoLocationGroup for this JobVertex, continue with the next one.
            if (null == coLocationGroup) {
                continue;
            }
            // If it's the first time we see this CoLocationGroup, initialize a new entry for it in the Map.
            if (!verticesPerCoLocationGroup.containsKey(coLocationGroup)) {
                verticesPerCoLocationGroup.put(coLocationGroup, new ArrayList<>());
            }
            // Add JobVertex's corresponding ScheduledJobVertex in this CoLocationGroup's List.
            verticesPerCoLocationGroup.get(coLocationGroup)
                    .add(this.getScheduledJobVertices().get(jobVertexIndex));
        }

        // For each of those groups, check whether their ScheduledJobVertices have been assigned on the same
        // HwResource. If not, then the co-location constraints are not respected, therefore return false.
        for (Map.Entry<CoLocationGroup, List<ScheduledJobVertex>> entry : verticesPerCoLocationGroup.entrySet()) {
            final List<ScheduledJobVertex> coLocatedVertices = entry.getValue();
            final HwResource assignedResource = coLocatedVertices.get(0).getAssignedResource();
            for (ScheduledJobVertex scheduledJobVertex : coLocatedVertices) {
                if (scheduledJobVertex.getAssignedResource() != assignedResource) {
                    return false;
                }
            }
        }
        return true;
    }


    // --------------------------------------------------------------------------------------------


    /**
     * Serialize the {@link HaierExecutionGraph} into a {@link List<SerializableScheduledJobVertex>}.
     *
     * @return a {@link List} of {@link SerializableScheduledJobVertex}.
     */
    public List<SerializableScheduledJobVertex> toSerializableScheduledJobVertexList() {
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

        final List<SerializableScheduledJobVertex> ret = new ArrayList<>(schedulableIndices.size());
        for (int i : schedulableIndices) {
            final SerializableScheduledJobVertex v = new SerializableScheduledJobVertex();
            v.setId(this.jobVertices[i].getID());
            v.setAssignedResource(this.scheduledJobVertices.get(i).getAssignedResource());

            final List<Integer> childrenIndices = this.scheduledJobVertices.get(i).getChildren();
            final JobVertexID[] childrenIDs = new JobVertexID[childrenIndices.size()];
            for (int j = 0; j < childrenIndices.size(); j++) {
                childrenIDs[j] = this.jobVertices[childrenIndices.get(j)].getID();
            }
            v.setChildren(childrenIDs);
            ret.add(v);
        }

        return ret;
    }

    /**
     * @return a pretty-printed JSON-formatted {@link List<SerializableScheduledJobVertex>}.
     */
    @Override
    public String toString() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this.toSerializableScheduledJobVertexList());
    }

}
