package com.dmytrohoi.pyplugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class PluginPythonDirectory extends PluginDataFile {

    /**
     * directory we represent
     */
    private final File dir;

    /**
     * @param dir directory we represent
     */
    public PluginPythonDirectory(File dir) {
        this.dir = dir;
    }

    @Override
    public InputStream getStream(String filename) throws IOException {
        File file = new File(dir, filename);
        if (!file.exists())
            return null;
        return new FileInputStream(file);
    }

    @Override
    public boolean shouldAddPathEntry() {
        return true;
    }

    @Override
    public boolean getNeedsSolidMeta() {
        return false;
    }

}
