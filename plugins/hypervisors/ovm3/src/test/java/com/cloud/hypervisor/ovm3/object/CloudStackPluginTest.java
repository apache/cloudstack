/*
 */
package com.cloud.hypervisor.ovm3.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.CloudStackPlugin;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.CloudStackPlugin.ReturnCode;

public class CloudStackPluginTest {
    private static final String VMNAME = "test";
    String domrIp = "169.254.3.2";
    String dom0Ip = "192.168.1.100";
    Integer domrPort = 3922;
    String host = "ovm-1";
    String path = "/tmp";
    String filename = "test.txt";
    String content = "This is some content";
    String bridge = "xenbr0";
    String vncPort = "5900";
    Integer port = 8899;
    Integer retries = 1;
    Integer interval = 1;

    ConnectionTest con = new ConnectionTest();
    CloudStackPlugin cSp = new CloudStackPlugin(con);
    XmlTestResultTest results = new XmlTestResultTest();

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
    public void testDomrCheckPort() throws Ovm3ResourceException {
        con.setResult(results.getBoolean(true));
        results.basicBooleanTest(cSp.domrCheckPort(host, port, retries, interval));
        /* test nothing */
        con.setResult(null);
        results.basicBooleanTest(
                cSp.domrCheckPort(host, port, retries, interval), false);
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
        String xml = "<?xml version='1.0'?>"
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
        con.setResult(xml);
        ReturnCode x = cSp.domrExec(domrIp, "ls");
        assertNotNull(x);
        assertEquals(x.getExit(), (Integer) 0);
        assertEquals(x.getRc(), true);
        assertEquals(x.getExit(), (Integer) 0);
        assertNotNull(x.getStdOut());

        /* failed */
        xml = xml.replace("<i8>0</i8>", "<i8>1</i8>");
        xml = xml.replace("<value><string></string></value>", "<value><string>Something went wrong!</string></value>");
        con.setResult(xml);
        ReturnCode y = cSp.domrExec(domrIp, "ls");
        assertNotNull(y);
        assertEquals(y.getRc(), false);
        assertEquals(y.getExit(), (Integer) 1);
        assertNotNull(x.getStdErr());
    }

    @Test
    public void testOvsDom0Stats() throws Ovm3ResourceException {
        String xml = "<?xml version='1.0'?>\n" +
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
        con.setResult(xml);
        Map<String, String> stats = cSp.ovsDom0Stats(bridge);
        results.basicStringTest(stats.get("cpu"), "1.5");
    }

    @Test
    public void TestOvsDomUStats() throws Ovm3ResourceException {
        String xml = "<?xml version='1.0'?>"
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
        con.setResult(xml);
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
}
