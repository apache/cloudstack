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

from ipaddress import *


def merge(dbag, data):

    if "config" in data:
        dbag['config'] = data["config"]
    if "health_checks_enabled" in data:
        dbag["health_checks_enabled"] = data["health_checks_enabled"]
    if "health_checks_basic_run_interval" in data:
        dbag["health_checks_basic_run_interval"] = data["health_checks_basic_run_interval"]
    if "health_checks_advanced_run_interval" in data:
        dbag["health_checks_advanced_run_interval"] = data["health_checks_advanced_run_interval"]
    if "excluded_health_checks" in data:
        dbag["excluded_health_checks"] = data["excluded_health_checks"]
    if "health_checks_config" in data:
        dbag["health_checks_config"] = data["health_checks_config"]

    return dbag
