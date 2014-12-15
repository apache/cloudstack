package com.cloud.hypervisor.ovm3.objects;
import org.junit.Test;

public class CommonTest {
    ConnectionTest con = new ConnectionTest();
    Common cOm = new Common(con);
    XmlTestResultTest results = new XmlTestResultTest();
    String echo = "put";
    String remoteUrl = "http://oracle:password@ovm-2:8899";

    @Test
    public void testGetApiVersion() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrap("<array><data>\n<value><int>3</int></value>\n</data></array>"));
        results.basicIntTest(cOm.getApiVersion(), 3);
    }

    @Test
    public void testSleep() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(cOm.sleep(1));
    }

    @Test
    public void testDispatch() throws Ovm3ResourceException {
        con.setResult(results.getString(echo));
        results.basicStringTest(cOm.dispatch(remoteUrl, "echo", echo), echo);
    }

    @Test
    public void testEcho() throws Ovm3ResourceException {
        con.setResult(results.getString(echo));
        results.basicStringTest(cOm.echo(echo), echo);
    }
}
