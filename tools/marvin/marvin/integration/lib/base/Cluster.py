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
from marvin.cloudstackAPI import addCluster
from marvin.cloudstackAPI import listClusters
from marvin.cloudstackAPI import updateCluster
from marvin.cloudstackAPI import deleteCluster

class Cluster(CloudStackEntity):


    def __init__(self, items):
        self.__dict__.update(items)


    def add(self, apiclient, clustername, hypervisor, zoneid, clustertype, podid, **kwargs):
        cmd = addCluster.addClusterCmd()
        cmd.clustername = clustername
        cmd.clustertype = clustertype
        cmd.hypervisor = hypervisor
        cmd.podid = podid
        cmd.zoneid = zoneid
        [setattr(cmd, key, value) for key,value in kwargs.items]
        cluster = apiclient.addCluster(cmd)


    @classmethod
    def list(self, apiclient, **kwargs):
        cmd = listClusters.listClustersCmd()
        [setattr(cmd, key, value) for key,value in kwargs.items]
        cluster = apiclient.listClusters(cmd)
        return map(lambda e: Cluster(e.__dict__), cluster)


    def update(self, apiclient, id, **kwargs):
        cmd = updateCluster.updateClusterCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        cluster = apiclient.updateCluster(cmd)


    def delete(self, apiclient, id, **kwargs):
        cmd = deleteCluster.deleteClusterCmd()
        cmd.id = id
        [setattr(cmd, key, value) for key,value in kwargs.items]
        cluster = apiclient.deleteCluster(cmd)
