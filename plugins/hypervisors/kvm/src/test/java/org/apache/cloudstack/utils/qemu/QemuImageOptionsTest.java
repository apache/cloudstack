package org.apache.cloudstack.utils.qemu;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class QemuImageOptionsTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        String imagePath = "/path/to/file";
        String secretName = "secretname";
        return Arrays.asList(new Object[][] {
            { null, imagePath, null, new String[]{"--image-opts","file.filename=/path/to/file"} },
            { QemuImg.PhysicalDiskFormat.QCOW2, imagePath, null, new String[]{"--image-opts",String.format("driver=qcow2,file.filename=%s", imagePath)} },
            { QemuImg.PhysicalDiskFormat.RAW, imagePath, secretName, new String[]{"--image-opts",String.format("driver=raw,file.filename=%s", imagePath)} },
            { QemuImg.PhysicalDiskFormat.QCOW2, imagePath, secretName, new String[]{"--image-opts", String.format("driver=qcow2,encrypt.key-secret=%s,file.filename=%s", secretName, imagePath)} },
            { QemuImg.PhysicalDiskFormat.LUKS, imagePath, secretName, new String[]{"--image-opts", String.format("driver=luks,file.filename=%s,key-secret=%s", imagePath, secretName)} }
        });
    }

    public QemuImageOptionsTest(QemuImg.PhysicalDiskFormat format, String filePath, String secretName, String[] expected) {
        this.format = format;
        this.filePath = filePath;
        this.secretName = secretName;
        this.expected = expected;
    }

    private final QemuImg.PhysicalDiskFormat format;
    private final String filePath;
    private final String secretName;
    private final String[] expected;

    @Test
    public void qemuImageOptionsFileNameTest() {
        QemuImageOptions options = new QemuImageOptions(format, filePath, secretName);
        Assert.assertEquals(expected, options.toCommandFlag());
    }
}
