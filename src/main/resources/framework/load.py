# /local/bin/python
# -*- coding: utf-8 -*-
"""
File: load.py
Version: 0.0.1
Author: hedgehoi (Dmytro Hoi)
License: GNU License

"""
# Standart Python import
import sys
from os.path import abspath, sep

sys.path.append(abspath('.') + sep + "plugins" + sep + "pyplugins-libs")

# Let Jython know about encoding
from org.python.core import codecs
codecs.setDefaultEncoding('utf-8')

# Bukkit/Spigot classes
from org.bukkit import Bukkit

# =========================================================================== #
# Redirect all standart stdout functions to Java.logging
# 1) Declare new stdout
class DebugPrintStdoutRedirect(object):
    """
    Redirect all standart stdout functions to default server logger with
    prefix '[PyPlugins] [PRINT]:'.

    """
    def write(self, text):
        # TODO: Remove duplication for print()
        Bukkit.getServer().getLogger().info(
            "[PyPlugins] [PRINT]: {}".format(text.rstrip())
        )

# 2) Set new stdout
sys.stdout = DebugPrintStdoutRedirect()

# End of __before__.py script
# =========================================================================== #
