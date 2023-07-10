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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class CloudInitUserDataProvider extends AdapterBase implements UserDataProvider {

    protected enum FormatType {
        CLOUD_CONFIG, BASH_SCRIPT, MIME, CLOUD_BOOTHOOK, INCLUDE_FILE
    }

    private static final String CLOUD_CONFIG_CONTENT_TYPE = "text/cloud-config";
    private static final String BASH_SCRIPT_CONTENT_TYPE = "text/x-shellscript";
    private static final String INCLUDE_FILE_CONTENT_TYPE = "text/x-include-url";
    private static final String CLOUD_BOOTHOOK_CONTENT_TYPE = "text/cloud-boothook";

    private static final Map<FormatType, String> formatContentTypeMap = Map.ofEntries(
            Map.entry(FormatType.CLOUD_CONFIG, CLOUD_CONFIG_CONTENT_TYPE),
            Map.entry(FormatType.BASH_SCRIPT, BASH_SCRIPT_CONTENT_TYPE),
            Map.entry(FormatType.CLOUD_BOOTHOOK, CLOUD_BOOTHOOK_CONTENT_TYPE),
            Map.entry(FormatType.INCLUDE_FILE, INCLUDE_FILE_CONTENT_TYPE)
    );

    private static final Logger LOGGER = Logger.getLogger(CloudInitUserDataProvider.class);

    private static final Session session = Session.getDefaultInstance(new Properties());

    @Override
    public String getName() {
        return "cloud-init";
    }

    protected boolean isGZipped(String userdata) {
        if (StringUtils.isEmpty(userdata)) {
            return false;
        }
        byte[] data = userdata.getBytes(StandardCharsets.ISO_8859_1);
        if (data.length < 2) {
            return false;
        }
        int magic = data[0] & 0xff | ((data[1] << 8) & 0xff00);
        return magic == GZIPInputStream.GZIP_MAGIC;
    }

    protected String extractUserDataHeader(String userdata) {
        if (isGZipped(userdata)) {
            throw new CloudRuntimeException("Gzipped user data can not be used together with other user data formats");
        }
        List<String> lines = Arrays.stream(userdata.split("\n"))
                .filter(x -> (x.startsWith("#") && !x.startsWith("##")) || (x.startsWith("Content-Type:")))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lines)) {
            throw new CloudRuntimeException("Failed to detect the user data format type as it " +
                    "does not contain a header");
        }
        return lines.get(0);
    }

    protected FormatType mapUserDataHeaderToFormatType(String header) {
        if (header.equalsIgnoreCase("#cloud-config")) {
            return FormatType.CLOUD_CONFIG;
        } else if (header.startsWith("#!")) {
            return FormatType.BASH_SCRIPT;
        } else if (header.equalsIgnoreCase("#cloud-boothook")) {
            return FormatType.CLOUD_BOOTHOOK;
        } else if (header.startsWith("#include")) {
            return FormatType.INCLUDE_FILE;
        } else if (header.startsWith("Content-Type:")) {
            return FormatType.MIME;
        } else {
            String msg = String.format("Cannot recognise the user data format type from the header line: %s." +
                    "Supported types are: cloud-config, bash script, cloud-boothook, include file or MIME", header);
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }

    /**
     * Detect the user data type
     * Reference: <a href="https://canonical-cloud-init.readthedocs-hosted.com/en/latest/explanation/format.html#user-data-formats" />
     */
    protected FormatType getUserDataFormatType(String userdata) {
        if (StringUtils.isBlank(userdata)) {
            String msg = "User data expected but provided empty user data";
            LOGGER.error(msg);
            throw new CloudRuntimeException(msg);
        }

        String header = extractUserDataHeader(userdata);
        return mapUserDataHeaderToFormatType(header);
    }

    private String getContentType(String userData, FormatType formatType) throws MessagingException {
        if (formatType == FormatType.MIME) {
            MimeMessage msg = new MimeMessage(session, new ByteArrayInputStream(userData.getBytes()));
            return msg.getContentType();
        }
        if (!formatContentTypeMap.containsKey(formatType)) {
            throw new CloudRuntimeException(String.format("Cannot get the user data content type as " +
                    "its format type %s is invalid", formatType.name()));
        }
        return formatContentTypeMap.get(formatType);
    }

    protected MimeBodyPart generateBodyPartMIMEMessage(String userData, FormatType formatType) throws MessagingException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        String contentType = getContentType(userData, formatType);
        bodyPart.setContent(userData, contentType);
        bodyPart.addHeader("Content-Transfer-Encoding", "base64");
        return bodyPart;
    }

    private Multipart getMessageContent(MimeMessage message) {
        Multipart messageContent;
        try {
            messageContent = (MimeMultipart) message.getContent();
        } catch (IOException | MessagingException e) {
            messageContent = new MimeMultipart();
        }
        return messageContent;
    }

    private void addBodyPartsToMessageContentFromUserDataContent(Multipart messageContent,
                                                                 MimeMessage msgFromUserdata) throws MessagingException, IOException {
        Multipart msgFromUserdataParts = (MimeMultipart) msgFromUserdata.getContent();
        int count = msgFromUserdataParts.getCount();
        int i = 0;
        while (i < count) {
            BodyPart bodyPart = msgFromUserdataParts.getBodyPart(0);
            messageContent.addBodyPart(bodyPart);
            i++;
        }
    }

    private MimeMessage createMultipartMessageAddingUserdata(String userData, FormatType formatType,
                                                           MimeMessage message) throws MessagingException, IOException {
        MimeMessage newMessage = new MimeMessage(session);
        Multipart messageContent = getMessageContent(message);

        if (formatType == FormatType.MIME) {
            MimeMessage msgFromUserdata = new MimeMessage(session, new ByteArrayInputStream(userData.getBytes()));
            addBodyPartsToMessageContentFromUserDataContent(messageContent, msgFromUserdata);
        } else {
            MimeBodyPart part = generateBodyPartMIMEMessage(userData, formatType);
            messageContent.addBodyPart(part);
        }
        newMessage.setContent(messageContent);
        return newMessage;
    }

    @Override
    public String appendUserData(String userData1, String userData2) {
        try {
            FormatType formatType1 = getUserDataFormatType(userData1);
            FormatType formatType2 = getUserDataFormatType(userData2);
            MimeMessage message = new MimeMessage(session);
            message = createMultipartMessageAddingUserdata(userData1, formatType1, message);
            message = createMultipartMessageAddingUserdata(userData2, formatType2, message);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            message.writeTo(output);
            return output.toString();
        } catch (MessagingException | IOException | CloudRuntimeException e) {
            String msg = String.format("Error attempting to merge user data as a multipart user data. " +
                    "Reason: %s", e.getMessage());
            LOGGER.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }
}
