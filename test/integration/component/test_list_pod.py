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

""" test for listPods
"""

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.utils import (cleanup_resources)
from marvin.lib.base import (Pod, Cluster, Capacities)
from marvin.cloudstackAPI import (updateConfiguration)


class TestListPod(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        super(TestListPod, cls)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()

        # build cleanup list
        self.cleanup = []

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_list_pod_with_overcommit(self):
        """Test List Pod Api with cluster CPU and Memory OverProvisioning
	    """

        podlist = Pod.list(self.apiclient)

        for pod in podlist:
            clusterlist = Cluster.list(self.apiclient, podid=pod.id)
            if len(clusterlist) > 1:

                updateCpuOvercommitCmd = updateConfiguration.updateConfigurationCmd()
                updateCpuOvercommitCmd.clusterid = clusterlist[0].id
                updateCpuOvercommitCmd.name="cpu.overprovisioning.factor"

                if clusterlist[0].cpuovercommitratio == clusterlist[1].cpuovercommitratio and clusterlist[0].cpuovercommitratio == "1.0":
                    cpuovercommit = "1.0"
                    updateCpuOvercommitCmd.value="2.0"
                    self.apiclient.updateConfiguration(updateCpuOvercommitCmd)

                elif clusterlist[0].cpuovercommitratio != clusterlist[1].cpuovercommitratio:
                    cpuovercommit = clusterlist[0].cpuovercommitratio

                else:
                    cpuovercommit = clusterlist[0].cpuovercommitratio
                    updateCpuOvercommitCmd.value="1.0"
                    self.apiclient.updateConfiguration(updateCpuOvercommitCmd)

                updateMemoryOvercommitCmd = updateConfiguration.updateConfigurationCmd()
                updateMemoryOvercommitCmd.clusterid = clusterlist[0].id
                updateMemoryOvercommitCmd.name="mem.overprovisioning.factor"

                if clusterlist[0].memoryovercommitratio == clusterlist[1].memoryovercommitratio and clusterlist[0].memoryovercommitratio == "1.0":
                    memoryovercommit = "1.0"
                    updateMemoryOvercommitCmd.value="2.0"
                    self.apiclient.updateConfiguration(updateMemoryOvercommitCmd)

                elif clusterlist[0].memoryovercommitratio != clusterlist[1].memoryovercommitratio:
                    memoryovercommit = clusterlist[0].memoryovercommitratio

                else:
                    memoryovercommit = clusterlist[0].memoryovercommitratio
                    updateMemoryOvercommitCmd.value="1.0"
                    self.apiclient.updateConfiguration(updateMemoryOvercommitCmd)

                podWithCap = Pod.list(self.apiclient, id=pod.id, showcapacities=True)
                cpucapacity = Capacities.list(self.apiclient, podid=pod.id, type=1)
                memorycapacity = Capacities.list(self.apiclient, podid=pod.id, type=0)

                updateCpuOvercommitCmd.value = cpuovercommit
                updateMemoryOvercommitCmd.value = memoryovercommit

                self.apiclient.updateConfiguration(updateCpuOvercommitCmd)
                self.apiclient.updateConfiguration(updateMemoryOvercommitCmd)

                self.assertEqual(
                    [cap for cap in podWithCap[0].capacity if cap.type == 1][0].capacitytotal,
                    cpucapacity[0].capacitytotal,
                    "listPods api returns wrong CPU capacity "
                )

                self.assertEqual(
                    [cap for cap in podWithCap[0].capacity if cap.type == 0][0].capacitytotal,
                    memorycapacity[0].capacitytotal,
                    "listPods api returns wrong memory capacity"
                )
