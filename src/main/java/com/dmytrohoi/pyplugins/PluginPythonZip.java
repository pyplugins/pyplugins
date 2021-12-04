package com.dmytrohoi.pyplugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Override;
import java.lang.String;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.bukkit.plugin.InvalidPluginException;

/**
 *
 */
public class PluginPythonZip extends PluginDataFile {

    /**
     * Zipfile we belong to
     */
    public ZipFile zip;

    /**
     * Absolute path of the zipfile, for reloading purposes.
     * @author gdude2002
     */
    public String filepath = null;

    /**
     * @param file Zipfile we belong to
     * @throws InvalidPluginException thrown if there is an error opening zip
     */
    public PluginPythonZip(File file) throws InvalidPluginException {
        filepath = file.getAbsolutePath();  // Store the path of the file
        try {
            this.reload();
        }
        catch (IOException e) {
            throw new InvalidPluginException(e);
        }
    }

    /**
     * @throws IOException thrown if there is an error opening zip
     */

    @Override
    public void reload() throws IOException {
        if (closed) {
            File file = new File(filepath);
            zip = new ZipFile(file);
            closed = false;
        }
    }

    public void close() throws IOException {
        zip.close();
        closed = true;
    }

    @Override
    public InputStream getStream(String filename) throws IOException {
        ZipEntry inside = zip.getEntry(filename);
        if (inside == null)
            return null;
        return zip.getInputStream(inside);
    }

    @Override
    public boolean shouldAddPathEntry() {
        return true;
    }

    @Override
    public boolean getNeedsSolidMeta() {
        return true;
    }
}
