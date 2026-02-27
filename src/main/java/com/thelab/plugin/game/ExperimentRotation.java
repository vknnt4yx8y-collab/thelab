package com.thelab.plugin.game;

import com.thelab.plugin.experiment.ExperimentType;

import java.util.*;

/** Manages the random selection and rotation of experiments for a game. */
public class ExperimentRotation {

    private List<ExperimentType> selectedExperiments = new ArrayList<>();
    private int currentIndex = -1;

    /**
     * Randomly selects {@code count} unique experiments from the pool.
     */
    public void selectExperiments(List<ExperimentType> pool, int count) {
        selectedExperiments.clear();
        currentIndex = -1;
        if (pool.isEmpty()) return;
        List<ExperimentType> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        int num = Math.min(count, shuffled.size());
        selectedExperiments = shuffled.subList(0, num);
        selectedExperiments = new ArrayList<>(selectedExperiments);
    }

    /** Advances to the next experiment. Returns true if there is a next one. */
    public boolean advance() {
        currentIndex++;
        return currentIndex < selectedExperiments.size();
    }

    /** Returns the current experiment type. */
    public ExperimentType getCurrentExperiment() {
        if (currentIndex < 0 || currentIndex >= selectedExperiments.size()) return null;
        return selectedExperiments.get(currentIndex);
    }

    /** Returns the next experiment, if available. */
    public Optional<ExperimentType> getNextExperiment() {
        int next = currentIndex + 1;
        if (next >= selectedExperiments.size()) return Optional.empty();
        return Optional.of(selectedExperiments.get(next));
    }

    /** Returns true if there are more experiments remaining (not yet played). */
    public boolean hasMore() {
        return currentIndex + 1 < selectedExperiments.size();
    }

    /** Returns the current 1-based round number. */
    public int getRound() { return Math.max(1, currentIndex + 1); }

    /** Returns total number of rounds. */
    public int getTotalRounds() { return selectedExperiments.size(); }

    /** Returns the selected experiments (unmodifiable). */
    public List<ExperimentType> getSelectedExperiments() {
        return Collections.unmodifiableList(selectedExperiments);
    }
}
