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

package com.cloud.hypervisor.ovm3.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.CloudstackPlugin.ReturnCode;

public class CloudStackPluginTest {
    private static final String VMNAME = "test";
    String domrIp = "169.254.3.2";
    public String getDomrIp() {
        return domrIp;
    }

    public String getDom0Ip() {
        return dom0Ip;
    }

    public Integer getDomrPort() {
        return domrPort;
    }

    String dom0Ip = "192.168.1.64";
    Integer domrPort = 3922;
    String host = "ovm-1";
    String path = "/tmp";
    String dirpath = "/tmp/testing";
    String filename = "test.txt";
    String content = "This is some content";
    String bridge = "xenbr0";
    String vncPort = "5900";
    Integer port = 8899;
    Integer retries = 1;
    Integer interval = 1;
    Integer timeout = 120;

    ConnectionTest con = new ConnectionTest();
    CloudstackPlugin cSp = new CloudstackPlugin(con);
    XmlTestResultTest results = new XmlTestResultTest();

    String domrExecXml = "<?xml version='1.0'?>"
            + "<methodResponse>"
            + "<params>"
            + "<param>"
            + "<value><struct>"
            + "<member>"
            + "<name>out</name>"
            + "<value><string>clearUsageRules.sh func.sh hv-kvp-daemon_3.1_amd64.deb monitorServices.py reconfigLB.sh redundant_router</string></value>"
            + "</member>"
            + "<member>"
            + "<name>err</name>"
            + "<value><string></string></value>"
            + "</member>"
            + "<member>"
            + "<name>rc</name>"
            + "<value><i8>0</i8></value>"
            + "</member>"
            + "</struct></value>"
            + "</param>"
            + "</params>"
            + "</methodResponse>";
    String dom0StatsXml = "<?xml version='1.0'?>\n" +
            "<methodResponse>\n" +
            "<params>\n" +
            "<param>\n" +
            "<value><struct>\n" +
            "<member>\n" +
            "<name>rx</name>\n" +
            "<value><string>11631523\n" +
            "</string></value>\n" +
            "</member>\n" +
            "<member>\n" +
            "<name>total</name>\n" +
            "<value><string>4293918720</string></value>\n" +
            "</member>\n" +
            "<member>\n" +
            "<name>tx</name>\n" +
            "<value><string>16927399\n" +
            "</string></value>\n" +
            "</member>\n" +
            "<member>\n" +
            "<name>cpu</name>\n" +
            "<value><string>1.5</string></value>\n" +
            "</member>\n" +
            "<member>\n" +
            "<name>free</name>\n" +
            "<value><string>3162505216</string></value>\n" +
            "</member>\n" +
            "</struct></value>\n" +
            "</param>\n" +
            "</params>\n" +
            "</methodResponse>";
    String domuStatsXml = "<?xml version='1.0'?>"
            + "<methodResponse>"
            + "<params>"
            + "<param>"
            + "<value><struct>"
            + "<member>"
            + "<name>uptime</name>"
            + "<value><string>862195495455</string></value>"
            + "</member>"
            + "<member>"
            + "<name>rx_bytes</name>"
            + "<value><string>52654010</string></value>"
            + "</member>"
            + "<member>"
            + "<name>wr_ops</name>"
            + "<value><string>521674</string></value>"
            + "</member>"
            + "<member>"
            + "<name>vcpus</name>"
            + "<value><string>1</string></value>"
            + "</member>"
            + "<member>"
            + "<name>cputime</name>"
            + "<value><string>295303661496</string></value>"
            + "</member>"
            + "<member>"
            + "<name>rd_ops</name>"
            + "<value><string>14790</string></value>"
            + "</member>"
            + "<member>"
            + "<name>rd_bytes</name>"
            + "<value><string>250168320</string></value>"
            + "</member>"
            + "<member>"
            + "<name>tx_bytes</name>"
            + "<value><string>161389183</string></value>"
            + "</member>"
            + "<member>"
            + "<name>wr_bytes</name>"
            + "<value><string>1604468736</string></value>"
            + "</member>"
            + "</struct></value>"
            + "</param>"
            + "</params>"
            + "</methodResponse>";
    private String dom0StorageCheckXml = "<?xml version='1.0'?>"
            + "<methodResponse>"
            + "<params>"
            + "<param>"
            + "<value><array><data>"
            + "<value><boolean>1</boolean></value>"
            + "<value><boolean>0</boolean></value>"
            + "</data></array></value>"
            + "</param>"
            + "</params>"
            + "</methodResponse>";
    public String getDom0StorageCheckXml() {
        return dom0StorageCheckXml;
    }
    public String getDomrExecXml() {
        return domrExecXml;
    }

