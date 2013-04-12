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
from marvin.integration.lib.base import CloudStackEntity
from marvin.cloudstackAPI import createVpnCustomerGateway
from marvin.cloudstackAPI import listVpnCustomerGateways
from marvin.cloudstackAPI import updateVpnCustomerGateway
from marvin.cloudstackAPI import deleteVpnCustomerGateway

class VpnCustomerGateway(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    @classmethod
    def create(cls, apiclient, VpnCustomerGatewayFactory, **kwargs):
        cmd = createVpnCustomerGateway.createVpnCustomerGatewayCmd()
        [setattr(cmd, factoryKey, factoryValue) for factoryKey, factoryValue in VpnCustomerGatewayFactory.__dict__.iteritems()]
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpncustomergateway = apiclient.createVpnCustomerGateway(cmd)
        return VpnCustomerGateway(vpncustomergateway.__dict__)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listVpnCustomerGateways.listVpnCustomerGatewaysCmd()
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpncustomergateway = apiclient.listVpnCustomerGateways(cmd)
        return map(lambda e: VpnCustomerGateway(e.__dict__), vpncustomergateway)


    def update(self, apiclient, ikepolicy, cidrlist, gateway, ipsecpsk, esppolicy, **kwargs):
        cmd = updateVpnCustomerGateway.updateVpnCustomerGatewayCmd()
        cmd.id = self.id
        cmd.cidrlist = cidrlist
        cmd.esppolicy = esppolicy
        cmd.gateway = gateway
        cmd.ikepolicy = ikepolicy
        cmd.ipsecpsk = ipsecpsk
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpncustomergateway = apiclient.updateVpnCustomerGateway(cmd)
        return vpncustomergateway


    def delete(self, apiclient, **kwargs):
        cmd = deleteVpnCustomerGateway.deleteVpnCustomerGatewayCmd()
        cmd.id = self.id
        [setattr(cmd, key, value) for key,value in kwargs.iteritems()]
        vpncustomergateway = apiclient.deleteVpnCustomerGateway(cmd)
        return vpncustomergateway
