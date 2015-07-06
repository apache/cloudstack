package com.cloud.utils.net;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.testing.EqualsTester;

public class Ip4AddressTest {

    @Test
    public void testEquals() throws Exception {
        new EqualsTester()
                .addEqualityGroup(new Ip4Address("0.0.0.1", "00:00:00:00:00:02"), new Ip4Address(1L, 2L))
                .addEqualityGroup(new Ip4Address("0.0.0.1", "00:00:00:00:00:00"), new Ip4Address(1L, 0L), new Ip4Address(1L, 0L), new Ip4Address(1L), new Ip4Address("0.0.0.1"))
                .testEquals();
    }

    @Test
    public void testIsSameAddressAs() {
        Assert.assertTrue("1 and one should be considdered the same address", new Ip4Address(1L, 5L).isSameAddressAs("0.0.0.1"));
        Assert.assertFalse("zero and 0L should be considdered the same address but a Long won't be accepted", new Ip4Address("0.0.0.0", "00:00:00:00:00:08").isSameAddressAs(0L));
    }

}
