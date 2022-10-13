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

import requests
from marvin.lib.utils import (validateList)
from marvin.lib.base import (VirtualMachine, FirewallRule, FirewallPolicy, TungstenTag,
                             ApplicationPolicySet, AddressGroup, ServiceGroup, LogicalRouter,
                             NetworkPolicy)
from marvin.cloudstackAPI import (listTungstenFabricProviders, listSystemVms)
from marvin.codes import PASS

def not_tungsten_fabric_zone(apiclient, zone_id):
    cmd = listTungstenFabricProviders.listTungstenFabricProvidersCmd()
    cmd.zoneid = zone_id

    response = apiclient.listTungstenFabricProviders(cmd)
    return validateList(response)[0] != PASS


def is_object_created(apiclient, zone_id, object, id):
    cmd = listTungstenFabricProviders.listTungstenFabricProvidersCmd()
    cmd.zoneid = zone_id

    response = apiclient.listTungstenFabricProviders(cmd)
    if validateList(response)[0] != PASS:
        raise Exception("Warning: zone tungsten provider doesn't exist")

    host = response[0].tungstenproviderhostname
    port = response[0].tungstenproviderport

    r = requests.get("http://%s:%s/%s/%s" % (host, port, object, id))
    status_code = r.status_code
    return status_code == 200


def is_object_deleted(apiclient, zone_id, object, id):
    cmd = listTungstenFabricProviders.listTungstenFabricProvidersCmd()
    cmd.zoneid = zone_id

    response = apiclient.listTungstenFabricProviders(cmd)
    if validateList(response)[0] != PASS:
        raise Exception("Warning: zone tungsten provider doesn't exist")

    host = response[0].tungstenproviderhostname
    port = response[0].tungstenproviderport

    r = requests.get("http://%s:%s/%s/%s" % (host, port, object, id))
    status_code = r.status_code
    return status_code == 404


def get_list_system_vm(apiclient, zone_id):
    cmd = listSystemVms.listSystemVmsCmd()
    cmd.zoneid = zone_id

    return apiclient.listSystemVms(cmd)


def cleanup_resources(api_client, zoneid, resources):
    for obj in resources:
        if isinstance(obj, VirtualMachine):
            obj.delete(api_client, expunge=True)
        elif isinstance(obj, FirewallRule):
            obj.delete(api_client, zoneid, obj.uuid)
        elif isinstance(obj, FirewallPolicy):
            obj.delete(api_client, zoneid, obj.uuid)
        elif isinstance(obj, TungstenTag):
            obj.delete(api_client, zoneid, obj.uuid)
        elif isinstance(obj, ApplicationPolicySet):
            obj.delete(api_client, zoneid, obj.uuid)
        elif isinstance(obj, ServiceGroup):
            obj.delete(api_client, zoneid, obj.uuid)
        elif isinstance(obj, AddressGroup):
            obj.delete(api_client, zoneid, obj.uuid)
        elif isinstance(obj, LogicalRouter):
            obj.delete(api_client, zoneid, obj.uuid)
        elif isinstance(obj, NetworkPolicy):
            obj.delete(api_client, zoneid, obj.uuid)
        else:
            obj.delete(api_client)
