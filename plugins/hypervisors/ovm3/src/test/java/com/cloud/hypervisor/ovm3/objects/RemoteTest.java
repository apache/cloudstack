package com.cloud.hypervisor.ovm3.objects;
import org.junit.Test;

public class RemoteTest {
    ConnectionTest con = new ConnectionTest();
    Remote rEm = new Remote(con);
    XmlTestResultTest results = new XmlTestResultTest();

    @Test
    public void TestSysShutdown() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(rEm.sysShutdown());
    }
    @Test
    public void TestSysReboot() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(rEm.sysReboot());
    }
}
