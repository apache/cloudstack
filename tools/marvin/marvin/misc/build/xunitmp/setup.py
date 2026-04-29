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


import os
from setuptools import setup

def read(fname):
    return open(os.path.join(os.path.dirname(__file__), fname)).read().strip()

VERSION = '0.1.0'

setup(
    name = "xunitmultiprocess",
    version = VERSION,
    author = "Prasanna Santhanam",
    author_email = "Prasanna.Santhanam@citrix.com",
    description = "Run tests written using CloudStack's Marvin testclient",
    license = 'ASL .0',
    classifiers = [
        "Intended Audience :: Developers",
        "Topic :: Software Development :: Testing",
        "Programming Language :: Python",
        ],

    py_modules = ['xunitmultiprocess'],
    zip_safe = False,

    entry_points = {
        'nose.plugins': ['xunitmultiprocess = xunitmultiprocess:Xunitmp']
        },
    install_requires = ['nose'],
)
