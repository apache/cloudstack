import cloudstackConnection
import asyncJobMgr
import dbConnection
from cloudstackAPI import * 

class cloudstackTestClient(object):
    def __init__(self, mgtSvr=None, port=8096, apiKey = None, securityKey = None, asyncTimeout=3600, defaultWorkerThreads=10, logging=None):
        self.connection = cloudstackConnection.cloudConnection(mgtSvr, port, apiKey, securityKey, asyncTimeout, logging)
        self.apiClient = cloudstackAPIClient.CloudStackAPIClient(self.connection)
        self.dbConnection = None
        self.asyncJobMgr = None
        self.ssh = None
        self.defaultWorkerThreads = defaultWorkerThreads
        
        
    def dbConfigure(self, host="localhost", port=3306, user='cloud', passwd='cloud', db='cloud'):
        self.dbConnection = dbConnection.dbConnection(host, port, user, passwd, db)
        
    
    def getDbConnection(self):
        return self.dbConnection

    def executeSql(self, sql=None):
        if sql is None or self.dbConnection is None:
            return None
        
        return self.dbConnection.execute()
    
    def executeSqlFromFile(self, sqlFile=None):
        if sqlFile is None or self.dbConnection is None:
            return None
        return self.dbConnection.executeSqlFromFile(sqlFile)
    
    def getApiClient(self):
        return self.apiClient
    
    def submitCmdsAndWait(self, cmds):
        if self.asyncJobMgr is None:
            self.asyncJobMgr = asyncJobMgr.asyncJobMgr(self.apiClient, self.dbConnection)
        return self.asyncJobMgr.submitCmdsAndWait(cmds)
    
    def submitJobs(self, job, ntimes=1, nums_threads=10, interval=1):
        if self.asyncJobMgr is None:
            self.asyncJobMgr = asyncJobMgr.asyncJobMgr(self.apiClient, self.dbConnection)
        self.asyncJobMgr.submitJobs(job, ntimes, nums_threads, interval)