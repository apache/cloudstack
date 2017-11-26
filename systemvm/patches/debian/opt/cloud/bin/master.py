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

from cs.CsRedundant import CsRedundant
from cs.CsDatabag import CsCmdLine
from cs.CsAddress import CsAddress
from cs.CsConfig import CsConfig
import logging
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-m", "--master",
                  action="store_true", default=False, dest="master",
                  help="Set router master")
parser.add_option("-b", "--backup",
                  action="store_true", default=False, dest="backup",
                  help="Set router backup")
parser.add_option("-f", "--fault",
                  action="store_true", default=False, dest="fault",
                  help="Notify Fault")
(options, args) = parser.parse_args()

config = CsConfig()
logging.basicConfig(filename=config.get_logger(),
                    level=config.get_level(),
                    format=config.get_format())
config.cmdline()
cl = CsCmdLine("cmdline", config)
#Update the configuration to set state as backup and let keepalived decide who the real Master is!
cl.set_master_state(False)
cl.save()

config.set_address()
red = CsRedundant(config)

if options.master:
    red.set_master()

if options.backup:
    red.set_backup()

if options.fault:
    red.set_fault()
