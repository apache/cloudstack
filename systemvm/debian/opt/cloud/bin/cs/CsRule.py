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
from . import CsHelper
import logging


class CsRule:
    """ Manage iprules
    Supported Types:
    fwmark
    """

    def __init__(self, dev):
        self.dev = dev
        self.tableNo = 100 + int(dev[3:])
        self.table = "Table_%s" % (dev)

    def addRule(self, rule):
        if not self.findRule(rule + " lookup " + self.table):
            cmd = "ip rule add " + rule + " table " + self.table
            CsHelper.execute(cmd)
            logging.info("Added rule %s for %s" % (cmd, self.table))

    def findRule(self, rule):
        for i in CsHelper.execute("ip rule show"):
            if rule in i.strip():
                return True
        return False

    def addMark(self):
        if not self.findMark():
            cmd = "ip rule add fwmark %s table %s" % (self.tableNo, self.table)
            CsHelper.execute(cmd)
            logging.info("Added fwmark rule for %s" % (self.table))

    def delMark(self):
        if self.findMark():
            cmd = "ip rule delete fwmark %s table %s" % (self.tableNo, self.table)
            CsHelper.execute(cmd)
            logging.info("Deleting fwmark rule for %s" % (self.table))

    def findMark(self):
        srch = "from all fwmark %s lookup %s" % (hex(self.tableNo), self.table)
        for i in CsHelper.execute("ip rule show"):
            if srch in i.strip():
                return True
        return False
