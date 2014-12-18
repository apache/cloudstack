#!/usr/bin/python
# -- coding: utf-8 --
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# This file is used by the tests to switch the redundancy status

from cs.CsConfig import CsConfig
from optparse import OptionParser
import logging

parser = OptionParser()
parser.add_option("-e", "--enable",
                  action="store_true", default=False, dest="enable",
                  help="Set router redundant")
parser.add_option("-d", "--disable",
                  action="store_true", default=False, dest="disable",
                  help="Set router non redundant")

(options, args) = parser.parse_args()

config = CsConfig()
logging.basicConfig(filename=config.get_logger(),
                    level=config.get_level(),
                    format=config.get_format())
config.set_cl()

if options.enable:
    config.get_cmdline().set_redundant("true")
if options.disable:
    config.get_cmdline().set_redundant("false")

config.get_cmdline().save()
