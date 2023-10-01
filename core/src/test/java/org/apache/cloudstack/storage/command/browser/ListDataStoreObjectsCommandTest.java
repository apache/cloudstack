package org.apache.cloudstack.storage.command.browser;

import com.cloud.agent.api.to.DataStoreTO;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ListDataStoreObjectsCommandTest {

    @Test
    public void testStartIndex() {
        DataStoreTO dataStore = Mockito.mock(DataStoreTO.class);
        ListDataStoreObjectsCommand cmd = new ListDataStoreObjectsCommand(dataStore, "path", 40, 10);
        assertEquals(40, cmd.getStartIndex());
        assertEquals(10, cmd.getPageSize());
        assertEquals("path", cmd.getPath());
        assertEquals(dataStore, cmd.getStore());
        assertFalse(cmd.executeInSequence());
    }

}
