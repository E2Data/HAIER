package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.features.TornadoFeatureVectorBean;
import gr.ntua.ece.cslab.e2datascheduler.beans.features.TornadoFeatureVectorRootBean;
import gr.ntua.ece.cslab.e2datascheduler.beans.features.TornadoVirtualDevice;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.udf.HaierObjectInputStreamOfFlinkUDF;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.udf.HaierUDFLoader;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.operators.util.UserCodeObjectWrapper;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * FIXME(ckatsak): Documentation
 */
public class TornadoFeatureExtractor {

    private static final Logger logger = Logger.getLogger(TornadoFeatureExtractor.class.getCanonicalName());

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String virtualDevicesFilePath = resourceBundle.getString("tornado.device.desc.path");
    private static final String featuresOutputFilePath = resourceBundle.getString("tornado.features.dump.path");

    /**
     * A {@link HaierUDFLoader} to allow dynamically loading arbitrary classes from users' JARs
     * at runtime, using the {@link ClassLoader} provided through the constructor as the parent.
     *
     * This is modified on each
     */
    private HaierUDFLoader haierUDFLoader;

    /**
     * A {@link List} of the available devices (i.e., {@link HwResource}s) in the cluster,
     * as previously reported by YARN.
     */
    private final List<HwResource> availableDevices;

    /**
     * A {@link List} of the {@link TornadoVirtualDevice}s configured statically to enable the
     * use of TornadoVM's "fake compilation" functionality.
     */
    private final List<TornadoVirtualDevice> virtualDevices;

    /**
     * Contains a mapping of each available {@link HwResource} in the cluster with one of the
     * statically configured {@link TornadoVirtualDevice}s that are fed into TornadoVM for the
     * "fake compilation".
     */
    private final Map<HwResource, TornadoVirtualDevice> deviceMapping;

    /**
     * Maps the name of each {@link TornadoVirtualDevice} to a {@link List} of {@link HwResource}s
     * that it may refer to, after removing all whitespaces from it (to match the output of
     * TornadoVM's "fake compilation" functionality).
     */
    private final Map<String, List<HwResource>> reverseDeviceMapping;

    /**
     * The {@link List} of {@link TornadoFeatureVector} for each available {@link HwResource} in
     * the cluster, for each offload-able {@link JobVertex} in the {@link JobGraph} examined.
     */
    private final Map<JobVertex, Map<HwResource, List<TornadoFeatureVector>>> featuresMap;


    // --------------------------------------------------------------------------------------------


    public TornadoFeatureExtractor(final List<HwResource> devices) throws IOException {
        if (null == devices) {
            throw new IllegalArgumentException("devices cannot be null");
        }
        this.availableDevices = devices;

        try {
            this.virtualDevices =
                    TornadoFeatureExtractor.parseVirtualDevices(TornadoFeatureExtractor.virtualDevicesFilePath);
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new IOException("Failed to parse the virtual devices file '" +
                    TornadoFeatureExtractor.virtualDevicesFilePath + "': " + e.getMessage());
        }

        this.deviceMapping = TornadoFeatureExtractor.createDeviceMapping(this.availableDevices, this.virtualDevices);
        this.reverseDeviceMapping = TornadoFeatureExtractor.createReverseDeviceMapping(this.deviceMapping);

        this.featuresMap = new HashMap<>();
    }


    // --------------------------------------------------------------------------------------------


    /**
     * Extract the code features for all available {@link HwResource}s and all operators in each
     * "offload-able" {@link JobVertex} in the provided {@link JobGraph}.
     *
     * @param jobGraph    The {@link JobGraph} to examine
     * @param classLoader The {@link ClassLoader} to be used as the parent of an internal {@link HaierUDFLoader}
     *                    to allow dynamically loading arbitrary classes from users' JARs at runtime
     * @return A {@link Map}, keyed by the "offload-able" {@link JobVertex} objects in the provided
     *         {@link JobGraph}, whose values are {@link Map}s keyed by the available {@link HwResource}s
     *         whose values are {@link List}s of {@link TornadoFeatureVector}s: one per operator
     *         in the {@link JobVertex}.
     * @throws IOException If some of the included serialized UDFs cannot be read
     * @throws ClassNotFoundException If some of the included serialized UDFs cannot be loaded
     */
    public synchronized Map<JobVertex, Map<HwResource, List<TornadoFeatureVector>>> extractFrom(
            final JobGraph jobGraph,
            final ClassLoader classLoader
    ) throws IOException, ClassNotFoundException {
        this.haierUDFLoader = new HaierUDFLoader(jobGraph, classLoader);

        for (JobVertex jobVertex : jobGraph.getVerticesSortedTopologicallyFromSources()) {
            if (!HaierExecutionGraph.isComputational(jobVertex)) {
                logger.finest("Skipping JobVertex '" + jobVertex.getID().toString() + "' (named '" +
                        jobVertex.getName() + "') as non-computational");
                continue;
            }
            this.featuresMap.put(jobVertex, this.fakeCompileJobVertex(jobVertex));
        }
        return this.featuresMap;
    }

