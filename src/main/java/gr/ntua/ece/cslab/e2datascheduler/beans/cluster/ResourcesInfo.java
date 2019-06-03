package gr.ntua.ece.cslab.e2datascheduler.beans.cluster;

import java.util.ArrayList;

/**
 * Helper class needed for automatically parsing YARN's response with Gson
 */
public class ResourcesInfo {

    private ArrayList<HwResource> resourceInformation;

    public ArrayList<HwResource> getInformation() {
        return resourceInformation;
    }


}
