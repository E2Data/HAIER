package gr.ntua.ece.cslab.e2datascheduler.beans.graph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
 * Sample JobGraph JSON
 *

     {"graph": [
               {"id":"0", "sourceCode":"__kernel void A(__global float* a, __global float* b, __global float* c, const int d) {int e = get_global_id(0);if (e < d) {c[e] = a[e] + b[e];}",
                "children":["1"]},
                {"id":"1", "sourceCode":"__kernel void A(__global float* a, __global float* b, __global float* c, const int d) {int e = get_global_id(0);if (e < d) {c[e] = a[e] + b[e];}",
                "children":[]}
           ],
      "roots": ["0"]
     }


 *
 */

/**
 * Object describing the input of a scheduling job in the form of a graph
 */
public class JobGraph {

    /**
     * An ArrayList representation of the graph.
     */
    private ArrayList<GraphNode> graph;

    protected Map<Integer, GraphNode> indexedGraph;

    /**
     * The set of root nodes of the graph.
     */
    protected ArrayList<Integer> roots;



    @JsonProperty("graph")
    public ArrayList<GraphNode> getGraph() {
        return graph;
    }

    public void setGraph(ArrayList<GraphNode> graph) {
        this.graph = graph;
    }

    public Map<Integer, GraphNode> getIndexedGraph() {
        return indexedGraph;
    }
    public ArrayList<Integer> getRoots() {
        return roots;
    }

    public void setRoots(ArrayList<Integer> roots) {
        this.roots = roots;
    }

    public boolean isRoot(GraphNode graphNode) {
        return roots.contains(graphNode);
    }

    public void setIndexedGraph(Map<Integer, GraphNode> indexedGraph) {
        this.indexedGraph = indexedGraph;
    }

    public int size() {
        return graph.size();
    }

    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }


    public void index(){
        indexedGraph = new HashMap<>();
        for(GraphNode g : graph){
            indexedGraph.put(g.getId(), g);
        }
    }

}
