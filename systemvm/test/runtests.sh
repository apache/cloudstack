#!/bin/bash
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

# requires netaddr

export PYTHONPATH="../debian/opt/cloud/bin/"
export PYTHONDONTWRITEBYTECODE=False

echo "Running pep8 to check systemvm/python code for errors"
pep8 --max-line-length=179 *py
pep8 --max-line-length=179 --exclude=monitorServices.py,baremetal-vr.py,passwd_server_ip.py `find ../debian -name \*.py`
if [ $? -gt 0 ]
then
    echo "Pylint failed, please check your code"
    exit 1
fi

echo "Running pylint to check systemvm/python code for errors"
pylint --disable=R,C,W *.py
pylint --disable=R,C,W `find ../debian -name \*.py`
if [ $? -gt 0 ]
then
    echo "Pylint failed, please check your code"
    exit 1
fi

echo "Running systemvm/python unit tests"
nosetests .
exit $?
