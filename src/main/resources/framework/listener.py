from com.dmytrohoi.pyplugins import PythonListener as PythonListener_

from inspect import isfunction


__all__ = (
    'PythonListener',
)


class PythonListener(PythonListener_):
    """
    Event listener class. Modified __init__ for auto adding handlers
    from local methods which was decorated by PythonEventHandler.

    NOTE: Includes `plugin` attribute to get access to Plugin object
        by `self.plugin`.

    """
    # listeners = [PyEventHandler("method", "event", "priority")]
    listeners = []

    def __init__(self, arg = None):
        if isinstance(arg, list):
            self.listeners = arg
        elif arg is not None:
            self.install(arg)

    def __call__(self, plugin):
        self.install(plugin)

    def install(self, plugin):
        # NOTE: Add reference to PythonPlugin to use in listeners scopes
        self.plugin = plugin

        for listener in self.listeners:
            if isfunction(listener.method):
                handler = listener.method
            else:
                handler = getattr(self, listener.method)

            self.addHandler(
                handler,
                listener.event,
                listener.priority
            )
