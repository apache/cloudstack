package com.cloud.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static com.cloud.utils.HumanReadableJson.getHumanReadableBytesJson;

public class HumanReadableJsonTest {

    @Test
    public void parseJsonObjectTest() {
        assertEquals("{}", getHumanReadableBytesJson("{}"));
    }
    @Test
    public void parseJsonArrayTest() {
        assertEquals("[]", getHumanReadableBytesJson("[]"));
        assertEquals("[{},{}]", getHumanReadableBytesJson("[{},{}]"));
    }
    @Test
    public void parseSimpleJsonTest() {
        assertEquals("[{\"object\":{}}]", getHumanReadableBytesJson("[{\"object\":{}}]"));
    }
    @Test
    public void parseComplexJsonTest() {
        assertEquals("[{\"object\":[]}]", getHumanReadableBytesJson("[{\"object\":[]}]"));
        assertEquals("[{\"object\":[{},{}]}]", getHumanReadableBytesJson("[{\"object\":[{},{}]}]"));
        assertEquals("[{\"object\":[]},{\"object\":[]}]", getHumanReadableBytesJson("[{\"object\":[]},{\"object\":[]}]"));
        assertEquals("[{\"object\":[{\"object\":[]}]},{\"object\":[]}]", getHumanReadableBytesJson("[{\"object\":[{\"object\":[]}]},{\"object\":[]}]"));
    }
    @Test
    public void parseMatchJsonTest() {
        assertEquals("[{\"size\":\"(0 bytes) 0\"}]", getHumanReadableBytesJson("[{\"size\": \"0\"}]"));
        assertEquals("[{\"size\":\"(0 bytes) 0\",\"bytesSent\":\"(0 bytes) 0\"}]", getHumanReadableBytesJson("[{\"size\": \"0\", \"bytesSent\": \"0\"}]"));
    }
}
