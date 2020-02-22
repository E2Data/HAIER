package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.JSONableFlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.JSONableScheduledJobVertex;

import org.apache.flink.runtime.jobgraph.IntermediateDataSet;
import org.apache.flink.runtime.jobgraph.JobEdge;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.runtime.jobgraph.JobVertexID;

import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.ArrayList;
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
     * TODO(ckatsak): Documentation
     */
    private final Map<String, Double> objectiveCosts;

    // --------------------------------------------------------------------------------------------

    public FlinkExecutionGraph(JobGraph jobGraph, JobVertex[] jobVertices) {
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
     * TODO(ckatsak)
     *
     * @param jobVertexIndex
     * @param assignedResource
     */
    public void assignResource(int jobVertexIndex, HwResource assignedResource) {
        this.scheduledJobVertices.get(jobVertexIndex).setAssignedResource(assignedResource);
    }

    // --------------------------------------------------------------------------------------------

    private static final List<String> NON_OPERATOR_NAMES = new ArrayList<String>() {{
        add("DataSource");
        add("DataSink");
        add("Sync");
        add("PartialSolution");
    }};

    /**
     * @return a JSON formatted JSONableFlinkExecutionGraph
     */
    @Override
    public String toString() {
        final List<Integer> schedulableIndices = new ArrayList<>();
        outer:
        for (int i = 0; i < this.jobVertices.length; i++) {
            for (String name : FlinkExecutionGraph.NON_OPERATOR_NAMES) {
                if (this.jobVertices[i].getName().startsWith(name)) {
                    continue outer;
                }
            }
            schedulableIndices.add(i);
        }

        final List<JSONableScheduledJobVertex> vs = new ArrayList<JSONableScheduledJobVertex>(schedulableIndices.size());
        for (int i : schedulableIndices) {
            JSONableScheduledJobVertex v = new JSONableScheduledJobVertex();
            v.setId(this.jobVertices[i].getID());
            v.setLayer(this.scheduledJobVertices.get(i).getLayer());
            v.setAssignedResource(this.scheduledJobVertices.get(i).getAssignedResource());

            final List<Integer> childrenIndices = this.scheduledJobVertices.get(i).getChildren();
            final JobVertexID[] childrenIDs = new JobVertexID[childrenIndices.size()];
            for (int j = 0; j < childrenIndices.size(); j++) {
                childrenIDs[j] = this.jobVertices[childrenIndices.get(j)].getID();
            }
            v.setChildren(childrenIDs);
            vs.add(v);
        }

        final JSONableFlinkExecutionGraph jfeg = new JSONableFlinkExecutionGraph();
        jfeg.setJSONableScheduledJobVertices(vs.toArray(new JSONableScheduledJobVertex[schedulableIndices.size()]));

        return new GsonBuilder().setPrettyPrinting().create().toJson(jfeg);
    }

}
