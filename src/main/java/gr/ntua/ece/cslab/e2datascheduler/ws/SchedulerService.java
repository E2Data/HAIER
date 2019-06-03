package gr.ntua.ece.cslab.e2datascheduler.ws;

import gr.ntua.ece.cslab.e2datascheduler.E2dScheduler;
import gr.ntua.ece.cslab.e2datascheduler.beans.SubmittedTask;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ScheduledGraphNode;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.JobGraph;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;

/**
 * REST API of {@link E2dScheduler}
 * Can be used for submitting and scheduling applications
 */

@Path("/e2data")
public class SchedulerService extends AbstractE2DataService {

    private static final Logger logger = Logger.getLogger(SchedulerService.class.getCanonicalName());

    private E2dScheduler scheduler;

    public SchedulerService(){
        scheduler = E2dScheduler.getInstance();
    }

    @Path("/schedule")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response schedule(SubmittedTask inputTask) {
        //TODO: @gmytil: Take care of error handling. What can go wrong during optimization?
        //FIXME: @gmytil: What is the appropriate HTTP status to return for each case?

        logger.info(inputTask.getPolicy().getObjectives()[0].getName());

        OptimizationPolicy policy = inputTask.getPolicy();
        JobGraph inputGraph = inputTask.getJobgraph();

        logger.info(inputGraph.toString());
        inputGraph.index();


        ExecutionGraph result = scheduler.schedule(inputGraph, policy);
        for(ScheduledGraphNode sn : result.getExecutionGraph()){
            logger.info(sn.toString());
        }
        return generateResponse(Response.Status.OK, result);
    }

    // just for testing setup
    @Path("/hello")
    @GET
    public Response schedule() {
        String happyMesg = "hello";
        return generateResponse(Response.Status.OK, happyMesg);
    }

}

