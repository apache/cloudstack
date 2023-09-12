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
from cs.CsHelper import *
from cs.CsRoute import CsRoute
from cs.CsRule import CsRule


def main(argv):
    if len(argv) != 3:
        exit(1)
    pubip = argv[1]
    prinet = argv[2]
    pubdev = get_device(pubip)
    if pubdev == "":
        exit(1)
    list = execute("ip -o -f inet addr show %s | awk '/scope global/ {print $4}'" % pubdev)
    if len(list) != 1:
        exit(1)
    pubnet = list[0]
    csroute = CsRoute()
    csroute.add_table(pubdev)
    csrule = CsRule(pubdev)
    csrule.addRule("from %s to %s" % (pubnet, prinet))
    cmd = "%s dev %s table %s" % (prinet, pubdev, csroute.get_tablename(pubdev))
    csroute.set_route(cmd)


if __name__ == '__main__':
    main(sys.argv)
