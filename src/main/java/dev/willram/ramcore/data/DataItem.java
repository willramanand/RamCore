package dev.willram.ramcore.data;

public abstract class DataItem {

    private transient boolean saving;
    private transient boolean shouldSave;

    public DataItem() {
        this.saving = false;
        this.shouldSave = true;
    }

    public boolean isSaving() {
        return saving;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
    }

    public boolean shouldNotSave() {
        return !shouldSave;
    }

    public void setShouldSave(boolean shouldSave) {
        this.shouldSave = shouldSave;
    }
}
