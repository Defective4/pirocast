package io.github.defective4.rpi.pirocast;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileManager {

    public static interface UpdateListener {
        void indexUpdated(int newIndex, File file);
    }

    private boolean error;
    private final List<File> library = new ArrayList<>();
    private final UpdateListener ls;
    private int selectedFile = 0;

    public FileManager(UpdateListener ls) {
        Objects.requireNonNull(ls);
        this.ls = ls;
    }

    public File getSelectedFile() {
        if (!hasFiles()) return null;
        return library.get(selectedFile);
    }

    public boolean hasFiles() {
        return !library.isEmpty();
    }

    public boolean isMissingDirectory() {
        return error;
    }

    public void listAudioFiles(File directory) {
        library.clear();
        selectedFile = 0;
        error = false;
        if (directory.isDirectory()) {
            List<File> files = recursiveAudioList(directory);
            library.addAll(files);
        } else error = true;
        ls.indexUpdated(selectedFile, getSelectedFile());
    }

    public void nextFile(int direction) {
        selectedFile += direction;
        if (selectedFile < 0) selectedFile = library.size() - 1;
        if (selectedFile >= library.size()) selectedFile = 0;
        ls.indexUpdated(selectedFile, getSelectedFile());
    }

    private static List<File> recursiveAudioList(File dir) {
        List<File> fs = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) fs.addAll(recursiveAudioList(f));
            else {
                String ctype = URLConnection.guessContentTypeFromName(f.getName());
                if (ctype != null && ctype.startsWith("audio/")) {
                    fs.add(f);
                }
            }
        }
        return fs;
    }
}
