package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation;

import java.util.ArrayList;
import java.util.List;

class TaskList {

    final List<Task> tasks;

    // --------------------------------------------------------------------------------------------

    TaskList(final List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            this.tasks.add(tasks.get(i));
        }
    }

    TaskList(final TaskList taskList) {
        this.tasks = new ArrayList<>(taskList.size());
        for (Task task : taskList.getTasks()) {
            this.tasks.add(new Task(task));
        }
    }

    // --------------------------------------------------------------------------------------------

    int size() {
        return tasks.size();
    }

    Task get(final int i) {
        return this.tasks.get(i);
    }

    List<Task> getTasks() {
        return tasks;
    }

    @Override
    public String toString() {
        return "TaskList<" + tasks + '>';
    }

}
