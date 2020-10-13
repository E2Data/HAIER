package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.SerializableScheduledJobVertex;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
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
import java.util.logging.Logger;


/**
 * A mapping between an Apache FLink's {@link JobGraph}'s {@link JobVertex} objects and the {@link HwResource}s
 * that they have been assigned to.
 */
public class HaierExecutionGraph {

    private static final Logger logger = Logger.getLogger(HaierExecutionGraph.class.getCanonicalName());

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
     * Check whether the given {@link JobVertex} can be offloaded to some heterogeneous architecture supported by
     * E2Data, based only on its name.
     *
     * FIXME(ckatsak): This is probably wrong and obsolete, but check cross-checking the new technique is pending.
     *
     * @param jobVertex the {@link JobVertex} at hand
     * @return {@code true} if it can be offloaded; {@code false} otherwise
     */
    private static boolean isComputationalBasedOnName(final JobVertex jobVertex) {
        for (String name : HaierExecutionGraph.NON_COMPUTATIONAL_OPERATOR_NAMES) {
            if (jobVertex.getName().startsWith(name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether the given {@link JobVertex} can be offloaded to some heterogeneous architecture supported by
     * E2Data.
     *
     * @param jobVertex the {@link JobVertex} at hand
     * @return {@code true} if it can be offloaded; {@code false} otherwise
     */
    public static boolean isComputational(final JobVertex jobVertex) {
        return isComputationalBasedOnDriverClass(jobVertex);
//        return isComputationalBasedOnName(jobVertex);
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
     * An exhaustive list of the Driver classes that are actually supported by TornadoVM at this time.
     */
    private static final List<String> TORNADOVM_SUPPORTED_DRIVER_CLASSES = new ArrayList<String>() {{
        add("ChainedMapDriver");
        add("ChainedAllReduceDriver");
        add("ChainedReduceCombineDriver");
        add("MapDriver");
        add("ReduceDriver");
    }};

    /**
     * Check whether the given {@link JobVertex} can be offloaded to some heterogeneous architecture supported by
     * E2Data, based on Flink Driver classes reported in it.
     *
     * @param jobVertex the {@link JobVertex} at hand
     * @return {@code true} if it can be offloaded; {@code false} otherwise
     */
    private static boolean isComputationalBasedOnDriverClass(final JobVertex jobVertex) {
        final Configuration config = jobVertex.getConfiguration();
        final ConfigOption<Integer> chainingNumOption = ConfigOptions
                .key("chaining.num")
                .defaultValue(0);
        final ConfigOption<String> driverClassOption = ConfigOptions
                .key("driver.class")
                .noDefaultValue();

        final int chainingNum = config.getInteger(chainingNumOption);

        // If the JobVertex does not contain chained operators, then just look for the value of the entry under the
        // "driver.class" key (note that it may be absent, in which case it is probably a "non-acceleratable" vertex)
        // to check whether it is one of the driver classes supported by TornadoVM.
        if (chainingNum == 0) {
            if (!config.contains(driverClassOption)) {
                return false;
            }
            final String driverClassCanonicalName = config.getString(driverClassOption);
            final String[] splitDriverClass = driverClassCanonicalName.split("\\.");
            final String driverClassBaseName = splitDriverClass[splitDriverClass.length - 1];
            return TORNADOVM_SUPPORTED_DRIVER_CLASSES.contains(driverClassBaseName);
        }
        //
        // Now, if the JobVertex at hand contains chained operators:
        //
        boolean chainStartingWithDataSource = false;
        // Check whether the first operator in the chain has a Driver class. If it does not, then the whole JobVertex
        // cannot be offloaded to an accelerator, unless the first operator in the chain is a DataSource.
        if (!config.contains(driverClassOption)) {
            // If the first operator in the chain is a DataSource, then the JobVertex might be acceleratable
            // (unless some of the rest of the operators in the chain is not acceleratable).
            // In any other case of missing a Driver class in the first operator in a chain, consider
            // it non-acceleratable. FIXME(ckatsak): Is this assumption correct? E.g., is a `CHAIN Sync -> Map` even
            //                                       possible, let alone acceleratable?
            if (jobVertex.getName().startsWith("CHAIN DataSource")) {
                chainStartingWithDataSource = true;
            } else {
                return false;
            }
        }
        // If the first operator in the chain does have a Driver class, then if this Driver class is not supported by
        // TornadoVM, the whole JobVertex is non-acceleratable. FIXME?(ckatsak): Related to the FIXME above.
        if (!chainStartingWithDataSource) {
            final String driverClassCanonicalName = config.getString(driverClassOption);
            final String[] splitDriverClass = driverClassCanonicalName.split("\\.");
            final String driverClassBaseName = splitDriverClass[splitDriverClass.length - 1];
            if (!TORNADOVM_SUPPORTED_DRIVER_CLASSES.contains(driverClassBaseName)) {
                return false;
            }
        }
        // Now, check the Driver classes of all the operators in the chain of the JobVertex one by one, and only
        // conclude that the JobVertex is acceleratable if all of them are supported by TornadoVM.
        for (int i = 0; i < chainingNum; i++) {
            // If chained operator's Driver class is missing, the JobVertex is not acceleratable.
            // FIXME(ckatsak): ^^  Another similar assumption to cross-check  ^^
            if (!config.containsKey("chaining.task." + i)) {
                return false;
            }
            final String driverClassCanonicalName = config.getString("chaining.task." + i, "");
            final String[] splitDriverClass = driverClassCanonicalName.split("\\.");
            final String driverClassBaseName = splitDriverClass[splitDriverClass.length - 1];
            if (!TORNADOVM_SUPPORTED_DRIVER_CLASSES.contains(driverClassBaseName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check whether the given {@link JobGraph} contains *any* {@link JobVertex} that can be offloaded to some
     * heterogeneous architecture supported by E2Data.
     *
     * @param jobGraph
     * @return
     */
    public static boolean containsAnyComputationalJobVertex(final JobGraph jobGraph) {
        for (JobVertex jobVertex : jobGraph.getVertices()) {
            if (HaierExecutionGraph.isComputational(jobVertex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Go through all {@link JobVertex} objects in the given {@link JobGraph} and log for each of them whether it is
     * offloadable to the heterogeneous architectures supported by E2Data or not.
     *
     * @param jobGraph the given {@link JobGraph} to examine
     */
    public static void logOffloadability(final JobGraph jobGraph) {
        String msg = "Checking \"offloadability\" of JobVertices in '" + jobGraph.toString() + "':\n";
        int total = 0;
        int acceleratable = 0;
        for (JobVertex jobVertex : jobGraph.getVerticesSortedTopologicallyFromSources()) {
            total++;
            if (HaierExecutionGraph.isComputational(jobVertex)) {
                acceleratable++;
                msg += total + ". JobVertex '" + jobVertex.toString() + "' is acceleratable\n";
            } else {
                msg += total + ". JobVertex '" + jobVertex.toString() + "' is NOT acceleratable\n";
            }
        }
        msg += "Offload-ability Summary: " + acceleratable + " out of " + total +" JobVertices in " +
                jobGraph.toString() + " are acceleratable!";
        logger.finest(msg);
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
        for (int jobVertexIndex = 0; jobVertexIndex < this.jobVertices.length; jobVertexIndex++) {
            if (!HaierExecutionGraph.isComputational(this.jobVertices[jobVertexIndex])) {
                continue;
            }
            schedulableIndices.add(jobVertexIndex);
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
