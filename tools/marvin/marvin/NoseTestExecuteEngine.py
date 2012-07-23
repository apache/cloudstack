import nose
import nose.core

class NoseTestExecuteEngine(object):
    """
    Runs the CloudStack tests using nose as the execution engine
    """
    
    def __init__(self, testclient=None, clientLog=None, resultLog=None):
            self.runner = \
            nose.core.TextTestRunner(stream=resultLog,
                                     descriptions=True, verbosity=2, config=self.cfg)
            
    def runTests(self):
         if self.workingdir is not None:
             nose.core.TestProgram(argv=options, testRunner=self.runner,
                                   config=self.cfg)
         elif self.filename is not None:
             tests = self.loader.loadTestsFromFile(self.filename)
             nose.core.TestProgram(argv=options, testRunner=self.runner,
                                   config=self.cfg)
         
