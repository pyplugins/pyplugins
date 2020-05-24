# /local/bin/python
# -*- coding: utf-8 -*-
"""
File: __before__.py
Version: 0.0.1
Author: hedgehoi (Dmytro Hoi)
License: GNU License

"""
# Standart Python import
import sys
from collections import namedtuple

# Let Jython know about encoding
from org.python.core import codecs
codecs.setDefaultEncoding('utf-8')

# Bukkit/Spigot classes
from org.bukkit import Bukkit
from org.bukkit.event import EventPriority
from org.bukkit.command import CommandExecutor as CommandExecutor_,\
                                TabCompleter as TabCompleter_
from java.util.logging import LogRecord, Level

# PyPlugin classes
from com.dmytrohoi.pyplugins import PythonPlugin as PythonPlugin_,\
                                    PythonListener as PythonListener_

# bStats
from com.dmytrohoi.pyplugins.bukkit import Metrics_

# =========================================================================== #
# Internal classes
PyEventHandler_ = namedtuple("PyEventHandler", ["method", "event", "priority"])
def PyEventHandler(method, event, priority=EventPriority.NORMAL):
    return PyEventHandler_(method, event, priority)


class PythonListener(PythonListener_):
    """
    Event listener class. Modified __init__ for auto adding handlers
    from local methods which was decorated by PythonEventHandler.

    NOTE: Includes `plugin` attribute to get access to Plugin object
        by `self.plugin`.

    """
    # listeners = [PyEventHandler("method", "event", "priority")]
    listeners = []

    def __init__(self, plugin, *args, **kwargs):
        self.plugin = plugin

        for listener in self.listeners:
            self.addHandler(
                getattr(self, listener.method),
                listener.event,
                listener.priority
            )


# Declare PyCommand dataclass
PyCommand_ = namedtuple("PyCommand", ["name", "executer", "completer"])
def PyCommand(name, executor, completer=None):
    return PyCommand_(name, executor, completer)


class PythonCommandExecutor(CommandExecutor_, TabCompleter_):
    """
    CommandExecutor class. Make somethink like Event API for Commands.

    Usage: Make custom plugin CommandExecutor class with parrent this class.
        Add methods to custom plugin CommandExecutor class and decorate methods
        by PythonCommandHandler.

    NOTE: Includes `plugin` attribute to get access to Plugin object
        by `self.plugin`.

    Link: https://www.spigotmc.org/wiki/create-a-simple-command/

    """
    # commands = [PyCommand("command", "commandMethod", "onTabCompleteMethod")]
    commands = []
    _cache = {}

    def __init__(self, plugin):
        self.plugin = plugin

        for command in self.commands:
            plugin_command = self.plugin.getCommand(command.name)
            if not plugin_command:
                self.plugin.logger.severe(
                    "Command {} not added to plugin.yml!".format(command.name)
                )
                continue

            plugin_command.setExecutor(self)
            if command.completer is not None:
                plugin_command.setTabCompleter(self)

            self._cache[command.name] = command

    def onCommand(self, sender, command, label, args):
        command_options = self._cache.get(command.getName())
        command_method = getattr(self, command_options.executer)
        return command_method(sender, command, label, args)

    def onTabComplete(self, sender, command, alias, args):
        command_options = self._cache.get(command.getName())
        command_method = getattr(self, command_options.completer)
        return command_method(sender, command, alias, args)


