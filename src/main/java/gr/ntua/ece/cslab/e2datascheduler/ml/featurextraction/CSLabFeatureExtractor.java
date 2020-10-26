package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction;

import gr.ntua.ece.cslab.e2datascheduler.beans.features.CSLabFeatureVector;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


/**
 * Given the source code of an OpenCL kernel in the form of a {@link String}, this class transforms it into a vector of
 * double-precision floating point values (the features) in order to feed them to a Machine Learning
 * {@link gr.ntua.ece.cslab.e2datascheduler.ml.Model}.
 *
 * The Feature Extractor is a singleton: only one instance of it may live in the memory of HAIER any given time.
 *
 * For now, this class is essentially a wrapper for a simple HTTP client that connects to a HTTP server's "/extract"
 * endpoint (at {@code ${fextractor.url}}).
 * It issues HTTP POST requests that contain a plain string (OpenCL kernel's source code) in their body, and it
 * receives a JSON-encoded dictionary-like object that contains the features extracted from that OpenCL kernel.
 * Example of such a response body:
 *    {
 *      "AggOps": 0,
 *      "BinOps": 150,
 *      "BitBinOps": 0,
 *      "CallOps": 18,
 *      "GlobalMemAcc": 34,
 *      "LoadOps": 254,
 *      "LocalMemAcc": 12,
 *      "OtherOps": 219,
 *      "PrivateMemAcc": 305,
 *      "StoreOps": 97,
 *      "VecOps": 0
 *    }
 *
 * NOTE(ckatsak):
 *      For now, this class should be put into use by subclasses of {@link gr.ntua.ece.cslab.e2datascheduler.ml.Model}
 *      (instead of an {@link gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer}), since the
 *      {@link gr.ntua.ece.cslab.e2datascheduler.ml.Model}{@code.predict()} method imposes that OpenCL kernels' source
 *      code is handed to the {@link gr.ntua.ece.cslab.e2datascheduler.ml.Model} (hence,
 *      {@link gr.ntua.ece.cslab.e2datascheduler.optimizer.Optimizer}s have no clue about features).
 */
@Deprecated
public class CSLabFeatureExtractor {

    public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String extractorURL = resourceBundle.getString("fextractor.url");

    private static CSLabFeatureExtractor extractor;

    private CSLabFeatureExtractor() {}

    public static CSLabFeatureExtractor getInstance(){
        if (extractor == null) {
            extractor = new CSLabFeatureExtractor();
        }
        return extractor;
    }

    public List<Double> extract(final String sourceCode) {
        final CSLabFeatureVector decodedVector = doPost(sourceCode);
        return decodedVector.getVector();
    }

    private static CSLabFeatureVector doPost(final String openclKernel) {
        final java.net.URL myURL;
        HttpURLConnection conn = null;
        CSLabFeatureVector v = null;
        try {
            myURL = new URL(extractorURL);
            conn = (HttpURLConnection) myURL.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            final DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(openclKernel);
            wr.flush();
            wr.close();

            final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            final StringBuilder jsonResponse = new StringBuilder();
            try {
                while ((inputLine = in.readLine()) != null) {
                    jsonResponse.append(inputLine);
                }
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            Gson gson = new Gson();
            v = gson.fromJson(jsonResponse.toString(), CSLabFeatureVector.class);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return v;
    }

}
