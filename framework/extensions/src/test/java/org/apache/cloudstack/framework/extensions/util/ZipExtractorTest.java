package org.apache.cloudstack.framework.extensions.util;

import java.io.IOException;

import org.junit.Test;

import junit.framework.TestCase;

public class ZipExtractorTest extends TestCase {

    @Test
    public void testExtractZip() throws IOException {
        String zipFile = "/Users/manoj/Downloads/cloudstack-firecracker-extension-main.zip";
        ZipExtractor.extractZipContents(zipFile, "/tmp/firecracker");
    }

}
