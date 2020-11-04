package gr.ntua.ece.cslab.e2datascheduler.ws;

import gr.ntua.ece.cslab.e2datascheduler.E2dScheduler;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.SerializableScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.beans.gui.CandidatePlan;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.beans.profiling.TornadoProfilingInfoRoot;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.udf.DummyEnvironment;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.udf.HaierObjectInputStreamOfFlinkUDF;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.udf.HaierUDFLoader;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.NSGAIIParameters;
import gr.ntua.ece.cslab.e2datascheduler.util.HaierLogHandler;
import gr.ntua.ece.cslab.e2datascheduler.util.SelectionQueue;

import org.apache.flink.api.common.functions.Function;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.util.UserCodeObjectWrapper;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.blob.PermanentBlobKey;
import org.apache.flink.runtime.execution.Environment;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.runtime.jobgraph.tasks.AbstractInvokable;
import org.apache.flink.runtime.jobmanager.scheduler.CoLocationGroup;
import org.apache.flink.runtime.jobmanager.scheduler.SlotSharingGroup;
import org.apache.flink.runtime.operators.BatchTask;
import org.apache.flink.runtime.operators.Driver;
import org.apache.flink.runtime.operators.chaining.ChainedDriver;
import org.apache.flink.runtime.operators.util.TaskConfig;
import org.apache.flink.util.InstantiationUtil;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * REST API of {@link E2dScheduler}
 * Can be used for submitting and scheduling applications
 */
@Path("/e2data")
public class SchedulerService extends AbstractE2DataService {

    private static final Logger logger = Logger.getLogger(SchedulerService.class.getCanonicalName());

    public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String tmpRootPath = resourceBundle.getString("haier.tmp.path");

    private final E2dScheduler scheduler;


    // --------------------------------------------------------------------------------------------


    public SchedulerService() {
        scheduler = E2dScheduler.getInstance();
    }

    // just for testing setup
    @Path("/hello")
    @GET
    public Response hello() {
        String happyMesg = "hello";

        return generateResponse(Response.Status.OK, happyMesg);
    }


    // --------------------------------------------------------------------------------------------


    @Path("/nsga2/params")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNSGAIIParameters() {
        logger.finest("Just received a GET request on /e2data/nsga2/params !");

        return generateResponse(Response.Status.OK, this.scheduler.retrieveConfiguration());
    }

    @Path("/nsga2/params")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setNSGAIIParameters(final NSGAIIParameters parameters) {
        logger.finest("Just received a PUT request on /e2data/nsga2/params !");

        if (null == parameters) {
            logger.info("NSGAIIParameters received by GUI is null");
            return generateResponse(Response.Status.BAD_REQUEST, "");
        }

        this.scheduler.configureOptimizer(parameters);
        return generateResponse(Response.Status.NO_CONTENT, "");
    }


    // --------------------------------------------------------------------------------------------


    @Path("/nsga2/{jobId}/plans")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlans(@PathParam("jobId") final String jobId) {
        logger.finest("Just received a GET request on /e2data/nsga2/" + jobId + "/plans !");

        // Retrieve Job's SelectionQueue. If it doesn't exist (just yet, possibly) return 404 NOT FOUND.
        final SelectionQueue<CandidatePlan> plansQueue = this.scheduler.getSelectionQueue(jobId);
        if (null == plansQueue) {
            return generateResponse(Response.Status.NOT_FOUND, "");
        }

        // If the SelectionQueue already exists (i.e., the Job in question is already being tracked by HAIER),
        // make an attempt to retrieve all execution plans calculated by the NSGAIIOptimizer.
        List<CandidatePlan> candidatePlans = null;
        try {
            candidatePlans = plansQueue.retrieveOptions(500);
        } catch (InterruptedException e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
            return generateResponse(Response.Status.INTERNAL_SERVER_ERROR, "InterruptedException");
        }

        // If no execution plans are available after 500ms return 202 ACCEPTED to prompt the GUI to retry later.
        if (null == candidatePlans) {
            logger.info("Candidate execution plans for Job " + jobId + " appear not to be ready to be sent yet.");
            return generateResponse(Response.Status.ACCEPTED, "");
        }
        logger.info("The list of candidate execution plans is being sent.");
        return generateResponse(Response.Status.OK, candidatePlans);
    }

