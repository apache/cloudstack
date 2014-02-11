package com.cloud.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UuidUtilsTest {

    @Test
    public void testValidateUUIDPass() throws Exception {
        String serviceUuid = "f81a9aa3-1f7d-466f-b04b-f2b101486bae";

        assertTrue(UuidUtils.validateUUID(serviceUuid));
    }

    @Test
    public void testValidateUUIDFail() throws Exception {
        String serviceUuid = "6fc6ce7-d503-4f95-9e68-c9cd281b13df";

        assertFalse(UuidUtils.validateUUID(serviceUuid));
    }
}