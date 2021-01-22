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

try:
    from setuptools import setup, find_packages
except ImportError:
    try:
        from distribute_setup import use_setuptools
        use_setuptools()
        from setuptools import setup, find_packages
    except ImportError:
        raise RuntimeError("python setuptools is required to build Marvin")


VERSION = "4.16.0.0-SNAPSHOT"

setup(name="Marvin",
      version=VERSION,
      description="Marvin - Python client for Apache CloudStack",
      author="The Apache CloudStack Team",
      author_email="dev@cloudstack.apache.org",
      maintainer="The Apache CloudStack Team",
      maintainer_email="dev@cloudstack.apache.org",
      long_description="Marvin is the Apache CloudStack python "
                       "client written around the unittest framework",
      platforms=("Any",),
      url="https://builds.apache.org/job/cloudstack-marvin/",
      packages=["marvin", "marvin.cloudstackAPI",
                "marvin.lib", "marvin.config", "marvin.sandbox",
                "marvin.sandbox.advanced", "marvin.sandbox.advancedsg",
                "marvin.sandbox.basic"],
      license="LICENSE.txt",
      install_requires=[
          "mysql-connector-python >= 1.1.6",
          "requests >= 2.2.1",
          "paramiko >= 1.13.0",
          "nose >= 1.3.3",
          "ddt >= 0.4.0",
          "pyvmomi >= 5.5.0",
          "netaddr >= 0.7.14",
          "dnspython",
          "ipmisim >= 0.7",
          "pytz",
          "retries",
          "PyCrypt",
          "urllib3"
      ],
      py_modules=['marvin.marvinPlugin'],
      zip_safe=False,
      entry_points={
          'nose.plugins': ['marvinPlugin = marvin.marvinPlugin:MarvinPlugin'],
          'console_scripts': ['marvincli = marvin.deployAndRun:main']
      },
      )
