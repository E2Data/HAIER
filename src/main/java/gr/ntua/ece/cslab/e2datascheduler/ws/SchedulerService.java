package gr.ntua.ece.cslab.e2datascheduler.ws;

import gr.ntua.ece.cslab.e2datascheduler.E2dScheduler;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.NSGAIIParameters;

import org.apache.flink.runtime.jobgraph.JobGraph;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;
import java.util.ResourceBundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * REST API of {@link E2dScheduler}
 * Can be used for submitting and scheduling applications
 */

@Path("/e2data")
public class SchedulerService extends AbstractE2DataService {

    private static final Logger logger = Logger.getLogger(SchedulerService.class.getCanonicalName());

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String tmpRootPath = resourceBundle.getString("haier.tmp.path");

    private E2dScheduler scheduler;

    public SchedulerService(){
        scheduler = E2dScheduler.getInstance();
    }

    // just for testing setup
    @Path("/hello")
    @GET
    public Response schedule() {
        String happyMesg = "hello";

        return generateResponse(Response.Status.OK, happyMesg);
    }

    @Path("/nsga2/params")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setNSGAIIParameters(final NSGAIIParameters parameters) {
        logger.info("Just received a PUT request on /e2data/nsga2/params !");
        this.scheduler.configureOptimizer(parameters);

        return generateResponse(Response.Status.NO_CONTENT, "");
    }

    @Path("/nsga2/params")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNSGAIIParameters() {
        logger.info("Just received a GET request on /e2data/nsga2/params !");

        return generateResponse(Response.Status.OK, this.scheduler.retrieveConfiguration());
    }

    @Path("/flink-schedule")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response flink_schedule(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {

        final String filePath = tmpRootPath + fileDetail.getFileName();

        try {
            SchedulerService.persistSerializedJobGraphFile(uploadedInputStream, filePath);
            logger.info("JobGraph file '" + fileDetail.getFileName() + "' has been uploaded successfully and stored in '" + filePath + "'.");
        } catch (IOException e) {
            logger.warning("Error retrieving JobGraph file '" + fileDetail.getFileName() + "':" + e.getMessage());
            e.printStackTrace();
            return generateResponse(Response.Status.INTERNAL_SERVER_ERROR, "Error retrieving JobGraph file '" + fileDetail.getFileName() + "'.");
        }

        JobGraph jobGraph;
        try (final ObjectInputStream objectIn = new ObjectInputStream(new FileInputStream(filePath))) {
            jobGraph = (JobGraph) objectIn.readObject();
            logger.info("Deserialized '" + jobGraph + "' from file '" + filePath + "'.");
        } catch (Exception e) {
            logger.warning("Error deserializing JobGraph from file '" + filePath + "': " + e.getMessage());
            e.printStackTrace();
            return generateResponse(Response.Status.INTERNAL_SERVER_ERROR, "Error deserializing JobGraph from file '" + fileDetail.getFileName() + "'.");
        } finally {
            if (!new File(filePath).delete()) {
                logger.warning("Error unlinking JobGraph file '" + filePath + "'.");
            }
        }

        // FIXME(ckatsak): For now, a default OptimizationPolicy object is
        //                 hardcoded here instead of being sent by Flink.
        //String policyStr = "{\"policy\": {\"objectives\": [ {\"name\":\"execTime\", \"targetFunction\":\"MIN\", \"combineFunction\":\"MAX\"}, {\"name\":\"powerCons\", \"targetFunction\":\"MIN\", \"combineFunction\":\"SUM\"} ]}}";
        String policyStr = "{" +
                            "\"policy\": {" +
                                "\"objectives\": [" +
                                    "{" +
                                        "\"name\": \"execTime\"," +
                                        "\"combineFunction\": \"MAX\"," +
                                        "\"targetFunction\": \"MIN\"" +
                                    "}," +
                                    "{" +
                                        "\"name\": \"powerCons\"," +
                                        "\"combineFunction\": \"SUM\"," +
                                        "\"targetFunction\": \"MIN\"" +
                                    "}" +
                                "]" +
                            "}" +
                        "}";

        final HaierExecutionGraph result = this.scheduler.schedule(jobGraph, OptimizationPolicy.parseJSON(policyStr));

        logger.info("Scheduling result for '" + jobGraph.toString() + "':\n\n" + result.toString() + "\n\nEnd of scheduling result.\n");

        return generateResponse(Response.Status.OK, "JobGraph '" + jobGraph.toString() + "' has been scheduled successfully!");
    }

    private static void persistSerializedJobGraphFile(
            final InputStream uploadedInputStream,
            final String filePath)
            throws IOException {
        final FileOutputStream out = new FileOutputStream(new File(filePath));
        final byte[] bytes = new byte[8192];  // XXX(ckatsak): looks dirty; is it optimal?
        int read;
        while ((read = uploadedInputStream.read(bytes)) != -1) {
            //logger.info("Just read " + read + " bytes from the file being uploaded.");
            out.write(bytes, 0, read);
        }
        out.flush();
        out.close();
    }

}

