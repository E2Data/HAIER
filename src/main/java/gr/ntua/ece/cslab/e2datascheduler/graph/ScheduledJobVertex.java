package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.TornadoFeatureVector;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer;

import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;

import java.util.ArrayList;
import java.util.List;


/**
 * A vertex in a {@link HaierExecutionGraph}.
 */
public class ScheduledJobVertex {

    /**
     * The index of the corresponding {@link JobVertex} object, known by {@link HaierExecutionGraph} (i.e. internal
     * to HAIER).
     */
    private final int jobVertexIndex;

    /**
     * The {@link JobVertex} object in Flink's {@link JobGraph} that corresponds to this {@link ScheduledJobVertex}.
     *
     * NOTE(ckatsak): This will probably be required when the integration of Flink and Tornado is completed, assuming
     *                there will be some sort of API to retrieve this Flink task's underlying OpenCL kernel source code.
     */
    private final JobVertex jobVertex;

    /**
     * The (internal to HAIER) indices of the "child" (i.e., "dependent") tasks of this {@link ScheduledJobVertex}
     * (actually, of the corresponding {@link JobVertex} in the original {@link JobGraph}).
     */
    private final ArrayList<Integer> childJobVertexIndices;

    /**
     * The hardware resource that the corresponding {@link JobVertex} has been assigned on by an {@link Optimizer}.
     */
    private HwResource assignedResource;

    /**
     * The extracted code features (for the ML {@link Model}) of the operators in this vertex.
     */
    private List<TornadoFeatureVector> tornadoFeatures;


    // -------------------------------------------------------------------------------------------


    public ScheduledJobVertex(
            final int jobVertexIndex,
            final JobVertex jobVertex,
            final ArrayList<Integer> childJobVertexIndices) {
        this.jobVertexIndex = jobVertexIndex;
        this.jobVertex = jobVertex;
        this.childJobVertexIndices = childJobVertexIndices;
    }


    // -------------------------------------------------------------------------------------------


    /**
     * Retrieve the {@link JobVertex} that this {@link ScheduledJobVertex} represents.
     *
     * @return The underlying {@link JobVertex}
     */
    public JobVertex getJobVertex() {
        return this.jobVertex;
    }

    /**
     * Retrieve a {@link List} of indices that represent the children vertices of this {@link ScheduledJobVertex}.
     *
     * @return A {@link List} of indices that represent children vertices
     */
    public List<Integer> getChildren() {
        return this.childJobVertexIndices;
    }

    /**
     * Retrieve the HAIER-internal index of the underlying {@link JobVertex}.
     *
     * @return The HAIER-internal index
     */
    public int getJobVertexIndex() {
        return this.jobVertexIndex;
    }

    /**
     * Retrieve the {@link HwResource} that the task (i.e., this {@link ScheduledJobVertex}, the underlying
     * {@link JobVertex}, etc) has been assigned on.
     *
     * @return The {@link HwResource} that this vertex has been assigned on
     */
    public HwResource getAssignedResource() {
        return this.assignedResource;
    }

    /**
     * Set the {@link HwResource} that this task (i.e., this {@link ScheduledJobVertex}, the underlying
     * {@link JobVertex}, etc) is assigned on.
     *
     * @param assignedResource The {@link HwResource} that this vertex is assigned on
     */
    public void setAssignedResource(final HwResource assignedResource) {
        this.assignedResource = assignedResource;
    }

    /**
     * Retrieve the extracted (and stored) code features (for the ML {@link Model}) of the operators in this vertex.
     *
     * @return The code features of the operators in this vertex
     */
    public List<TornadoFeatureVector> getTornadoFeatures() {
        return this.tornadoFeatures;
    }

    /**
     * Set (and store) the extracted code features (for the ML {@link Model}) of the operators in this vertex.
     *
     * @param tornadoFeatures The code features of the operators in this vertex
     */
    public void setTornadoFeatures(final List<TornadoFeatureVector> tornadoFeatures) {
        this.tornadoFeatures = tornadoFeatures;
    }


    // -------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        return "ScheduledJobVertex:\n\tJobVertex:\t" + this.jobVertex.toString() +
                "\n\tScheduled On:\t" + this.assignedResource.toString();
    }

}
