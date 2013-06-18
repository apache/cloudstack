package com.cloud.utils;

import java.io.File;
import java.io.IOException;

import javax.naming.ConfigurationException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class ProcessUtilTest {

    File pidFile;

    @Before
    public void setup() throws IOException {
        pidFile = File.createTempFile("test", ".pid");
    }

    @After
    public void cleanup() {
        FileUtils.deleteQuietly(pidFile);
    }

    @Test
    public void pidCheck() throws ConfigurationException, IOException {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        FileUtils.writeStringToFile(pidFile, "123456\n");
        ProcessUtil.pidCheck(pidFile.getParent(), pidFile.getName());
        String pidStr = FileUtils.readFileToString(pidFile);
        Assert.assertFalse("pid can not be blank", pidStr.isEmpty());
        int pid = Integer.parseInt(pidStr.trim());
        int maxPid = Integer.parseInt(FileUtils.readFileToString(new File("/proc/sys/kernel/pid_max")).trim());
        Assert.assertTrue(pid <= maxPid);
    }

    @Test(expected = ConfigurationException.class)
    public void pidCheckBadPid() throws ConfigurationException, IOException {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        FileUtils.writeStringToFile(pidFile, "intentionally not number");
        ProcessUtil.pidCheck(pidFile.getParent(), pidFile.getName());
    }

    @Test(expected = ConfigurationException.class)
    public void pidCheckEmptyPid() throws ConfigurationException, IOException {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        FileUtils.writeStringToFile(pidFile, "intentionally not number");
        ProcessUtil.pidCheck(pidFile.getParent(), pidFile.getName());
    }

}
