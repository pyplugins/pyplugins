package com.dmytrohoi.pyplugins;

import java.io.File;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.python.util.PythonInterpreter;

/**
 * Python Plugin representation
*/
public class PythonPlugin implements Plugin {
    private boolean isEnabled = false;
    private boolean initialized = false;
    private PluginLoader loader = null;
    private Server server = null;
    private File file = null;
    private PluginDescriptionFile description = null;
    private File dataFolder = null;
    private Configuration config = null;
    private boolean naggable = true;
    private FileConfiguration newConfig = null;
    private File configFile = null;
    private PluginLogger logger = null;
    private PluginDataFile dataFile = null; //data file used for retrieving resources

    /**
     * interpreter that was used to load this plugin.
     */
    PythonInterpreter interp;

    /**
     * Returns a value indicating whether or not this plugin is currently enabled
     *
     * @return true if this plugin is enabled, otherwise false
     */
    public final boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Sets the enabled state of this plugin
     *
     * @param enabled true if enabled, otherwise false
     */
    protected void setEnabled(final boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;

            if (isEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    public void onEnable() { }

    public void onDisable() { }

    /**
     * Overridable, is called after all plugins have been instantiated.
     */
    public void onLoad() {}

    /**
     * Gets the initialization status of this plugin
     *
     * @return true if this plugin is initialized, otherwise false
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Initializes this plugin with the given variables.
     *
     * This method should never be called manually.
     *
     * @param loader PluginLoader that is responsible for this plugin
     * @param server Server instance that is running this plugin
     * @param description PluginDescriptionFile containing metadata on this plugin
     * @param dataFolder Folder containing the plugin's data
     * @param file File containing this plugin
     * @param classLoader ClassLoader which holds this plugin
     */
    protected final void initialize(PluginLoader loader, Server server, PluginDescriptionFile description, File dataFolder, File file ) {
        if (initialized) {
            return;
        }

        this.initialized = true;
        this.loader = loader;
        this.server = server;
        this.file = file;
        this.description = description;
        this.dataFolder = dataFolder;
        this.config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
    }

    /**
     * Gets the associated PluginLoader responsible for this plugin
     *
     * @return PluginLoader that controls this plugin
     */
    public final PluginLoader getPluginLoader() {
        return loader;
    }

    /**
     * Returns the Server instance currently running this plugin
     *
     * @return Server running this plugin
     */
    public final Server getServer() {
        return server;
    }

    /**
     * Returns the file which contains this plugin
     *
     * @return File containing this plugin
     */
    protected File getFile() {
        return file;
    }

    /**
     * Returns the plugin.yaml file containing the details for this plugin
     *
     * @return Contents of the plugin.yaml file
     */
    public PluginDescriptionFile getDescription() {
        return description;
    }

    /**
     * Returns the folder that the plugin data's files are located in. The
     * folder might not yet exist.
     *
     * @return File data folder
     */
    public File getDataFolder() {
        return dataFolder;
    }


    /**
     * Returns the main configuration located at
     * <plugin name>/config.yml and loads the file. If the configuration file
     * does not exist and it cannot be loaded, no error will be emitted and
     * the configuration file will have no values.
     *
     * @return The FileConfiguration.
     */
    public FileConfiguration getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    /**
     * {@inheritDoc}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    /**
     * Gets the command with the given name, specific to this plugin
     *
     * @param name Name or alias of the command
     * @return PluginCommand if found, otherwise null
     */
    public PluginCommand getCommand(String name) {
        String alias = name.toLowerCase();
        PluginCommand command = getServer().getPluginCommand(alias);

        if ((command != null) && (command.getPlugin() != this)) {
            command = getServer().getPluginCommand(getDescription().getName().toLowerCase() + ":" + alias);
        }

        if ((command != null) && (command.getPlugin() == this)) {
            return command;
        } else {
            return null;
        }
    }

    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        getServer().getLogger().severe("Plugin " + getDescription().getFullName() + " does not contain any generators that may be used in the default world!");
        return null;
    }

    public final boolean isNaggable() {
        return naggable;
    }

    public final void setNaggable(boolean canNag) {
        this.naggable = canNag;
    }

    public Logger getLogger() {
        String prefix = getDescription().getPrefix();
        return Logger.getLogger(prefix != null ? prefix : getName());
    }

    @Override
    public String toString() {
        return getDescription().getFullName();
    }

    @Override
    public InputStream getResource(String filename) {
        if(filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            dataFile.reload();
            return dataFile.getStream(filename);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + getFile());
        }

        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
        configFile = outFile;
    }

    public void reloadConfig() {
        if (configFile != null){
            newConfig = YamlConfiguration.loadConfiguration(configFile);

            InputStream defConfigStream = getResource("config.yml");

            if (defConfigStream != null) {
                Reader targetReader = new InputStreamReader(defConfigStream);
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(targetReader);

                newConfig.setDefaults(defConfig);
            }
        } else {
            InputStream defConfigStream = getResource("config.yml");
            if (defConfigStream != null) {
                Reader targetReader = new InputStreamReader(defConfigStream);
                newConfig = YamlConfiguration.loadConfiguration(targetReader);
            }
        }
    }

    public void saveConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            Logger.getLogger(
                PythonPlugin.class.getName()
                ).log(Level.SEVERE,
                      "Could not save config to " + configFile, ex);
        }
    }

    public void saveDefaultConfig() {
        saveResource("config.yml", false);
    }

    protected void setDataFile(PluginDataFile file) {
        this.dataFile = file;
    }

    @Override
    public String getName() {
        return getDescription().getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return null;
    }
}
