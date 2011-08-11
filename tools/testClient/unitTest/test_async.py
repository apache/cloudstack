from cloudstackAPI import *
import cloudstackException
import cloudstackTestClient
import sys

    
class jobs():
    def __init__(self, zoneId):
        self.zoneId = zoneId
        
    def run(self):
        try:
            cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
            cmd.id = 4
            self.apiClient.destroyVirtualMachine(cmd)
        except cloudstackException.cloudstackAPIException, e:
            print str(e)
        except :
            print sys.exc_info()

if __name__ == "__main__":
    ''' to logging the testclient
    logger = logging.getLogger("test_async")
    fh = logging.FileHandler("test.log")
    logger.addHandler(fh)
    logger.setLevel(logging.DEBUG)
    testclient = cloudstackTestClient.cloudstackTestClient(mgtSvr="localhost", logging=logger)
    '''
    testclient = cloudstackTestClient.cloudstackTestClient(mgtSvr="localhost")
    testclient.dbConfigure()
    api = testclient.getApiClient()
    
    #testclient.submitJobs(jobs(1), 10, 10, 1)

    cmds = []
    for i in range(20):
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = 4 + i
        cmds.append(cmd)
    
    asyncJobResult = testclient.submitCmdsAndWait(cmds)
    
    for handle, jobStatus in asyncJobResult.iteritems():
        if jobStatus.status:
            print jobStatus.result.id, jobStatus.result.templatename, jobStatus.startTime, jobStatus.endTime
        else:
            print jobStatus.result, jobStatus.startTime, jobStatus.endTime
            
        print jobStatus.duration