# =========================================================================== #
class ConfigurationCommands_(PythonCommandExecutor):
    commands = []
    sub_commands = {}
    options = {}

    def __init__(self, plugin, available_options=[], command=None):
        if not command:
            prefix = plugin.getDescription().getPrefix() or plugin.getName()
            command = prefix.lower() + '-' + 'config'

        self.commands.append(
            PyCommand(command,
                      'config',
                      'config_completer')
        )
        super(ConfigurationCommands_, self).__init__(plugin)

        for raw_option in available_options:
            if isinstance(raw_option, (tuple, list)):
                option = {raw_option[0]: raw_option[1]}
            elif isinstance(raw_option, str):
                option = {raw_option: str}
            else:
                self.plugin.logger.severe(
                    'Could not load comfiguration commands! Please change your ' \
                    'available_options by PyPlugins Documentaion.'
                )
                return

            self.options.update(option)

        if self.options:
            options_update = {
                'params': self._params_subcommand,
                'set': self._set_subcommand
            }
        else:
            options_update = {}
        # Configuration sub-commands
        sub_commands = {'reload': self._reload_subcommand}
        sub_commands.update(options_update)

        self.sub_commands.update(sub_commands)

    def config(self, sender, command, label, args):
        if not sender.isOp():
            sender.sendMessage("§4 You don't have permission for this command!")
            return True

        if not args or args[0] not in self.sub_commands.keys():
            sender.sendMessage('Available commands: {}'.format(
                               ', '.join(self.sub_commands)))
            return True

        elif args[0] in self.sub_commands.keys():
            sub_command = args.pop(0)
            self.sub_commands[sub_command](sender, args)

    def _reload_subcommand(self, sender, args):
        if args:
            sender.sendMessage('§4 Arguments not provided!')
            return False

        self.plugin.reloadConfig()
        sender.sendMessage('§2Configuration reloaded!')
        return True

    def _params_subcommand(self, sender, args):
        if args:
            sender.sendMessage('§4 Arguments not provided!')
            return False
        config = self.plugin.getConfig()
        params = []
        for option in self.options.keys():
            params.append('{}={}'.format(option, config.get(option)))
        sender.sendMessage('Current params: {}'.format(", ".join(params)))
        return True

    def _set_subcommand(self, sender, args):
        arguments_length = len(args)
        config = self.plugin.getConfig()

        if arguments_length > 2:
            sender.sendMessage('§4 Invalid arguments count!')

        elif arguments_length == 2 and args[0] in self.options:
            key = args[0]
            value = self.options.get(key)(args[1])

            try:
                config.set(key, value)
            except Exception as e:
                sender.sendMessage('§4 {}!'.format(e))
                return True
            sender.sendMessage('§2 Param {} now is {}!'.format(key, value))
            config.options().copyDefaults(True)
            self.plugin.saveConfig()
            self.plugin.reloadConfig()
            return True

        sender.sendMessage('Params available to set: {}'.format(
            ', '.join(self.options.keys())
        ))
        return True

    def config_completer(self, sender, command, alias, args):
        if not sender.isOp():
            return []

        sub_command = args[0]
        args_count = len(args)
        if not sub_command:
            return self.sub_commands.keys()

        elif args_count == 1:
            return filter(lambda ch: ch.startswith(args[0]),
                          self.sub_commands.keys())

        elif args_count == 2 and sub_command == 'set':
            return filter(lambda ch: ch.startswith(args[1]),
                          self.options.keys())

        elif args_count == 3 and sub_command == 'set' and not args[2]:
            return [str(value_type) for value_type in self.options.values()]

        return []


# =========================================================================== #
# Extend PythonPlugin with framework methods
class PythonPlugin(PythonPlugin_):
    def __init__(self, *args, **kwargs):
        super(PythonPlugin, self).__init__(*args, **kwargs)

    def apply_command_executor(self, command_executor):
        command_executor(self)

    def add_configuration(self, available_options=[], config_class=ConfigurationCommands_):
        config_class(self, available_options)
        try:
            self.saveDefaultConfig()
        except:
            self.logger.info('Config alredy exists.')

        if hasattr(self, 'default_config'):
            config = self.getConfig()
            for key, options in self.default_config.items():
                config.addDefault(key, options.get("value"))

            config.options().copyDefaults(True)
            self.saveConfig()

    def add_bstats(self, pluginId):
        Metrics_(self, pluginId)

# =========================================================================== #
# Redirect all standart stdout functions to Java.logging
# 1) Declare new stdout
class DebugPrintStdoutRedirect(object):
    """
    Redirect all standart stdout functions to default server logger with
    prefix '[PyPlugins] [PRINT] '.

    """
    def write(self, text):
        prefix = '[PyPlugins] [PRINT] '
        if text.endswith('\n'):
            text = text[:-1]

        # TODO: Remove duplication for print()
        Bukkit.getServer().getLogger().info(prefix + text)

# 2) Set new stdout
sys.stdout = DebugPrintStdoutRedirect()

# End of __before__.py script
# =========================================================================== #
