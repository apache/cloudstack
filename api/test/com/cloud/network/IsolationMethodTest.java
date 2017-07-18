package com.cloud.network;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by dahn on 18/07/17.
 */
public class IsolationMethodTest {
    @Test
    public void equalsTest() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla");
        assertEquals(PhysicalNetwork.IsolationMethod.UNKNOWN_PROVIDER, method.provider);
        assertEquals(new PhysicalNetwork.IsolationMethod("bla", PhysicalNetwork.IsolationMethod.UNKNOWN_PROVIDER), method);
    }

    @Test
    public void toStringTest() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla", "blob");
        assertEquals("bla", method.toString());

    }

    @Test
    public void remove() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla", "blob");

        PhysicalNetwork.IsolationMethod.remove("bla","blob");
        assertNull(PhysicalNetwork.IsolationMethod.getIsolationMethod("bla"));

        method = new PhysicalNetwork.IsolationMethod("blob", "bla");

        PhysicalNetwork.IsolationMethod.remove(method);
        assertNull(PhysicalNetwork.IsolationMethod.getIsolationMethod("bla"));
    }
}