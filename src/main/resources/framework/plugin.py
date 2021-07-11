from com.dmytrohoi.pyplugins import PythonPlugin as PythonPlugin_
from com.dmytrohoi.pyplugins.bukkit import Metrics as Metrics_

# NOTE: For development purposes
# from command_executor import PythonCommandExecutor
# from configuration import ConfigurationCommands


__all__ = (
    'PythonPlugin',
)


# =========================================================================== #
# Extend PythonPlugin with framework methods
class PythonPlugin(PythonPlugin_):
    def __init__(self, *args, **kwargs):
        super(PythonPlugin, self).__init__(*args, **kwargs)
        self.bukkit = Bukkit

    def get_server(self):
        return self.bukkit.getServer()

    def add_commands(self, commands):
        new_commands = PythonCommandExecutor(commands=commands)
        self.apply_command_executor(new_commands)

    def add_listeners(self, listeners):
        pm = self.get_server().getPluginManager()
        for listener in listeners:
            pm.registerEvents(listener(self), self)

    def apply_command_executor(self, command_executor):
        command_executor(self)

    def add_configuration(self, available_options=[], config_class=ConfigurationCommands):
        config_class(self, available_options)
        try:
            self.saveDefaultConfig()
        except:
            self.logger.info('Config already exists.')

        if hasattr(self, 'default_config'):
            config = self.getConfig()
            for key, options in self.default_config.items():
                config.addDefault(key, options.get("value"))

            config.options().copyDefaults(True)
            self.saveConfig()

    def add_bstats(self, pluginId):
        """Add bStats metrics for this plugin.

        ..note:
            Usage: add this command to your PythonPlugin class as:
            ..code: python
                class SomePythonPlugin(PythonPlugin):

                    def onEnable(self):
                        self.add_bstats(123456789)

        :param int pluginId: bStats plugin identifier
        """
        Metrics_(self, pluginId)
