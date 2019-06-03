package gr.ntua.ece.cslab.e2datascheduler.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.JobGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;

import java.io.IOException;

/*
    Sample Task JSON
    {"jobgraph": {"graph": [
                        {"id":"0", "sourceCode":"__kernel void A(__global float* a, __global float* b, __global float* c, const int d) {int e = get_global_id(0);if (e < d) {c[e] = a[e] + b[e];}",
                         "children":["1"]},
                        {"id":"1", "sourceCode":"__kernel void A(__global float* a, __global float* b, __global float* c, const int d) {int e = get_global_id(0);if (e < d) {c[e] = a[e] + b[e];}",
                         "children":[]}
                         ],
                "roots": ["0"]
                },
      "policy": {"objectives": [
                                {"name":"execTime", "targetFunction":"MIN", "combineFunction":"MAX"},
                                {"name":"powerCons", "targetFunction":"MIN", "combineFunction":"SUM"}
                               ]
                }
     }
 */


public class SubmittedTask {

    private JobGraph jobgraph;
    private OptimizationPolicy policy;

    public JobGraph getJobgraph() {
        return jobgraph;
    }

    public OptimizationPolicy getPolicy() {
        return policy;
    }


    public static void main(String[] args){
        String g = "{\"jobgraph\": {\"graph\": [\n" +
                "                        {\"id\":\"0\", \"sourceCode\":\"__kernel void A(__global float* a, __global float* b, __global float* c, const int d) {int e = get_global_id(0);if (e < d) {c[e] = a[e] + b[e];}\",\n" +
                "                         \"children\":[\"1\"]},\n" +
                "                        {\"id\":\"1\", \"sourceCode\":\"__kernel void A(__global float* a, __global float* b, __global float* c, const int d) {int e = get_global_id(0);if (e < d) {c[e] = a[e] + b[e];}\",\n" +
                "                         \"children\":[]}\n" +
                "                         ],\n" +
                "                \"roots\": [\"0\"]\n" +
                "                },\n" +
                "      \"policy\": {\"objectives\": [\n" +
                "                                {\"name\":\"execTime\", \"targetFunction\":\"MIN\", \"combineFunction\":\"MAX\"},\n" +
                "                                {\"name\":\"powerCons\", \"targetFunction\":\"MIN\", \"combineFunction\":\"SUM\"}\n" +
                "                               ]\n" +
                "                }\n" +
                "     }";

        ObjectMapper mapper = new ObjectMapper();
        SubmittedTask st = null;
        try {
            st = mapper.readValue(g, SubmittedTask.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
