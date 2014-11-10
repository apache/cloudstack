package com.cloud.hypervisor.ovm3.object;

import org.junit.Test;

public class PoolTest {
    ConnectionTest con = new ConnectionTest();
    Pool pool = new Pool(con);
    XmlTestResultTest results = new XmlTestResultTest();

    private String UUID = "0004fb0000020000ba9aaf00ae5e2d73";
    private String VIP = "192.168.1.230";
    private String ALIAS = "Pool 0";
    private String HOST = "ovm-1";
    private String HOST2 = "ovm-2";
    private String IP = "192.168.1.64";
    private String IP2 = "192.168.1.65";
    private String EMPTY = results.escapeOrNot("<?xml version=\"1.0\" ?>"
            + "<Discover_Server_Pool_Result/>");
    private String DISCOVERPOOL = results
            .escapeOrNot("<?xml version=\"1.0\" ?>"
                    + "<Discover_Server_Pool_Result>" + "  <Server_Pool>"
                    + "    <Unique_Id>"
                    + UUID
                    + "</Unique_Id>"
                    + "    <Pool_Alias>"
                    + ALIAS
                    + "</Pool_Alias>"
                    + "    <Master_Virtual_Ip>"
                    + VIP
                    + "</Master_Virtual_Ip>"
                    + "    <Member_List>"
                    + "      <Member>"
                    + "        <Registered_IP>"
                    + IP
                    + "</Registered_IP>"
                    + "      </Member>"
                    + "      <Member>"
                    + "        <Registered_IP>"
                    + IP2
                    + "</Registered_IP>"
                    + "      </Member>"
                    + "    </Member_List>"
                    + "  </Server_Pool>" + "</Discover_Server_Pool_Result>");

    @Test
    public void testDiscoverPool() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(pool.discoverServerPool(), false);
        results.basicBooleanTest(pool.isInAPool(), false);
        results.basicBooleanTest(pool.isInPool(UUID), false);
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERPOOL));
        results.basicBooleanTest(pool.discoverServerPool());
        results.basicBooleanTest(pool.isInAPool(), true);
        results.basicBooleanTest(pool.isInPool(UUID), true);
        results.basicStringTest(pool.getPoolId(), UUID);
        results.basicStringTest(pool.getPoolId(), UUID);
        results.basicStringTest(pool.getPoolAlias(), ALIAS);
        results.basicStringTest(pool.getPoolMasterVip(), VIP);
        results.basicBooleanTest(pool.getPoolMemberList().contains(IP));
        results.basicBooleanTest(pool.getPoolMemberList().contains(IP2));
    }

    @Test
    public void testCreatePool() throws Ovm3ResourceException {
        pool.createServerPool(ALIAS, UUID, VIP, 1,
                HOST, IP);
    }

    @Test
    public void testJoinPool() throws Ovm3ResourceException {
        Integer poolsize = 2;
        pool.joinServerPool(ALIAS, UUID, VIP, poolsize,
                HOST2, IP2);
    }

}
