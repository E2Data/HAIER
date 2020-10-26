package gr.ntua.ece.cslab.e2datascheduler.ml.impl;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * Dummy class that mocks a model and serves integration purposes
 */
@Deprecated
public class DemoModel extends Model {

    /**
     * Predictions for all kernels can be cached here for future use.
     */
    private final Map<ScheduledJobVertex, Double> predictionCache = new HashMap<>();

    /**
     * Hard-coded URL for the model.
     */
    private static final String MODEL_URL = "http://silver1.cslab.ece.ntua.gr:8000";

    /**
     * FIXME(ckatsak): Hard-coded mapping between devices and model's output
     *                 array.
     */
    private static final Map<String, Integer> DEVICE_TO_INDEX = createNewStaticDeviceMapping();

    final String sourceCode = "__kernel void A(__global float* a, __global float* b, const int c) {const int d = get_global_id(0);int e, f, g, h;unsigned int i, j, k, l;for (unsigned int m = 0; m < k; m++) {for (unsigned int h = 0; h < i; h++) {for (unsigned int i = e; i < c; i++) {for (unsigned int j = a[g]; j < i; ++j) {a[i * c + i] = 0;}b[j] = j;}}}}";

    private static Map<String, Integer> createNewStaticDeviceMapping() {
        final Map<String, Integer> ret = new HashMap<>();
        ret.put("cpu", 0);
        ret.put("gtx", 1);
        ret.put("tesla", 2);
        return ret;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void load(String path) {}

    @Override
    public double predict(
            final String objective,
            final HwResource device,
            final ScheduledJobVertex scheduledJobVertex) {
        // FIXME(ckatsak): objective is ignored by the corresponding model for now: only the execution time
        //                 is predicted, but the same prediction is returned for power consumption.
        if (!this.predictionCache.containsKey(scheduledJobVertex)) {
            final Map<String, Double> predictions = DemoModel.doPost(this.sourceCode);
            this.predictionCache.put(scheduledJobVertex, predictions.get(device.getName()));
        }
        //System.err.println("[DemoModel] Prediction retrieved for '" + device.getName() + "': " + this.predictionCache.get(sourceCode));

        return this.predictionCache.get(scheduledJobVertex);
    }

    // --------------------------------------------------------------------------------------------

    private static Map<String, Double> doPost(final String sourceCode) {
        URL myURL = null;
        HttpURLConnection conn = null;
        Map<String, Double> ret = new HashMap<>();
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
            //ret = new Gson().fromJson("{\"cpu\":34.29806470131643,\"gtx\":7.3018525495080375,\"tesla\":16.416737770578887}", ret.getClass());
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