    @Path("/nsga2/{jobId}/plans")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setPlan(@PathParam("jobId") final String jobId, final CandidatePlan candidatePlan) {
        logger.finest("Just received a POST request on /e2data/nsga2/" + jobId + "/plans !");

        // Make sure the input CandidatePlan is OK.
        if (null == candidatePlan) {
            logger.warning("CandidatePlan sent is null.");
            return generateResponse(Response.Status.BAD_REQUEST, "");
        }
        logger.finer("CandidatePlan selected by the GUI: " + candidatePlan);

        // Retrieve Job's SelectionQueue. It should definitely exist by now (since a GET must have been preceded),
        // hence the error in case of failure.
        final SelectionQueue<CandidatePlan> plansQueue = this.scheduler.getSelectionQueue(jobId);
        if (null == plansQueue) {
            logger.severe("SelectionQueue for job " + jobId + " could not be found.");
            return generateResponse(Response.Status.INTERNAL_SERVER_ERROR, "");
        }

        plansQueue.submitChoice(candidatePlan);
        return generateResponse(Response.Status.NO_CONTENT, "");
    }


    // --------------------------------------------------------------------------------------------


    @Path("/logs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogs() {
        logger.finest("Just received a GET request on /e2data/logs !");

        final BlockingQueue<String> logsQueue = HaierLogHandler.getLogsQueue();
        final List<String> readyLogs = new LinkedList<>();
        while (null != logsQueue.peek()) {
            final String logMsg = logsQueue.poll();
            readyLogs.add(Base64.getEncoder().encodeToString(logMsg.getBytes()));
        }

        return generateResponse(Response.Status.OK, readyLogs);
    }


    // --------------------------------------------------------------------------------------------


    @Path("/profiling")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response profilingInfo(final TornadoProfilingInfoRoot tornadoProfilingInfoRoot) {
        logger.finest("Just received a POST request on /e2data/profiling/" + " !");

        // Make sure the input TornadoProfilingInfoRoot is OK.
        if (null == tornadoProfilingInfoRoot) {
            logger.warning("The TornadoProfilingInfoRoot object received appears to be null.");
            return generateResponse(Response.Status.BAD_REQUEST, "");
        }
        logger.finer("TornadoProfilingInfoRoot object received by Tornado: " + tornadoProfilingInfoRoot);

        // FIXME(ckatsak): Make use of it, somehow!
        //                  - 500 INTERNAL SERVER ERROR on processing failure

        return generateResponse(Response.Status.NO_CONTENT, "");
    }


    // --------------------------------------------------------------------------------------------


