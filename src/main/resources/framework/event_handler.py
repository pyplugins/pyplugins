from org.bukkit.event import EventPriority


__all__ = (
    'PyEventHandler',
)


class PyEventHandler(object):
    __slots__ = ('method', 'event', 'priority')

    def __init__(self, method, event, priority=EventPriority.NORMAL):
        self.method = method
        self.event = event
        self.priority = priority
