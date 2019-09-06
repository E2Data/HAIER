package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

import org.apache.flink.runtime.jobgraph.JobVertex;

import java.util.ArrayList;
import java.util.List;

public class ScheduledJobVertex {

    /**
     * The index of the corresponding JobVertex object, known by
     * FlinkExecutionGraph.
     *
     * FIXME(ckatsak): Not sure if really needed, at least for the time being
     */
    private final int jobVertexIndex;

    /**
     * The JobVertex object in Flink's JobGraph that corresponds to this
     * ScheduledJobVertex.
     *
     * NOTE(ckatsak): This will probably be required when the integration of
     * Flink and Tornado is completed, assuming there will be some sort of API
     * to retrieve this Flink task's underlying OpenCL kernel source code.
     */
    private final JobVertex jobVertex;

    /**
     * The HAIER-internal indices of the "child" ("dependent") tasks of this
     * SchuduledJobVertex (actually, of the corresponding JobVertex in the
     * original JobGraph).
     */
    private final ArrayList<Integer> childJobVertexIndices;

    /**
     * The source code of the OpenCL kernel related to the corresponding
     * JobVertex object, as a String.
     */
    private final String sourceCode;

    /**
     * FIXME(ckatsak):
     */
     private static final String kernelsDir = "/home/users/ckatsak/haier_tmp/kernels/";

    /**
     * The hardware resource that the corresponding JobVertex has been assigned
     * on by a HAIER Optimizer.
     */
    private HwResource assignedResource;

    /**
     * The order of this ScheduledJobVertex object's Layer.
     */
    private int layer;

    // -------------------------------------------------------------------------------------------

    public ScheduledJobVertex(int jobVertexIndex, JobVertex jobVertex, ArrayList<Integer> childJobVertexIndices) {
        this.jobVertexIndex = jobVertexIndex;
        this.jobVertex = jobVertex;
        this.childJobVertexIndices = childJobVertexIndices;

        //this.sourceCode = jobVertex.getSourceCode();  // or equivalent
        //this.sourceCode = "STUB FOR OPENCL KERNEL SOURCE CODE";
        // FIXME(ckatsak): ^^ awaiting Flink-Tornado integration's API

        // XXX(ckatsak): Demo
        this.sourceCode = retrieveSourceCode();
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

    public int getLayer() {
        return this.layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    // -------------------------------------------------------------------------------------------

    private String retrieveSourceCode() {
        //return "STUB FOR OPENCL KERNEL SOURCE CODE";
        return "__kernel void A(__global float* a, __global float* b, const int c) {const int d = get_global_id(0);int e, f, g, h;unsigned int i, j, k, l;for (unsigned int m = 0; m < k; m++) {for (unsigned int h = 0; h < i; h++) {for (unsigned int i = e; i < c; i++) {for (unsigned int j = a[g]; j < i; ++j) {a[i * c + i] = 0;}b[j] = j;}}}}";
    }

    // -------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ScheduledJobVertex:\n\tJobVertex:\t" + this.jobVertex.toString() + "\n\tScheduled On:\t" + this.assignedResource.toString();
    }

}
