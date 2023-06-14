// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.userdata;

import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Assert;
import org.junit.Test;

public class CloudInitUserDataProviderTest {

    private final CloudInitUserDataProvider provider = new CloudInitUserDataProvider();

    @Test
    public void testGetUserDataFormatType() {
        String userdata = "## template: jinja\n" +
                "#cloud-config\n" +
                "runcmd:\n" +
                "   - echo 'TestVariable {{ ds.meta_data.variable1 }}' >> /tmp/variable\n" +
                "   - echo 'Hostname {{ ds.meta_data.public_hostname }}' > /tmp/hostname";
        CloudInitUserDataProvider.FormatType type = provider.getUserDataFormatType(userdata);
        Assert.assertEquals(CloudInitUserDataProvider.FormatType.CLOUD_CONFIG, type);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetUserDataFormatTypeNoHeader() {
        String userdata = "password: password\nchpasswd: { expire: False }\nssh_pwauth: True";
        provider.getUserDataFormatType(userdata);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetUserDataFormatTypeInvalidType() {
        String userdata = "#invalid-type\n" +
                "password: password\nchpasswd: { expire: False }\nssh_pwauth: True";
        provider.getUserDataFormatType(userdata);
    }

    @Test
    public void testAppendUserData() {
        String templateData = "#cloud-config\n" +
                "password: atomic\n" +
                "chpasswd: { expire: False }\n" +
                "ssh_pwauth: True";
        String vmData = "#!/bin/bash\n" +
                "date > /provisioned";
        String multipartUserData = provider.appendUserData(templateData, vmData);
        Assert.assertTrue(multipartUserData.contains("Content-Type: multipart"));
    }

    @Test
    public void testAppendUserDataMIMETemplateData() {
        String templateData = "Content-Type: multipart/mixed; boundary=\"//\"\n" +
                "MIME-Version: 1.0\n" +
                "\n" +
                "--//\n" +
                "Content-Type: text/cloud-config; charset=\"us-ascii\"\n" +
                "MIME-Version: 1.0\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "Content-Disposition: attachment; filename=\"cloud-config.txt\"\n" +
                "\n" +
                "#cloud-config\n" +
                "\n" +
                "# Upgrade the instance on first boot\n" +
                "# (ie run apt-get upgrade)\n" +
                "#\n" +
                "# Default: false\n" +
                "# Aliases: apt_upgrade\n" +
                "package_upgrade: true";
        String vmData = "#!/bin/bash\n" +
                "date > /provisioned";
        String multipartUserData = provider.appendUserData(templateData, vmData);
        Assert.assertTrue(multipartUserData.contains("Content-Type: multipart"));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAppendUserDataInvalidUserData() {
        String templateData = "password: atomic\n" +
                "chpasswd: { expire: False }\n" +
                "ssh_pwauth: True";
        String vmData = "#!/bin/bash\n" +
                "date > /provisioned";
        provider.appendUserData(templateData, vmData);
    }
}