    /**
     * Extract the code features of all operators in the provided {@link JobVertex}.
     *
     * Part of the functionality is delegated to private method {@code fakeCompileOperator()}.
     *
     * @param jobVertex The {@link JobVertex} to examine
     * @return A {@link Map} where each entry's key is a {@link HwResource} that is available in
     *         the cluster and its value is a {@link List} of {@link TornadoFeatureVector}, one per
     *         operator in the {@link JobVertex} at hand.
     * @throws IOException If some of the included serialized UDFs cannot be read
     * @throws ClassNotFoundException If some of the included serialized UDFs cannot be loaded
     */
    private Map<HwResource, List<TornadoFeatureVector>> fakeCompileJobVertex(final JobVertex jobVertex)
            throws IOException, ClassNotFoundException {
        logger.finest("Fake-compiling JobVertex '" + jobVertex.getID().toString() + "' (named '" +
                jobVertex.getName() + "')...");

        final Configuration config = jobVertex.getConfiguration();
        final ConfigOption<Integer> chainingNumOption = ConfigOptions
                .key("chaining.num")
                .defaultValue(0);
        final int chainingNum = config.getInteger(chainingNumOption);
        final ConfigOption<String> driverClassOption = ConfigOptions
                .key("driver.class")
                .noDefaultValue();

        // In all cases, initialize/allocate the Map entry for the current JobVertex.
        final Map<HwResource, List<TornadoFeatureVector>> vertexFeatureVectors = new HashMap<>();
        this.featuresMap.put(jobVertex, vertexFeatureVectors);
        for (Map.Entry<HwResource, TornadoVirtualDevice> deviceMappingPair : this.deviceMapping.entrySet()) {
            vertexFeatureVectors.put(deviceMappingPair.getKey(), new ArrayList<>(chainingNum + 1));
        }

        // If the JobVertex does *not* contain chained operators, then we should just look up the value of the
        // entry under the key "driver.class". Note that, since we already know that the JobVertex at hand *is*
        // acceleratable, we merely need to identify whether it is a `*MapDriver` or a `*ReduceDriver`.
        if (0 == chainingNum) {
            this.fakeCompileOperator(
                    vertexFeatureVectors,
                    config.getValue(driverClassOption),
                    config.getBytes("udf", null)
            );
            return vertexFeatureVectors;
        }

        // If the "acceleratable" JobVertex at hand contains chained operators, then we should loop through them
        // (mind that this time the first one might be a `DataSource`) and "fake compile" each of them separately.
        if (!jobVertex.getName().startsWith("CHAIN DataSource")) {
            this.fakeCompileOperator(
                    vertexFeatureVectors,
                    config.getValue(driverClassOption),
                    config.getBytes("udf", null)
            );
        }
        // Continue with the subsequent chained operators
        for (int i = 0; i < chainingNum; i++) {
            final ConfigOption chainingTaskOption = ConfigOptions
                    .key("chaining.task." + i)
                    .noDefaultValue();
            this.fakeCompileOperator(
                    vertexFeatureVectors,
                    config.getValue(chainingTaskOption),
                    config.getBytes("chaining.taskconfig." + i + ".udf", null)
            );
        }

        return vertexFeatureVectors;
    }

