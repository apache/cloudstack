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
from marvin.cloudstackAPI import addTrafficMonitor
from marvin.cloudstackAPI import listTrafficMonitors
from marvin.cloudstackAPI import deleteTrafficMonitor

class TrafficMonitor(CloudStackEntity.CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, url, zoneid, **kwargs):
        cmd = addTrafficMonitor.addTrafficMonitorCmd()
        cmd.url = url
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        trafficmonitor = apiclient.addTrafficMonitor(cmd)


    @classmethod
    def list(self, apiclient, zoneid, **kwargs):
        cmd = listTrafficMonitors.listTrafficMonitorsCmd()
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        trafficmonitor = apiclient.listTrafficMonitors(cmd)
        return map(lambda e: TrafficMonitor(e.__dict__), trafficmonitor)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteTrafficMonitor.deleteTrafficMonitorCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        trafficmonitor = apiclient.deleteTrafficMonitor(cmd)
