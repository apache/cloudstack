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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class CloudInitUserDataProviderTest {

    private final CloudInitUserDataProvider provider = new CloudInitUserDataProvider();
    private final static String CLOUD_CONFIG_USERDATA = "## template: jinja\n" +
            "#cloud-config\n" +
            "runcmd:\n" +
            "   - echo 'TestVariable {{ ds.meta_data.variable1 }}' >> /tmp/variable\n" +
            "   - echo 'Hostname {{ ds.meta_data.public_hostname }}' > /tmp/hostname";
    private final static String CLOUD_CONFIG_USERDATA1 = "#cloud-config\n" +
            "password: atomic\n" +
            "chpasswd: { expire: False }\n" +
            "ssh_pwauth: True";
    private final static String SHELL_SCRIPT_USERDATA = "#!/bin/bash\n" +
            "date > /provisioned";
    private final static String SHELL_SCRIPT_USERDATA1 = "#!/bin/bash\n" +
            "mkdir /tmp/test";
    private final static String SINGLE_BODYPART_CLOUDCONFIG_MULTIPART_USERDATA =
            "Content-Type: multipart/mixed; boundary=\"//\"\n" +
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
    private static final Session session = Session.getDefaultInstance(new Properties());

    @Test
    public void testGetUserDataFormatType() {
        CloudInitUserDataProvider.FormatType type = provider.getUserDataFormatType(CLOUD_CONFIG_USERDATA);
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

    private MimeMultipart getCheckedMultipartFromMultipartData(String multipartUserData, int count) {
        MimeMultipart multipart = null;
        Assert.assertTrue(multipartUserData.contains("Content-Type: multipart"));
        try {
            MimeMessage msgFromUserdata = new MimeMessage(session,
                    new ByteArrayInputStream(multipartUserData.getBytes()));
            multipart = (MimeMultipart)msgFromUserdata.getContent();
            Assert.assertEquals(count, multipart.getCount());
        } catch (MessagingException | IOException e) {
            Assert.fail(String.format("Failed with exception, %s", e.getMessage()));
        }
        return multipart;
    }

    @Test
    public void testAppendUserData() {
        String multipartUserData = provider.appendUserData(Base64.encodeBase64String(CLOUD_CONFIG_USERDATA1.getBytes()),
                Base64.encodeBase64String(SHELL_SCRIPT_USERDATA.getBytes()));
        getCheckedMultipartFromMultipartData(multipartUserData, 2);
    }

    @Test
    public void testAppendSameShellScriptTypeUserData() {
        String result = SHELL_SCRIPT_USERDATA + "\n\n" +
                SHELL_SCRIPT_USERDATA1.replace("#!/bin/bash\n", "");
        String appendUserData = provider.appendUserData(Base64.encodeBase64String(SHELL_SCRIPT_USERDATA.getBytes()),
                Base64.encodeBase64String(SHELL_SCRIPT_USERDATA1.getBytes()));
        Assert.assertEquals(result, appendUserData);
    }

    @Test
    public void testAppendSameCloudConfigTypeUserData() {
        String result = CLOUD_CONFIG_USERDATA + "\n\n" +
                CLOUD_CONFIG_USERDATA1.replace("#cloud-config\n", "");
        String appendUserData = provider.appendUserData(Base64.encodeBase64String(CLOUD_CONFIG_USERDATA.getBytes()),
                Base64.encodeBase64String(CLOUD_CONFIG_USERDATA1.getBytes()));
        Assert.assertEquals(result, appendUserData);
    }

    @Test
    public void testAppendUserDataMIMETemplateData() {
        String multipartUserData = provider.appendUserData(
                Base64.encodeBase64String(SINGLE_BODYPART_CLOUDCONFIG_MULTIPART_USERDATA.getBytes()),
                Base64.encodeBase64String(SHELL_SCRIPT_USERDATA.getBytes()));
        getCheckedMultipartFromMultipartData(multipartUserData, 2);
    }

    @Test
    public void testAppendUserDataExistingMultipartWithSameType() {
        String templateData = provider.appendUserData(Base64.encodeBase64String(CLOUD_CONFIG_USERDATA1.getBytes()),
                Base64.encodeBase64String(SHELL_SCRIPT_USERDATA.getBytes()));
        String multipartUserData = provider.appendUserData(Base64.encodeBase64String(templateData.getBytes()),
                Base64.encodeBase64String(SHELL_SCRIPT_USERDATA1.getBytes()));
        String resultantShellScript = SHELL_SCRIPT_USERDATA + "\n\n" +
                SHELL_SCRIPT_USERDATA1.replace("#!/bin/bash\n", "");
        MimeMultipart mimeMultipart = getCheckedMultipartFromMultipartData(multipartUserData, 2);
        try {
            for (int i = 0; i < mimeMultipart.getCount(); ++i) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.getContentType().startsWith("text/x-shellscript")) {
                    Assert.assertEquals(resultantShellScript, provider.getBodyPartContentAsString(bodyPart));
                } else if (bodyPart.getContentType().startsWith("text/cloud-config")) {
                    Assert.assertEquals(CLOUD_CONFIG_USERDATA1, provider.getBodyPartContentAsString(bodyPart));
                }
            }
        } catch (MessagingException | IOException | CloudRuntimeException e) {
            Assert.fail(String.format("Failed with exception, %s", e.getMessage()));
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAppendUserDataInvalidUserData() {
        String templateData = CLOUD_CONFIG_USERDATA1.replace("#cloud-config\n", "");
        provider.appendUserData(Base64.encodeBase64String(templateData.getBytes()),
                Base64.encodeBase64String(SHELL_SCRIPT_USERDATA.getBytes()));
    }

    @Test
    public void testIsGzippedUserDataWithCloudConfigData() {
        Assert.assertFalse(provider.isGZipped(CLOUD_CONFIG_USERDATA));
    }

    private String createBase64EncodedGzipDataAsString() throws IOException {
        byte[] input = CLOUD_CONFIG_USERDATA.getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream outputStream = new GZIPOutputStream(arrayOutputStream);
        outputStream.write(input,0, input.length);
        outputStream.close();

        return Base64.encodeBase64String(arrayOutputStream.toByteArray());
    }

    @Test
    public void testIsGzippedUserDataWithValidGzipData() {
        try {
            String gzipped = createBase64EncodedGzipDataAsString();
            Assert.assertTrue(provider.isGZipped(gzipped));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAppendUserDataWithGzippedData() {
        try {
            provider.appendUserData(Base64.encodeBase64String(CLOUD_CONFIG_USERDATA.getBytes()),
                    createBase64EncodedGzipDataAsString());
            Assert.fail("Gzipped data shouldn't be appended with other data");
        } catch (IOException e) {
            Assert.fail("Exception encountered: " + e.getMessage());
        }
    }
}