    /**
     * Extract the code features of an operator of a {@link JobVertex} from its UDF, which is
     * provided as a {@code byte[]} and also update the {@link JobVertex}'s entry in this
     * object's local state.
     *
     * Part of the functionality is delegated to private methods {@code fakeCompileMapOperator()}
     * and {@code fakeCompileReduceOperator()}.
     *
     * @param vertexFeatureVectors This {@link TornadoFeatureExtractor}'s local state for the
     *                             {@link JobVertex} that is currently being examined
     * @param driverClass          Flink's "driver.class" of the operator at hand
     * @param serializedUDF        The UDF of the operator at hand, serialized into a {@code byte[]}
     * @throws IOException If the included serialized UDF cannot be read
     * @throws ClassNotFoundException If the included serialized UDF cannot be loaded
     */
    private void fakeCompileOperator(
            final Map<HwResource, List<TornadoFeatureVector>> vertexFeatureVectors,
            final String driverClass,
            final byte[] serializedUDF
    ) throws IOException, ClassNotFoundException {
        if (null == driverClass) {
            logger.severe("Parameter 'driverClass' cannot be null");
            throw new IllegalArgumentException("Parameter 'driverClass' cannot be null");
        }
        if (null == serializedUDF) {
            logger.severe("Parameter 'serializedUDF' cannot be null");
            throw new IllegalArgumentException("Parameter 'serializedUDF' cannot be null");
        }

        final Map<HwResource, TornadoFeatureVector> results;

        // FIXME(ckatsak): Probably a big switch statement here, to differentiate on the bytecode manipulation
        //                 procedure depending on the `driver.class` at hand. Or maybe just pass the `driver.class` to
        //                 the next two methods and let them have their own smaller switch statements to deal with it.
        if (driverClass.endsWith("MapDriver")) {
            results = this.fakeCompileMapOperator(serializedUDF);
        } else {
            results = this.fakeCompileReduceOperator(serializedUDF);
        }

        for (Map.Entry<HwResource, TornadoFeatureVector> deviceFeatureVectorPair : results.entrySet()) {
            vertexFeatureVectors
                    .get(deviceFeatureVectorPair.getKey())
                    .add(deviceFeatureVectorPair.getValue());
        }
    }

