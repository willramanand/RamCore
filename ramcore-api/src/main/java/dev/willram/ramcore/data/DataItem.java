package dev.willram.ramcore.data;

public abstract class DataItem {
    private int dataVersion;

    private transient boolean saving;
    private transient boolean shouldSave;
    private transient boolean dirty;

    public DataItem() {
        this.dataVersion = 1;
        this.saving = false;
        this.shouldSave = true;
        this.dirty = false;
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

    public boolean shouldSave() {
        return shouldSave;
    }

    public boolean dirty() {
        return this.dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    public int dataVersion() {
        return this.dataVersion;
    }

    public void dataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }
}
