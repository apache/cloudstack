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

from setuptools import setup
from sys import version
import sys

setup(name="Marvin",
      version="0.1.0",
      description="Marvin - Python client for testing cloudstack",
      author="Edison Su",
      author_email="Edison.Su@citrix.com",
      maintainer="Prasanna Santhanam",
      maintainer_email="Prasanna.Santhanam@citrix.com",
      long_description="Marvin is the cloudstack testclient written around the python unittest framework",
      platforms=("Any",),
      url="http://jenkins.cloudstack.org:8080/job/marvin",
      packages=["marvin", "marvin.cloudstackAPI", "marvin.integration",
                "marvin.integration.lib", "marvin.sandbox",
                "marvin.sandbox.advanced", "marvin.sandbox.basic"],
      license="LICENSE.txt",
      install_requires=[
          "mysql-connector-python",
          "paramiko",
          "nose"
      ],
     )
