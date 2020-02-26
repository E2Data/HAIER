package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

class DeviceTaskPair {

    final HwResource device;
    final Task task;

    DeviceTaskPair(final HwResource device, final Task task) {
        this.device = device;
        this.task = task;
    }

    @Override
    public String toString() {
        return "(" + device + ", " + task + ")";
    }

}
