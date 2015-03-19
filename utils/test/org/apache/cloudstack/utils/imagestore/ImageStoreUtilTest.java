package org.apache.cloudstack.utils.imagestore;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class ImageStoreUtilTest {

    @Test
    public void testgeneratePostUploadUrl() throws MalformedURLException {
        String ssvmdomain = "*.realhostip.com";
        String ipAddress = "10.147.28.14";
        String uuid = UUID.randomUUID().toString();

        //ssvm domain is not set
        String url = ImageStoreUtil.generatePostUploadUrl(null, ipAddress, uuid);
        assertPostUploadUrl(url, ipAddress, uuid);

        //ssvm domain is set to empty value
        url = ImageStoreUtil.generatePostUploadUrl("", ipAddress, uuid);
        assertPostUploadUrl(url, ipAddress, uuid);

        //ssvm domain is set to a valid value
        url = ImageStoreUtil.generatePostUploadUrl(ssvmdomain, ipAddress, uuid);
        assertPostUploadUrl(url, ipAddress.replace(".", "-") + ssvmdomain.substring(1), uuid);
    }

    private void assertPostUploadUrl(String urlStr, String domain, String uuid) throws MalformedURLException {
        URL url = new URL(urlStr);
        Assert.assertNotNull(url);
        Assert.assertEquals(url.getHost(), domain);
        Assert.assertEquals(url.getPath(), "/upload/" + uuid);
    }

}