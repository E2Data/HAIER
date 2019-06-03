package gr.ntua.ece.cslab.e2datascheduler.ml.util;

import com.google.gson.Gson;
import gr.ntua.ece.cslab.e2datascheduler.beans.features.FeatureVector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Given an OpenCL source code in the form of a string, the extractor transforms it into
 * a vector of double values (features) in order to feed a ML model.
 *
 * The extractor is a singleton. Only one instance of it lives in the memory of the scheduler
 *
 * For now, the FeatureExtractor class is essentially a wrapper for a simple
 * HTTP client that connects to a HTTP server's "/extract" endpoint at
 * "localhost:54242".
 * It performs POST requests containing a plain string (opencl kernel's source
 * code) in their body, and receives a JSON-encoded dictionary-like object
 * that contains the features extracted from that kernel.
 * Example of a response body:
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
 *      For now, this class should be put into use by subclasses of Model
 *      (instead of an Optimizer), since Model's interface (method `predict`)
 *      imposes that kernels' source code is handed to the Model (hence,
 *      Optimizers have no clue about features).
 */
public class FeatureExtractor {

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String extractorURL = resourceBundle.getString("fextractor.url");

    private static FeatureExtractor extractor;

    private FeatureExtractor(){}

    public static FeatureExtractor getInstance(){
        if(extractor == null){
            extractor = new FeatureExtractor();
        }
        return extractor;
    }

    public List<Double> extract(String sourceCode){

        FeatureVector decodedVector = doPost(sourceCode);

        return decodedVector.getVector();
    }

    private static FeatureVector doPost(String openclKernel) {
        java.net.URL myURL = null;
        HttpURLConnection conn = null;
        FeatureVector v = null;
        try{
            myURL = new URL(extractorURL);
            conn = (HttpURLConnection) myURL.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());

            wr.writeBytes(openclKernel);

            if (wr != null) {
                wr.flush();
                wr.close();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder jsonResponse = new StringBuilder();
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
            v = gson.fromJson(jsonResponse.toString(), FeatureVector.class);

        } catch (IOException ex) {
            ex.printStackTrace();

        }
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return v;
    }



}