    @Path("/flink-schedule")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response flink_schedule(
            //@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") final FormDataBodyPart formDataBodyPart,
            @FormDataParam("file") final FormDataContentDisposition fileDetail) {
        final InputStream uploadedInputStream = formDataBodyPart.getValueAs(InputStream.class);
        logger.finest("Just received a POST request on /e2data/flink-schedule !");

        final String filePath = tmpRootPath + fileDetail.getFileName();
        try {
            SchedulerService.persistSerializedJobGraphFile(uploadedInputStream, filePath);
            logger.info("JobGraph file '" + fileDetail.getFileName() +
                    "' has been uploaded successfully and stored in '" + filePath + "'.");
        } catch (IOException e) {
            logger.warning("Error retrieving JobGraph file '" + fileDetail.getFileName() + "':" + e.getMessage());
            e.printStackTrace();
            return generateResponse(Response.Status.INTERNAL_SERVER_ERROR,
                                    "Error retrieving JobGraph file '" + fileDetail.getFileName() + "'.");
        }

        JobGraph jobGraph;
        try (final ObjectInputStream objectIn = new ObjectInputStream(new FileInputStream(filePath))) {
            jobGraph = (JobGraph) objectIn.readObject();
            logger.info("Deserialized '" + jobGraph + "' from file '" + filePath + "'.");
        } catch (Exception e) {
            logger.warning("Error deserializing JobGraph from file '" + filePath + "': " + e.getMessage());
            e.printStackTrace();
            return generateResponse(Response.Status.INTERNAL_SERVER_ERROR,
                                    "Error deserializing JobGraph from file '" + fileDetail.getFileName() + "'.");
        } finally {
            if (!new File(filePath).delete()) {
                logger.warning("Error unlinking JobGraph file '" + filePath + "'.");
            }
        }

        // TODO(ckatsak): For now, a default OptimizationPolicy object is
        //                hardcoded here instead of being sent by Flink.
        //final String policyStr = "{\"policy\": {\"objectives\": [ {\"name\":\"execTime\", \"targetFunction\":\"MIN\", \"combineFunction\":\"MAX\"}, {\"name\":\"powerCons\", \"targetFunction\":\"MIN\", \"combineFunction\":\"SUM\"} ]}}";
        final String policyStr = "{" +
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

        this.debuggingInspection(jobGraph); // FIXME(ckatsak) debugging logging

        final E2dScheduler.SchedulingResult result =
                this.scheduler.schedule(jobGraph, OptimizationPolicy.parseJSON(policyStr));
        if (null == result) {
            logger.severe("E2dScheduler.SchedulingResult is null; something serious is going on!");
            return generateResponse(Response.Status.INTERNAL_SERVER_ERROR, "");
        }
        if (result.isEmpty()) {
            logger.info("It looks like none of the JobVertices in '" + jobGraph.toString() +
                    "' can be offloaded to an accelerator.");
            return generateResponse(Response.Status.OK, new ArrayList<>(0));
        }
        final HaierExecutionGraph resultGraph = result.getResult();
        if (null == resultGraph) {
            logger.severe("E2dScheduler.SchedulingResult.getResult() returned null!");
            logger.severe("Error allocating resources for '" + jobGraph.toString() + "'");
            return generateResponse(Response.Status.INTERNAL_SERVER_ERROR, "");
        }

        logger.info("Scheduling result for '" + jobGraph.toString() + "':\n" + result);

        final List<SerializableScheduledJobVertex> responseBody = resultGraph.toSerializableScheduledJobVertexList();
        logger.finest("Response body returned to Flink for '" + jobGraph.toString() + "':\n" +
                new GsonBuilder().setPrettyPrinting().create().toJson(responseBody));
        return generateResponse(Response.Status.OK, responseBody);
    }

    private static void persistSerializedJobGraphFile(
            final InputStream uploadedInputStream,
            final String filePath)
            throws IOException {
        final FileOutputStream out = new FileOutputStream(new File(filePath));
        final byte[] bytes = new byte[8192];
        int read;
        while ((read = uploadedInputStream.read(bytes)) != -1) {
            logger.finest("Read " + read + " bytes from the uploaded serialized JobGraph.");
            out.write(bytes, 0, read);
        }
        out.flush();
        out.close();
    }


    // --------------------------------------------------------------------------------------------


