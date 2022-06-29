package org.apache.cloudstack.utils.cryptsetup;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class KeyFileTest {

    @Test
    public void keyFileTest() throws IOException {
        byte[] contents = "the quick brown fox".getBytes();
        KeyFile keyFile = new KeyFile(contents);
        System.out.printf("New test KeyFile at %s%n", keyFile);
        Path path = keyFile.getPath();

        Assert.assertTrue(keyFile.isSet());

        // check contents
        byte[] fileContents = Files.readAllBytes(path);
        Assert.assertArrayEquals(contents, fileContents);

        // delete file on close
        keyFile.close();

        Assert.assertFalse("key file was not cleaned up", Files.exists(path));
        Assert.assertFalse("key file is still set", keyFile.isSet());
    }
}
