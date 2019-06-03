package gr.ntua.ece.cslab.e2datascheduler.beans.cluster;

/**
 * Helper class needed for automatically parsing YARN's response with Gson
 */
public class Resource {

    private ResourcesInfo resourceInformations;

    public ResourcesInfo getResourceInfoWrapper() {
        return resourceInformations;
    }
}
