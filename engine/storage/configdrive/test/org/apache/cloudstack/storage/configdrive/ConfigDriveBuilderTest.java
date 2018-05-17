package org.apache.cloudstack.storage.configdrive;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigDriveBuilderTest {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConfigDriveBuild() {

        List<String[]> actualVmData = Arrays.asList(
                new String[]{"userdata", "user_data", "c29tZSB1c2VyIGRhdGE="},
                new String[]{"metadata", "service-offering", "offering"},
                new String[]{"metadata", "availability-zone", "zone1"},
                new String[]{"metadata", "local-hostname", "hostname"},
                new String[]{"metadata", "local-ipv4", "192.168.111.111"},
                new String[]{"metadata", "public-hostname", "7.7.7.7"},
                new String[]{"metadata", "public-ipv4", "7.7.7.7"},
                new String[]{"metadata", "vm-id", "uuid"},
                new String[]{"metadata", "instance-id", "i-x-y"},
                new String[]{"metadata", "public-keys", "ssh-rsa some-key"},
                new String[]{"metadata", "cloud-identifier", String.format("CloudStack-{%s}", "uuid")},
                new String[]{"password", "vm_password", "password123"}
        );
        String encodedString = ConfigDriveBuilder.buildConfigDrive(actualVmData, "i-x-y.iso", "config-2");
    }
}