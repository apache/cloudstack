from cloudstackTestCase import *
from cloudstackAPI import *
class TestListHost(cloudstackTestCase):

	def test_listHost(self):
		apiClient = self.testClient.getApiClient()
		listhostcmd=listHosts.listHostsCmd()
		listhostcmd.zoneid=1
		listhostcmd.type="Routing"
		listhostresponse = apiClient.listHosts(listhostcmd)
		
		self.debug("Total Number of Hosts: " + str(len(listhostresponse)))
			
		for i in listhostresponse:
			self.debug("id: " + i.id +" pod id: " + i.podid +" host tag: " + i.hosttags)