package com.dmytrohoi.pyplugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;
import org.bukkit.plugin.UnknownDependencyException;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * A jython plugin loader. depends on JavaPluginLoader and SimplePluginManager.
 */
public class PythonPluginLoader implements PluginLoader {

    private final Server server;

    /**
     * Filter - matches all of the following, for the regex illiterate:
     *
     * For directories:
     * <pre>
     * plugin_pyplugin
     * plugin.pyplugin
     *
     * Zip File:
     * plugin_pyplugin.zip
     * plugin.pyplugin.zip
     * plugin.pyplugin
     * </pre>
     */
    public static final Pattern[] fileFilters = new Pattern[] {
            Pattern.compile("^(.*)\\.pyplugin$"),
            Pattern.compile("^(.*)_pyplugin$"),
            Pattern.compile("^(.*)\\.pyplugin\\.zip$"),
            Pattern.compile("^(.*)_pyplugin\\.zip$"),
    };

    private HashSet<String> loadedplugins = new HashSet<String>();

    /**
     * @param server server to initialize with
     */
    public PythonPluginLoader(Server server) {
        this.server = server;
    }

    public Plugin loadPlugin(File file) throws InvalidPluginException/*, UnknownDependencyException*/ {
        return loadPlugin(file, false);
    }

    public Plugin loadPlugin(File file, boolean ignoreSoftDependencies)
            throws InvalidPluginException/*, InvalidDescriptionException, UnknownDependencyException*/ {

        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(file.getPath() + " does not exist"));
        }

        PluginDataFile data = null;

        String file_name = file.getName();
        if ((file.getName().endsWith(".pyplugin") || file.getName().endsWith("_pyplugin")) && file.isDirectory()) {
            data = new PluginPythonDirectory(file);
        } else if ((file.getName().endsWith(".pyplugin") || file.getName().endsWith(".pyplugin.zip") || file.getName().endsWith("_pyplugin.zip")) && !file.isDirectory())  {
            data = new PluginPythonZip(file);
        }

        try {
            return loadPlugin(file, ignoreSoftDependencies, data);
        } finally {
            try {
                data.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Properties setDefaultPythonPath(Properties props, String file_path) {
        String pythonPathProp = props.getProperty("python.path");
        String new_value;

        if (pythonPathProp == null) {
            new_value  = file_path;
        } else {
            new_value = pythonPathProp + java.io.File.pathSeparator + file_path + java.io.File.pathSeparator;
        }

        props.setProperty("python.path", new_value);
        return props;
    }

    private Plugin loadPlugin(File file, boolean ignoreSoftDependencies, PluginDataFile data) throws InvalidPluginException/*, InvalidDescriptionException, UnknownDependencyException*/ {
        Properties props;
    	System.out.println("[PyPlugins] Loading Plugin " + file.getName());
        PythonPlugin result = null;
        PluginDescriptionFile description = null;

        try {
            InputStream stream = data.getStream("plugin.yml");
            if (stream == null){
                throw new InvalidPluginException(new Exception("You must include plugin.yml!"));
            }
            description = new PluginDescriptionFile(stream);
            if (stream != null)
                stream.close();
        } catch (IOException ex) {
            throw new InvalidPluginException(ex);
        } catch (YAMLException ex) {
            throw new InvalidPluginException(ex);
        } catch (InvalidDescriptionException ex) {
            throw new InvalidPluginException(ex);
        }

        File dataFolder = new File(file.getParentFile(), description.getName());

        if (dataFolder.getAbsolutePath().equals(file.getAbsolutePath())) {
            throw new InvalidPluginException(new Exception(String.format("Projected datafolder: '%s' for %s is the same file as the plugin itself (%s)",
                    dataFolder,
                    description.getName(),
                    file)));
        }

        if (dataFolder.exists() && !dataFolder.isDirectory()) {
            throw new InvalidPluginException(new Exception(String.format("Projected datafolder: '%s' for %s (%s) exists and is not a directory",
                    dataFolder,
                    description.getName(),
                    file)));
        }

        List<String> depend;

        try {
            depend = description.getDepend();
            if (depend == null) {
                depend = new ArrayList<>();
            }
        } catch (ClassCastException ex) {
            throw new InvalidPluginException(ex);
        }

        for (String pluginName : depend) {
            if (!isPluginLoaded(pluginName)) {
                throw new UnknownDependencyException(pluginName);
            }
        }
        props = PySystemState.getBaseProperties();
        props = setDefaultPythonPath(props, file.getAbsolutePath());

        PySystemState state = new PySystemState();
        state.initialize(System.getProperties(), props, null);
        PyList pythonpath = state.path;
        PyString filepath = new PyString(file.getAbsolutePath());
    	pythonpath.append(filepath);

        List<String> mainfile_names = Arrays.asList("plugin.py", "main.py", "__main__.py", "__init__.py");
        InputStream instream = null;
        for (String mainfile_name : mainfile_names){
            try {
                instream = data.getStream(mainfile_name);
                if (instream == null) {
                    continue;
                }
                break;
            } catch (IOException e) {
                throw new InvalidPluginException(e);
            }
        }

        if (instream == null) {
            throw new InvalidPluginException(new FileNotFoundException("Can not find: " + String.join(", ", mainfile_names) + "."));
        }

        try {
            PyDictionary table = new PyDictionary();
            PythonInterpreter interp = new PythonInterpreter(table, state);

            String[] before_plugin_scripts = {"__before__.py"};
            String[] after_plugin_scripts = {"__after__.py"};

            // Run scripts designed to be run before plugin creation
            for (String script : before_plugin_scripts) {
	            InputStream metastream = this.getClass().getClassLoader().getResourceAsStream("python/" + script);
	            interp.execfile(metastream);
	            metastream.close();
            }

            interp.execfile(instream);
            instream.close();

            String mainclass = description.getMain();
            PyObject pyClass = interp.get(mainclass);
            if (pyClass == null) {
                pyClass = interp.get("Plugin");
                if (pyClass == null) {
                    throw new InvalidPluginException(new Exception("Can not find Mainclass."));
                }
            } else {
                result = (PythonPlugin) pyClass.__call__().__tojava__(PythonPlugin.class);
            }
            interp.set("PYPLUGIN", result);

            result.interp = interp;

            // Run scripts designed to be run after plugin creation
            for (String script : after_plugin_scripts) {
	            InputStream metastream = this.getClass().getClassLoader().getResourceAsStream("python/" + script);
	            interp.execfile(metastream);
	            metastream.close();
            }

            result.initialize(this, server, description, dataFolder, file);
            result.setDataFile(data);

        } catch (Throwable t) {
            throw new InvalidPluginException(t);
        }

        String plugin_name = description.getName();
        if (!loadedplugins.contains(plugin_name)) {
            loadedplugins.add(plugin_name);
        } else {
            throw new InvalidPluginException("Some plugins with name " + plugin_name + " is loaded!");
        }
        return result;
    }

    private boolean isPluginLoaded(String name) {
        if (loadedplugins.contains(name))
            return true;
        if (ReflectionHelper.isJavaPluginLoaded(server.getPluginManager(), name))
            return true;
        return false;
    }

    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }

    public void disablePlugin(Plugin plugin) {
        if (!(plugin instanceof PythonPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }

        if (plugin.isEnabled()) {
            PythonPlugin pyPlugin = (PythonPlugin) plugin;

            try {
                pyPlugin.setEnabled(false);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred while disabling " + plugin.getDescription().getFullName()
                                + " (Is it up to date?): " + ex.getMessage(), ex);
            }

            server.getPluginManager().callEvent(new PluginDisableEvent(plugin));

            String pluginName = pyPlugin.getDescription().getName();
            if (loadedplugins.contains(pluginName))
                loadedplugins.remove(pluginName);
        }
    }

    public void enablePlugin(Plugin plugin) {
        if (!(plugin instanceof PythonPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }

        if (!plugin.isEnabled()) {
            PythonPlugin pyPlugin = (PythonPlugin) plugin;

            String pluginName = pyPlugin.getDescription().getName();

            if (!loadedplugins.contains(pluginName))
                loadedplugins.add(pluginName);

            try {
                pyPlugin.setEnabled(true);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred while enabling " + plugin.getDescription().getFullName()
                                + " (Is it up to date?): " + ex.getMessage(), ex);
            }

            // Perhaps abort here, rather than continue going, but as it stands,
            // an abort is not possible the way it's currently written
            server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        boolean useTimings = server.getPluginManager().useTimings();
        Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
        PythonListener pyListener = (PythonListener)listener;

        for(Map.Entry<Class<? extends Event>, Set<PythonEventHandler>> entry : pyListener.handlers.entrySet()) {
            Set<RegisteredListener> eventSet = new HashSet<RegisteredListener>();

            for(final PythonEventHandler handler : entry.getValue()) {
                EventExecutor executor = new EventExecutor() {
                    @Override
                    public void execute(Listener listener, Event event) throws EventException {
                        ((PythonListener)listener).fireEvent(event, handler);
                    }
                };
                if(useTimings) {
                    eventSet.add(new TimedRegisteredListener(pyListener, executor, handler.priority, plugin, false));
                } else {
                    eventSet.add(new RegisteredListener(pyListener, executor, handler.priority, plugin, false));
                }
            }
            ret.put(entry.getKey(), eventSet);
        }
        return ret;
    }

    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");

        InputStream stream = null;
        PluginDataFile data = null;


        String file_name = file.getName();
        if ((file.getName().endsWith(".pyplugin") || file.getName().endsWith("_pyplugin")) && file.isDirectory()) {
            data = new PluginPythonDirectory(file);
        } else if ((file.getName().endsWith(".pyplugin") || file.getName().endsWith(".pyplugin.zip") || file.getName().endsWith("_pyplugin.zip")) && !file.isDirectory())  {
            try {
                data = new PluginPythonZip(file);
            } catch (InvalidPluginException ex) {
                throw new InvalidDescriptionException(ex);
            }
        }

        try {
            stream = data.getStream("plugin.yml");
            if(stream == null) {
                throw new InvalidDescriptionException(new InvalidPluginException(new FileNotFoundException("Plugin does not contain plugin.yml")));
            }
            return new PluginDescriptionFile(stream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {}
            }
        }
        return null;
    }
}
