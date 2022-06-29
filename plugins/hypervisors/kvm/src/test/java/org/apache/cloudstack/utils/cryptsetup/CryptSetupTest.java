package org.apache.cloudstack.utils.cryptsetup;

import org.apache.cloudstack.secret.PassphraseVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class CryptSetupTest {
    CryptSetup cryptSetup = new CryptSetup();

    @Before
    public void setup() {
        Assume.assumeTrue(cryptSetup.isSupported());
    }

    @Test
    public void cryptSetupTest() throws IOException, CryptSetupException {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
        Path path = Files.createTempFile("cryptsetup", ".tmp",PosixFilePermissions.asFileAttribute(permissions));

        // create a 1MB file to use as a crypt device
        RandomAccessFile file = new RandomAccessFile(path.toFile(),"rw");
        file.setLength(10<<20);
        file.close();

        String filePath = path.toAbsolutePath().toString();
        PassphraseVO passphrase = new PassphraseVO();

        cryptSetup.luksFormat(passphrase.getPassphrase(), CryptSetup.LuksType.LUKS, filePath);

        Assert.assertTrue(cryptSetup.isLuks(filePath));

        Assert.assertTrue(Files.deleteIfExists(path));
    }

    @Test
    public void cryptSetupNonLuksTest() throws IOException {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
        Path path = Files.createTempFile("cryptsetup", ".tmp",PosixFilePermissions.asFileAttribute(permissions));

        Assert.assertFalse(cryptSetup.isLuks(path.toAbsolutePath().toString()));
        Assert.assertTrue(Files.deleteIfExists(path));
    }
}
