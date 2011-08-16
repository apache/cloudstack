import unittest
import random
from cloudstackAPI import *

class TestCase1(unittest.case.TestCase):

    def test_cloudstackapi(self):
        apiClient = self.testClient.getApiClient()
        listtmcmd = listTemplates.listTemplatesCmd()
        listtmcmd.id = 10
        listtmcmd.zoneid = 1
        listtmcmd.templatefilter = "featured"
        listtmresponse = apiClient.listTemplates(listtmcmd)
        if listtmresponse is not None and len(listtmresponse) > 0:
            self.debug(listtmresponse[0].isready)
            self.debug("we are here")
        else:
            self.debug("we are there")
            
            