    private void debuggingInspection(final JobGraph jobGraph) {
        logger.info("JVM Runtime Information:\n" + E2dScheduler.JVMRuntimeInfo());
///        for (JobVertex vertex : jobGraph.getVerticesSortedTopologicallyFromSources()) {
///            String str = "JobVertex: " + vertex.getID().toString() + ", " + vertex.getName() + "\n";
///            HashMap confData = (HashMap) vertex.getConfiguration().toMap();
///            if (confData.containsKey("driver.class")) {
///                str += "CKATSAK: driver.class is \"" + confData.get("driver.class").toString() + "\"\n";
///            }
///            if (confData.containsKey("udf")) {
///                str += "CKATSAK: found a UDF -- (getClass(): " + confData.get("udf").getClass() + ")\n";
///                //if (confData.get("udf") instanceof byte[]) {
///                //    byte[] udf = (byte[]) confData.get("udf");
///                //    str += "CKATSAK: (udf size == " + udf.length + " bytes)\n";
///                //}
///                str += "CKATSAK: (UDF = " + confData.get("udf") + ")\n";
///            }
///            logger.finest(str);
///        }

        final HaierUDFLoader haierUDFLoader = new HaierUDFLoader(jobGraph, this.getClass().getClassLoader());
        final List<Class<?>> mapClasses = haierUDFLoader.getAllUserDefinedMapFunctions();
        final List<Class<?>> reduceClasses = haierUDFLoader.getAllUserDefinedReduceFunctions();

        for (JobVertex vertex : jobGraph.getVerticesSortedTopologicallyFromSources()) {
            // General information about the JobVertex at hand:
            String inspectionMsg = "JobVertex: " + vertex.getID().toString() + "\n";
            inspectionMsg += new GsonBuilder().setPrettyPrinting().create().toJson(vertex.getID()) + "\n";
            inspectionMsg += "name: \"" + vertex.getName() + "\"\n";
            final SlotSharingGroup ssg = vertex.getSlotSharingGroup();
            final CoLocationGroup clg = vertex.getCoLocationGroup();
            inspectionMsg += "SlotSharingGroup: " + (ssg != null ? ssg.toString() : null) +
                    ";\nCoLocationGroup: " + (clg != null ? clg.toString() : null) + "\n";
            // Create the `ConfigOption` objects to query the `Configuration` object:
            final ConfigOption<String> driverClassOption = ConfigOptions
                    .key("driver.class")
                    .noDefaultValue();
//            final ConfigOption<byte[]> udfOption = ConfigOptions
//                    .key("udf")
//                    .defaultValue(null);
            // Use the `ConfigOption`s to query the `Configuration`:
            if (vertex.getConfiguration().contains(driverClassOption)) {
                final String driverClass = vertex.getConfiguration().getValue(driverClassOption);
                inspectionMsg += "CKATSAK: driver.class is \"" + driverClass + "\"\n";
            }
            final byte[] udf = vertex.getConfiguration().getBytes("udf", null);
            inspectionMsg += "CKATSAK: found a UDF (size = " + (udf != null ? udf.length : 0) + "):\n" + udf;
            logger.finest(inspectionMsg);

            inspectionMsg = "";
            final HashMap<String, String> confData = (HashMap<String, String>) vertex.getConfiguration().toMap();
            for (Map.Entry<String, String> entry : confData.entrySet()) {
                inspectionMsg += "\"" + entry.getKey() + "\" : \"" + entry.getValue() + "\"\n";
            }
            logger.finest("Whole configuration:\n" + inspectionMsg);

            logger.finest("(JobVertex-" + vertex.getID().toString() + ").getInvokableClassName(): " +
                    vertex.getInvokableClassName() + "\n");

            // ====================================================================================
            //   vvv   User's lambda deserialization   vvv
            // ====================================================================================

            inspectionMsg = "";
            if (null != udf && vertex.getConfiguration().contains(driverClassOption)) {
                final String driverClass = vertex.getConfiguration().getValue(driverClassOption);
                if (driverClass.endsWith("MapDriver")) {
                    final UserCodeObjectWrapper userCodeObjectWrapper;
                    final MapFunction mapLambda;
                    try (final ObjectInputStream objectInputStream =
                                 new HaierObjectInputStreamOfFlinkUDF(new ByteArrayInputStream(udf), haierUDFLoader)
                    ) {
                        userCodeObjectWrapper = (UserCodeObjectWrapper) objectInputStream.readObject();
                        inspectionMsg += "Successfully deserialized the UDF byte array: " + userCodeObjectWrapper + "\n";
                        mapLambda = (MapFunction) userCodeObjectWrapper.getUserCodeObject();
                        inspectionMsg += "Successfully retrieved the Map lambda from it: " + mapLambda + "\n";
                    } catch (final IOException | ClassNotFoundException e) {
                        final StringWriter sw = new StringWriter();
                        inspectionMsg += "Error deserializing the UDF byte array... :\n";
                        e.printStackTrace(new PrintWriter(sw));
                        inspectionMsg += sw.toString();
                    }
                    logger.finest(inspectionMsg + "\n\n");
                }
            }

            //this.udfFromJobGraph(vertex);

            // ====================================================================================
            //   ^^^   User's lambda deserialization   ^^^
            // ====================================================================================
        }
        HaierExecutionGraph.logOffloadability(jobGraph);

        // ========================================================================================
        //   vvv   User's JAR inspection/manipulation   vvv
        // ========================================================================================

        final List<org.apache.flink.core.fs.Path> jars = new ArrayList<>();
        String userJarsPaths = "User Jars Paths:\n";
        for (org.apache.flink.core.fs.Path p : jobGraph.getUserJars()) {
            userJarsPaths += "- " + p.getName() + " @ " + p.getPath() + "\n";
            jars.add(p);
        }
        logger.finest(userJarsPaths);
        userJarsPaths = "User Jar BLOB Keys:\n";
        for (PermanentBlobKey p : jobGraph.getUserJarBlobKeys()) {
            userJarsPaths += "- " + p.toString() + "\n";
        }
        logger.finest(userJarsPaths);

        logger.finest("jobGraph.hasUsercodeJarFiles() = " + jobGraph.hasUsercodeJarFiles());

        String inspectionMsg = "Scanning JAR files for .class files:";
        for (org.apache.flink.core.fs.Path path : jars) {
            inspectionMsg += "\n* Class names in '" + path.getPath() + "':\n";
            try {
                final List<String> foundClasses = HaierUDFLoader.scanJarFileForClasses(new File(path.getPath()));
                for (String className : foundClasses) {
                    inspectionMsg += "\t- " + className + "\n";
                }
            } catch (IOException e) {
                final StringWriter sw = new StringWriter();
                inspectionMsg += "Error scanning for .class files in '" + path.getPath() + "' ... :\n";
                e.printStackTrace(new PrintWriter(sw));
                inspectionMsg += sw.toString() + "";
            }
        }
        logger.finest(inspectionMsg + "\n");

        inspectionMsg = "\n* User-defined classes implementing `MapFunction`:\n";
        for (Class<?> clazz : haierUDFLoader.getAllUserDefinedMapFunctions()) {
            inspectionMsg += "\t- " + clazz.toString() + "\n";
        }
        inspectionMsg += "\n* User-defined classes implementing `ReduceFunction`:\n";
        for (Class<?> clazz : haierUDFLoader.getAllUserDefinedReduceFunctions()) {
            inspectionMsg += "\t- " + clazz.toString() + "\n";
        }
        logger.finest(inspectionMsg);

        // ========================================================================================
        //   ^^^   User's JAR inspection/manipulation   ^^^
        // ========================================================================================
    }

