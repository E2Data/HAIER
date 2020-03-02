package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

import org.apache.flink.runtime.jobgraph.JobVertex;

import java.util.ArrayList;
import java.util.List;

public class ScheduledJobVertex {

    /**
     * The index of the corresponding {@link JobVertex} object, known by {@link HaierExecutionGraph} (i.e. internal
     * to HAIER).
     */
    private final int jobVertexIndex;

    /**
     * The {@link JobVertex} object in Flink's {@link org.apache.flink.runtime.jobgraph.JobGraph} that corresponds
     * to this {@link ScheduledJobVertex}.
     *
     * NOTE(ckatsak): This will probably be required when the integration of Flink and Tornado is completed, assuming
     *                there will be some sort of API to retrieve this Flink task's underlying OpenCL kernel source code.
     */
    private final JobVertex jobVertex;

    /**
     * The (internal to HAIER) indices of the "child" ("dependent") tasks of this {@link ScheduledJobVertex} (actually,
     * of the corresponding {@link JobVertex} in the original {@link org.apache.flink.runtime.jobgraph.JobGraph}).
     */
    private final ArrayList<Integer> childJobVertexIndices;

    /**
     * The source code of the OpenCL kernel related to the corresponding {@link JobVertex} object, as a {@link String}.
     */
    private final String sourceCode;

    /**
     * The hardware resource that the corresponding JobVertex has been assigned
     * on by a HAIER Optimizer.
     */
    private HwResource assignedResource;

    // -------------------------------------------------------------------------------------------

    public ScheduledJobVertex(
            final int jobVertexIndex,
            final JobVertex jobVertex,
            final ArrayList<Integer> childJobVertexIndices) {
        this.jobVertexIndex = jobVertexIndex;
        this.jobVertex = jobVertex;
        this.childJobVertexIndices = childJobVertexIndices;

        //this.sourceCode = jobVertex.getSourceCode();  // or equivalent
        //this.sourceCode = "STUB FOR OPENCL KERNEL SOURCE CODE";
        // FIXME(ckatsak): ^^ awaiting Flink-Tornado integration's API

        // XXX(ckatsak): Demo
        this.sourceCode = this.retrieveSourceCode();
    }

    public JobVertex getJobVertex() {
        return this.jobVertex;
    }

    public List<Integer> getChildren() {
        return this.childJobVertexIndices;
    }

    public int getJobVertexIndex() {
        return this.jobVertexIndex;
    }

    public String getSourceCode() {
        return this.sourceCode;
    }

    public HwResource getAssignedResource() {
        return this.assignedResource;
    }

    public void setAssignedResource(HwResource assignedResource) {
        this.assignedResource = assignedResource;
    }

    // -------------------------------------------------------------------------------------------

    private String retrieveSourceCode() {
        //return "STUB FOR OPENCL KERNEL SOURCE CODE";
        return "__kernel void A(__global float* a, __global float* b, const int c) {const int d = get_global_id(0);int e, f, g, h;unsigned int i, j, k, l;for (unsigned int m = 0; m < k; m++) {for (unsigned int h = 0; h < i; h++) {for (unsigned int i = e; i < c; i++) {for (unsigned int j = a[g]; j < i; ++j) {a[i * c + i] = 0;}b[j] = j;}}}}";
    }

    // -------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ScheduledJobVertex:\n\tJobVertex:\t" + this.jobVertex.toString() +
                "\n\tScheduled On:\t" + this.assignedResource.toString();
    }

}
