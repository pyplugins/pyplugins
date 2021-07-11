"""
Contains PyCommand class that provide storing of Command data.

(c) 2021, Dmytro Hoi, MIT License
"""


__all__ = (
    'PyCommand',
)


class PyCommand(object):
    __slots__ = ('name', 'executor', 'completer')

    def __init__(self, name, executor, completer = None):
        self.name = name
        self.executor = executor
        self.completer = completer
