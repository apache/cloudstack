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

import pytest

from marvin.lib.common import get_zone,get_domain,get_template
from marvin.lib.base import ServiceOffering,Account,VirtualMachine


@pytest.fixture()
def test_client(request):
    if request.cls is not None:
        return request.node.cls.testClient
    else:
        return request.node.testClient

@pytest.fixture()
def zone(test_client):
    apiClient = test_client.getApiClient()
    zone = get_zone(apiClient, test_client.getZoneForTests())
    return zone

@pytest.fixture()
def tiny_service_offering(test_client, zone):
    apiClient = test_client.getApiClient()

    params = {
        "name": "Tiny Instance",
        "displaytext": "Tiny Instance",
        "cpunumber": 1,
        "cpuspeed": 100,
        "memory": 128,
        }

    if zone.localstorageenabled:
        params["storagetype"] = "local"

    return ServiceOffering.create(apiClient, params)

@pytest.fixture()
def domain(test_client):
    apiClient = test_client.getApiClient()
    return get_domain(apiClient)

@pytest.fixture()
def account(test_client, domain):
    params = {
        "email": "test-account@test.com",
        "firstname": "test",
        "lastname": "test",
        "username": "test-account",
        "password": "password"
    }

    apiclient = test_client.getApiClient()

    return Account.create(apiclient, params, domainid=domain.id)

@pytest.fixture()
def template(test_client, zone):
    return get_template(
        test_client.getApiClient(),
        zone.id,
        "CentOS 5.6 (64-bit)"
    )

@pytest.fixture()
def vm(test_client, account, template, tiny_service_offering, zone):
    params = {
        "displayname": "testserver",
        "username": "root",
        "password": "password",
        "ssh_port": 22,
        "hypervisor": "XenServer",
        "privateport": 22,
        "publicport": 22,
        "protocol": 'TCP',
    }
    virtual_machine = VirtualMachine.create(
        test_client.getApiClient(),
        params,
        zoneid=zone.id,
        templateid=template.id,
        accountid=account.name,
        domainid=account.domainid,
        serviceofferingid=tiny_service_offering.id,
        mode=zone.networktype
    )

    return virtual_machine
