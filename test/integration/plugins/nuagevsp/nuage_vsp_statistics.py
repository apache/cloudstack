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

import jpype
from jpype import *
from jpype import java
from jpype import javax

J_STRING = "java.lang.String"


class VsdDataCollector:
    def __init__(self, name, host="localhost", port=1099, debug=False,
                 output_channel=None):
        self.jmx = ConnectionJmx(host, port, debug, output_channel)
        self.jmx.create_management_object(name=name)
        self.saved_stats = None
        self.saved_vsd_calls = 0

    def start_test(self):
        self.saved_stats = self.jmx.get_attribute("VsdStatisticsReport")
        self.saved_vsd_calls = self.jmx.get_attribute("VSDStatistics")
        return

    def end_test(self):
        self.jmx.print_rapport(old_stats=self.saved_stats,
                               old_vsd_count=self.saved_vsd_calls)
        return


class ConnectionJmx:

    def __init__(self, host="localhost", port=1099, debug=False,
                 output_channel=None):
        self.host = host
        self.port = port
        self.url = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi" % (host, port)
        self.debug = debug
        self.mbean = None
        self.output_channel = output_channel
        jpype.startJVM(jpype.get_default_jvm_path())
        if debug:
            self.__print_to_ouput_channel("JVM loaded")
            self.__print_to_ouput_channel(jpype.get_default_jvm_path())

        jmx_url = javax.management.remote.JMXServiceURL(self.url)
        jmx_soc = javax.management.remote.JMXConnectorFactory.connect(
            jmx_url, java.util.HashMap())
        self.connection = jmx_soc.getMBeanServerConnection()
        if self.debug:
            self.__print_to_ouput_channel("Connection successful")

    def __print_to_ouput_channel(self, text):
        if self.output_channel:
            self.output_channel.debug(text)
        else:
            print text

    def create_management_object(self, domain="com.cloud",
                                 type="NuageVspResource",
                                 name="Nuage VSD - 0.0.0.0"):
        if name is not None:
            object_name = domain + ":type=" + type + ", name=" + name
        else:
            object_name = domain + ":" + type

        if self.debug:
            self.__print_to_ouput_channel(object_name)

        self.mbean = javax.management.ObjectName(object_name)
        return self.mbean

    def get_vsd_statistics_by_request_and_entity_type(self,
                                                      entity_type,
                                                      request_type,
                                                      mbean=None):
        self._get_stats_by_entity_or_request_type(
            mbean, [[entity_type, request_type]],
            "getVsdStatisticsByEntityType")

    def _jStringArray(self, elements):
        return jpype.JArray(java.lang.String)(elements)

    def get_vsd_statistics_by_request_type(self, request_type, mbean=None):
        return self._get_stats_by_entity_or_request_type(
            mbean, [request_type], "getVsdStatisticsByRequestType")

    def get_vsd_statistics_by_entity_type(self, entity_type, mbean=None):
        return self._get_stats_by_entity_or_request_type(
            mbean, [entity_type], "getVsdStatisticsByEntityType")

    def _get_stats_by_entity_or_request_type(self, vars, method, mbean=None):
        if not mbean:
            mbean = self.mbean
        jarray = self._jStringArray(vars)
        signature = self._jStringArray([J_STRING for _ in vars])
        result = self.connection.invoke(mbean, method, jarray, signature)
        if self.debug:
            self.__print_to_ouput_channel(vars + ": " + str(result))
        return result

    def get_attribute(self, attribute, mbean=None):
        if not mbean:
            mbean = self.mbean

        result = self.connection.getAttribute(mbean, attribute)
        if self.debug:
            self.__print_to_ouput_channel("Attribute " + attribute + ": " +
                                          str(result))
        return result

    def print_rapport(self, mbean=None, old_stats=None, old_vsd_count=0):
        if not mbean:
            mbean = self.mbean
        stat = self.get_attribute("VsdStatisticsReport", mbean)
        number_of_vsd_calls = int(str(self.get_attribute("VSDStatistics",
                                                         mbean)))
        number_of_vsd_calls = number_of_vsd_calls - int(str(old_vsd_count))

        self.__print_to_ouput_channel("\n================"
                                      "RAPPORT:"
                                      "================\n")
        self.__print_to_ouput_channel("Total VSD calls: " +
                                      str(number_of_vsd_calls))
        self.__print_to_ouput_channel("For each Entity:\n")
        self.__print_total_for_entity(stat, old_stats)
        self.__print_to_ouput_channel("\nFor each Request:\n")
        self.__print_total_per_request(stat, old_stats)
        self.__print_to_ouput_channel("\nCombined:\n")
        self.__print_total_per_entity_and_request(stat, old_stats)
        self.__print_to_ouput_channel("\n============="
                                      "END OF RAPPORT"
                                      "=============")

    def __print_total_per_request(self, stat, old_stat=None):
        data = dict()

        entries = ((entry.getKey(), entry.getValue().get())
                   for requestmap in stat.values()
                   for entry in requestmap.entrySet())

        for request, value in entries:
            if request in data:
                data[request] += value
            else:
                data[request] = value

        if old_stat:
            old_entries = ((entry.getKey(), entry.getValue().get())
                           for requestmap in old_stat.values()
                           for entry in requestmap.entrySet())

            for request, value in old_entries:
                if request in data:
                    data[request] -= value
                else:
                    data[request] = 0

        for key, value in data.iteritems():
            self.__print_to_ouput_channel(" " + str(key) + ": " + str(value))

    def __print_total_per_entity_and_request(self, stat, old_stat=None):
        for entity in stat:
            self.__print_to_ouput_channel(entity + ":")
            for request in stat[entity]:
                previous = 0
                if old_stat and old_stat[entity] and old_stat[entity][request]:
                    previous = int(str(old_stat[entity][request]))

                current = int(str(stat[entity][request]))

                self.__print_to_ouput_channel(" " + str(request) + ":" +
                                              str(current - previous))
            self.__print_to_ouput_channel("--------------------"
                                          "--------------------")

    def __print_total_for_entity(self, stat, old_stat=None):
        for entity in stat:
            total = 0
            for val in stat[entity]:
                minus = 0
                if old_stat and old_stat[entity] and old_stat[entity][val]:
                    minus = int(str(old_stat[entity][val]))
                total = str(stat[entity][val])

                total = int(total) - minus
            self.__print_to_ouput_channel(" " + str(entity) +
                                          ": " + str(total))
