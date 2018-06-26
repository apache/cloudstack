4.11.2-SNAPSHOT#!/usr/bin/env python
4.11.2-SNAPSHOT# Licensed to the Apache Software Foundation (ASF) under one
4.11.2-SNAPSHOT# or more contributor license agreements.  See the NOTICE file
4.11.2-SNAPSHOT# distributed with this work for additional information
4.11.2-SNAPSHOT# regarding copyright ownership.  The ASF licenses this file
4.11.2-SNAPSHOT# to you under the Apache License, Version 2.0 (the
4.11.2-SNAPSHOT# "License"); you may not use this file except in compliance
4.11.2-SNAPSHOT# with the License.  You may obtain a copy of the License at
4.11.2-SNAPSHOT#
4.11.2-SNAPSHOT#   http://www.apache.org/licenses/LICENSE-2.0
4.11.2-SNAPSHOT#
4.11.2-SNAPSHOT# Unless required by applicable law or agreed to in writing,
4.11.2-SNAPSHOT# software distributed under the License is distributed on an
4.11.2-SNAPSHOT# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
4.11.2-SNAPSHOT# KIND, either express or implied.  See the License for the
4.11.2-SNAPSHOT# specific language governing permissions and limitations
4.11.2-SNAPSHOT# under the License.
4.11.2-SNAPSHOT
4.11.2-SNAPSHOTtry:
4.11.2-SNAPSHOT    from setuptools import setup, find_packages
4.11.2-SNAPSHOTexcept ImportError:
4.11.2-SNAPSHOT    try:
4.11.2-SNAPSHOT        from distribute_setup import use_setuptools
4.11.2-SNAPSHOT        use_setuptools()
4.11.2-SNAPSHOT        from setuptools import setup, find_packages
4.11.2-SNAPSHOT    except ImportError:
4.11.2-SNAPSHOT        raise RuntimeError("python setuptools is required to build Marvin")
4.11.2-SNAPSHOT
4.11.2-SNAPSHOT
4.11.2-SNAPSHOTVERSION = "4.11.2-SNAPSHOT"
4.11.2-SNAPSHOT
4.11.2-SNAPSHOTsetup(name="Marvin",
4.11.2-SNAPSHOT      version=VERSION,
4.11.2-SNAPSHOT      description="Marvin - Python client for Apache CloudStack",
4.11.2-SNAPSHOT      author="The Apache CloudStack Team",
4.11.2-SNAPSHOT      author_email="dev@cloudstack.apache.org",
4.11.2-SNAPSHOT      maintainer="The Apache CloudStack Team",
4.11.2-SNAPSHOT      maintainer_email="dev@cloudstack.apache.org",
4.11.2-SNAPSHOT      long_description="Marvin is the Apache CloudStack python "
4.11.2-SNAPSHOT                       "client written around the unittest framework",
4.11.2-SNAPSHOT      platforms=("Any",),
4.11.2-SNAPSHOT      url="https://builds.apache.org/job/cloudstack-marvin/",
4.11.2-SNAPSHOT      packages=["marvin", "marvin.cloudstackAPI",
4.11.2-SNAPSHOT                "marvin.lib", "marvin.config", "marvin.sandbox",
4.11.2-SNAPSHOT                "marvin.sandbox.advanced", "marvin.sandbox.advancedsg",
4.11.2-SNAPSHOT                "marvin.sandbox.basic"],
4.11.2-SNAPSHOT      license="LICENSE.txt",
4.11.2-SNAPSHOT      install_requires=[
4.11.2-SNAPSHOT          "mysql-connector-python >= 1.1.6",
4.11.2-SNAPSHOT          "requests >= 2.2.1",
4.11.2-SNAPSHOT          "paramiko >= 1.13.0",
4.11.2-SNAPSHOT          "nose >= 1.3.3",
4.11.2-SNAPSHOT          "ddt >= 0.4.0",
4.11.2-SNAPSHOT          "pyvmomi >= 5.5.0",
4.11.2-SNAPSHOT          "netaddr >= 0.7.14",
4.11.2-SNAPSHOT          "dnspython",
4.11.2-SNAPSHOT          "ipmisim >= 0.7"
4.11.2-SNAPSHOT      ],
4.11.2-SNAPSHOT      extras_require={
4.11.2-SNAPSHOT        "nuagevsp": ["vspk", "PyYAML", "futures", "netaddr", "retries", "jpype1"]
4.11.2-SNAPSHOT      },
4.11.2-SNAPSHOT      py_modules=['marvin.marvinPlugin'],
4.11.2-SNAPSHOT      zip_safe=False,
4.11.2-SNAPSHOT      entry_points={
4.11.2-SNAPSHOT          'nose.plugins': ['marvinPlugin = marvin.marvinPlugin:MarvinPlugin'],
4.11.2-SNAPSHOT          'console_scripts': ['marvincli = marvin.deployAndRun:main']
4.11.2-SNAPSHOT      },
4.11.2-SNAPSHOT      )
