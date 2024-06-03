WARNING! This repository has been archived and will not be maintained AND/OR updated for new versions of Spigot. The main reason is that Jython still does not support the Python 3 version. Another reason: the plugin was implemented (literally constructed, original authors and projects was mentioned in this file and saved as authors in code) from several unsupported plugins at the time of the release of Minecraft version 1.15 for use on a single public server and was used as a minimalistic/pythonic way to integrate plugins, written in Python, in Minecraft. I never fully considered myself the author of this code, for those who want to continue it - the minimum requirement for updating to a new version may be to add the correct dependencies to the pom file.
====================

PyPlugins - Python Plugin Loader
====================

[![GitHub](https://img.shields.io/github/license/pyplugins/pyplugins)](https://github.com/pyplugins/pyplugins/blob/master/LICENSE)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/pyplugins/pyplugins)](https://github.com/pyplugins/pyplugins/releases)
[![GitHub Release Date](https://img.shields.io/github/release-date/pyplugins/pyplugins)](https://github.com/pyplugins/pyplugins/releases)
[![GitHub All Releases](https://img.shields.io/github/downloads/pyplugins/pyplugins/total?label=github%20downloads)](https://github.com/pyplugins/pyplugins/releases)
[![Spigot](https://img.shields.io/badge/spigot-1.15.2-orange)](https://www.spigotmc.org/resources/pyplugins.79244/)
[![Spiget Downloads](https://img.shields.io/spiget/downloads/79244?label=spigot%20downloads)](https://www.spigotmc.org/resources/pyplugins.79244/)
[![Spiget Stars](https://img.shields.io/spiget/rating/79244)](https://www.spigotmc.org/resources/pyplugins.79244/)
[![Spiget tested server versions](https://img.shields.io/spiget/tested-versions/79244)](https://www.spigotmc.org/resources/pyplugins.79244/)
[![bStats Players](https://img.shields.io/bstats/players/7627)](https://bstats.org/plugin/bukkit/PyPlugins/7627)
[![bStats Servers](https://img.shields.io/bstats/servers/7627)](https://bstats.org/plugin/bukkit/PyPlugins/7627)

<p align="center">
<img src="./docs/img/logo.png">
</p>

PyPlugins - is a plugins loader for Bukkit/Spigot (PaperMC) to load
plugins are written on Python 2.7 (via Jython 2.7.2).

The creation of the plugin was inspired by [Macuyiko/minecraft-python](https://github.com/Macuyiko/minecraft-python), [masteroftime/Python-Plugin-Loader](https://github.com/masteroftime/Python-Plugin-Loader) and [cyberlis/pploader](https://github.com/cyberlis/pploader).


## Installation
-----------------------

### PyPlugins (loader)
*******

Available two versions of the loader plugin:
 - with included Jython (recommended to use);
 - without Jython (requires inclusion of `jython.jar` in the folder `server/lib/`)

Steps:
1. Put `PyPlugins-with[out]-Jython-*.*.*.jar` ([download link](https://github.com/pyplugins/pyplugins/releases)) in your `server/plugins/` directory
2. Run server

### Python plugins install
*************

1. Put the `<PluginName>.pyplugin` sources (directory or zip file) in your `server/plugins/` directory
2. Run server


Create plugin on Python
===========

The PyPlugins contains a few way to create plugin for Spigot on Python.

It's can be:
 1. Clear Java-like approach (just write the code as a Java plugin, but using Python);
 2. Using internal pyplugins-framework (recomended);

First approach examples you can see [here](https://www.spigotmc.org/wiki/spigot-plugin-development/).

The next paragrphes about internal pyplugins-framework.

## Plugin sources

Your plugin can use the following paths to the plugin source code:

- A zip whos name ends in either `.pyplugin.zip`, `_pyplugin.zip` or just `.pyplugin`
- A directory whose name ends in `.pyplugin` or `_pyplugin` (actual for windows users)

Zips with the `.pyplugin` extension are recommended if you release any plugins. When
you use a zip, your must specify your own metadata; it will not allow guessed
metadata.

When using a dir or a zip, your zip or dir must contain a main python file
(with names: `main.py`, `plugin.py`, `__main__.py` or `__init__.py`) and
a `plugin.yml` configuration file containing metadata (see the following section).


## Plugin metadata

Plugins require metadata. The absolute minimum metadata is a name version and main class.
The 'main' field of plugin metadata has special behavior:

- the main is used to search for a main class before searching the default
   class name.

`plugin.yml` is able to set all metadata fields that exist
in bukkit, and should be used for all plugins that you release. plugin.yml is
used in all java plugins (as it is the only option for java plugins). as such,
opening up java plugin jars is a good way to learn what can go in it.
Or you can read about it here http://wiki.bukkit.org/Plugin_YAML
Here isan example of plugin.yml:

    name: SamplePlugin
    main: SampleClass
    version: 0.1-dev
    commands:
        samplecommand:
            description: send a sample message
            usage: /<command>

Summary of fields:

- "main" - name of main python file or name of main class
- "name" - name of plugin to show in /plugins list and such. used to name the
   config directory. for this reason it must not equal the full name of the
   plugin file.
- "version" - version of plugin. shown in errors, and other plugins can access it
- "website" - mainly for people reading the code


## Clear Java-like approach
---------------------------

Minimum requirements:

- Main class have to be extended from PythonPlugin class. (You don't have to
import it, because it is auto imported on startup of loader plugin).
- Your main class must have onEnable() and onDisable() methods.

[Code example and learn more..](https://github.com/pyplugins/pyplugins/wiki/Java-like-approach)


## The pyplugins-framework approach
---------------------------

The same as Java-like minimum requirements:

- Main class have to be extended from PythonPlugin class. (You don't have to
import it, because it is auto imported on startup of loader plugin).
- Your main class must have onEnable() and onDisable() methods.

Handlers are available to easily create your Python plugin:

- **PythonCommandExecutor class** (CommandsAPI)

    You can inherit your own `PluginNameCommandExecutor` class from `PythonCommandExecutor` to make handlers for "executeCommand" and "onTabComplete" actions (the command must be declared in `plugin.yml`).  Just create methods for these actions and make the `commands` attribute of your `PluginNameCommandExecutor` class with instances of `PyCommand` class (with command and methods names).
    Also can be used as commands list acceptor (functional approach) and able to get `PyCommand`s as first argument on initialization.

- **PythonListener class** (EventsAPI)

    Similar to CommandsAPI, but with `PythonListener` class as parent, the `listeners` attribute (for save your handlers) of class with instances of `PyEventHandler` (requires name of method to execute, Bukkit event object and (optional) Bukkit ptiority object).
    Also can be used as listener list acceptor (functional approach) and able to get `PyEventHandler`s as first argument on initialization.

[And more! Read..](https://github.com/pyplugins/pyplugins/wiki/pyplugins-framework)

Links
============

- [Wiki](https://github.com/pyplugins/pyplugins/wiki)

- [Plugins List](https://github.com/pyplugins/pyplugins-list)

- [FAQ](https://github.com/pyplugins/pyplugins/wiki/FAQ)

- [Spigot Resource Page](https://www.spigotmc.org/resources/pyplugins.79244/)

Donate
============

Follow **Sponsor** button on GitHub page.

![Statistics](https://bstats.org/signatures/bukkit/PyPlugins.svg)

Development
============

**NOTE:**

[bStats](https://github.com/Bastian/bStats-Metrics) version 2.x.x does not allow the use of the `org.bukkit.plugin.Plugin` class for plugins and requires `org.bukkit.plugin.java.JavaPlugin`.

PyPlugins currently uses bStats 1.8, and needs to be added from the local jar:

``` bash
mvn install:install-file -Dfile=bstats-bukkit-1.8.jar -DgroupId=org.bstats -DartifactId=bstats-bukkit -Dversion=1.8 -Dpackaging=jar
```

-------

Author: @dmytrohoi
