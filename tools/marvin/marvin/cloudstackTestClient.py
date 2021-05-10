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

from marvin.cloudstackConnection import CSConnection
from marvin.asyncJobMgr import asyncJobMgr
from marvin.dbConnection import DbConnection
from marvin.cloudstackAPI import *
from marvin.codes import (FAILED, PASS, ADMIN, DOMAIN_ADMIN,
                          USER, SUCCESS, XEN_SERVER)
from marvin.configGenerator import ConfigManager
from marvin.cloudstackException import GetDetailExceptionInfo
from marvin.lib.utils import (random_gen, validateList)
from marvin.cloudstackAPI.cloudstackAPIClient import CloudStackAPIClient
import copy

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
                 logger=None,
                 test_data_filepath=None,
                 zone=None,
                 hypervisor_type=None):
        self.__mgmtDetails = mgmt_details
        self.__dbSvrDetails = dbsvr_details
        self.__csConnection = None
        self.__dbConnection = None
        self.__testClient = None
        self.__asyncTimeOut = async_timeout
        self.__logger = logger
        self.__apiClient = None
        self.__userApiClient = None
        self.__asyncJobMgr = None
        self.__id = None
        self.__hypervisor = hypervisor_type
        self.__testDataFilePath = test_data_filepath
        self.__parsedTestDataConfig = None
        self.__zone = zone
        self.__setHypervisorInfo()

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
        return copy.deepcopy(self.__parsedTestDataConfig)

    def getZoneForTests(self):
        '''
        @Name : getZoneForTests
        @Desc : Provides the Zone against which Tests are to run
                If zone name provided to marvin plugin is none
                it will get it from Test Data Config File
                Even, if  it is not available, return None
        @Output : Returns the Zone Name
        '''
        return self.__zone

    def getHypervisorInfo(self):
        '''
        @Name : getHypervisorInfo
        @Desc : Provides the hypervisor Information to test users
        @Output : Return Hypervisor Information
        '''
        return self.__hypervisor

    def __setHypervisorInfo(self):
        '''
        @Name : __setHypervisorInfo
        @Desc:  Set the HyperVisor details;
                default to XenServer
        '''
        try:
            if not self.__hypervisor:
                self.__hypervisor = XEN_SERVER
            return SUCCESS
        except Exception as e:
            print("\n Exception Occurred Under __setHypervisorInfo " \
                  "%s" % GetDetailExceptionInfo(e))
            return FAILED

    def __createApiClient(self):
        try:
            '''
            Step1 : Create a CS Connection Object
            '''
            mgmt_details = self.__mgmtDetails
            self.__csConnection = CSConnection(mgmt_details,
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
            if mgmt_details.apiKey is None:
                list_user = listUsers.listUsersCmd()
                list_user.account = "admin"
                list_user_res = self.__apiClient.listUsers(list_user)
                if list_user_res is None or\
                        (validateList(list_user_res)[0] != PASS):
                    self.__logger.error("__createApiClient: API "
                                        "Client Creation Failed")
                    return FAILED

                getuser_keys = getUserKeys.getUserKeysCmd()
                getuser_keys.id = list_user_res[0].id
                getuser_keys_res = self.__apiClient.getUserKeys(getuser_keys)
                if getuser_keys_res is None :
                    self.__logger.error("__createApiClient: API "
                                        "Client Creation Failed")
                    return FAILED

                api_key = getuser_keys_res.apikey
                security_key = getuser_keys_res.secretkey

                user_id = list_user_res[0].id
                if api_key is None:
                    ret = self.__getKeys(user_id)
                    if ret != FAILED:
                        mgmt_details.apiKey = ret[0]
                        mgmt_details.securityKey = ret[1]
                    else:
                        self.__logger.error("__createApiClient: API Client "
                                            "Creation Failed while "
                                            "Registering User")
                        return FAILED
                else:
                    mgmt_details.port = 8080
                    mgmt_details.apiKey = api_key
                    mgmt_details.securityKey = security_key
                '''
                Now Create the Connection objects and Api Client using
                new details
                '''
                self.__csConnection = CSConnection(mgmt_details,
                                                   self.__asyncTimeOut,
                                                   self.__logger)
                self.__apiClient = CloudStackAPIClient(self.__csConnection)
            return SUCCESS
        except Exception as e:
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
        passwd = 'cloud' if self.__dbSvrDetails.passwd is None \
            else self.__dbSvrDetails.passwd
        db = 'cloud' if self.__dbSvrDetails.db is None \
            else self.__dbSvrDetails.db
        self.__dbConnection = DbConnection(host, port, user, passwd, db)

    def __getKeys(self, userid):
        '''
        @Name : ___getKeys
        @Desc : Retrieves the API and Secret Key for the provided Userid
        @Input: userid: Userid to register
        @Output: FAILED or tuple with apikey and secretkey
        '''
        try:
            register_user = registerUserKeys.registerUserKeysCmd()
            register_user.id = userid
            register_user_res = \
                self.__apiClient.registerUserKeys(register_user)
            if not register_user_res:
                return FAILED

            getuser_keys = getUserKeys.getUserKeysCmd()
            getuser_keys.id = userid
            getuser_keys_res = self.__apiClient.getUserKeys(getuser_keys)
            if getuser_keys_res is None :
                self.__logger.error("__createApiClient: API "
                                "Client Creation Failed")
                return FAILED

            api_key = getuser_keys_res.apikey
            security_key = getuser_keys_res.secretkey
            return (api_key, security_key)
        except Exception as e:
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
            '''
            1. Check Config,Zone,Hypervisor Information
            '''
            self.__configObj = ConfigManager(self.__testDataFilePath)

            if not self.__configObj or not self.__hypervisor:
                self.__logger.error("createTestClient : "
                                    "Either Hypervisor is None or "
                                    "Not able to create "
                                    "ConfigManager Object")
                return FAILED

            self.__parsedTestDataConfig = self.__configObj.getConfig()
            self.__logger.debug("Parsing Test data successful")

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
                    error("==== Test Client Creation Failed ====")
            else:
                self.__logger.\
                    debug("==== Test Client Creation Successful ====")
            return ret
        except Exception as e:
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
        @Input: UserName: Username to be created in cloudstack
                DomainName: Domain under which the above account be created
                accType: Type of Account EX: Root,Non Root etc
        @Output: Return the API client for the user
        '''
        try:
            if not self.isAdminContext():
                return self.__apiClient
            mgmt_details = self.__mgmtDetails
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
            cmd.listall = True
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
            listuser.domainid = domId
            listuser.listall = True

            listuserRes = self.__apiClient.listUsers(listuser)
            userId = listuserRes[0].id

            getuser_keys = getUserKeys.getUserKeysCmd()
            getuser_keys.id = listuserRes[0].id
            getuser_keys_res = self.__apiClient.getUserKeys(getuser_keys)
            if getuser_keys_res is None or\
                (validateList(getuser_keys_res) != PASS):
                self.__logger.error("__createApiClient: API "
                                "Client Creation Failed")

            apiKey = getuser_keys_res.apikey
            securityKey = getuser_keys_res.secretkey

            if apiKey is None:
                ret = self.__getKeys(userId)
                if ret != FAILED:
                    mgmt_details.apiKey = ret[0]
                    mgmt_details.securityKey = ret[1]
                else:
                    self.__logger.error("__createUserApiClient: "
                                        "User API Client Creation."
                                        " While Registering User Failed")
                    return FAILED
            else:
                mgmt_details.port = 8080
                mgmt_details.apiKey = apiKey
                mgmt_details.securityKey = securityKey

            newUserConnection =\
                CSConnection(mgmt_details,
                             self.__csConnection.asyncTimeout,
                             self.__csConnection.logger)
            self.__userApiClient = CloudStackAPIClient(newUserConnection)
            self.__userApiClient.connection = newUserConnection
            self.__userApiClient.hypervisor = self.__hypervisor
            return self.__userApiClient
        except Exception as e:
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

    def getUserApiClient(self, UserName=None, DomainName=None, type=0):
        """
        @Name : getUserApiClient
        @Desc : Provides the User API Client to test Users
        0 - user ; 1 - admin;2 - domain admin
        @OutPut : FAILED In case of an issue
                  else User API Client
        """
        if UserName is None or DomainName is None:
            return FAILED
        return self.__createUserApiClient(UserName, DomainName, type)

    def submitCmdsAndWait(self, cmds, workers=1, apiclient=None):
        '''
        @Desc : FixME, httplib has issue if more than one thread submitted
        '''
        if not apiclient:
            apiclient = self.__apiClient
        if self.__asyncJobMgr is None:
            self.__asyncJobMgr = asyncJobMgr(apiclient,
                                             self.__dbConnection)
        return self.__asyncJobMgr.submitCmdsAndWait(cmds, workers)

    def submitJob(self, job, ntimes=1, nums_threads=10, interval=1):
        '''
        @Desc : submit one job and execute the same job
                ntimes, with nums_threads of threads
        '''
        if self.__asyncJobMgr is None:
            self.__asyncJobMgr = asyncJobMgr(self.__apiClient,
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
            self.__asyncJobMgr = asyncJobMgr(self.__apiClient,
                                             self.__dbConnection)
        self.__asyncJobMgr.submitJobs(jobs, nums_threads, interval)
