from cloudstackAPI import *
try:
    import unittest2 as unittest
except ImportError:
    import unittest
import cloudstackTestClient

class cloudstackTestCase(unittest.case.TestCase):
    clstestclient = None
    
    def __init__(self, args):
        unittest.case.TestCase.__init__(self, args)
        self.testClient = cloudstackTestClient.cloudstackTestClient()
        
    @classmethod
    def getClsTestClient(cls):
        return cls.clstestclient
