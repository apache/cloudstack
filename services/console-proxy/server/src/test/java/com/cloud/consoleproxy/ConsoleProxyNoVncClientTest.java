package com.cloud.consoleproxy;

import org.junit.Assert;
import org.junit.Test;

public class ConsoleProxyNoVncClientTest {
    @Test
    public void rewriteServerNameInServerInitTest() {
        String serverName = "server123, backend:TLS";
        byte[] serverInitTestBytes = new byte[]{ 4, 0, 3, 0, 32, 24, 0, 1, 0, -1, 0, -1, 0, -1, 16, 8, 0, 0, 0, 0, 0, 0, 0, 15, 81, 69, 77, 85, 32, 40, 105, 45, 50, 45, 56, 45, 86, 77, 41};
        byte[] newServerInit = ConsoleProxyNoVncClient.rewriteServerNameInServerInit(serverInitTestBytes, serverName);

        byte[] expectedBytes = new byte[]{4, 0, 3, 0, 32, 24, 0, 1, 0, -1, 0, -1, 0, -1, 16, 8, 0, 0, 0, 0, 0, 0, 0, 22, 115, 101, 114, 118, 101, 114, 49, 50, 51, 44, 32, 98, 97, 99, 107, 101, 110, 100, 58, 84, 76, 83};
        Assert.assertArrayEquals(newServerInit, expectedBytes);
    }
}