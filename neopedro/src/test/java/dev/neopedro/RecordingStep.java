package dev.neopedro;

/** A scriptable step for tests: finishes after a set number of updates and counts every call. */
class RecordingStep implements Step {
    int starts;
    int updates;
    int stops;
    private final int updatesUntilDone;
    private final String label;

    RecordingStep(String label, int updatesUntilDone) {
        this.label = label;
        this.updatesUntilDone = updatesUntilDone;
    }

    @Override
    public void start() {
        starts++;
        updates = 0;
    }

    @Override
    public void update() {
        updates++;
    }

    @Override
    public boolean isDone() {
        return starts > 0 && updates >= updatesUntilDone;
    }

    @Override
    public void stop() {
        stops++;
    }

    @Override
    public String name() {
        return label;
    }
}
