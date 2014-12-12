package com.cloud.hypervisor.ovm3.object;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.Pool;

public class PoolTest {
    ConnectionTest con = new ConnectionTest();
    Pool pool = new Pool(con);
    XmlTestResultTest results = new XmlTestResultTest();

    private String UUID = "0004fb0000020000ba9aaf00ae5e2d73";
    private String BOGUSUUID = "deadbeefdeadbeefdeadbeefdeadbeef";
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
    public void testDiscoverServerPool() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(EMPTY));
        results.basicBooleanTest(pool.isInAPool(), false);
        results.basicBooleanTest(pool.isInPool(UUID), false);
        results.basicBooleanTest(pool.discoverServerPool(), false);
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
    public void poolMembers() throws Ovm3ResourceException {
        List<String> poolHosts = new ArrayList<String>();
        poolHosts.add(IP);
        poolHosts.add(IP2);
        con.setResult(results.simpleResponseWrapWrapper(EMPTY));
        results.basicBooleanTest(pool.getPoolMemberList().contains(IP), false);

        con.setResult(results.simpleResponseWrapWrapper(DISCOVERPOOL));
        results.basicBooleanTest(pool.discoverServerPool());
        con.setResult(results.getNil());
        results.basicBooleanTest(pool.removePoolMember(IP), true);
        results.basicBooleanTest(pool.addPoolMember(IP), true);
        results.basicBooleanTest(pool.setPoolMemberList(poolHosts), true);
    }

    @Test
    public void testCreateServerPool() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(
                pool.createServerPool(ALIAS, UUID, VIP, 1, HOST, IP), true);
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERPOOL));
        results.basicBooleanTest(pool.discoverServerPool());
        results.basicBooleanTest(
                pool.createServerPool(ALIAS, UUID, VIP, 1, HOST, IP), true);

    }

    @Test(expected = Ovm3ResourceException.class)
    public void testCreateServerPoolFail1() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERPOOL));
        results.basicBooleanTest(
                pool.createServerPool(ALIAS, BOGUSUUID, VIP, 1, HOST, IP),
                false);
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testCreateServerPoolFail2() throws Ovm3ResourceException {
        con.setResult(results.errorResponseWrap(1,
                "exceptions.Exception:Repository already exists"));
        results.basicBooleanTest(
                pool.createServerPool(ALIAS, UUID, VIP, 1, HOST, IP), false);
    }

    @Test
    public void testJoinServerPool() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        Integer poolsize = 2;
        results.basicBooleanTest(
                pool.joinServerPool(ALIAS, UUID, VIP, poolsize, HOST2, IP2),
                true);
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERPOOL));
        results.basicBooleanTest(pool.discoverServerPool());
        results.basicBooleanTest(
                pool.joinServerPool(ALIAS, UUID, VIP, poolsize, HOST2, IP2),
                true);
    }
    @Test(expected = Ovm3ResourceException.class)
    public void testJoinServerPoolFail1() throws Ovm3ResourceException {
        Integer poolsize = 2;
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERPOOL));
        results.basicBooleanTest(pool.joinServerPool(ALIAS, BOGUSUUID, VIP,
                poolsize, HOST2, IP2), false);
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testJoinServerPoolFail() throws Ovm3ResourceException {
        con.setResult(results
                .errorResponseWrap(1,
                        "exceptions.Exception:Server already a member of pool: "
                                + UUID));
        Integer poolsize = 2;
        results.basicBooleanTest(pool.joinServerPool(ALIAS, UUID, VIP, poolsize,
                HOST2, IP2), false);
    }

    @Test
    public void testValidPoolRoles() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(pool.setServerRoles(pool.getValidRoles()),
                true);

    }

    @Test(expected = Ovm3ResourceException.class)
    public void testValidPoolRolesInvalid() throws Ovm3ResourceException {
        String broken = "broken_token";
        con.setResult(results
                .errorResponseWrap(1,
                "exceptions.Exception:Invalid roles: set(['xen', '" + broken
                        + "', 'utility'])"));
        results.basicBooleanTest(pool.setServerRoles(pool.getValidRoles()),
                false);
    }
}
