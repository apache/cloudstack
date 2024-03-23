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
import java.io.InputStream;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.sun.mail.util.BASE64DecoderStream;

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

    private static final Session session = Session.getDefaultInstance(new Properties());

    @Override
    public String getName() {
        return "cloud-init";
    }

    protected boolean isGZipped(String encodedUserdata) {
        if (StringUtils.isEmpty(encodedUserdata)) {
            return false;
        }
        byte[] data = Base64.decodeBase64(encodedUserdata);
        if (data.length < 2) {
            return false;
        }
        int magic = data[0] & 0xff | ((data[1] << 8) & 0xff00);
        return magic == GZIPInputStream.GZIP_MAGIC;
    }

    protected String extractUserDataHeader(String userdata) {
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
            logger.error(msg);
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
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        String header = extractUserDataHeader(userdata);
        return mapUserDataHeaderToFormatType(header);
    }

    private String getContentType(String userData, FormatType formatType) throws MessagingException {
        if (formatType == FormatType.MIME) {
            NoIdMimeMessage msg = new NoIdMimeMessage(session, new ByteArrayInputStream(userData.getBytes()));
            return msg.getContentType();
        }
        if (!formatContentTypeMap.containsKey(formatType)) {
            throw new CloudRuntimeException(String.format("Cannot get the user data content type as " +
                    "its format type %s is invalid", formatType.name()));
        }
        return formatContentTypeMap.get(formatType);
    }

    protected String getBodyPartContentAsString(BodyPart bodyPart) throws MessagingException, IOException {
        Object content = bodyPart.getContent();
        if (content instanceof BASE64DecoderStream) {
            return new String(((BASE64DecoderStream)bodyPart.getContent()).readAllBytes());
        } else if (content instanceof ByteArrayInputStream) {
            return new String(((ByteArrayInputStream)bodyPart.getContent()).readAllBytes());
        } else if (content instanceof String) {
            return (String)bodyPart.getContent();
        }
        throw new CloudRuntimeException(String.format("Failed to get content for multipart data with content type: %s", getBodyPartContentType(bodyPart)));
    }

    private String getBodyPartContentType(BodyPart bodyPart) throws MessagingException {
        String contentType = StringUtils.defaultString(bodyPart.getDataHandler().getContentType(), bodyPart.getContentType());
        return  contentType.contains(";") ? contentType.substring(0, contentType.indexOf(';')) : contentType;
    }

    protected MimeBodyPart generateBodyPartMimeMessage(String userData, String contentType) throws MessagingException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(userData, contentType);
        bodyPart.addHeader("Content-Transfer-Encoding", "base64");
        return bodyPart;
    }

    protected MimeBodyPart generateBodyPartMimeMessage(String userData, FormatType formatType) throws MessagingException {
        return generateBodyPartMimeMessage(userData, getContentType(userData, formatType));
    }

    private Multipart getMessageContent(NoIdMimeMessage message) {
        Multipart messageContent;
        try {
            messageContent = (MimeMultipart) message.getContent();
        } catch (IOException | MessagingException e) {
            messageContent = new MimeMultipart();
        }
        return messageContent;
    }

    private void addBodyPartToMultipart(Multipart existingMultipart, MimeBodyPart bodyPart) throws MessagingException, IOException {
        boolean added = false;
        final int existingCount = existingMultipart.getCount();
        for (int j = 0; j < existingCount; ++j) {
            MimeBodyPart existingBodyPart = (MimeBodyPart)existingMultipart.getBodyPart(j);
            String existingContentType = getBodyPartContentType(existingBodyPart);
            String newContentType = getBodyPartContentType(bodyPart);
            if (existingContentType.equals(newContentType)) {
                String existingContent = getBodyPartContentAsString(existingBodyPart);
                String newContent = getBodyPartContentAsString(bodyPart);
                // generating a combined content MimeBodyPart to replace
                MimeBodyPart combinedBodyPart = generateBodyPartMimeMessage(
                        simpleAppendSameFormatTypeUserData(existingContent, newContent), existingContentType);
                existingMultipart.removeBodyPart(j);
                existingMultipart.addBodyPart(combinedBodyPart, j);
                added = true;
                break;
            }
        }
        if (!added) {
            existingMultipart.addBodyPart(bodyPart);
        }
    }

    private void addBodyPartsToMessageContentFromUserDataContent(Multipart existingMultipart,
                                                                 NoIdMimeMessage msgFromUserdata) throws MessagingException, IOException {
        MimeMultipart newMultipart = (MimeMultipart)msgFromUserdata.getContent();
        final int existingCount = existingMultipart.getCount();
        final int newCount = newMultipart.getCount();
        for (int i = 0; i < newCount; ++i) {
            BodyPart bodyPart = newMultipart.getBodyPart(i);
            if (existingCount == 0) {
                existingMultipart.addBodyPart(bodyPart);
                continue;
            }
            addBodyPartToMultipart(existingMultipart, (MimeBodyPart)bodyPart);
        }
    }

    private NoIdMimeMessage createMultipartMessageAddingUserdata(String userData, FormatType formatType,
                                                           NoIdMimeMessage message) throws MessagingException, IOException {
        NoIdMimeMessage newMessage = new NoIdMimeMessage(session);
        Multipart messageContent = getMessageContent(message);

        if (formatType == FormatType.MIME) {
            NoIdMimeMessage msgFromUserdata = new NoIdMimeMessage(session, new ByteArrayInputStream(userData.getBytes()));
            addBodyPartsToMessageContentFromUserDataContent(messageContent, msgFromUserdata);
        } else {
            MimeBodyPart part = generateBodyPartMimeMessage(userData, formatType);
            addBodyPartToMultipart(messageContent, part);
        }
        newMessage.setContent(messageContent);
        return newMessage;
    }

    private String simpleAppendSameFormatTypeUserData(String userData1, String userData2) {
        return String.format("%s\n\n%s", userData1, userData2.substring(userData2.indexOf('\n')+1));
    }

    private void checkGzipAppend(String encodedUserData1, String encodedUserData2) {
        if (isGZipped(encodedUserData1) || isGZipped(encodedUserData2)) {
            throw new CloudRuntimeException("Gzipped user data can not be used together with other user data formats");
        }
    }

    @Override
    public String appendUserData(String encodedUserData1, String encodedUserData2) {
        try {
            checkGzipAppend(encodedUserData1, encodedUserData2);
            String userData1 = new String(Base64.decodeBase64(encodedUserData1));
            String userData2 = new String(Base64.decodeBase64(encodedUserData2));
            FormatType formatType1 = getUserDataFormatType(userData1);
            FormatType formatType2 = getUserDataFormatType(userData2);
            if (formatType1.equals(formatType2) && List.of(FormatType.CLOUD_CONFIG, FormatType.BASH_SCRIPT).contains(formatType1)) {
                return simpleAppendSameFormatTypeUserData(userData1, userData2);
            }
            NoIdMimeMessage message = new NoIdMimeMessage(session);
            message = createMultipartMessageAddingUserdata(userData1, formatType1, message);
            message = createMultipartMessageAddingUserdata(userData2, formatType2, message);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            message.writeTo(output);
            return output.toString();
        } catch (MessagingException | IOException | CloudRuntimeException e) {
            String msg = String.format("Error attempting to merge user data as a multipart user data. " +
                    "Reason: %s", e.getMessage());
            logger.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    /* This is a wrapper class just to remove Message-ID header from the resultant
       multipart data which may contain server details.
     */
    private class NoIdMimeMessage extends MimeMessage {
        NoIdMimeMessage (Session session) {
            super(session);
        }
        NoIdMimeMessage (Session session, InputStream is) throws MessagingException {
            super(session, is);
        }
        @Override
        protected void updateMessageID() throws MessagingException {
            removeHeader("Message-ID");
        }
    }
}
