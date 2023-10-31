package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SimulatorObjectStoreProviderImplTest {

    private SimulatorObjectStoreProviderImpl simulatorObjectStoreProviderImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        simulatorObjectStoreProviderImpl = new SimulatorObjectStoreProviderImpl();
    }

    @Test
    public void testGetName() {
        String name = simulatorObjectStoreProviderImpl.getName();
        assertEquals("Simulator", name);
    }

    @Test
    public void testGetTypes() {
        Set<DataStoreProvider.DataStoreProviderType> types = simulatorObjectStoreProviderImpl.getTypes();
        assertEquals(1, types.size());
        assertEquals("OBJECT", types.toArray()[0].toString());
    }
}