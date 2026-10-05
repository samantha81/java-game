package com.assignment.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

// Stores the best (fastest) completion time per level in a "records.properties" file.
public class RecordStore {
    private final File file;
    private final Properties props = new Properties();

    public RecordStore() {
        this(new File("records.properties"));
    }

    public RecordStore(File file) {
        this.file = file;
        load();
    }

    // Load saved times from the file (skip if missing/unreadable).
    private void load() {
        if (!file.exists()) return;
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("Could not read records: " + e.getMessage());
        }
    }

    // Write the times back to the file.
    private void save() {
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "Meowhen United - best level times (milliseconds)");
        } catch (IOException e) {
            System.err.println("Could not save records: " + e.getMessage());
        }
    }

    // Best time for a level in ms, or -1 if no run has been recorded yet.
    public long getBest(int level) {
        String v = props.getProperty(key(level));
        if (v == null) return -1;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Store a finishing time only if it beats the best; returns true on a new best.
    public boolean submit(int level, long timeMs) {
        long best = getBest(level);
        if (best < 0 || timeMs < best) {
            props.setProperty(key(level), Long.toString(timeMs));
            save();
            return true;
        }
        return false;
    }

    // properties key for a level's best time
    private String key(int level) {
        return "level" + level + ".best";
    }
}
