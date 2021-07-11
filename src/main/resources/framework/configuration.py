
__all__ = (
    'ConfigurationCommands',
)


# =========================================================================== #
class ConfigurationCommands(PythonCommandExecutor):
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
        super(ConfigurationCommands, self).__init__(plugin)

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
