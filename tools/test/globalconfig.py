import json
import os
import random
import sys

class GlobalConfig:
    """Updates global configuration values"""
    def __init__(self, api):
        self._api = api

    def update(self, key, value):
        jsonresult = self._api.GET({'command': 'updateConfiguration', 'name':key,
                      'value':value})
        if  jsonresult is  None:
           print "Failed to update configuration"
           return 1

        return 0
