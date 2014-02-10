package com.cloud.consoleproxy;

import junit.framework.Assert;

import org.junit.Test;

public class ConsoleProxyRdpClientTest {

    @Test
    public void testMapMouseDownModifierButton1Mask() throws Exception {
        int code = 0;
        int modifiers = 960;
        int expected = 1024 + 960;

        ConsoleProxyRdpClient rdpc = new ConsoleProxyRdpClient();
        int actual = rdpc.mapMouseDownModifier(code, modifiers);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMapMouseDownModifierButton2() throws Exception {
        int code = 1;
        int modifiers = 0xffff;
        int expected = 960;

        ConsoleProxyRdpClient rdpc = new ConsoleProxyRdpClient();
        int actual = rdpc.mapMouseDownModifier(code, modifiers);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMapMouseDownModifierButton3Mask() throws Exception {
        int code = 2;
        int modifiers = 960;
        int expected = 4096 + 960;

        ConsoleProxyRdpClient rdpc = new ConsoleProxyRdpClient();
        int actual = rdpc.mapMouseDownModifier(code, modifiers);

        Assert.assertEquals(expected, actual);
    }

}
