from cloudstackTestCase import *

class TestCase1(cloudstackTestCase):

    def test_cloudstackapi(self):
        apiClient = self.testClient.getApiClient()
        listtmcmd = listTemplates.listTemplatesCmd()
        listtmcmd.id = 10
        listtmcmd.zoneid = 1
        listtmcmd.templatefilter = "featured"
        listtmresponse = apiClient.listTemplates(listtmcmd)
        if listtmresponse is not None and len(listtmresponse) > 0:
            self.debug(listtmresponse)
            self.debug("we are here")
        else:
            self.debug("we are there")
            
        
