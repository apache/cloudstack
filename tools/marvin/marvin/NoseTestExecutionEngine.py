try:
    import unittest2 as unittest
except ImportError:
    import unittest

from functools import partial
import nose
import nose.config
import nose.core
import os
import sys
import logging

module_logger = "testclient.nose"

def testCaseLogger(message, logger=None):
    if logger is not None:
        logger.debug(message)

class TestCaseExecuteEngine(object):
    def __init__(self, testclient, testCaseFolder, testcaseLogFile=None, testResultLogFile=None):
        self.testclient = testclient
        self.debuglog = testcaseLogFile
	self.testCaseFolder = testCaseFolder
	self.testResultLogFile = testResultLogFile
	self.cfg = nose.config.Config()
	self.cfg.configureWhere(self.testCaseFolder)
	self.cfg.configureLogging()

    def run(self):
	self.args =  ["--debug-log="+self.debuglog]
	suite = nose.core.TestProgram(argv = self.args, config = self.cfg)
        result = suite.runTests()
        print result
