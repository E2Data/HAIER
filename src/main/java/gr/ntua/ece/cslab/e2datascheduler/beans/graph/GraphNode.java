package gr.ntua.ece.cslab.e2datascheduler.beans.graph;

import com.google.gson.GsonBuilder;

import java.util.List;


/**
 * Bean that describes a node of the input graph
 *
 * !!! This class is only for development and testing purposes !!!
 * Eventually, the JobGraph struct of Apache Flink is going to be used.
 */
public class GraphNode {

    protected int id; // unique id within a graph; facilitates graph traversal
    protected String sourceCode; // string containing the corresponding OpenCL code
    protected List<Integer> children; // list of children nodes (the corresponding ids)

    public GraphNode(){}

    public GraphNode(int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public List<Integer> getChildren() {
        return children;
    }

    public void setChildren(List<Integer> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
