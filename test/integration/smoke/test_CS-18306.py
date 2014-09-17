#!/usr/bin/env python
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

'''
@pytest.mark.tags(tags=["advanced"], required_hardware="false")
def test_01_create_disk_offering(vm):
    assert vm is not None
'''

import time

class TestP:
    def test_a(self):
        assert True == True
    def test_b(self):
        assert True == True
    def test_aaa(self):
        assert True == True

def test_aa():
    assert True == True

def test_bb():
    assert True == True

class TestA:
    def test_aaa(self):
        assert True == True
    def test_bbb(self):
        assert True == True
    def test_ccc(self):
        assert True == True


def test_cc():
    assert True == True
