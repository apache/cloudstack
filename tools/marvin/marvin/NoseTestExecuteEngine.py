import unittest
import nose
import nose.core
import os
import sys
import logging
import xmlrunner
from cloudstackTestCase import cloudstackTestCase
from marvinPlugin import MarvinPlugin
from nose.plugins.xunit import Xunit
from nose.plugins.attrib import AttributeSelector
from nose.plugins.multiprocess import MultiProcessTestRunner

class NoseTestExecuteEngine(object):
    """
    Runs the CloudStack tests using nose as the execution engine
    """
    
    def __init__(self, testclient=None, workingdir=None, filename=None, clientLog=None, resultLog=None, format="text", xmlDir='xml-reports'):
        self.testclient = testclient
        self.logformat = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s")
        self.suite = []

        if clientLog is not None:
            self.logfile = clientLog
            self.logger = logging.getLogger("NoseTestExecuteEngine")
            fh = logging.FileHandler(self.logfile) 
            fh.setFormatter(self.logformat)
            self.logger.addHandler(fh)
            self.logger.setLevel(logging.DEBUG)
        if resultLog is not None:
            ch = logging.StreamHandler()
            ch.setLevel(logging.ERROR)
            ch.setFormatter(self.logformat)
            self.logger.addHandler(ch)
            fp = open(resultLog, "w")
            self.testResultLogFile = fp
        else:
            self.testResultLogFile = sys.stderr
 
        if workingdir is not None:
            self.workingdir = workingdir
            self.test_picker = MarvinPlugin()
            self.test_picker.setClient(self.testclient)
            self.test_picker.setClientLog(self.logfile)
        elif filename is not None:
            self.test_picker = MarvinPlugin()
            self.test_picker.setClient(self.testclient)
            self.test_picker.setClientLog(self.logfile)
            self.filename = filename
        else:
            raise EnvironmentError("Need to give either a test directory or a test file")
        
        plug_mgr = nose.plugins.manager.PluginManager()
        plug_mgr.addPlugin(self.test_picker)
        plug_mgr.addPlugin(Xunit())
        plug_mgr.addPlugin(AttributeSelector())
        plug_mgr.addPlugin(MultiProcessTestRunner())
        self.cfg = nose.config.Config()
        self.cfg.plugins = plug_mgr
        
        if format == "text":
            self.runner = \
            nose.core.TextTestRunner(stream=self.testResultLogFile,
                                     descriptions=1, verbosity=2, config=self.cfg)
        else:
            self.runner = xmlrunner.XMLTestRunner(output=xmlDir, verbose=True)
            
    def runTests(self):
         options = ["--process-timeout=3600", "--with-xunit", "-a tags=advanced", "--processes=5"] #TODO: Add support for giving nose args
         #DEBUG
#         options = ["--process-timeout=3600", "--with-xunit", "--collect-only"]
         #DEBUG
#         options = ["--process-timeout=3600"]
         options.append("-w%s" %self.workingdir)
         
         if self.workingdir is not None:
             nose.core.TestProgram(argv=options, testRunner=self.runner,
                                   config=self.cfg)
         elif self.filename is not None:
             tests = self.loader.loadTestsFromFile(self.filename)
             nose.core.TestProgram(argv=options, testRunner=self.runner,
                                   config=self.cfg)
         
