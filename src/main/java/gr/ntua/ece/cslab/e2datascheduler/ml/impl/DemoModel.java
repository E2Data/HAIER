package gr.ntua.ece.cslab.e2datascheduler.ml.impl;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.util.FeatureExtractor;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

/**
 * Dummy class that mocks a model and serves integration purposes
 */
public class DemoModel extends Model {

    /**
     * Predictions for all kernels can be cached here for future use.
     */
    private final Map<String, Double> predictionCache = new HashMap<>();

    /**
     * Hard-coded URL for the model.
     */
    private static final String MODEL_URL = "http://silver1.cslab.ece.ntua.gr:8000";

    /**
     * FIXME(ckatsak): Hard-coded mapping between devices and model's output
     *                 array.
     */
    private static final Map<String, Integer> DEVICE_TO_INDEX = createNewStaticDeviceMapping();

    private static Map<String, Integer> createNewStaticDeviceMapping() {
        final Map<String, Integer> ret = new HashMap<String, Integer>();
        ret.put("cpu", 0);
        ret.put("gtx", 1);
        ret.put("tesla", 2);
        return ret;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void load(String path) {}

    @Override
    public double predict(String objective, HwResource device, String sourceCode) {
        // FIXME(ckatsak): objective is ignored by the corresponding model for
        //                 now: only the execution time is predicted, but the
        //                 same prediction is returned for power consumption.
        if (!this.predictionCache.containsKey(sourceCode)) {
            final List<Double> predictions = DemoModel.doPost(sourceCode);
            this.predictionCache.put(sourceCode, predictions.get(DemoModel.DEVICE_TO_INDEX.get(device.getName())));
        }
        //System.err.println("[DemoModel] Prediction retrieved for '" + device.getName() + "': " + this.predictionCache.get(sourceCode));

        return this.predictionCache.get(sourceCode);
    }

    // --------------------------------------------------------------------------------------------

    private static List<Double> doPost(final String sourceCode) {
        URL myURL = null;
        HttpURLConnection conn = null;
        List<Double> ret = new ArrayList<>();
        try {
            myURL = new URL(DemoModel.MODEL_URL);
            conn = (HttpURLConnection) myURL.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            final DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            // FIXME(ckatsak): a couple of hard-coded sizes for now
            wr.writeBytes("{\"kernel\":\"" + sourceCode + "\",\"mem_alloc_bytes\":\"5050\",\"h2d_bytes\":\"15430\"}");
            if (wr != null) {
                wr.flush();
                wr.close();
            }

            final BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            final StringBuilder jsonResponse = new StringBuilder();
            try {
                while ((inputLine = in.readLine()) != null) {
                    jsonResponse.append(inputLine);
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //System.err.printf("[DemoModel] Model's JSON response:\n\n%s\n\n", jsonResponse.toString());
            //ret = new Gson().fromJson("[34.2980647, 7.30185255, 16.41673777]", ret.getClass());
            ret = new Gson().fromJson(jsonResponse.toString(), ret.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return ret;
    }

}
