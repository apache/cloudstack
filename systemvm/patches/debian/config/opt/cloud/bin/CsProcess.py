import os
import re
import CsHelper
import logging

class CsProcess(object):
    """ Manipulate processes """

    def __init__(self, search):
        self.search = search

    def start(self, thru, background = ''):
        #if(background):
            #cmd = cmd + " &"
        logging.info("Started %s", " ".join(self.search))
        os.system("%s %s %s" % (thru, " ".join(self.search), background))

    def find(self):
        self.pid = []
        for i in CsHelper.execute("ps aux"):
            items = len(self.search)
            proc = re.split("\s+", i)[items*-1:]
            matches = len([m for m in proc if m in self.search])
            if matches == items:
                self.pid.append(re.split("\s+", i)[1])
        return len(self.pid) > 0

