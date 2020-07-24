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
import os
import re
from . import CsHelper
import logging


class CsProcess(object):
    """ Manipulate processes """

    def __init__(self, search):
        self.search = search

    def start(self, thru, background=''):
        # if(background):
        #     cmd = cmd + " &"
        logging.info("Started %s", " ".join(self.search))
        os.system("%s %s %s" % (thru, " ".join(self.search), background))

    def kill_all(self):
        pids = self.find_pid()
        for p in pids:
            CsHelper.execute("kill -9 %s" % p)

    def find_pid(self):
        self.pid = []
        items = len(self.search)
        for i in CsHelper.execute("ps aux"):
            items = len(self.search)
            decodedItem = i.decode()
            proc = re.split(r"\s+", decodedItem)[10:]
            matches = len([m for m in proc if m in self.search])
            if matches == items:
                self.pid.append(re.split(r"\s+", decodedItem)[1])

        log_str = "CsProcess:: Searching for process ==> %s and found PIDs ==> %s" % ("self.search", "self.pid")
        logging.debug(log_str)
        return self.pid

    def find(self):
        has_pid = len(self.find_pid()) > 0
        return has_pid

    def kill(self, pid):
        if pid > 1:
            CsHelper.execute("kill -9 %s" % pid)

    def grep(self, str):
        for i in CsHelper.execute("ps aux"):
            if i.find(str) != -1:
                return re.split(r"\s+", i)[1]
        return -1
