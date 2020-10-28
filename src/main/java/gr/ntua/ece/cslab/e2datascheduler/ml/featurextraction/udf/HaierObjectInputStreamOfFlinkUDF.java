package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.udf;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Logger;


/**
 * Deserialize a Flink UDF using an instance of {@link HaierUDFLoader}.
 */
public final class HaierObjectInputStreamOfFlinkUDF extends ObjectInputStream {

    private static final Logger logger = Logger.getLogger(HaierObjectInputStreamOfFlinkUDF.class.getCanonicalName());

    private final HaierUDFLoader haierUDFLoader;

    /**
     * Deserialize a Flink UDF using the provided {@link HaierUDFLoader}.
     *
     * @param inputStream The {@link InputStream} for the serialized UDF
     * @param haierUDFLoader The given {@link HaierUDFLoader} to use
     * @throws IOException In case of an error during the initialization of the parent {@link ObjectInputStream}
     */
    public HaierObjectInputStreamOfFlinkUDF(final InputStream inputStream, final HaierUDFLoader haierUDFLoader)
            throws IOException {
        super(inputStream);
        this.haierUDFLoader = haierUDFLoader;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass objectStreamClass) throws ClassNotFoundException {
        final String className = objectStreamClass.getName();
        logger.finest("Attempting to deserialize '" + className + "'...");
        return Class.forName(className, false, this.haierUDFLoader.urlClassLoader);
    }
}
