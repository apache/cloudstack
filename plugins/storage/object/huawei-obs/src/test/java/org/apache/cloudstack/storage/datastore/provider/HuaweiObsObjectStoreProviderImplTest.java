package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider.DataStoreProviderType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class HuaweiObsObjectStoreProviderImplTest {

    private HuaweiObsObjectStoreProviderImpl huaweiObsObjectStoreProviderImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        huaweiObsObjectStoreProviderImpl = new HuaweiObsObjectStoreProviderImpl();
    }

    @Test
    public void testGetName() {
        String name = huaweiObsObjectStoreProviderImpl.getName();
        assertEquals("Huawei OBS", name);
    }

    @Test
    public void testGetTypes() {
        Set<DataStoreProviderType> types = huaweiObsObjectStoreProviderImpl.getTypes();
        assertEquals(1, types.size());
        assertEquals("OBJECT", types.toArray()[0].toString());
    }
}
