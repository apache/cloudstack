package com.cloud.utils;

import org.junit.Assert;
import org.junit.Test;

public class TernaryTest {
    @Test
    public void testEquals() {
        Assert.assertEquals(new Ternary<String, String, String>("a", "b", "c"), new Ternary<String, String, String>(
                "a", "b", "c"));
        Assert.assertFalse(new Ternary<String, String, String>("a", "b", "c")
                .equals(new Ternary<String, String, String>("a", "b", "d")));
        Assert.assertFalse(new Ternary<String, String, String>("a", "b", "c").equals(""));
        Assert.assertFalse(new Ternary<String, String, String>("a", "b", "c").equals(null));
        Assert.assertFalse(new Ternary<String, String, String>("a", "b", "c")
                .equals(new Pair<String, String>("a", "b")));
    }
}
