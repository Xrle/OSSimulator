package com.cd00827.OSSimulator;

import java.io.File;
import java.nio.file.Path;

/**
 * Wrapper for input files
 * @author cd00827
 */
public class InputFile {
    public File file;

    /**
     * Constructor
     * @param file File
     */
    public InputFile(File file) {
        this.file = file;
    }

    /**
     * Get the path to this file
     * @return Path
     */
    public Path getPath() {
        return this.file.toPath();
    }

    /**
     * Get the file's name
     * @return File name
     */
    @Override
    public String toString() {
        return this.file.getName();
    }
}
