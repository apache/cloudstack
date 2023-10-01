package org.apache.cloudstack.storage.command.browser;


import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ListDataStoreObjectsAnswerTest {

    @Test
    public void testGetters() {
        ListDataStoreObjectsAnswer answer = new ListDataStoreObjectsAnswer(true, 2,
                Arrays.asList("file1", "file2"), Arrays.asList("path1", "path2"),
                Arrays.asList("/mnt/datastore/path1", "/mnt/datastore/path2"), Arrays.asList(false, false),
                Arrays.asList(1024L, 2048L), Arrays.asList(123456789L, 987654321L));

        assertTrue(answer.isPathExists());
        assertEquals(2, answer.getCount());
        assertEquals(Arrays.asList("file1", "file2"), answer.getNames());
        assertEquals(Arrays.asList("path1", "path2"), answer.getPaths());
        assertEquals(Arrays.asList("/mnt/datastore/path1", "/mnt/datastore/path2"), answer.getAbsPaths());
        assertEquals(Arrays.asList(false, false), answer.getIsDirs());
        assertEquals(Arrays.asList(1024L, 2048L), answer.getSizes());
        assertEquals(Arrays.asList(123456789L, 987654321L), answer.getLastModified());
    }

    @Test
    public void testEmptyLists() {
        ListDataStoreObjectsAnswer answer = new ListDataStoreObjectsAnswer(true, 0, null, null, null, null, null, null);

        assertTrue(answer.isPathExists());
        assertEquals(0, answer.getCount());
        assertEquals(Collections.emptyList(), answer.getNames());
        assertEquals(Collections.emptyList(), answer.getPaths());
        assertEquals(Collections.emptyList(), answer.getAbsPaths());
        assertEquals(Collections.emptyList(), answer.getIsDirs());
        assertEquals(Collections.emptyList(), answer.getSizes());
        assertEquals(Collections.emptyList(), answer.getLastModified());
    }
}
