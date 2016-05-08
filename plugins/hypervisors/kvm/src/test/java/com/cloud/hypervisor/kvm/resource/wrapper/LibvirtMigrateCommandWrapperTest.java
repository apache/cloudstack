//
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
//
package com.cloud.hypervisor.kvm.resource.wrapper;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.io.Charsets;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Resources;

public class LibvirtMigrateCommandWrapperTest {
    static String fullfile;
    static String targetfile;

    @BeforeClass
    public static void setup() {
        try {
            fullfile = Resources.toString(Resources.getResource("original.xml"), Charsets.UTF_8);
            targetfile = Resources.toString(Resources.getResource("expected.xml"), Charsets.UTF_8);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testReplaceIpForVNCInDescFile() {
        final String targetIp = "192.168.22.21";
        final LibvirtMigrateCommandWrapper lw = new LibvirtMigrateCommandWrapper();
        final String result = lw.replaceIpForVNCInDescFile(fullfile, targetIp);
        assertTrue("transformation does not live up to expectation:\n" + result, targetfile.equals(result));
    }

    @Test
    public void testReplaceIpForVNCInDesc() {
        final String xmlDesc =
                "<domain type='kvm' id='3'>" +
                "  <devices>" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='10.10.10.1'>" +
                "      <listen type='address' address='10.10.10.1'/>" +
                "    </graphics>" +
                "  </devices>" +
                "</domain>";
        final String expectedXmlDesc =
                "<domain type='kvm' id='3'>" +
                "  <devices>" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='10.10.10.10'>" +
                "      <listen type='address' address='10.10.10.10'/>" +
                "    </graphics>" +
                "  </devices>" +
                "</domain>";
        final String targetIp = "10.10.10.10";
        final LibvirtMigrateCommandWrapper lw = new LibvirtMigrateCommandWrapper();
        final String result = lw.replaceIpForVNCInDescFile(xmlDesc, targetIp);
        assertTrue("transformation does not live up to expectation:\n" + result, expectedXmlDesc.equals(result));
    }

    @Test
    public void testReplaceFqdnForVNCInDesc() {
        final String xmlDesc =
                "<domain type='kvm' id='3'>" +
                "  <devices>" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='localhost.local'>" +
                "      <listen type='address' address='localhost.local'/>" +
                "    </graphics>" +
                "  </devices>" +
                "</domain>";
        final String expectedXmlDesc =
                "<domain type='kvm' id='3'>" +
                "  <devices>" +
                "    <graphics type='vnc' port='5900' autoport='yes' listen='localhost.localdomain'>" +
                "      <listen type='address' address='localhost.localdomain'/>" +
                "    </graphics>" +
                "  </devices>" +
                "</domain>";
        final String targetIp = "localhost.localdomain";
        final LibvirtMigrateCommandWrapper lw = new LibvirtMigrateCommandWrapper();
        final String result = lw.replaceIpForVNCInDescFile(xmlDesc, targetIp);
        assertTrue("transformation does not live up to expectation:\n" + result, expectedXmlDesc.equals(result));
    }
}
