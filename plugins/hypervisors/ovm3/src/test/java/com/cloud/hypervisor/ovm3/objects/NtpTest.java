package com.cloud.hypervisor.ovm3.objects;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class NtpTest {
    ConnectionTest con = new ConnectionTest();
    Ntp nTp = new Ntp(con);
    XmlTestResultTest results = new XmlTestResultTest();
    String details = results.simpleResponseWrapWrapper("<array>\n"
            + "<data>\n"
            + "<value>\n"
            + "<array>\n"
            + "<data>\n"
            + "<value><string>ovm-1</string></value>\n"
            + "<value><string>ovm-2</string></value>\n"
            + "</data>\n"
            + "</array>\n"
            + "</value>\n"
            + "<value><boolean>1</boolean></value>\n"
            + "<value><boolean>1</boolean></value>\n"
            + "</data>\n"
            + "</array>\n");

    public void testGetNtp() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(nTp.getDetails());
    }

    @Test
    public void testEnableNtp() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(nTp.enableNtp());
    }
    @Test
    public void testDisableNtp() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(nTp.disableNtp());
    }

    @Test
    public void testGetDetails() throws Ovm3ResourceException {
        con.setResult(details);
        results.basicBooleanTest(nTp.getDetails());
        results.basicBooleanTest(nTp.isRunning());
        results.basicBooleanTest(nTp.isServer());
    }

    @Test
    public void testSetNTP() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(nTp.setNtp("ovm-1", true));
        con.setResult(details);
        results.basicBooleanTest(nTp.getDetails());
        List<String> ntpHosts = new ArrayList<String>();
        nTp.setServers(ntpHosts);
        results.basicBooleanTest(nTp.setNtp(true), false);
    }

    @Test
    public void testServerAdditionRemoval() throws Ovm3ResourceException {
        List<String> ntpHosts = new ArrayList<String>();
        con.setResult(details);
        nTp.getDetails();
        ntpHosts = nTp.getServers();
        assertEquals(ntpHosts.size(), 2);
        nTp.removeServer("ovm-2");
        ntpHosts = nTp.getServers();
        assertEquals(ntpHosts.size(), 1);
        nTp.removeServer("ovm-2");
        ntpHosts = nTp.getServers();
        assertEquals(ntpHosts.size(), 1);
        nTp.addServer("ovm-1");
        ntpHosts = nTp.getServers();
        assertEquals(ntpHosts.size(), 1);
    }
}
