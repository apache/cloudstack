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


def merge(dbag, rules):
    for rule in rules["rules"]:
        source_ip = rule["source_ip_address"]
        destination_ip = rule["destination_ip_address"]
        revoke = rule["revoke"]

        newrule = dict()
        newrule["public_ip"] = source_ip
        newrule["internal_ip"] = destination_ip

        if rules["type"] == "staticnatrules":
            newrule["type"] = "staticnat"
        elif rules["type"] == "forwardrules":
            newrule["type"] = "forward"
            newrule["public_ports"] = rule["source_port_range"]
            newrule["internal_ports"] = rule["destination_port_range"]
            newrule["protocol"] = rule["protocol"]

        if not revoke:
            if rules["type"] == "staticnatrules":
                dbag[source_ip] = [newrule]
            elif rules["type"] == "forwardrules":
                if source_ip in dbag.keys():
                    newkey = getRuleKey(newrule)
                    dbag[source_ip][newkey] = newrule
                else:
                    newkey = getRuleKey(newrule)
                    dbag[source_ip] = {newkey: newrule}
        else:
            if rules["type"] == "staticnatrules":
                if source_ip in dbag.keys():
                    del dbag[source_ip]
            elif rules["type"] == "forwardrules":
                if source_ip in dbag.keys():
                    newkey = getRuleKey(newrule)
                    if newkey in dbag[source_ip].keys():
                        del dbag[source_ip][newkey]
    return dbag


def getRuleKey(rule):
    if rule["type"] == "staticnat":
        return "s%s" % (rule["public_ip"])
    elif rule["type"] == "forward":
        return "f%s%s%s" \
               % (rule["public_ip"], rule["public_ports"], rule["protocol"])


# Compare function checks only the public side,
# those must be equal the internal details could change
def ruleCompare(ruleA, ruleB):
    if not ruleA["type"] == ruleB["type"]:
        return False
    if ruleA["type"] == "staticnat":
        return ruleA["public_ip"] == ruleB["public_ip"]
    elif ruleA["type"] == "forward":
        return ruleA["public_ip"] == ruleB["public_ip"] \
               and ruleA["public_ports"] == ruleB["public_ports"] \
               and ruleA["protocol"] == ruleB["protocol"]
