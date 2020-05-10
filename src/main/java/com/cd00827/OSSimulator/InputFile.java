package com.cd00827.OSSimulator;

import java.io.File;
import java.nio.file.Path;

public class InputFile {
    public File file;

    public InputFile(File file) {
        this.file = file;
    }

    public Path getPath() {
        return this.file.toPath();
    }

    @Override
    public String toString() {
        return this.file.getName();
    }
}
