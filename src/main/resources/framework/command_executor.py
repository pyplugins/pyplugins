from org.bukkit.command import CommandExecutor as CommandExecutor_, \
                                TabCompleter as TabCompleter_


from inspect import isfunction


__all__ = (
    'PythonCommandExecutor',
)


class PythonCommandExecutor(CommandExecutor_, TabCompleter_):
    """
    CommandExecutor class. Make somethink like Event API for Commands.

    Usage: Make custom plugin CommandExecutor class with parrent this class.
        Add methods to custom plugin CommandExecutor class and decorate methods
        by PythonCommandHandler.

    ..note:
        Class approach includes `plugin` attribute to get access to Plugin object
        by `self.plugin`.

        https://www.spigotmc.org/wiki/create-a-simple-command/

    """
    # commands = [PyCommand("command", "commandMethod", "onTabCompleteMethod")]

    def __init__(self, arg = None):
        """
        Creates new instance of PythonCommandExecutor class (functional approach)
        or install executor to PythonPlugin (class approach).

        :param Optional(Union(List[PyCommand], PythonPlugin)) arg:
            command list for instance usage or PythonPlugin instance for class approach
        """
        # super(PythonCommandExecutor, self).__init__(*args, **kwargs)

        if isinstance(arg, list):
            self.commands = arg

        elif arg is not None:
            self.install(arg)

    def __call__(self, plugin):
        self.install(plugin)

    def _reorganize(self):
        """Create dictionary from commands list and store in self.commands."""
        self.commands = {command.name: command for command in self.commands}

    def install(self, plugin):
        """

        :param PythonPlugin plugin: framework PythonPlugin class object
        """
        # NOTE: Add reference to PythonPlugin to use in executors scopes
        self.plugin = plugin

        if isinstance(self.commands, list):
            self._reorganize()

        for command_name, command_data in self.commands.items():
            plugin_command = plugin.getCommand(command_name)
            if not plugin_command:
                plugin.logger.severe(
                    "Command {} not added to plugin.yml!".format(command_name)
                )
                continue

            plugin_command.setExecutor(self)
            if command_data.completer is not None:
                plugin_command.setTabCompleter(self)

    def onCommand(self, sender, command, label, args):
        command_ = self.commands[command.getName()]
        if isfunction(command_.executor):
            return command_.executor(sender, command, label, args)

        executor = getattr(self, command_.executor)
        return executor(sender, command, label, args)

    def onTabComplete(self, sender, command, alias, args):
        command_ = self.commands[command.getName()]
        if isfunction(command_.completer):
            return command_.completer(sender, command, alias, args)

        completer = getattr(self, command_.completer)
        return completer(sender, command, alias, args)