    /*
     * FIXME(ckatsak): UDF extraction from JobGraph; only temporarily here
     */
    private void udfFromJobGraph(final JobVertex vertex) {
        final ClassLoader classLoader = getClass().getClassLoader();
        final Configuration taskConf = vertex.getConfiguration();
        final Environment environment = new DummyEnvironment(taskConf, classLoader);
        final Class<? extends AbstractInvokable> invokableClass = vertex.getInvokableClass(classLoader);
        final TaskConfig taskConfig = new TaskConfig(taskConf);

        try {
            final Constructor<? extends AbstractInvokable> statelessCtor = invokableClass.getConstructor(Environment.class);
            final AbstractInvokable invokable = statelessCtor.newInstance(environment);

            // Get non-chained Function
            if (taskConfig.getNumberOfChainedStubs() == 0 && taskConfig.hasStubWrapper()) {
                final Class<? extends Driver<Function, Object>> driverClass = taskConfig.getDriver();
                final Driver driver = InstantiationUtil.instantiate(driverClass, Driver.class);

                if (invokable instanceof BatchTask) {
                    driver.setup((BatchTask) invokable);
                }

                final Class stubType = driver.getStubType();
                final Function stub =
                        (Function) taskConfig.getStubWrapper(classLoader).getUserCodeObject(stubType, classLoader);
                final String stubStr = stub.toString();
                logger.finest(stubStr);
            }

            // Get chained Function(s)
            // Note: currently getting a NullPointerException in "ct.setup()" due to incomplete shortcuts
            int numChained = taskConfig.getNumberOfChainedStubs();
            for (int i = 0; i < numChained; ++i) {
                final ChainedDriver<?, ?> ct;
                try {
                    final Class<? extends ChainedDriver<?, ?>> ctc = taskConfig.getChainedTask(i);
                    ct = ctc.newInstance();
                    final String taskName = taskConfig.getChainedTaskName(i);
                    logger.finest(taskName);

                    ct.setup(taskConfig, "DummyTask", null,
                            invokable, classLoader, environment.getExecutionConfig(), new HashMap());
                    final Function stub = ct.getStub();
                    final String stubStr = stub.toString();
                    logger.finest(stubStr);
                } catch (final Exception ex) {
                    throw new RuntimeException("Could not instantiate chained task driver.", ex);
                }
            }
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

}

