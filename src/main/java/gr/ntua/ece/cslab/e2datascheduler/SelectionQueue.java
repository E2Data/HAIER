package gr.ntua.ece.cslab.e2datascheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class SelectionQueue<T> {

    private final BlockingQueue<List<T>> optionsQueue;
    private final BlockingQueue<T> choiceQueue;

    public SelectionQueue() {
        this.optionsQueue = new ArrayBlockingQueue<>(1);
        this.choiceQueue = new ArrayBlockingQueue<>(1);
    }

    public void submitOptions(final List<T> options) throws IllegalStateException {
        this.optionsQueue.add(options);
    }

    public List<T> retrieveOptions(final int blockMilliSeconds) throws InterruptedException {
        return this.optionsQueue.poll(blockMilliSeconds, TimeUnit.MILLISECONDS);
    }

    public void submitChoice(final T choice) {
        this.choiceQueue.add(choice);
    }

    public T retrieveChoice(final int blockMilliSeconds) throws InterruptedException {
        return this.choiceQueue.poll(blockMilliSeconds, TimeUnit.MILLISECONDS);
    }

    // Quick & dirty single-threaded test
    public static void main(String[] args) throws InterruptedException {
        final SelectionQueue<Integer> sq = new SelectionQueue<>();

        final int size = 10;
        final List<Integer> options = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            options.add(i, i);
        }
        System.err.println("Submitting options...");
        sq.submitOptions(options);
        System.err.println("Submitted options!");

        System.err.println("Retrieving options...");
        final List<Integer> retrievedOptions = sq.retrieveOptions(500);
        System.err.println("Retrieved options!");

        final int choice = retrievedOptions.get(retrievedOptions.size() - 1);
        System.err.println("Submitting the choice (" + choice + ")...");
        sq.submitChoice(choice);
        System.err.println("Submitted choice!");

        System.err.println("Retrieving choice...");
        final int retrievedChoice = sq.retrieveChoice(500);
        System.err.println("Retrieved choice (" + retrievedChoice + ")!");
    }

}
