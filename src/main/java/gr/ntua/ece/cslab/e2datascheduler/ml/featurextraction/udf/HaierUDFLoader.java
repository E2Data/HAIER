package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.udf;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.jobgraph.JobGraph;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Dynamically load arbitrary user-defined classes as found in Flink users' JAR files referenced by a
 * {@link JobGraph} at runtime.
 */
public final class HaierUDFLoader {

    private static final Logger logger = Logger.getLogger(HaierUDFLoader.class.getCanonicalName());

    final URLClassLoader urlClassLoader;
    private final List<File> jarFiles;

    /**
     * Dynamically load arbitrary user-defined classes as found in Flink users' JAR files referenced by the provided
     * {@link JobGraph} at runtime, using the {@code SystemClassLoader}.
     *
     * @param jobGraph The {@link JobGraph} associated with this {@link HaierUDFLoader}
     */
    public HaierUDFLoader(final JobGraph jobGraph) {
        this(jobGraph, ClassLoader.getSystemClassLoader());
    }

    /**
     * Dynamically load arbitrary user-defined classes as found in Flink users' JAR files referenced by the provided
     * {@link JobGraph} at runtime.
     *
     * @param jobGraph The {@link JobGraph} associated with this {@link HaierUDFLoader}
     * @param classLoader The parent {@link ClassLoader}
     */
    public HaierUDFLoader(final JobGraph jobGraph, final ClassLoader classLoader) {
        this.jarFiles = new ArrayList<>();
        final List<URL> urls = new ArrayList<>();
        for (Path path : jobGraph.getUserJars()) {
            final File jarFile = new File(path.getPath());
            this.jarFiles.add(jarFile);
            try {
                urls.add(jarFile.toURI().toURL());
            } catch (final MalformedURLException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }
        this.urlClassLoader = URLClassLoader.newInstance(urls.toArray(new URL[0]), classLoader);
    }


    // --------------------------------------------------------------------------------------------


    /**
     * Retrieve a {@link List} of all the {@link Class}es that implement Flink's {@link MapFunction} interface in
     * JARs referenced by the {@link JobGraph} associated with this instance of {@link HaierUDFLoader}.
     *
     * @return A {@link List} of {@link Class}es that implement Flink's {@link MapFunction}
     */
    public List<Class<?>> getAllUserDefinedMapFunctions() {
        return this.getAllUserDefinedFunctions(MapFunction.class);
    }

    /**
     * Retrieve a {@link List} of all the {@link Class}es that implement Flink's {@link ReduceFunction} interface in
     * JARs referenced by the {@link JobGraph} associated with this instance of {@link HaierUDFLoader}.
     *
     * @return A {@link List} of {@link Class}es that implement Flink's {@link ReduceFunction}
     */
    public List<Class<?>> getAllUserDefinedReduceFunctions() {
        return this.getAllUserDefinedFunctions(ReduceFunction.class);
    }

    /**
     * Retrieve a {@link List} of all the {@link Class}es that can be assigned to the given {@link Class}
     * {@code assignableTo} and are present in any of the JARs referenced by the {@link JobGraph} associated
     * with this instance of {@link HaierUDFLoader}.
     *
     * @param assignableTo The {@link Class} that the returned values should be assignable to
     * @return A {@link List} of {@link Class}es that are assignable to the provided {@link Class}
     *         {@code assignableTo}
     */
    private List<Class<?>> getAllUserDefinedFunctions(final Class<?> assignableTo) {
        final List<Class<?>> userDefinedFunctions = new ArrayList<>();

        for (File jarFile : this.jarFiles) {
            try {
                userDefinedFunctions.addAll(this.scanJarFileForAssignableClasses(jarFile, assignableTo));
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }

        return userDefinedFunctions;
    }


    // --------------------------------------------------------------------------------------------


    /**
     * Scan the given JAR file for {@link Class}es that are assignable to {@link MapFunction}, dynamically load
     * them and return them as a {@link List} of {@link Class} objects.
     *
     * @param file The provided JAR file
     * @return A {@link List} of {@link Class} objects that are assignable to {@link MapFunction}
     * @throws IOException If any error occurs during processing the provided JAR file
     */
    private List<Class<?>> scanJarFileForMapFunctionClasses(final File file) throws IOException {
        return this.scanJarFileForAssignableClasses(file, MapFunction.class);
    }

    /**
     * Scan the given JAR file for {@link Class}es that are assignable to {@link ReduceFunction}, dynamically load
     * them and return them as a {@link List} of {@link Class} objects.
     *
     * @param file The provided JAR file
     * @return A {@link List} of {@link Class} objects that are assignable to {@link ReduceFunction}
     * @throws IOException If any error occurs during processing the provided JAR file
     */
    private List<Class<?>> scanJarFileForReduceFunctionClasses(final File file) throws IOException {
        return this.scanJarFileForAssignableClasses(file, ReduceFunction.class);
    }

    /**
     * Scan the given JAR file for {@link Class}es that are assignable to the given {@link Class} (a class, abstract
     * class or interface), dynamically load them and return them as a {@link List} of {@link Class} objects.
     *
     * @param file The provided JAR file
     * @param assignableTo The class, abstract class or interface that the returned values must be assignable to
     * @return A {@link List} of {@link Class} objects that are assignable to the provided class, abstract class
     *         or interface
     * @throws IOException If any error occurs during processing the provided JAR file.
     */
    private List<Class<?>> scanJarFileForAssignableClasses(final File file, final Class<?> assignableTo)
            throws IOException {
        final List<Class<?>> ret = new ArrayList<>();

        for (String classFile : HaierUDFLoader.scanJarFileForClasses(file)) {
            final Class<?> clazz;
            try {
                clazz = Class.forName(classFile, true, this.urlClassLoader);
                if (assignableTo.isAssignableFrom(clazz) && !clazz.equals(assignableTo)) {
                    ret.add(clazz);
                }
//                logger.finest("Loading class '" + classFile + "'...");
//                final Class<?> tmp = this.urlClassLoader.loadClass(classFile);
//                logger.finest("Loaded class '" + classFile + "': " + tmp);
            } catch (final ClassNotFoundException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }

        return ret;
    }

    /**
     * Scan a JAR file for .class files to return a {@link List} containing the full name of all found classes (in
     * the form: {@code packageName.className}).
     *
     * @param file The JAR file to be searched for {@code .class} files
     * @return All found {@code .class} files with their full-name as a {@link List} of {@link String}s
     * @throws IOException If an error occurs during processing of the JAR file
     * @throws IllegalArgumentException If the provided file is null, or it does not exist, or it is not a JAR file
     */
    public static List<String> scanJarFileForClasses(final File file)
            throws IOException, IllegalArgumentException {
        if (null == file || !file.exists())
            throw new IllegalArgumentException("Invalid JAR file to scan provided");
        if (!file.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("No JAR file provided ('" + file.getName() + "')");
        }

        final JarFile jarFile = new JarFile(file);
        final Enumeration<JarEntry> jarEntries = jarFile.entries();
        final List<String> containedClasses = new ArrayList<>();
        while (jarEntries.hasMoreElements()) {
            final JarEntry jarEntry = jarEntries.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                String jarEntryName = jarEntry.getName();
                jarEntryName = jarEntryName.substring(0, jarEntryName.lastIndexOf(".class"));
                if (jarEntryName.contains("/")) {
                    jarEntryName = jarEntryName.replaceAll("/", ".");
                }
                if (jarEntryName.contains("\\")) {
//                    jarEntryName = jarEntryName.replaceAll("\\", ".");
                    throw new IllegalArgumentException(
                            "FIXME(ckatsak): Weird name containing backslashes: '" + jarEntryName + "'");
                }
                containedClasses.add(jarEntryName);
            }
        }
        return containedClasses;
    }

}