    public String getDom0StatsXml() {
        return dom0StatsXml;
    }

    public String getDomuStatsXml() {
        return domuStatsXml;
    }

    @Test
    public void testDom0CheckStorageHealthCheck() throws Ovm3ResourceException {
        con.setResult(dom0StorageCheckXml);
        results.basicBooleanTest(cSp.dom0CheckStorageHealthCheck("", "", "", 120, 1));
        results.basicBooleanTest(cSp.dom0CheckStorageHealthCheck(), false);
    }
    @Test
    public void testOvsUploadFile() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.ovsUploadFile(path, filename, content));
    }

    @Test
    public void testOvsUploadSshKey() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.ovsUploadSshKey(path, content));
    }

    @Test
    public void testOvsDomrUploadFile() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.ovsDomrUploadFile(VMNAME, path, filename, content));
    }

    @Test
    public void testGetVncPort() throws Ovm3ResourceException {
        con.setResult(results.getString(vncPort));
        results.basicStringTest(cSp.getVncPort(VMNAME), vncPort);
    }

    @Test
    public void testDom0CheckPort() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.dom0CheckPort(host, port, retries, interval));
        /* test nothing */
        con.setNull();
        results.basicBooleanTest(
                cSp.dom0CheckPort(host, port, retries, interval), false);
        /* for the last test we need to fake the timeout... */
    }

    @Test
    public void testDom0Ip() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.dom0HasIp(dom0Ip));
        con.setResult(results.getBoolean(false));
        results.basicBooleanTest(cSp.dom0HasIp(dom0Ip), false);
    }

    @Test
    public void testDomrExec() throws Ovm3ResourceException {
        con.setResult(domrExecXml);
        ReturnCode x = cSp.domrExec(domrIp, "ls");
        assertNotNull(x);
        assertEquals(x.getExit(), (Integer) 0);
        assertEquals(x.getRc(), true);
        assertEquals(x.getExit(), (Integer) 0);
        assertNotNull(x.getStdOut());

        /* failed */
        domrExecXml = domrExecXml.replace("<i8>0</i8>", "<i8>1</i8>");
        domrExecXml = domrExecXml.replace("<value><string></string></value>", "<value><string>Something went wrong!</string></value>");
        con.setResult(domrExecXml);
        ReturnCode y = cSp.domrExec(domrIp, "ls");
        assertNotNull(y);
        assertEquals(y.getRc(), false);
        assertEquals(y.getExit(), (Integer) 1);
        assertNotNull(x.getStdErr());
    }

    @Test
    public void testOvsDom0Stats() throws Ovm3ResourceException {
        con.setResult(dom0StatsXml);
        Map<String, String> stats = cSp.ovsDom0Stats(bridge);
        results.basicStringTest(stats.get("cpu"), "1.5");
    }

    @Test
    public void TestOvsDomUStats() throws Ovm3ResourceException {
        con.setResult(domuStatsXml);
        Map<String, String> stats = cSp.ovsDomUStats(VMNAME);
        results.basicStringTest(stats.get("cputime"), "295303661496");
    }

    @Test
    public void TestDomrCheckPort() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.domrCheckPort(domrIp, domrPort));
    }

    @Test
    public void TestDomrCheckSsh() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.domrCheckSsh(domrIp));
    }

    @Test
    public void TestOvsControlInterface() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.ovsControlInterface("control0", "169.254.0.1/16"));
    }

    @Test
    public void TestPing() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.ping(host));
    }

    @Test
    public void TestOvsCheckFile() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.ovsCheckFile(filename));
    }

    @Test
    public void dom0CheckStorageHealth() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.dom0CheckStorageHealth("", "", "", 120));
        con.setResult(results.getBoolean(false));
        results.basicBooleanTest(cSp.dom0CheckStorageHealth("", "", "", 120), false);
    }
    @Test
    public void TestovsMkdirs() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(cSp.ovsMkdirs(dirpath));
    }
    @Test
    public void TestovsMkdirsDirExists() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.ovsMkdirs(dirpath), false);
    }
    @Test
    public void TestovsMkdirs2() throws Ovm3ResourceException {
            con.setResult(results.getNil());
            results.basicBooleanTest(cSp.ovsMkdirs(dirpath, 755));
    }

}