    private Map<HwResource, TornadoFeatureVector> fakeCompileMapOperator(final byte[] serializedUDF)
            throws IOException, ClassNotFoundException {
        final UserCodeObjectWrapper userCodeObjectWrapper;
        final MapFunction udf;
        try (final ObjectInputStream objectInputStream =
                     new HaierObjectInputStreamOfFlinkUDF(new ByteArrayInputStream(serializedUDF), this.haierUDFLoader)
        ) {
            userCodeObjectWrapper = (UserCodeObjectWrapper) objectInputStream.readObject();
            logger.finest("Successfully deserialized the UDF byte array into a UserCodeObjectWrapper");
            udf = (MapFunction) userCodeObjectWrapper.getUserCodeObject();
            logger.finest("Successfully retrieved the user-defined MapFunction from the UserCodeObjectWrapper");
        } catch (final IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error deserializing UDF: " + e.getMessage(), e);
            throw e;
//            final Map<HwResource, TornadoFeatureVector> ret = new HashMap<>();
//            for (Map.Entry<HwResource, TornadoVirtualDevice> deviceMappingPair : this.deviceMapping.entrySet()) {
//                ret.put(deviceMappingPair.getKey(), TornadoFeatureVector.newDummy()); // FIXME(ckatsak): handle error
//            }
//            return ret;
        }

        /*
         * FIXME(ckatsak): Implementation
         *  - switch depending on exact `driver.class`
         *  - bytecode manipulation of the `MapFunction`
         *  - "fake compilation" invocation
         *  - parseTornadoFeatureVectors()
         *  - split TornadoFeatureVectors per HwResource (taking into account this.deviceMapping)
         *  - return the final Map
         */

        // Parse all TornadoFeatureVectors from the output on local filesystem...
        List<TornadoFeatureVector> allTornadoFeatureVectors;
        try {
            allTornadoFeatureVectors = TornadoFeatureExtractor.parseTornadoFeatureVectors(
                    TornadoFeatureExtractor.featuresOutputFilePath
            );
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        // ...group them by the HwResource that each of them refers to...
        final Map<HwResource, List<TornadoFeatureVector>> tornadoFeatureVectorsByHwResource =
                this.groupFeaturesByHwResource(allTornadoFeatureVectors);
        // ...and combine them to create the final Map for the operator
        final Map<HwResource, TornadoFeatureVector> ret = this.combineFeatureVectors(tornadoFeatureVectorsByHwResource);
        return ret;
    }

    private Map<HwResource, TornadoFeatureVector> fakeCompileReduceOperator(final byte[] serializedUDF)
            throws IOException, ClassNotFoundException {
        final UserCodeObjectWrapper userCodeObjectWrapper;
        final ReduceFunction udf;
        try (final ObjectInputStream objectInputStream =
                     new HaierObjectInputStreamOfFlinkUDF(new ByteArrayInputStream(serializedUDF), this.haierUDFLoader)
        ) {
            userCodeObjectWrapper = (UserCodeObjectWrapper) objectInputStream.readObject();
            logger.finest("Successfully deserialized the UDF byte array into a UserCodeObjectWrapper");
            udf = (ReduceFunction) userCodeObjectWrapper.getUserCodeObject();
            logger.finest("Successfully retrieved the user-defined ReduceFunction from the UserCodeObjectWrapper");
        } catch (final IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error deserializing UDF: " + e.getMessage(), e);
            throw e;
//            final Map<HwResource, TornadoFeatureVector> ret = new HashMap<>();
//            for (Map.Entry<HwResource, TornadoVirtualDevice> deviceMappingPair : this.deviceMapping.entrySet()) {
//                ret.put(deviceMappingPair.getKey(), TornadoFeatureVector.newDummy()); // FIXME(ckatsak): handle error
//            }
//            return ret;
        }

        /*
         * FIXME(ckatsak): Implementation
         *  - switch depending on exact `driver.class`
         *  - bytecode manipulation of the `ReduceFunction`
         *  - "fake compilation" invocation
         *  - parseTornadoFeatureVectors()
         *  - split TornadoFeatureVectors per HwResource (taking into account this.deviceMapping)
         *  - return the final Map
         */

        // Parse all TornadoFeatureVectors from the output on local filesystem...
        List<TornadoFeatureVector> allTornadoFeatureVectors;
        try {
            allTornadoFeatureVectors = TornadoFeatureExtractor.parseTornadoFeatureVectors(
                    TornadoFeatureExtractor.featuresOutputFilePath
            );
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        // ...group them by the HwResource that each of them refers to...
        final Map<HwResource, List<TornadoFeatureVector>> tornadoFeatureVectorsByHwResource =
                this.groupFeaturesByHwResource(allTornadoFeatureVectors);
        // ...and combine them to create the final Map for the operator
        final Map<HwResource, TornadoFeatureVector> ret = this.combineFeatureVectors(tornadoFeatureVectorsByHwResource);
        return ret;
    }


    // --------------------------------------------------------------------------------------------


    private Map<HwResource, List<TornadoFeatureVector>> groupFeaturesByHwResource(
            final List<TornadoFeatureVector> tornadoFeatureVectors
    ) {
        final Map<HwResource, List<TornadoFeatureVector>> ret = new HashMap<>();

        for (TornadoFeatureVector tornadoFeatureVector : tornadoFeatureVectors) {
            final String virtualDeviceName = tornadoFeatureVector.getBean().getDevice();
            for (HwResource hwResource : this.reverseDeviceMapping.get(virtualDeviceName)) {
                if (!ret.containsKey(hwResource)) {
                    ret.put(hwResource, new ArrayList<>());
                }
                ret.get(hwResource).add(tornadoFeatureVector);
            }
        }

        return ret;
    }

    /**
     * Combine the {@link TornadoFeatureVector}s in the {@link List} for each {@link HwResource}.
     *
     * For now, it is a single field-by-field addition of the given {@link TornadoFeatureVector}s.
     *
     * @param vectorsByHwResource A {@link Map} that contains each {@link HwResource} and its associated
     *                            {@link List} of {@link TornadoFeatureVector}s
     * @return A {@link Map} that contains the same {@link HwResource}s as the one passed as input, but
     *         mapped to a single {@link TornadoFeatureVector}: the combination of those in the input.
     */
    private Map<HwResource, TornadoFeatureVector> combineFeatureVectors(
            final Map<HwResource, List<TornadoFeatureVector>> vectorsByHwResource
    ) {
        final Map<HwResource, TornadoFeatureVector> ret = new HashMap<>();
        for (Map.Entry<HwResource, List<TornadoFeatureVector>> entry : vectorsByHwResource.entrySet()) {
            ret.put(entry.getKey(), new TornadoFeatureVector(entry.getValue()));
        }
        return ret;
    }


    // --------------------------------------------------------------------------------------------


    /**
     * Parse the {@link List} of {@link TornadoVirtualDevice}s from the virtual devices JSON file
     * configured via the {@code config.properties} file.
     *
     * @return The {@link List} of {@link TornadoVirtualDevice}s present in the cluster
     * @throws IOException If the JSON deserialization fails for any reason, propagated from jackson
     */
    public static List<TornadoVirtualDevice> parseVirtualDevices(final String virtualDevicesFilePath)
            throws IOException {
        final File virtualDevicesFile = new File(virtualDevicesFilePath);
        final ObjectMapper objectMapper = new ObjectMapper();
        final TornadoVirtualDevice[] tornadoVirtualDevices = objectMapper.readValue(
                virtualDevicesFile,
                TornadoVirtualDevice[].class
        );
        return new ArrayList<>(Arrays.asList(tornadoVirtualDevices));
    }
    // NOTE(ckatsak): Quick & dirty deserialization test
//    public static void main(String[] args) throws IOException {
//        System.out.println(TornadoFeatureExtractor.parseVirtualDevices(TornadoFeatureExtractor.virtualDevicesFilePath));
//    }

    /**
     * Create a mapping between devices in the cluster (as reported by YARN, i.e., in the form of
     * {@link HwResource}s) and virtual devices used by TornadoVM for its "fake compilation"
     * functionality (i.e., {@link TornadoVirtualDevice}s).
     *
     * @param availableDevices A {@link List} of {@link HwResource}s that represent the devices as reported by YARN
     * @param virtualDevices   A {@link List} of {@link TornadoVirtualDevice} that represent the virtual devices fed
     *                         into TornadoVM for its "fake compilation" utility
     * @return A {@link Map} to match each one of the provided {@link HwResource}s to one of the provided
     *         {@link TornadoVirtualDevice}s, indexed by the former
     */
    public static Map<HwResource, TornadoVirtualDevice> createDeviceMapping(
            final List<HwResource> availableDevices,
            final List<TornadoVirtualDevice> virtualDevices) {
        // First, transform the device name of every TornadoVirtualDevice so as to resemble those
        // reported by YARN and index all TornadoVirtualDevices by their transformed device name.
        final Map<String, TornadoVirtualDevice> virtualDeviceTransformedNames = new HashMap<>();
        for (TornadoVirtualDevice virtualDevice : virtualDevices) {
            final String virtualDeviceType = virtualDevice.getDeviceType().toLowerCase();
            // In the case of a GPU or an FPGA, prepend YARN's standard prefix
            // and transform it in a way similar to what we did in YARN. E.g.:
            //      "Tesla V100-SXM2-32GB" --> "yarn.io/gpu-teslav100sxm232gb",
            //      "GeForce GTX 1060 6GB" --> "yarn.io/gpu-geforcegtx10606gb".
            if (virtualDeviceType.endsWith("gpu")) {
                virtualDeviceTransformedNames.put(
                        "yarn.io/gpu-" + virtualDevice
                                .getDeviceName()
                                .replaceAll(" ", "")
                                .replaceAll("-", "")
                                .toLowerCase(),
                        virtualDevice
                );
            } else if (virtualDeviceType.endsWith("fpga")) {
                virtualDeviceTransformedNames.put(
                        "yarn.io/fpga-" + virtualDevice
                                .getDeviceName()
                                .replaceAll(" ", "")
                                .replaceAll("-", "")
                                .toLowerCase(),
                        virtualDevice
                );
            } else if (virtualDeviceType.endsWith("cpu")) {
                // In the case of a CPU, prepend the number of cores to the standard (in YARN) "vcores" name.
                virtualDeviceTransformedNames.put(
                        virtualDevice.getAvailableProcessors() + "-vcores",
                        virtualDevice
                );
            } else {
                throw new IllegalStateException("TornadoVirtualDevice of unknown deviceType '" +
                        virtualDeviceType + "': '" + virtualDevice + "'");
            }
        }

        // Initialize a List for any HwResource that isn't matched with a TornadoVirtualDevice.
        final List<HwResource> unmatchedHwResources = new ArrayList<>();
        // Also initialize a Map for the result mapping to be returned.
        final Map<HwResource, TornadoVirtualDevice> ret = new HashMap<>();
        // Loop through every available HwResource...
        for (HwResource hwResource : availableDevices) {
            // ...reconstruct the name of a hypothetical matching TornadoVirtualDevice...
            final String tornadoVirtualDeviceExpectedName =
                    (hwResource.getName().equals("vcores"))
                            ? hwResource.getValue() + "-vcores"
                            : hwResource.getName();
            // ...and check if it's already in the Map. If it is, then put it in the result mapping.
            // Otherwise, put it in the List of the unmatched HwResources and log the upcoming failure.
            if (virtualDeviceTransformedNames.containsKey(tornadoVirtualDeviceExpectedName)) {
                ret.put(
                        hwResource,
                        virtualDeviceTransformedNames.get(tornadoVirtualDeviceExpectedName)
                );
                // Although multiple HwResources of the same model may exist and be available in the
                // cluster, but they will be reported only once in the virtual devices JSON file.
                // Therefore, eagerly removing TornadoVirtualDevices from the temp Map after
                // they have been matched with a HwResource would probably be unsafe:
//                virtualDeviceTransformedNames.remove(tornadoVirtualDeviceExpectedName);
            } else {
                logger.severe("HwResource '" + hwResource.getName() + "@" + hwResource.getHost() +
                        "' seems to have no candidate TornadoVirtualDevice to be matched with!");
                unmatchedHwResources.add(hwResource);
            }
        }
        // If this List isn't empty by the end of the mapping, there will be unrecoverable
        // errors, since there will be HwResources against whom TornadoVM will not be
        // capable of "fake compiling" to retrieve the required code features.
        if (0 != unmatchedHwResources.size()) {
            String unmatchedHwResourcesMsg = "Unmatched HwResources:\n";
            for (HwResource unmatchedHwResource : unmatchedHwResources) {
                unmatchedHwResourcesMsg += "\t- " + unmatchedHwResource.toString() + "\n";
            }
            logger.severe(unmatchedHwResourcesMsg);
            throw new IllegalStateException(
                    "No HwResources can remain unmatched with a TornadoVirtualDevice; " + unmatchedHwResourcesMsg);
        }
        return ret;
    }

    /**
     * Based on the provided {@link Map} between {@link HwResource}s and {@link TornadoVirtualDevice}s,
     * create a mapping between the name of each {@link TornadoVirtualDevice} (after removing any whitespace
     * character in it, to match it with the output of TornadoVM's "fake compilation") and any number of the
     * given {@link HwResource}s that may be referred by it.
     *
     * @param deviceMapping The {@link Map} between {@link HwResource}s and {@link TornadoVirtualDevice}s
     * @return A {@link Map} where each entry maps a {@link TornadoVirtualDevice} name (whitespace removed,
     *         as a {@link String}) and a {@link List} of {@link HwResource}s that may be referred by it
     */
    public static Map<String, List<HwResource>> createReverseDeviceMapping(
            final Map<HwResource, TornadoVirtualDevice> deviceMapping) {
        final Map<String, List<HwResource>> ret = new HashMap<>();
        for (Map.Entry<HwResource, TornadoVirtualDevice> deviceMappingPair : deviceMapping.entrySet()) {
            final String name = deviceMappingPair.getValue().getDeviceName().replaceAll(" ", "");
            if (!ret.containsKey(name)) {
                ret.put(name, new ArrayList<>());
            }
            ret.get(name).add(deviceMappingPair.getKey());
        }
        return ret;
    }

    /**
     * <pre>
     * FIXME(ckatsak): Implementation
     *  - Parse the file where TornadoVM spits its "fake compilation" results (i.e., the code features)
     *  - Construct the appropriate TornadoFeatureVector objects from the parsed TornadoFeatureVectorBean objects
     *   * May need to combine multiple TornadoFeatureVectorBean objects into a single TornadoFeatureVector for each
     *     VirtualDevice that the "fake compilation" happens for, although this might not be the best place to do that
     * </pre>
     *
     * Parse the output of TornadoVM's "fake compilation" functionality that is stored in the file
     * at the provided path {@code fileName}.
     *
     * @param filePath The file where TornadoVM's output is stored
     * @return A {@link List} of the {@link TornadoFeatureVector}s that have been parsed from the file
     * @throws FileNotFoundException If the provided file path is not present in the local filesystem
     * @throws JsonParseException    If an error occurs during JSON deserialization, e.g., mainly due
     *                               to nonconformity with the spec
     * @throws IOException           If any other parsing or deserialization error occurs
     */
    public static List<TornadoFeatureVector> parseTornadoFeatureVectors(final String filePath)
            throws FileNotFoundException, JsonParseException, IOException {
        // Parse all TornadoFeatureVectorRootBeans scattered in the input file and collect them into a List.
        final List<TornadoFeatureVectorRootBean> rootBeans = new ArrayList<>();
        try (final FileInputStream fis = new FileInputStream(filePath)) {
            final JsonFactory jsonFactory = new JsonFactory();
            final JsonParser jsonParser = jsonFactory.createParser(fis);
            jsonParser.setCodec(new ObjectMapper());
            jsonParser.nextToken();
            while (jsonParser.hasCurrentToken()) {
                rootBeans.add(jsonParser.readValueAs(TornadoFeatureVectorRootBean.class));
                jsonParser.nextToken();
            }
        } catch (final FileNotFoundException e) {
            logger.log(Level.SEVERE, "Error opening file '" + filePath + "': " + e.getMessage(), e);
            throw e;
        } catch (final JsonParseException e) {
            logger.log(Level.SEVERE, "Error during JSON parsing: " + e.getMessage(), e);
            throw e;
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }

        // Using the TornadoFeatureVectorBean of every TornadoFeatureVectorRootBean, create
        // the TornadoFeatureVectors and collect them into the List to be returned.
        final List<TornadoFeatureVector> tornadoFeatureVectors = new ArrayList<>(rootBeans.size());
        for (TornadoFeatureVectorRootBean rootBean : rootBeans) {
            // There should always be one TornadoFeatureVectorBean per TornadoFeatureVectorRootBean, so...
            for (TornadoFeatureVectorBean bean : rootBean.getTornadoFeatureVectorBeanMap().values()) {
                tornadoFeatureVectors.add(new TornadoFeatureVector(bean));
            }
        }
        return tornadoFeatureVectors;
    }


    // --------------------------------------------------------------------------------------------


    // NOTE(ckatsak): Quick & dirty test of static methods
    public static void main(String[] args) throws IOException {
        final List<HwResource> hwResources = new ArrayList<>(3);
        final HwResource r1 = new HwResource();
        r1.setName("yarn.io/gpu-teslav100sxm232gb");
        r1.setHost("silver1.cslab.ece.ntua.gr");
        r1.setValue(1);
        hwResources.add(r1);
        final HwResource r2 = new HwResource();
        r2.setName("yarn.io/gpu-geforcegtx10606gb");
        r2.setHost("silver1.cslab.ece.ntua.gr");
        r2.setValue(1);
        hwResources.add(r2);
        final HwResource r3 = new HwResource();
        r3.setName("vcores");
        r3.setHost("silver1.cslab.ece.ntua.gr");
        r3.setValue(40);
        hwResources.add(r3);
        final HwResource r4 = new HwResource();
        r4.setName("yarn.io/gpu-teslav100sxm232gb");
        r4.setHost("silver1.cslab.ece.ntua.gr");
        r4.setValue(1);
        hwResources.add(r4);

        final Map<HwResource, TornadoVirtualDevice> ret = TornadoFeatureExtractor.createDeviceMapping(
                hwResources,
                TornadoFeatureExtractor.parseVirtualDevices(TornadoFeatureExtractor.virtualDevicesFilePath)
        );
        for (Map.Entry<HwResource, TornadoVirtualDevice> entry : ret.entrySet()) {
            System.out.println("\n" + entry.getKey() + "\t-->\t" + entry.getValue());
        }

        System.out.println("================================ TornadoFeatureVectors ================================");
        final List<TornadoFeatureVector> tornadoFeatureVectors =
                TornadoFeatureExtractor.parseTornadoFeatureVectors(TornadoFeatureExtractor.featuresOutputFilePath);
        for (TornadoFeatureVector tornadoFeatureVector : tornadoFeatureVectors) {
            System.out.println(tornadoFeatureVector);
        }
        System.out.println("\nTotal number: " + tornadoFeatureVectors.size());

        System.out.println("================================ Reverse Device Mapping ===============================");
        System.out.println(TornadoFeatureExtractor.createReverseDeviceMapping(ret));
    }

}
