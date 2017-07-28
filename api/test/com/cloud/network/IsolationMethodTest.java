package com.cloud.network;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IsolationMethodTest {
    @After
    public void cleanTheRegistry() {
        PhysicalNetwork.IsolationMethod.registeredIsolationMethods.removeAll(PhysicalNetwork.IsolationMethod.registeredIsolationMethods);
    }

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
    public void getGeneric() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla", "blob");
        method = new PhysicalNetwork.IsolationMethod("bla");

        assertEquals("blob",PhysicalNetwork.IsolationMethod.getIsolationMethod("bla").getProvider());
    }

    @Test
    public void removeUnregistered() throws Exception {
        assertFalse(PhysicalNetwork.IsolationMethod.remove("bla", "blob"));
    }

    @Test
    public void removeSuccesfulGeneric() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla", "blob");
        method = new PhysicalNetwork.IsolationMethod("bla");

        PhysicalNetwork.IsolationMethod.remove("bla", "blob");
        assertEquals(PhysicalNetwork.IsolationMethod.UNKNOWN_PROVIDER,PhysicalNetwork.IsolationMethod.getIsolationMethod("bla").getProvider());
    }

    @Test(expected= PhysicalNetwork.IsolationMethod.IsolationMethodNotRegistered.class)
    public void removeSuccesfulSpecificByString() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla", "blob");

        PhysicalNetwork.IsolationMethod.remove("bla", "blob");
        PhysicalNetwork.IsolationMethod.getIsolationMethod("bla");
    }

    @Test(expected= PhysicalNetwork.IsolationMethod.IsolationMethodNotRegistered.class)
    public void removeSuccesfulSpecificObject() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla");

        PhysicalNetwork.IsolationMethod.remove(method);
        PhysicalNetwork.IsolationMethod.getIsolationMethod("bla");
    }

    @Test
    public void getIsolationMethodTest() throws Exception {
        PhysicalNetwork.IsolationMethod method = new PhysicalNetwork.IsolationMethod("bla");
        final PhysicalNetwork.IsolationMethod isolationMethod = PhysicalNetwork.IsolationMethod.getIsolationMethod("bla");
        assertEquals(method, isolationMethod);
    }
}