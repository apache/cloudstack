from cloudstackTestCase import *
from cloudstackAPI import *
import uuid

class TestDeployVM(cloudstackTestCase):
	
	def createServiceOfferings(self, name, tag):
		apiClient = self.testClient.getApiClient()
		createSOcmd=createServiceOffering.createServiceOfferingCmd()
		createSOcmd.name=name
		createSOcmd.displaytext=name
		createSOcmd.storagetype="local"
		createSOcmd.cpunumber=1
		createSOcmd.cpuspeed=512
		createSOcmd.memory=7168
		createSOcmd.offerha="false"
		createSOcmd.hosttags=tag
		createSOresponse = apiClient.createServiceOffering(createSOcmd)
		return createSOresponse.id 
		
	def createSecurityGroups(self,name):
		apiClient = self.testClient.getApiClient()
		createSGcmd=createSecurityGroup.createSecurityGroupCmd()
		createSGcmd.account="admin"
		createSGcmd.domainid="1"
		createSGcmd.description=name
		createSGcmd.name=name
		createSGresponse= apiClient.createSecurityGroup(createSGcmd)
		return createSGresponse.id 
		
	def createSecurityGroupIngressRules(self,sgId,sgIdList,cidrlist):
		print sgId +" : " + sgIdList
		apiClient = self.testClient.getApiClient()
		sgIngresscmd=authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
		sgIngresscmd.account="admin"
		sgIngresscmd.domainid="1"
		sgIngresscmd.cidrlist = cidrlist
		sgIngresscmd.startport = "80"
		sgIngresscmd.endport = "80"
		sgIngresscmd.protocol = "TCP"
		sgIngresscmd.securitygroupid = sgId
		sgIngresscmd.usersecuritygrouplist = sgIdList
		sgIngressresponse=apiClient.authorizeSecurityGroupIngress(sgIngresscmd)
		return sgIngressresponse.id 
		
			
	def test_depVm(self):
		so=[]
		so1 = self.createServiceOfferings("S01","TAG1")
		so2 = self.createServiceOfferings("S02","TAG2")
		so3 = self.createServiceOfferings("S02","TAG3")
		
		so.append(so1)
		so.append(so1)
		so.append(so1)
		so.append(so1)
		so.append(so1)
		so.append(so2)
		so.append(so2)
		so.append(so3)
		so.append(so3)
		so.append(so3)
		so.append(so3)
		so.append(so3)
		
		sgName= "Test-"+ str(uuid.uuid4())
		sgId = self.createSecurityGroups(sgName)
		sgIdList=[]
		sgIdList.append(sgId)
		#self.createSecurityGroupIngressRules(sgId,sgIdList,None)
		soCount=0
		depVmcmdList=[]
		for i in range(100):
			depVmcmd=deployVirtualMachine.deployVirtualMachineCmd()
			depVmcmd.userdata="U29uZyBUbyBCZSBTdW5nIGJ5IHRoZSBGYXRoZXIgb2YgSW5mYW50IEZlbWFsZSBDaGlsZHJlbgpieSBPZ2RlbiBOYXNoCgpNeSBoZWFydCBsZWFwcyB1cCB3aGVuIEkgYmVob2xkCkEgcmFpbmJvdyBpbiB0aGUgc2t5OwpDb250cmFyaXdpc2UsIG15IGJsb29kIHJ1bnMgY29sZApXaGVuIGxpdHRsZSBib3lzIGdvIGJ5LgpGb3IgbGl0dGxlIGJveXMgYXMgbGl0dGxlIGJveXMsCk5vIHNwZWNpYWwgaGF0ZSBJIGNhcnJ5LApCdXQgbm93IGFuZCB0aGVuIHRoZXkgZ3JvdyB0byBtZW4sCkFuZCB3aGVuIHRoZXkgZG8sIHRoZXkgbWFycnkuCk5vIG1hdHRlciBob3cgdGhleSB0YXJyeSwKRXZlbnR1YWxseSB0aGV5IG1hcnJ5LgpBbmQsIHN3aW5lIGFtb25nIHRoZSBwZWFybHMsClRoZXkgbWFycnkgbGl0dGxlIGdpcmxzLgoKT2gsIHNvbWV3aGVyZSwgc29tZXdoZXJlLCBhbiBpbmZhbnQgcGxheXMsCldpdGggcGFyZW50cyB3aG8gZmVlZCBhbmQgY2xvdGhlIGhpbS4KVGhlaXIgbGlwcyBhcmUgc3RpY2t5IHdpdGggcHJpZGUgYW5kIHByYWlzZSwKQnV0IEkgaGF2ZSBiZWd1biB0byBsb2F0aGUgaGltLgpZZXMsIEkgbG9hdGhlIHdpdGggbG9hdGhpbmcgc2hhbWVsZXNzClRoaXMgY2hpbGQgd2hvIHRvIG1lIGlzIG5hbWVsZXNzLgpUaGlzIGJhY2hlbG9yIGNoaWxkIGluIGhpcyBjYXJyaWFnZQpHaXZlcyBuZXZlciBhIHRob3VnaHQgdG8gbWFycmlhZ2UsCkJ1dCBhIHBlcnNvbiBjYW4gaGFyZGx5IHNheSBrbmlmZQpCZWZvcmUgaGUgd2lsbCBodW50IGhpbSB"
			depVmcmd.zoneid=1
			depVmcmd.templateid=2
			depVmcmd.serviceofferingid=so[soCount]
			depVmcmd.securitygroupids=sgId
			depVmcmd.domainid=1
			depVmcmd.account="admin"
			depVmcmdList.append(depVmcmd)
			if soCount == 11:
				soCount = 0
			else:
				soCount = soCount + 1
				
		asyncJobResult=self.testClient.submitCmdsAndWait(depVmcmdList)	
		for handle, jobStatus in asyncJobResult.iteritems():
			if jobStatus.status:
				self.debug("Job Id: " + str(jobStatus.result[0].id) + "Status: " + str(jobStatus.result[0].state) + " Start Time: "+str(jobStatus.startTime)+" End Time: "+ str(jobStatus.endTime)+ " Response Time: " + str(jobStatus.duration))
			else:
				self.debug("Status: " +str(jobStatus.result)+" Start Time: "+str(jobStatus.startTime)+" End Time: "+ str(jobStatus.endTime)+ " Response Time: " + str(jobStatus.duration))
				
		
	