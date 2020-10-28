package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.File;
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
    private static final String virtualDevicesFilePath = resourceBundle.getString("tornado.virtual.devices.path");

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

    private final Map<TornadoVirtualDevice, HwResource> deviceMapping;

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
        this.featuresMap = new HashMap<>();
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

    private static Map<TornadoVirtualDevice, HwResource> createDeviceMapping(
            final List<HwResource> availableDevices,
            final List<TornadoVirtualDevice> virtualDevices) {
        /*
         * FIXME(ckatsak): Implementation
         *  - Figure out how YARN transforms device names when they are retrieved through OpenCL API;
         *  - Transform device names reported by TornadoVM the same way to create the mapping.
         */
        return null; // FIXME(ckatsak)
    }

    private static List<TornadoFeatureVector> parseTornadoFeatureVectors() {
        /*
         * FIXME(ckatsak): Implementation
         *  - Parse the file where TornadoVM spits its "fake compilation" results (i.e., the code features)
         *  - Construct the appropriate TornadoFeatureVector objects from the parsed TornadoFeatureVectorBean objects
         *   * May need to combine multiple TornadoFeatureVectorBean objects into a single TornadoFeatureVector for
         *     each VirtualDevice that the "fake compilation" happens for
         */
        return null; // FIXME(ckatsak)
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
     */
    public synchronized Map<JobVertex, Map<HwResource, List<TornadoFeatureVector>>> extractFrom(
            final JobGraph jobGraph,
            final ClassLoader classLoader) {
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
     */
    private Map<HwResource, List<TornadoFeatureVector>> fakeCompileJobVertex(final JobVertex jobVertex) {
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
        for (Map.Entry<TornadoVirtualDevice, HwResource> deviceMappingPair : this.deviceMapping.entrySet()) {
            vertexFeatureVectors.put(deviceMappingPair.getValue(), new ArrayList<>(chainingNum + 1));
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
     */
    private void fakeCompileOperator(
            final Map<HwResource, List<TornadoFeatureVector>> vertexFeatureVectors,
            final String driverClass,
            final byte[] serializedUDF) {
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

    private Map<HwResource, TornadoFeatureVector> fakeCompileMapOperator(final byte[] serializedUDF) {
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

            final Map<HwResource, TornadoFeatureVector> ret = new HashMap<>();
            for (Map.Entry<TornadoVirtualDevice, HwResource> deviceMappingPair : this.deviceMapping.entrySet()) {
                ret.put(deviceMappingPair.getValue(), TornadoFeatureVector.newDummy()); // FIXME(ckatsak): handle error
            }
            return ret;
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
        return null; // FIXME(ckatsak)
    }

    private Map<HwResource, TornadoFeatureVector> fakeCompileReduceOperator(final byte[] serializedUDF) {
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

            final Map<HwResource, TornadoFeatureVector> ret = new HashMap<>();
            for (Map.Entry<TornadoVirtualDevice, HwResource> deviceMappingPair : this.deviceMapping.entrySet()) {
                ret.put(deviceMappingPair.getValue(), TornadoFeatureVector.newDummy()); // FIXME(ckatsak): handle error
            }
            return ret;
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
        return null; // FIXME(ckatsak)
    }

}
