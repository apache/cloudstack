# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from cloudstackConnection import CSConnection
import asyncJobMgr
from dbConnection import DbConnection
from cloudstackAPI import *
import random
import string
import hashlib
from codes import (FAILED, PASS, ADMIN, DOMAIN_ADMIN,
                   USER, SUCCESS, XEN_SERVER)
from configGenerator import ConfigManager
from marvin.lib import utils
from marvin.cloudstackException import GetDetailExceptionInfo
from marvin.lib.utils import (random_gen, validateList)
from marvin.cloudstackAPI.cloudstackAPIClient import CloudStackAPIClient


class CSTestClient(object):
    '''
    @Desc  : CloudStackTestClient is encapsulated entity for creating and
         getting various clients viz., apiclient,
         user api client, dbconnection, test Data parsed
         information etc
    @Input :
         mgmt_details : Management Server Details
         dbsvr_details: Database Server details of Management \
                       Server. Retrieved from configuration file.
         async_timeout : Timeout for Async queries
         default_worker_threads : Number of worker threads
         logger : provides logging facilities for this library
         zone : The zone on which test suites using this test client will run
    '''
    def __init__(self, mgmt_details,
                 dbsvr_details,
                 async_timeout=3600,
                 default_worker_threads=10,
                 logger=None,
                 test_data_filepath=None,
                 zone=None):
        self.__mgmtDetails = mgmt_details
        self.__dbSvrDetails = dbsvr_details
        self.__csConnection = None
        self.__dbConnection = None
        self.__testClient = None
        self.__asyncTimeOut = async_timeout
        self.__logger = logger
        self.__defaultWorkerThreads = default_worker_threads
        self.__apiClient = None
        self.__userApiClient = None
        self.__asyncJobMgr = None
        self.__id = None
        self.__testDataFilePath = test_data_filepath
        self.__parsedTestDataConfig = None
        self.__zone = zone

    @property
    def identifier(self):
        return self.__id

    @identifier.setter
    def identifier(self, id):
        self.__id = id

    def getParsedTestDataConfig(self):
        '''
        @Name : getParsedTestDataConfig
        @Desc : Provides the TestData Config needed for
                Tests are to Run
        @Output : Returns the Parsed Test Data Dictionary
        '''
        return self.__parsedTestDataConfig

    def getZoneForTests(self):
        '''
        @Name : getZoneForTests
        @Desc : Provides the Zone against which Tests are to run
                If zone name provided to marvin plugin is none
                it will get it from Test Data Config File
                Even, if  it is not available, return None
        @Output : Returns the Zone Name
        '''
        if self.__zone is None:
            if self.__parsedTestDataConfig is not None:
                self.__zone = self.__parsedTestDataConfig.get("zone")
        return self.__zone

    def __setHypervisorToClient(self):
        '''
        @Name : ___setHypervisorToClient
        @Desc:  Set the HyperVisor Details under API Client;
                default to Xen
        '''
        if self.__mgmtDetails.hypervisor:
            self.__apiClient.hypervisor = self.__mgmtDetails.hypervisor
        else:
            self.__apiClient.hypervisor = XEN_SERVER

    def __createApiClient(self):
        try:
            '''
            Step1 : Create a CS Connection Object
            '''
            self.__csConnection = CSConnection(self.__mgmtDetails,
                                               self.__asyncTimeOut,
                                               self.__logger)

            '''
            Step2 : Create API Client with earlier created connection object
            '''
            self.__apiClient = CloudStackAPIClient(self.__csConnection)

            '''
            Step3:  If API Key is not provided as part of Management Details,
                    then verify and register
            '''
            if self.__mgmtDetails.apiKey is None:
                list_user = listUsers.listUsersCmd()
                list_user.account = "admin"
                list_user_res = self.__apiClient.listUsers(list_user)
                if list_user_res == FAILED or list_user_res is None or\
                        (validateList(list_user_res)[0] != PASS):
                    self.__logger.error("__createApiClient: API "
                                        "Client Creation Failed")
                    return FAILED

                user_id = list_user_res[0].id
                api_key = list_user_res[0].apikey
                security_key = list_user_res[0].secretkey

                if api_key is None:
                    ret = self.__getKeys(user_id)
                    if ret != FAILED:
                        self.__mgmtDetails.port = 8080
                        self.__mgmtDetails.apiKey = ret[0]
                        self.__mgmtDetails.securityKey = ret[1]
                    else:
                        self.__logger.error("__createApiClient: API Client "
                                            "Creation Failed while "
                                            "Registering User")
                        return FAILED
                '''
                Now Create the Connection objects and Api Client using
                new details
                '''
                self.__csConnection = CSConnection(self.__mgmtDetails,
                                                   self.__asyncTimeOut,
                                                   self.__logger)
                self.__apiClient = CloudStackAPIClient(self.__csConnection)
            '''
            Set the HyperVisor Details to Client default to Xen
            '''
            self.__setHypervisorToClient()
            return SUCCESS
        except Exception, e:
            self.__logger.exception(" Exception Occurred Under "
                                    "__createApiClient: %s" %
                                    GetDetailExceptionInfo(e))
            return FAILED

    def __createDbConnection(self):
        '''
        @Name : ___createDbConnection
        @Desc : Creates the CloudStack DB Connection
        '''
        host = "localhost" if self.__dbSvrDetails.dbSvr is None \
            else self.__dbSvrDetails.dbSvr
        port = 3306 if self.__dbSvrDetails.port is None \
            else self.__dbSvrDetails.port
        user = "cloud" if self.__dbSvrDetails.user is None \
            else self.__dbSvrDetails.user
        passwd = 'cloud' if self.__dbSvrDetails.passd is None \
            else self.__dbSvrDetails.passd
        db = 'cloud' if self.__dbSvrDetails.db is None \
            else self.__dbSvrDetails.db
        self.__dbConnection = DbConnection(host, port, user, passwd, db)

    def __getKeys(self, userid):
        '''
        @Name : ___getKeys
        @Desc : Retrieves the API and Secret Key for the provided Userid
        '''
        try:
            register_user = registerUserKeys.registerUserKeysCmd()
            register_user.id = userid
            register_user_res = \
                self.__apiClient.registerUserKeys(register_user)
            if register_user_res == FAILED:
                return FAILED
            return (register_user_res.apikey, register_user_res.secretkey)
        except Exception, e:
            self.__logger.exception("Exception Occurred Under __geKeys : "
                                    "%s" % GetDetailExceptionInfo(e))
            return FAILED

    def createTestClient(self):
        '''
        @Name : createTestClient
        @Desc : Creates the Test Client.
                The test Client is used by test suites
                Here we create ParsedTestData Config.
                Creates a DB Connection.
                Creates an API Client
        @Output : FAILED In case of an issue\Failure
                  SUCCESS in case of Success of this function
        '''
        try:
            '''
            1. Create Config Object
               Provides the Configuration Object to test suites through
               getConfigParser. The purpose of this config object is to
               parse the default config and provide dictionary of the
               config so users can use that configuration.
               Users can later call getConfig on this object and it will
               return the default parsed config dictionary from default
               configuration file. They can overwrite it with
               providing their own configuration file as well.
            '''
            self.__configObj = ConfigManager(self.__testDataFilePath)
            if self.__configObj:
                self.__parsedTestDataConfig = self.__configObj.getConfig()
                self.__logger.debug("Parsing Test data successful")
            else:
                self.__logger.error("createTestClient : Not able to create "
                                    "ConfigManager Object")
                return FAILED
            '''
            2. Create DB Connection
            '''
            self.__createDbConnection()
            '''
            3. Creates API Client
            '''
            ret = self.__createApiClient()
            if ret == FAILED:
                self.__logger.\
                    error("********Test Client Creation Failed********")
            else:
                self.__logger.\
                    debug("********Test Client Creation Successful********")
            return ret
        except Exception, e:
            self.__logger.exception("Exception Occurred "
                                    "Under createTestClient "
                                    ": %s" % GetDetailExceptionInfo(e))
            return FAILED

    def isAdminContext(self):
        """
        @Name : isAdminContext
        @Desc:A user is a regular user if he fails to listDomains;
        if he is a domain-admin, he can list only domains that are non-ROOT;
        if he is an admin, he can list the ROOT domain successfully
        """
        try:
            listdom = listDomains.listDomainsCmd()
            listdom.name = 'ROOT'
            listdomres = self.__apiClient.listDomains(listdom)
            if listdomres != FAILED:
                rootdom = listdomres[0].name
                if rootdom == 'ROOT':
                    return ADMIN
                else:
                    return DOMAIN_ADMIN
            return USER
        except:
            return USER

    def __createUserApiClient(self, UserName, DomainName, acctType=0):
        '''
        @Name : ___createUserApiClient
        @Desc : Creates a User API Client with given
                UserName\DomainName Parameters
        '''
        try:
            if not self.isAdminContext():
                return self.__apiClient

            listDomain = listDomains.listDomainsCmd()
            listDomain.listall = True
            listDomain.name = DomainName
            try:
                domains = self.__apiClient.listDomains(listDomain)
                domId = domains[0].id
            except:
                cdomain = createDomain.createDomainCmd()
                cdomain.name = DomainName
                domain = self.__apiClient.createDomain(cdomain)
                domId = domain.id

            cmd = listAccounts.listAccountsCmd()
            cmd.name = UserName
            cmd.domainid = domId
            try:
                accounts = self.__apiClient.listAccounts(cmd)
                acctId = accounts[0].id
            except:
                createAcctCmd = createAccount.createAccountCmd()
                createAcctCmd.accounttype = acctType
                createAcctCmd.domainid = domId
                createAcctCmd.email = "test-" + random_gen()\
                    + "@cloudstack.org"
                createAcctCmd.firstname = UserName
                createAcctCmd.lastname = UserName
                createAcctCmd.password = 'password'
                createAcctCmd.username = UserName
                acct = self.__apiClient.createAccount(createAcctCmd)
                acctId = acct.id

            listuser = listUsers.listUsersCmd()
            listuser.username = UserName

            listuserRes = self.__apiClient.listUsers(listuser)
            userId = listuserRes[0].id
            apiKey = listuserRes[0].apikey
            securityKey = listuserRes[0].secretkey

            if apiKey is None:
                ret = self.__getKeys(userId)
                if ret != FAILED:
                    mgtDetails = self.__mgmtDetails
                    mgtDetails.apiKey = ret[0]
                    mgtDetails.securityKey = ret[1]
                else:
                    self.__logger.error("__createUserApiClient: "
                                        "User API Client Creation."
                                        " While Registering User Failed")
                    return FAILED

            newUserConnection =\
                CSConnection(mgtDetails,
                             self.__csConnection.asyncTimeout,
                             self.__csConnection.logger)
            self.__userApiClient = CloudStackAPIClient(newUserConnection)
            self.__userApiClient.connection = newUserConnection
            self.__userApiClient.hypervisor = self.__apiClient.hypervisor
            return self.__userApiClient
        except Exception, e:
            self.__logger.exception("Exception Occurred "
                                    "Under getUserApiClient : %s" %
                                    GetDetailExceptionInfo(e))
            return FAILED

    def close(self):
        if self.__csConnection is not None:
            self.__csConnection.close()

    def getDbConnection(self):
        '''
        @Name : getDbConnection
        @Desc : Retrieves the DB Connection Handle
        '''
        return self.__dbConnection

    def getConfigParser(self):
        '''
        @Name : getConfigParser
        @Desc : Provides the ConfigManager Interface to TestClients
        '''
        return self.__configObj

    def getApiClient(self):
        if self.__apiClient:
            self.__apiClient.id = self.identifier
            return self.__apiClient
        return None

    def getUserApiClient(self, account, domain, type=0):
        """
        @Name : getUserApiClient
        @Desc : Provides the User API Client to Users
        0 - user ; 1 - admin;2 - domain admin
        @OutPut : FAILED In case of an issue
                  else User API Client
        """
        return FAILED if (self.__createUserApiClient(account,
                                                     domain,
                                                     type)
                          == FAILED) \
            else self.__userApiClient

    def submitCmdsAndWait(self, cmds, workers=1):
        '''
        @Desc : FixME, httplib has issue if more than one thread submitted
        '''
        if self.__asyncJobMgr is None:
            self.__asyncJobMgr = asyncJobMgr.asyncJobMgr(self.__apiClient,
                                                         self.__dbConnection)
        return self.__asyncJobMgr.submitCmdsAndWait(cmds, workers)

    def submitJob(self, job, ntimes=1, nums_threads=10, interval=1):
        '''
        @Desc : submit one job and execute the same job
                ntimes, with nums_threads of threads
        '''
        if self.__asyncJobMgr is None:
            self.__asyncJobMgr = asyncJobMgr.asyncJobMgr(self.__apiClient,
                                                         self.__dbConnection)
        self.__asyncJobMgr.submitJobExecuteNtimes(job, ntimes,
                                                  nums_threads,
                                                  interval)

    def submitJobs(self, jobs, nums_threads=10, interval=1):
        '''
        @Desc :submit n jobs, execute them with nums_threads
               of threads
        '''
        if self.__asyncJobMgr is None:
            self.__asyncJobMgr = asyncJobMgr.asyncJobMgr(self.__apiClient,
                                                         self.__dbConnection)
        self.__asyncJobMgr.submitJobs(jobs, nums_threads, interval)
