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

"""
Base class for NCC Orchestration
"""
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import *
from marvin.lib.base import Domain, Account
from marvin.lib.utils import validateList
from marvin.codes import PASS,FAILED
from marvin.cloudstackException import (InvalidParameterException,
                                        GetDetailExceptionInfo)
from os import system
from subprocess import call
import requests, json, urllib.request, urllib.parse, urllib.error

class NCC:

    def __init__(self, nccip, nsip, csip, logger=None):
        self.nccip = nccip
        self.nsip = nsip
        self.csip = csip
        self.logger = logger
        self.__lastError = ''

    def registerCCP(self, apiclient):
        """
        Register CCP with NCC
        """
        auth_keys = self.getAdminKeys(apiclient)
        url = "http://"+self.nccip+"/cs/cca/v1/cloudstacks"
        cs_url = "http://"+self.csip+":8080/"
        payload = {"cloudstack": {
            "name": "Cloudstack",
            "apikey": auth_keys[0],
            "secretkey": auth_keys[1],
            "driver_username": "admin",
            "driver_password": "nsroot",
            "cloudstack_uri": cs_url
            }
        }
        cmd_response = self.sendCmdToNCC(url, payload)
        if cmd_response == FAILED:
            raise Exception("Error:  %s" % self.__lastError)

    def registerNS(self):
        url = "http://"+self.nccip+"/nitro/v1/config/managed_device/"
        payload = 'object={"params":{"action":"add_device"}, "managed_device":{"ip_address":"%s",\
                  "profile_name":"ns_nsroot_profile", "sync_operation":"false"}}' % self.nsip
        headers = {'Content-Type': 'application/x-www-form-urlencoded'}
        cmd_response = self.sendCmdToNS(url, payload, header=headers)
        if cmd_response == FAILED:
            raise Exception("Error:  %s" % self.__lastError)

    def assignNStoCSZone(self):
        cs_zone = self.getCSZoneFromNCC()
        if cs_zone == FAILED:
            raise Exception("Error:  %s" % self.__lastError)
        url = "http://"+self.nccip+"/nitro/v1/config/tag/"
        payload = 'object={"tag": {"entity_type": "managed_device", "entity_id": "%s",\
                  "tag_key": "zone", "tag_value": "%s"}}' % (self.nsip, cs_zone)
        header = {'Content-Type':'application/x-www-form-urlencoded'}
        cmd_response = self.sendCmdToNS(url, payload, header=header)
        if cmd_response == FAILED:
            raise Exception("Error:  %s" % self.__lastError)

    def createServicePackages(self, name, platform_type, device_ip, isolation_policy="shared"):
        tnt_group = self.createTenantGroup(name)
        if tnt_group.status_code != 201:
            raise Exception("Error:  %s" % self.__lastError)
        tnt_group_id = json.loads(tnt_group.content)["tenantgroups"][0]["id"]
        dv_group = self.createDeviceGroup(name, platform_type)
        if dv_group.status_code != 201:
            raise Exception("Error:  %s" % self.__lastError)
        dv_group_id = json.loads(dv_group.content)["devicegroups"][0]["id"]
        if isolation_policy.lower() == "shared":
            srv_pkg = self.createServicePackageShared(name, tnt_group_id, dv_group_id, isolation_policy )
        elif isolation_policy.lower() == "dedicated":
            srv_pkg = self.createServicePackageDedicated(name, tnt_group_id, dv_group_id, isolation_policy )
        else:
            raise  Exception("NS device must be either in shared or dedicated mode")
        if srv_pkg.status_code != 201:
            raise Exception("Error:  %s" % self.__lastError)
        dev_add_res =self.addDevicetoServicePackage(dv_group_id, device_ip)
        if dev_add_res == FAILED:
            raise Exception ("Error:  %s" % self.__lastError)
        srv_pkg_id = json.loads(srv_pkg.content)["servicepackages"][0]["id"]
        publish_srv_pkg_res = self.publishServicePackage(srv_pkg_id)
        if publish_srv_pkg_res == FAILED:
            raise Exception("Error:  %s" % self.__lastError)
        return (dv_group_id, tnt_group_id, srv_pkg_id)

    def createTenantGroup(self, name):
        url = "http://"+self.nccip+"/admin/v1/tenantgroups"
        payload = {"tenantgroups": [{"name": name}]}
        res = self.sendCmdToNCC(url, payload)
        return res

    def createDeviceGroup(self, name, platform_type, device_type="netscaler"):
        url = "http://"+self.nccip+"/admin/v1/devicegroups"
        payload = {"devicegroups":[{"name": name,
                                    "device_type": device_type,
                                    "platform_type": platform_type
                                   }]
        }
        res = self.sendCmdToNCC(url, payload)
        return res

    def createServicePackageShared(self, name, tenant_group, device_group, allocation, device_type="netscaler"):
        url = "http://"+self.nccip+"/admin/v1/servicepackages"
        payload = {"servicepackages":[{"allocationgroups": [{"device_type": device_type,
                                                             "allocationpolicy":allocation,
                                                             "placement_scheme": "ROUNDROBIN",
                                                             "deviceaffinity": "onedevice",
                                                             "devicegroup":{"ref": device_group}
                                                             }],
                                       "name": name,
                                       "isdefault": "false",
                                       "tenantgroup": {"ref": tenant_group}
                                      }]
        }
        res = self.sendCmdToNCC(url, payload)
        return res

    def createServicePackageDedicated(self, name, tenant_group, device_group, allocation, device_type="netscaler"):
        url = "http://"+self.nccip+"/admin/v1/servicepackages"
        payload = {"servicepackages":[{"allocationgroups": [{"device_type": device_type,
                                                             "allocationpolicy":allocation,
                                                             #"placement_scheme": "roundrobin or leastentity",
                                                             "devicegroup":{"ref": device_group}
                                                            }],
                                       "name": name,
                                       "isdefault": "false",
                                       "tenantgroup": {"ref": tenant_group}
                                      }]
        }
        res = self.sendCmdToNCC(url, payload)
        return res
   
    def addDevicetoServicePackage(self, devicegroup_id, device_ip):
        url = "http://"+self.nccip+"/admin/v1/devicegroups/"+devicegroup_id+"/devices"
        payload = {"devices":[{"ref":device_ip }]}
        res = self.sendCmdToNCC(url, payload, method="PUT")
        return res

    def removeDevicefromServicePackage(self, devicegroup_id):
        url = "http://"+self.nccip+"/admin/v1/devicegroups/"+devicegroup_id+"/devices"
        payload = {"devices":[]}
        res = self.sendCmdToNCC(url, payload, method="PUT")
        return res

    def publishServicePackage(self, pkg_id):
        url = "http://"+self.nccip+"/cs/cca/v1/servicepackages"
        payload = {"servicepackages":[{"servicepackageid":pkg_id}]}
        res = self.sendCmdToNCC(url, payload)
        return res

    def getCSZoneFromNCC(self):
        url = "http://"+self.nccip+"/cs/cca/v1/zones"
        res = self.sendCmdToNCC(url, method="GET")
        if res != FAILED:
            zoneid = json.loads(res.content)["zones"][0]
            return zoneid
        else:
            return FAILED

    def sendCmdToNCC(self, url, payload={}, method="POST", header={'content-type': 'application/json'}):
        try:
            # self.logger.debug("url :%s" % url)
            # self.logger.debug("payload: %s" % payload)
            if method == "POST":
                #self.logger.debug("====Sending POST Request====")
                return self.sendPostRequstToNCC(url, payload, header)
            if method == "GET":
                #self.logger.debug("====Sending GET Request====")
                return self.sendGetRequestToNCC(url, payload, header)
            if method == "PUT":
                return self.sendPutRequestToNCC(url, payload, header)
            if method == "DELETE":
                self.logger.debug("Trying delete")
                return self.sendDeleteRequestToNCC(url, header)
        except Exception as e:
            self.__lastError = e
            # self.logger.exception("sendCmdToNCC: Exception:%s" %
            #                       GetDetailExceptionInfo(e))
            return FAILED


    def sendGetRequestToNCC(self, url, payload, header):
        try:
            res = requests.get(url, json=payload, auth=("nsroot", "nsroot"), headers=header)
            return res
        except Exception as e:
            self.__lastError = e
            # self.logger.exception("sendGetRequestToNCC : Exception Occured: %s" %
            #                       str(self.__lastError))
            return FAILED

    def sendPostRequstToNCC(self, url, payload, header):
        try:
            res = requests.post(url, json=payload, auth=("nsroot", "nsroot"), headers=header)
            return res
        except Exception as e:
            self.__lastError = e
            # self.logger.exception("sendPostRequstToNCC: Exception Occured: %s" %
            #                       str(self.__lastError))
            return FAILED

    def sendPutRequestToNCC(self, url, payload, header):
         try:
            res = requests.put(url, json=payload, auth=("nsroot", "nsroot"), headers=header)
            return res
         except Exception as e:
            self.__lastError = e
            # self.logger.exception("sendPostRequstToNCC: Exception Occured: %s" %
            #                       str(self.__lastError))
            return FAILED

    def sendDeleteRequestToNCC(self, url, header):
         try:
            res = requests.delete (url, auth=("nsroot", "nsroot"), headers=header)
            return res
         except Exception as e:
            self.__lastError = e
            # self.logger.exception("sendPostRequstToNCC: Exception Occured: %s" %
            #                       str(self.__lastError))
            return FAILED

    def sendCmdToNS(self, url, payload={}, method="POST", header={'content-type': 'application/json'}):
        try:
            # self.logger.debug("url :%s" % url)
            # self.logger.debug("payload: %s" % payload)
            if method == "POST":
                #self.logger.debug("====Sending POST Request====")
                return self.sendPostRequstToNS(url, payload, header)
            if method == "GET":
                #self.logger.debug("====Sending GET Request====")
                return self.sendGetRequestToNS(url, payload, header)
        except Exception as e:
            self.__lastError = e
            # self.logger.exception("sendCmdToNCC: Exception:%s" %
            #                       GetDetailExceptionInfo(e))
            return FAILED

    def sendPostRequstToNS(self, url, payload, header):
        try:
            res = requests.post(url, data=payload, auth=("nsroot", "nsroot"), headers=header)
            return res
        except Exception as e:
            self.__lastError = e
            # self.logger.exception("sendPostRequstToNCC: Exception Occured: %s" %
            #                       str(self.__lastError))
            return FAILED

    def sendGetRequestToNS(self, url, payload, header):
        try:
            res = requests.get(url, data=payload, auth=("nsroot", "nsroot"), headers=header)
            return res
        except Exception as e:
            self.__lastError = e
            # self.logger.exception("sendGetRequestToNCC : Exception Occured: %s" %
            #                       str(self.__lastError))
            return FAILED

    def getAdminKeys(self, apiClient):
        domains = Domain.list(apiClient, name="ROOT")
        listuser = listUsers.listUsersCmd()
        listuser.username = "admin"
        listuser.domainid = domains[0].id
        listuser.listall = True
        listuserRes = apiClient.listUsers(listuser)
        userId = listuserRes[0].id
        apiKey = listuserRes[0].apikey
        securityKey = listuserRes[0].secretkey
        return [apiKey, securityKey]

    def cleanup_ncc(self, device_gp_id, srv_pkg_uuid, srv_pkg_id, tnt_group_id):
        self.removeDevicefromServicePackage(device_gp_id)
        # Remove service package reference from Cloudplatform
        url = "http://"+self.nccip+"/cs/cca/v1/servicepackages/"+srv_pkg_uuid
        self.logger.debug("Sending DELETE SP uuid: %s " % url)
        res = self.sendCmdToNCC(url, method="DELETE")


        # Remove Service package from NCC
        url = "http://"+self.nccip+"/admin/v1/servicepackages/"+srv_pkg_id
        self.logger.debug("Sending DELETE SP : %s " % url)
        res = self.sendCmdToNCC(url, method="DELETE")


        # Remove Device group
        url = "http://"+self.nccip+"/admin/v1/devicegroups/"+device_gp_id
        self.logger.debug("Sending DELETE devicegroup: %s " % url)
        res = self.sendCmdToNCC(url, method="DELETE")


        # Remove Tenant group
        url = "http://"+self.nccip+"/admin/v1/tenantgroups/"+tnt_group_id
        self.logger.debug("Sending DELETE tenant group: %s " % url)
        res = self.sendCmdToNCC(url, method="DELETE")
        self.logger.debug("Result: %s" % res)
        return res
