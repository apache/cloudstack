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
import logging
from cs.CsDatabag import CsDataBag
from .CsFile import CsFile
import json

MON_CONFIG = "/etc/monitor.conf"
HC_CONFIG = "/root/health_checks_data.json"


class CsMonitor(CsDataBag):
    """ Manage Monitor script schedule and health checks for router """

    def get_basic_check_interval(self):
        return self.dbag["health_checks_basic_run_interval"] if "health_checks_basic_run_interval" in self.dbag else 3

    def get_advanced_check_interval(self):
        return self.dbag["health_checks_advanced_run_interval"] if "health_checks_advanced_run_interval" in self.dbag else 0

    def setupMonitorConfigFile(self):
        if "config" in self.dbag:
            procs = [x.strip() for x in self.dbag['config'].split(',')]
            file = CsFile(MON_CONFIG)
            for proc in procs:
                bits = [x for x in proc.split(':')]
                if len(bits) < 5:
                    continue
                for i in range(0, 4):
                    file.add(bits[i], -1)
            file.commit()

    def setupHealthCheckCronJobs(self):
        cron_rep_basic = self.get_basic_check_interval()
        cron_rep_advanced = self.get_advanced_check_interval()
        cron = CsFile("/etc/cron.d/process")
        cron.deleteLine("root /usr/bin/python3 /root/monitorServices.py")
        cron.add("SHELL=/bin/bash", 0)
        cron.add("PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin", 1)
        if cron_rep_basic > 0:
            cron.add("*/" + str(cron_rep_basic) + " * * * * root /usr/bin/python3 /root/monitorServices.py basic", -1)
        if cron_rep_advanced > 0:
            cron.add("*/" + str(cron_rep_advanced) + " * * * * root /usr/bin/python3 /root/monitorServices.py advanced", -1)
        cron.commit()

    def setupHealthChecksConfigFile(self):
        hc_data = {}
        hc_data["health_checks_basic_run_interval"] = self.get_basic_check_interval()
        hc_data["health_checks_advanced_run_interval"] = self.get_advanced_check_interval()
        hc_data["health_checks_enabled"] = self.dbag["health_checks_enabled"] if "health_checks_enabled" in self.dbag else False

        if "excluded_health_checks" in self.dbag:
            excluded_checks = self.dbag["excluded_health_checks"]
            hc_data["excluded_health_checks"] = [ch.strip() for ch in excluded_checks.split(",")] if len(excluded_checks) > 0 else []
        else:
            hc_data["excluded_health_checks"] = []

        if "health_checks_config" in self.dbag:
            hc_data["health_checks_config"] = self.dbag["health_checks_config"]
        else:
            hc_data["health_checks_config"] = {}

        with open(HC_CONFIG, 'w') as f:
            json.dump(hc_data, f, ensure_ascii=False, indent=4)

    def process(self):
        self.setupMonitorConfigFile()
        self.setupHealthChecksConfigFile()
        self.setupHealthCheckCronJobs()
