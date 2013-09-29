package com.cloud.utils.encoding;

import org.junit.Assert;
import org.junit.Test;

public class UrlEncoderTest {
    @Test
    public void encode() {
        Assert.assertEquals("%2Ftmp%2F", new URLEncoder().encode("/tmp/"));
        Assert.assertEquals("%20", new URLEncoder().encode(" "));
        Assert.assertEquals("%5F", new URLEncoder().encode("_"));
        Assert.assertEquals("%25", new URLEncoder().encode("%"));
    }
}
