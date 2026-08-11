// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.veeam.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Test;

public class CloudConfigUtilTest {

    @Test
    public void testExtractIpv4Address_ReturnsIpv4FromNetworkConfigWriteFile() {
        final String userData = "#cloud-config\n"
                + "write_files:\n"
                + "  - path: /etc/network/network_config.yml\n"
                + "    content: |\n"
                + "      interfaces:\n"
                + "        - ipv4:\n"
                + "            enabled: true\n"
                + "            dhcp: false\n"
                + "            address:\n"
                + "              - ip: 10.20.30.40\n";

        final Optional<String> result = CloudConfigUtil.extractIpv4Address(userData);

        assertEquals(Optional.of("10.20.30.40"), result);
    }

    @Test
    public void testExtractIpv4Address_ReturnsEmptyWhenNetworkConfigUsesDhcp() {
        final String userData = "#cloud-config\n"
                + "write_files:\n"
                + "  - path: /etc/network/network_config.yml\n"
                + "    content: |\n"
                + "      interfaces:\n"
                + "        - ipv4:\n"
                + "            enabled: true\n"
                + "            dhcp: true\n"
                + "            address:\n"
                + "              - ip: 10.20.30.40\n";

        assertFalse(CloudConfigUtil.extractIpv4Address(userData).isPresent());
    }

    @Test
    public void testExtractIpv4Address_ReturnsEmptyForInvalidYaml() {
        assertFalse(CloudConfigUtil.extractIpv4Address("write_files: [").isPresent());
    }

    @Test
    public void testExtractIpv4Address_NormalizesEscapedNewLines() {
        final String userData = "write_files:\\n"
                + "  - path: /tmp/network_config.yml\\n"
                + "    content: |\\n"
                + "      interfaces:\\n"
                + "        - ipv4:\\n"
                + "            enabled: true\\n"
                + "            dhcp: false\\n"
                + "            address:\\n"
                + "              - ip: 192.168.1.25\\n";

        assertEquals(Optional.of("192.168.1.25"), CloudConfigUtil.extractIpv4Address(userData));
    }
}
