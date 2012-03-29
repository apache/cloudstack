import cloudstackConnection
import asyncJobMgr
import dbConnection
import uuid
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
    
    def createNewApiClient(self, UserName, DomainName, acctType):
        listDomain = listDomains.listDomainsCmd()
        listDomain.name = DomainName
        try:
            domains = self.apiClient.listDomains(listDomain)
            domId = domains[0].id
        except:
            cdomain = createDomain.createDomainCmd()
            cdomain.name = DomainName
            domain = self.apiClient.createDomain(cdomain)
            domId = domain.id
        
        cmd = listAccounts.listAccountsCmd()
        cmd.name = UserName
        exist = True
        try:
            accounts = self.apiClient.listAccounts(cmd)
            acctId = accounts[0].id
        except:
            createAcctCmd = createAccount.createAccountCmd()
            createAcctCmd.accounttype = acctType
            createAcctCmd.domainid = domId
            createAcctCmd.email = str(uuid.uuid4()) + "@citrix.com"
            createAcctCmd.firstname = UserName
            createAcctCmd.lastname = UserName
            createAcctCmd.password = "password"
            createAcctCmd.username = UserName
            acct = self.apiClient.createAccount(createAcctCmd)
            acctId = acct.id
        
        listuser = listUsers.listUsersCmd() 
        listuser.username = UserName
        
        listuserRes = self.apiClient.listUsers(listuser)
        userId = listuserRes[0].id
        apiKey = listuserRes[0].apikey
        securityKey = listuserRes[0].secretkey
     
        if apiKey is None:
            registerUser = registerUserKeys.registerUserKeysCmd()
            registerUser.id = userId
            registerUserRes = self.apiClient.registerUserKeys(registerUser)
            apiKey = registerUserRes.apikey
            securityKey = registerUserRes.secretkey
        
        nConnection = cloudstackConnection.cloudConnection(self.connection.mgtSvr, self.connection.port, apiKey, securityKey, self.connection.asyncTimeout, self.connection.logging)
        self.connection.close()
        self.connection = nConnection
        
        self.apiClient = cloudstackAPIClient.CloudStackAPIClient(self.connection)
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