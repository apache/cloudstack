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

from marvin.utils import initTestClass,getMarvin
from .VM import (vm,tiny_service_offering,template,test_client,account,domain,zone)

def pytest_configure(config):
    config.addinivalue_line("markers",
        "attr(name): tag tests")

    result = getMarvin()
    if result is None:
        pytest.fail("failed to init marvin plugin")



def pytest_runtest_setup(item):
    attrmarker = item.get_marker("attr")
    if attrmarker.kwargs["required_hardware"]:
        pytest.skip("doesnt have hardware")

@pytest.fixture(scope="class", autouse=True)
def marvin_inject_testclass(request):
    if request.cls is None:
        return

    test = request.cls
    initTestClass(test, request.node.nodeid)


@pytest.fixture(scope="function", autouse=True)
def marvin_init_function(request):
    if request.cls is not None:
        return
    marvinObj = getMarvin()
    setattr(request.node, "testClient", marvinObj.getTestClient())

    marvinObj.getTestClient().identifier = request.node.name


