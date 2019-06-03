package gr.ntua.ece.cslab.e2datascheduler.beans.cluster;


import java.util.ArrayList;
import java.util.Iterator;

/**
 * Bean that describes a cluster node
 */
public class ClusterNode {

    private String nodeHTTPAddress;
    private Resource usedResource;
    private Resource availableResource;

    private int httpPort;
    private String domainName;

    public String getNodeHTTPAddress() {
        return nodeHTTPAddress;
    }

    public void setNodeHTTPAddress(String nodeHTTPAddress) {
        this.nodeHTTPAddress = nodeHTTPAddress;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getDomainName() {
        return domainName;
    }

    public void extractURL(){
        String[] address = nodeHTTPAddress.split(":");
        domainName = address[0];
        httpPort = Integer.parseInt(address[1]);
    }

    public ArrayList<HwResource> getUsedResources(){
        return usedResource.getResourceInfoWrapper().getInformation();
    }

    public ArrayList<HwResource> getAvailableResources(){
        return availableResource.getResourceInfoWrapper().getInformation();
    }

    public void indexAllResourcesByHost(){
        ArrayList<HwResource>  usedResources = getUsedResources();
        for(HwResource ur : usedResources){
            ur.setHost(domainName);
        }

        ArrayList<HwResource>  availableResources = getAvailableResources();
        for(HwResource ar : availableResources){
            ar.setHost(domainName);
        }

        // remove memory from available resources, as it is not a candidate device for task placement
        Iterator<HwResource> iter = availableResources.iterator();
        while(iter.hasNext()){
            HwResource currentresource = iter.next();
            if(currentresource.getName().equals("memory-mb")){
                iter.remove();
            }
        }
    }

}

