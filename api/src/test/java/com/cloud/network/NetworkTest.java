package com.cloud.network;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class NetworkTest {

    @Test
    public void testProviderContains() {
        List<Network.Provider> providers = new ArrayList<>();
        providers.add(Network.Provider.VirtualRouter);

        // direct instance present
        assertTrue("List should contain VirtualRouter provider", providers.contains(Network.Provider.VirtualRouter));

        // resolved provider by name (registered provider)
        Network.Provider resolved = Network.Provider.getProvider("VirtualRouter");
        assertNotNull("Resolved provider should not be null", resolved);
        assertTrue("List should contain resolved VirtualRouter provider", providers.contains(resolved));

        // transient provider with same name should be considered equal (equals by name)
        Network.Provider transientProvider = Network.Provider.createTransientProvider("NetworkExtension");
        assertFalse("List should not contain the transient provider", providers.contains(transientProvider));

        providers.add(transientProvider);
        assertTrue("List should contain the transient provider", providers.contains(transientProvider));

        // another transient provider with same name should be considered equal
        Network.Provider transientProviderNew = Network.Provider.createTransientProvider("NetworkExtension");
        assertTrue("List should contain the new transient provider with same name", providers.contains(transientProviderNew));
    }
}

