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
    from distribute_setup import use_setuptools
    use_setuptools()
    from setuptools import setup, find_packages

# Use following rules for versioning:
# <cli major version>.<cloudstack minor version>.<cloudstack major version>
# Example: For CloudStack 4.1.x, CLI version should be 0.1.4
version = "0.0.4"
name = "cloudmonkey"

setup(
    name = name,
    version = version,
    author = "The Apache CloudStack Team",
    author_email = "cloudstack-dev@incubator.apache.org",
    maintainer = "Rohit Yadav",
    maintainer_email = "bhaisaab@apache.org",
    url = "http://incubator.apache.org/cloudstack",
    description = "Command Line Interface for Apache CloudStack",
    long_description="cloudmonkey is a command line interface for Apache "
                     "CloudStack powered by CloudStack Marvin testclient",
    platforms=("Any",),
    license = 'ASL 2.0',
    packages=find_packages(),
    install_requires=['clint>=0.3.0'],
    include_package_data = True,
    zip_safe = False,
    classifiers = [
        "Development Status :: 4 - Beta",
        "Environment :: Console",
        "Intended Audience :: Developers",
        "Intended Audience :: End Users/Desktop",
        "Operating System :: OS Independent",
        "Programming Language :: Python",
        "Topic :: Software Development :: Testing",
        "Topic :: Software Development :: Interpreters",
        "Topic :: Utilities",
    ],
    entry_points="""
    [console_scripts]
    cloudmonkey = cloudmonkey:main
    """,
)
