
package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider.DataStoreProviderType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MinIOObjectStoreProviderImplTest {

    private MinIOObjectStoreProviderImpl minioObjectStoreProviderImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        minioObjectStoreProviderImpl = new MinIOObjectStoreProviderImpl();
    }

    @Test
    public void testGetName() {
        String name = minioObjectStoreProviderImpl.getName();
        assertEquals("MinIO", name);
    }

    @Test
    public void testGetTypes() {
        Set<DataStoreProviderType> types = minioObjectStoreProviderImpl.getTypes();
        assertEquals(1, types.size());
        assertEquals("OBJECT", types.toArray()[0].toString());
    }
}