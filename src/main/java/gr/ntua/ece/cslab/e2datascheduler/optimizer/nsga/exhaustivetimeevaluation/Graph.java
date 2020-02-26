package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

class Graph {

    private final List<Task> tasks;
    private final List<Task> sources;
    private final List<Task> sinks;

    // --------------------------------------------------------------------------------------------

    Graph(final List<Task> tasks) {
        this.tasks = tasks;
        this.tasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                return task.getIndex() - t1.getIndex();
            }
        });

        /* Assert that the given Task IDs form a continuous range [0, n-1]. */
        for (int i = 0; i < this.tasks.size(); i++) {
            assert i == this.tasks.get(i).getIndex() : "i != tasks.index";
        }

        this.sources = new ArrayList<>();
        this.sinks = new ArrayList<>();
        for (Task task : this.tasks) {
            if (task.isSource()) {
                this.sources.add(task);
            }
            if (task.getChildren().isEmpty()) {
                this.sinks.add(task);
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    List<Task> getTasks() {
        return tasks;
    }

    List<Task> getSources() {
        return sources;
    }

    List<Task> getSinks() {
        return sinks;
    }

    @Override
    public String toString() {
        return "<Graph{" + this.tasks + "}>";
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the GCD of two integers.
     * @param x is the first integer.
     * @param y is the second integer.
     * @return the GCD of the two integers.
     */
    private static int gcd(int x, int y) {
        while (y != 0) {
            int temp = x;
            //noinspection SuspiciousNameCombination
            x = y;
            y = temp % y;
        }
        return x;
    }

    /**
     * Returns the GCD of a List of integers.
     * @param durations is the list of integers.
     * @return the GCD of the given list of integers.
     */
    private static int multiGcd(final List<Integer> durations) {
        int totalGcd = gcd(durations.get(0), durations.get(1));
        if (durations.size() > 2) {
            for (int i = 2; i < durations.size(); i++) {
                totalGcd = gcd(totalGcd, durations.get(i));
            }
        }
        return totalGcd;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Calculates the total time of execution for every possible execution scenario of the Graph.
     *
     * NOTE: It is assumed that the parents of all Tasks have already been calculated (correctly) prior to calling this
     * method.
     */
    List<Double> haierEvaluation() {
        /* Initialization */
        TaskList tasks = new TaskList(this.tasks);
        HashMap<HwResource, Integer> devices = new HashMap<>();
        List<Integer> fakeDurations = new ArrayList<>(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (!devices.containsKey(task.getDevice())) {
                devices.put(task.getDevice(), 0);
            }
            fakeDurations.add(task.getFakeDuration());
        }
        int step = multiGcd(fakeDurations);

        /* Main Iteration */
        List<Evaluation> completedEvaluations = new ArrayList<>();
        Stack<Evaluation> stack = new Stack<>();
        stack.push(new Evaluation(this, 0, new TaskList(tasks), (HashMap<HwResource, Integer>) devices.clone()));
        while (!stack.isEmpty()) {
            //System.err.printf("\n\n\n----------------------------------- NEW EVALUATION -----------------------------------\n");
            //System.err.printf("LEN(completedEvaluations) = %d\n", completedEvaluations.size());
            Evaluation currentEvaluation = stack.pop();
            if (currentEvaluation.isComplete()) {
                System.err.println("curr_eval IS COMPLETE!");
            }
            /* For the current evaluation, loop as long as it is not complete. */
            while (!currentEvaluation.isComplete()) {
                //System.err.printf("\n\n-------------------- TIME=%d (step=%d) --------------------\n", currentEvaluation.getCurrentTime(), step);
                boolean forkRequired = false;

                Queue<Task> remainingTasksInRound = new LinkedList<>();
                for (int i = 0; i < currentEvaluation.getTasks().size(); i++) {
                    remainingTasksInRound.add(currentEvaluation.getTasks().get(i));
                }

                HashMap<HwResource, List<Task>> taskGroups = new HashMap<>();
                for (HwResource device : currentEvaluation.getDevices().keySet()) {
                    taskGroups.put(device, new ArrayList<>());
                }

                while (!remainingTasksInRound.isEmpty()) {
                    //System.err.printf("LEN(remainingTasksInRound) = %d\n", remainingTasksInRound.size());
                    Task task = remainingTasksInRound.remove();
                    /* If current Task can now be run on its own device: */
                    if (task.getPathCost() == -1 && currentEvaluation.dataDepReady(task) &&
                            currentEvaluation.getDevices().get(task.getDevice()) <= currentEvaluation.getCurrentTime()) {
                        //System.err.printf("READY: %s\n", task);
                        /* Check if any of the remaining tasks can now be run on the same device as well, and put
                         *  them all in the associated taskGroup of the device. */
                        List<Task> conflicts = new ArrayList<>();
                        for (Task t : remainingTasksInRound) {
                            if (t.getPathCost() == -1 && currentEvaluation.dataDepReady(t) &&
                                    t.getDevice() == task.getDevice() && !taskGroups.get(t.getDevice()).contains(t)) {
                                //System.err.printf("FOUND COLLISION with %s\n", t);
                                taskGroups.get(task.getDevice()).add(t);
                                //System.err.printf("NEW taskGroups: %s\n", taskGroups);
                                conflicts.add(t);
                            }
                        }
                        /* If collision was indeed found, push current Task on the top of device's list, too.
                         *  Otherwise, proceed normally with calculating time.*/
                        if (!taskGroups.get(task.getDevice()).isEmpty()) {
                            taskGroups.get(task.getDevice()).add(0, task);
                            forkRequired = true;
                            //System.err.printf("FINAL taskGroups: %s\n", taskGroups);
                            for (Task t : conflicts) {
                                if (!remainingTasksInRound.remove(t)) {
                                    //System.err.printf("ERROR: remainingTasksInRound did not contain task %s\n", t);
                                    System.exit(1);
                                }
                            }
                        } else {
                            task.setPathCost(currentEvaluation.getCurrentTime() + task.getFakeDuration());
                            currentEvaluation.getDevices().put(task.getDevice(), task.getPathCost());
                        }
                    }
                }

                if (forkRequired) {
                    //System.err.printf(" ---> FORK REQUIRED!\n");
                    /*
                     * At this point, we need to create N sets of tuples, in the form '(device, task)', based on
                     * taskGroup. E.g.:
                     *      sets = { 'D1': [], 'D2': [], 'D3': [] }
                     *      taskGroups = { 'D1': [t1, t2, t3], 'D2': [t4, t5], 'D3': [t6, t7] }
                     * In the case of N=3, we have 3 sets of tuples:
                     *      S1 = [ ('D1', t1), ('D1', t2), ('D1', t3) ]
                     *      S2 = [ ('D2', t4), ('D2', t5) ]
                     *      S3 = [ ('D3', t6), ('D3', t7) ]
                     * so sets is populated as follows:
                     *      sets = { 'D1': S1, 'D2': S2, 'D3': S3 }
                     * i.e.:
                     *      sets = {
                     *                  'D1' : [ ('D1', t1), ('D1', t2), ('D1', t3) ],
                     *                  'D2' : [ ('D2', t4), ('D2', t5) ],
                     *                  'D3' : [ ('D3', t6), ('D3', t7) ]
                     *             }
                     * To calculate all possible forks at this point, we merely need to calculate the Cartesian Product
                     * S1xS2xS3 (or in general: S1 x S2 x ... x SN).
                     */
                    HashMap<HwResource, List<DeviceTaskPair>> sets = new HashMap<>();
                    for (Map.Entry<HwResource, List<Task>> entry : taskGroups.entrySet()) {
                        if (entry.getValue().isEmpty()) {
                            continue;
                        }
                        sets.put(entry.getKey(), new ArrayList<>(entry.getValue().size()));
                        for (Task task : entry.getValue()) {
                            sets.get(entry.getKey()).add(new DeviceTaskPair(entry.getKey(), task));
                        }
                    }
                    //System.err.printf("SETS (len=%d): %s\n", sets.size(), sets);
                    /*
                     * If N=1, e.g. taskGroups = { 'D1': [t1, t2] }, the fork should be quite straightforward, since no
                     * Cartesian Product should be required at all.
                     */
                    if (1 == sets.size()) {
                        /* sets = { 'D1': [ ('D1', t1), ('D1', t2) ] } */
                        List<DeviceTaskPair> forks = new ArrayList<>();
                        for (Map.Entry<HwResource, List<DeviceTaskPair>> entry : sets.entrySet()) {
                            for (DeviceTaskPair pair : entry.getValue()) {
                                forks.add(pair);
                            }
                        }
                        assert forks.size() == 1 : "forks.size() != 1";
                        //System.err.printf("FORKS = %s\n", forks);
                        for (DeviceTaskPair pair : forks) {
                            //System.err.printf("\tPair: %s\n", pair);
                            assert pair.device == pair.task.getDevice() : "pair.device != pair.task.device";
                            TaskList newTasks = new TaskList(currentEvaluation.getTasks());
                            HashMap<HwResource, Integer> newDevices = (HashMap<HwResource, Integer>) currentEvaluation.getDevices().clone();
                            newTasks.get(pair.task.getIndex()).setPathCost(
                                    currentEvaluation.getCurrentTime() + pair.task.getFakeDuration());
                            newDevices.put(pair.device, newTasks.get(pair.task.getIndex()).getPathCost());
                            stack.push(new Evaluation(this,
                                    currentEvaluation.getCurrentTime() + step,
                                    newTasks,
                                    newDevices));
                        }
                    } else {
                        /*
                         * sets is as explained above, i.e.:
                         *      sets = { 'D1': S1, 'D2': S2, 'D3': S3 }
                         * therefore:
                         *      sets.values() = [ S1, S2, S3 ]
                         * hence fork should be in the form of:
                         *      fork = ( ('D1', t1), ('D2', t4), ('D3', t6) )
                         * Before flattening it, fork is actually in the form of:
                         *      fork = [ [ Pair('D1', t1), Pair('D2', t4) ], Pair('D3', t6) ]
                         */
                        List<DeviceTaskPair>[] lists = new ArrayList[sets.values().size()];
                        int i = 0;
                        for (List<DeviceTaskPair> l : sets.values()) {
                            lists[i++] = l;
                        }
                        for (Object multiLevelFork : CartesianProduct.product(lists)) {
                            List<DeviceTaskPair> fork = CartesianProduct.flatten((List<?>) multiLevelFork);
                            //System.err.printf("CART_PROD (FORK): " + fork);
                            TaskList newTasks = new TaskList(currentEvaluation.getTasks());
                            HashMap<HwResource, Integer> newDevices = (HashMap<HwResource, Integer>) currentEvaluation.getDevices().clone();
                            for (DeviceTaskPair pair : fork) {
                                assert pair.device == pair.task.getDevice() : "pair.device != pair.task.device";
                                newTasks.get(pair.task.getIndex()).setPathCost(
                                        currentEvaluation.getCurrentTime() + pair.task.getFakeDuration());
                                newDevices.put(pair.device, newTasks.get(pair.task.getIndex()).getPathCost());
                                stack.push(new Evaluation(this,
                                        currentEvaluation.getCurrentTime() + step,
                                        newTasks,
                                        newDevices));
                            }
                        }
                    }
                    currentEvaluation = stack.pop();
                    continue;
                }
                currentEvaluation.timeStep(step);
                //System.err.printf("LEN(stack) = %d\n", stack.size());
                //System.err.printf("curr_eval.devices = %s\n", currentEvaluation.getDevices());
                //System.err.printf("curr_eval.tasks = %s\n", currentEvaluation.getTasks());
            }
            completedEvaluations.add(currentEvaluation);
        }
        //System.err.printf("\nCOMPLETED EVALUATIONS:\n");
        //completedEvaluations.forEach(System.err::println);

        List<Double> ret = new ArrayList<>(completedEvaluations.size());
        for (Evaluation eval : completedEvaluations) {
            /* Here, we compare the times of sink Tasks to determine which will be the last one to finish the execution.
             * Alternatively, we could just compare the device availability times and grab the max one. */
            double evalMaxSink = 0.0d;
            for (Task task : this.sinks) {
                int pathCost = eval.getTasks().get(task.getIndex()).getPathCost();
                if (pathCost > (int) evalMaxSink * Task.FAKE_DURATION_FACTOR) {
                    evalMaxSink = pathCost / (double) Task.FAKE_DURATION_FACTOR;
                }
            }
            ret.add(evalMaxSink);
        }
        return ret;
    }

}
