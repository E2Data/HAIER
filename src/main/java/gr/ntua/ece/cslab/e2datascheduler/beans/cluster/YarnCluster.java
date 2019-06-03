package gr.ntua.ece.cslab.e2datascheduler.beans.cluster;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Singleton that maintains a single representation of the YARN cluster in scheduler's memory
 */
public class YarnCluster {

    public static ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String yarnURL = resourceBundle.getString("yarn.url");

    private static YarnCluster cluster = null;

    private ClusterNode[] nodes;

    private YarnCluster(){}

    /*
     * For now, it always returns a clsuter instance (fresh or stale).
     * The only case where a null value is returned is when YarnCluster has never been initialized
     * FIXME: Probably should be changed to not allow a stale status
     */
    public static YarnCluster getInstance(){
        YarnCluster newCluster = receiveClusterStatus();
        if(newCluster != null){
            cluster = newCluster;
        }
        return cluster;
    }

    public ClusterNode[] getNodes() {
        return nodes;
    }

    public void setNodes(ClusterNode[] nodes) {
        this.nodes = nodes;
    }

    private static YarnCluster parseJSON(String clusterJson){
        JsonElement jRootElement = new JsonParser().parse(clusterJson);
        JsonElement nodesRootObject = jRootElement.getAsJsonObject().get("nodes");
        JsonElement nodesJson = nodesRootObject.getAsJsonObject().get("node");

        Gson gson = new Gson();
        ClusterNode[] cnodesList = gson.fromJson(nodesJson, ClusterNode[].class);

        YarnCluster cluster = new YarnCluster();
        cluster.setNodes(cnodesList);
        return cluster;
    }

    private static YarnCluster receiveClusterStatus(){
        URL myURL = null;
        HttpURLConnection conn = null;
        YarnCluster liveCluster = null;
        try{

            myURL = new URL(yarnURL);

            conn = (HttpURLConnection) myURL.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

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


            liveCluster =  YarnCluster.parseJSON(jsonResponse.toString());
            for(ClusterNode node: liveCluster.getNodes()){
                node.extractURL();
                node.indexAllResourcesByHost();
            }

        } catch (IOException ex) {
            ex.printStackTrace();

        }
        catch (NullPointerException e){
            // in case liveCluster is null, the iteration of the nodes will return a NullPointerException
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return liveCluster;
    }



    //FIXME: this main is only for showcasing sample use of a YarnCluster
    public static void main(String[] args){

        YarnCluster cluster  = YarnCluster.getInstance();

        for(ClusterNode node: cluster.getNodes()){
            System.out.println("Node: " + node.getDomainName());
            ArrayList<HwResource> availableResources = node.getAvailableResources();
            for(HwResource r : availableResources){
                System.out.println("Resource: "+ r.getName() +" multiplicity: " + r.getValue());
            }

        }
    }
}
