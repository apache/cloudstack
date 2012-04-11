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
        
    def close(self):
        if self.connection is not None:
            self.connection.close()
        if self.dbConnection is not None:
            self.dbConnection.close()
        
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
    
    '''FixME, httplib has issue if more than one thread submitted'''
    def submitCmdsAndWait(self, cmds, workers=1):
        if self.asyncJobMgr is None:
            self.asyncJobMgr = asyncJobMgr.asyncJobMgr(self.apiClient, self.dbConnection)
        return self.asyncJobMgr.submitCmdsAndWait(cmds, workers)
    
    '''submit one job and execute the same job ntimes, with nums_threads of threads'''
    def submitJob(self, job, ntimes=1, nums_threads=10, interval=1):
        if self.asyncJobMgr is None:
            self.asyncJobMgr = asyncJobMgr.asyncJobMgr(self.apiClient, self.dbConnection)
        self.asyncJobMgr.submitJobExecuteNtimes(job, ntimes, nums_threads, interval)
        
    '''submit n jobs, execute them with nums_threads of threads'''    
    def submitJobs(self, jobs, nums_threads=10, interval=1):
        if self.asyncJobMgr is None:
            self.asyncJobMgr = asyncJobMgr.asyncJobMgr(self.apiClient, self.dbConnection)
        self.asyncJobMgr.submitJobs(jobs, nums_threads, interval)