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
import os
from marvin.marvinPlugin import MarvinInit
from marvin.codes import FAILED

global_marvin_obj = None
def pytest_configure():
    configFile = os.environ.get("MARVIN_CONFIG", os.path.join(os.path.dirname(os.path.realpath(__file__)),"..", "..", "..", "setup", "dev", "advanced.cfg"))
    deployDcb = False
    deployDc = os.environ.get("MARVIN_DEPLOY_DC", "false")
    if deployDc in ["True", "true"]:
        deployDcb = True
    zoneName = os.environ.get("MARVIN_ZONE_NAME", "Sandbox-simulator")
    hypervisor_type = os.environ.get("MARVIN_HYPERVISOR_TYPE", "simulator")
    logFolder = os.environ.get("MARVIN_LOG_FOLDER", os.path.expanduser(os.path.join("~","marvin")))

    global global_marvin_obj
    global_marvin_obj = MarvinInit(configFile,
               deployDcb,
               None,
               zoneName,
               hypervisor_type,
               logFolder)

    result = global_marvin_obj.init()
    if result == FAILED:
        pytest.fail("failed to init marvin plugin")

@pytest.fixture(scope="class", autouse=True)
def marvin_inject_testclass(request):
    global global_marvin_obj
    test = request.cls
    setattr(test, "debug", global_marvin_obj.getLogger().debug)
    setattr(test, "info", global_marvin_obj.getLogger().info)
    setattr(test, "warn", global_marvin_obj.getLogger().warning)
    setattr(test, "error",global_marvin_obj.getLogger().error)
    setattr(test, "testClient", global_marvin_obj.getTestClient())
    setattr(test, "config", global_marvin_obj.getParsedConfig())
    setattr(test, "clstestclient", global_marvin_obj.getTestClient())
    if hasattr(test, "user"):
        # when the class-level attr applied. all test runs as 'user'
        request.testClient.getUserApiClient(test.UserName,
                                           test.DomainName,
                                           test.AcctType)
