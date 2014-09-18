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

from marvin.utils import initTestClass,getMarvin
from .VM import (vm,tiny_service_offering,template,test_client,account,domain,zone)

def pytest_configure(config):
    config.addinivalue_line("markers",
        "tags(name): tag tests")

    marvin_init_tags()

def pytest_collection_finish(session):
    units = []
    for item in session.items:
        if item.instance is None and item.cls is None:
            units.append(item.nodeid)
        elif item.instance is not None:
            instance = item.instance
            name = instance.__module__ + ":" + instance.__class__.__name__
            units.append(name)
        else:
            name = item.cls
            units.append(name)

    print units

g_marvin_filter = {
    "tags":[],
}

def tobool(str):
    if str in ["True", "true"]:
        return True
    else:
        return False
def marvin_init_tags():
    tags = os.environ.get("MARVIN_TAGS", "advanced,required_hardware=false").split(",")
    global g_marvin_filter
    for t in tags:
        if t.startswith("required_hardware"):
            g_marvin_filter["required_hardware"] = t.split("=")[1]
        else:
            g_marvin_filter["tags"].append(t)

def pytest_runtest_setup(item):
    global g_marvin_filter
    attrmarker = item.get_marker("tags")
    if attrmarker is None:
        return

    if "required_hardware" in attrmarker.kwargs:
        if attrmarker.kwargs["required_hardware"] != g_marvin_filter["required_hardware"]:
            pytest.skip("doesnt match hardware")
    elif "tags" in attrmarker.kwargs:
        found = False
        for t in attrmarker.kwargs["tags"]:
            if t in g_marvin_filter["tags"]:
                found = True

        if found is not True:
            pytest.skip("doesn't match tags")

@pytest.fixture(scope="session", autouse=True)
def marvin_init_session():
    result = getMarvin()
    logger = result.getLogger()
    logger.debug("in session")
    if result is None:
        pytest.fail("failed to init marvin plugin")

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
    logger = marvinObj.getLogger()
    #logger.debug("in function")

    setattr(request.node, "testClient", marvinObj.getTestClient())

    marvinObj.getTestClient().identifier = request.node.name


