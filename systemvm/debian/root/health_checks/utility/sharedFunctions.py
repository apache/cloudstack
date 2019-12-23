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

import json


def getHealthChecksData(additionalDataKey=None):
    with open('/root/health_checks_data.json', 'r') as hc_data_file:
        hc_data = json.load(hc_data_file)

    # If no specific key is requested return all the data as JSON
    if additionalDataKey is None:
        return hc_data

    if additionalDataKey not in hc_data["health_checks_config"]:
        return None

    data = hc_data["health_checks_config"][additionalDataKey].strip().split(";")
    addData = []
    for line in data:
        line = line.strip()
        if len(line) == 0:
            continue
        entries = line.split(',')
        d = {}
        for entry in entries:
            entry = entry.strip()
            if len(entry) == 0:
                continue
            keyVal = entry.split("=")
            if len(keyVal) == 2:
                d[keyVal[0].strip()] = keyVal[1].strip()
        if len(d) > 0:
            addData.append(d)

    return addData


def formatPort(portStart, portEnd, delim="-"):
    return portStart if portStart == portEnd else portStart + delim + portEnd
