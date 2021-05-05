/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cloudstack.utils.mailing;

import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPTransport;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import junit.framework.TestCase;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.mail.EmailConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SMTPMailSenderTest extends TestCase {

    private SMTPMailSender smtpMailSender;
    private Map<String, String> configsMock = Mockito.mock(Map.class);
    private String namespace = "test";
    private String enabledProtocols = "mail.smtp.ssl.protocols";

    @Before
    public void before() {
        smtpMailSender = new SMTPMailSender(configsMock, namespace);
    }

    private String getConfigName(String config) {
        return String.format("%s.%s", namespace, config);
    }

    @Test
    public void validateSetSessionPropertiesUseStartTLSTrue() {
        Map<String, String> configs = new HashMap<>();
        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_STARTTLS), "true");

        smtpMailSender.configs = configs;
        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertTrue(props.getUseStartTLS());
    }

    @Test
    public void validateSetSessionPropertiesUseStartTLSFalse() {
        Map<String, String> configs = new HashMap<>();
        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_STARTTLS), "false");

        smtpMailSender.configs = configs;
        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertFalse(props.getUseStartTLS());
    }

    @Test
    public void validateSetSessionPropertiesUseStartTLSUndefinedUseDefaultFalse() {
        SMTPMailSender smtpMailSender = new SMTPMailSender(configsMock, namespace);

        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertFalse(props.getUseStartTLS());
    }

    @Test
    public void validateSetSessionPropertiesUseAuthTrue() {
        Map<String, String> configs = new HashMap<>();
        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_AUTH), "true");

        smtpMailSender.configs = configs;
        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertTrue(props.getUseAuth());
    }

    @Test
    public void validateSetSessionPropertiesUseAuthFalse() {
        Map<String, String> configs = new HashMap<>();
        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_AUTH), "false");

        smtpMailSender.configs = configs;
        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertFalse(props.getUseAuth());
    }

    @Test
    public void validateSetSessionPropertiesUseAuthUndefinedUseDefaultFalse() {
        SMTPMailSender smtpMailSender = new SMTPMailSender(configsMock, namespace);

        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertFalse(props.getUseAuth());
    }

    @Test
    public void validateSetSessionPropertiesDebugModeTrue() {
        Map<String, String> configs = new HashMap<>();
        configs.put(getConfigName(SMTPMailSender.CONFIG_DEBUG_MODE), "true");

        smtpMailSender.configs = configs;
        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertTrue(props.getDebugMode());
    }

    @Test
    public void validateSetSessionPropertiesDebugModeFalse() {
        Map<String, String> configs = new HashMap<>();
        configs.put(getConfigName(SMTPMailSender.CONFIG_DEBUG_MODE), "false");

        smtpMailSender.configs = configs;
        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertFalse(props.getDebugMode());
    }

    @Test
    public void validateSetSessionPropertiesDebugModeUndefinedUseDefaultFalse() {
        SMTPMailSender smtpMailSender = new SMTPMailSender(configsMock, namespace);

        SMTPSessionProperties props = smtpMailSender.configureSessionProperties();

        assertFalse(props.getDebugMode());
    }

    @Test
    public void validateSMTPMailSenderConstructorHostDefinedAsNullNoSessionCreated() {
        SMTPMailSender smtpMailSender = new SMTPMailSender(new HashMap<>(), namespace);

        assertNull(smtpMailSender.sessionProps.getHost());
        assertNull(smtpMailSender.session);
    }

    @Test
    public void validateSMTPMailSenderConstructorHostDefinedAsEmptyNoSessionCreated() {
        Map<String, String> configs = new HashMap<>();

        String host = "";

        configs.put(getConfigName(SMTPMailSender.CONFIG_HOST), host);
        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(host, smtpMailSender.sessionProps.getHost());
        assertNull(smtpMailSender.session);
    }

    @Test
    public void validateSMTPMailSenderConstructorHostDefinedAsBlankNoSessionCreated() {
        Map<String, String> configs = new HashMap<>();

        String host = "    ";

        configs.put(getConfigName(SMTPMailSender.CONFIG_HOST), host);
        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(host, smtpMailSender.sessionProps.getHost());
        assertNull(smtpMailSender.session);
    }

    @Test
    public void validateSMTPMailSenderConstructorHostDefinedSessionCreated() {
        Map<String, String> configs = new HashMap<>();

        String host = "smtp.acme.org";

        configs.put(getConfigName(SMTPMailSender.CONFIG_HOST), host);
        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(host, smtpMailSender.sessionProps.getHost());
        assertNotNull(smtpMailSender.session);
    }

    private Map<String, String> getConfigsWithHost() {
        Map<String, String> configs = new HashMap<>();

        String host = "smtp.acme.org";

        configs.put(getConfigName(SMTPMailSender.CONFIG_HOST), host);

        return configs;
    }

    @Test
    public void validateSMTPMailSenderConstructorPortUndefinedUseDefault25() {
        Map<String, String> configs = getConfigsWithHost();

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(25, smtpMailSender.sessionProps.getPort());
    }

    @Test
    public void validateSMTPMailSenderConstructorPortDefined() {
        Map<String, String> configs = getConfigsWithHost();

        int port = 465;
        configs.put(getConfigName(SMTPMailSender.CONFIG_PORT), String.valueOf(port));

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(port, smtpMailSender.sessionProps.getPort());
    }

    @Test
    public void validateSMTPMailSenderConstructorWithTimeoutUndefined() {
        Map<String, String> configs = getConfigsWithHost();

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNull(smtpMailSender.sessionProps.getTimeout());
    }

    @Test
    public void validateSMTPMailSenderConstructorWithTimeoutDefined() {
        Map<String, String> configs = getConfigsWithHost();

        Integer timeout = 12345;
        configs.put(getConfigName(SMTPMailSender.CONFIG_TIMEOUT), String.valueOf(timeout));

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(timeout, smtpMailSender.sessionProps.getTimeout());
    }

    @Test
    public void validateSMTPMailSenderConstructorWithConnectionTimeoutUndefined() {
        Map<String, String> configs = getConfigsWithHost();

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNull(smtpMailSender.sessionProps.getConnectionTimeout());
    }

    @Test
    public void validateSMTPMailSenderConstructorWithConnectionTimeoutDefined() {
        Map<String, String> configs = getConfigsWithHost();

        Integer connectionTimeout = 12345;
        configs.put(getConfigName(SMTPMailSender.CONFIG_CONNECTION_TIMEOUT), String.valueOf(connectionTimeout));

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(connectionTimeout, smtpMailSender.sessionProps.getConnectionTimeout());
    }

    @Test
    public void validateSMTPMailSenderConstructorWithUsernameUndefined() {
        Map<String, String> configs = getConfigsWithHost();

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNull(smtpMailSender.sessionProps.getUsername());
    }

    @Test
    public void validateSMTPMailSenderConstructorWithUsernameDefinedAsEmpty() {
        Map<String, String> configs = getConfigsWithHost();

        String username = "";
        configs.put(getConfigName(SMTPMailSender.CONFIG_USERNAME), username);

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNotNull(smtpMailSender.sessionProps.getUsername());
        assertEquals(username, smtpMailSender.session.getProperties().get(EmailConstants.MAIL_SMTP_USER));
    }

    @Test
    public void validateSMTPMailSenderConstructorWithUsernameDefinedAsBlank() {
        Map<String, String> configs = getConfigsWithHost();

        String username = "     ";
        configs.put(getConfigName(SMTPMailSender.CONFIG_USERNAME), username);

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNotNull(smtpMailSender.sessionProps.getUsername());
        assertEquals(username, smtpMailSender.session.getProperties().get(EmailConstants.MAIL_SMTP_USER));
    }

    @Test
    public void validateSMTPMailSenderConstructorWithValidUsername() {
        Map<String, String> configs = getConfigsWithHost();

        String username = "test@test.com";
        configs.put(getConfigName(SMTPMailSender.CONFIG_USERNAME), username);

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNotNull(smtpMailSender.sessionProps.getUsername());
        assertEquals(username, smtpMailSender.session.getProperties().get(EmailConstants.MAIL_SMTP_USER));
    }

    @Test
    public void validateSMTPMailSenderConstructorWithProtocolsUndefined() {
        Map<String, String> configs = getConfigsWithHost();

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNull(smtpMailSender.sessionProps.getEnabledSecurityProtocols());
        assertNull(smtpMailSender.session.getProperties().get(enabledProtocols));
    }

    @Test
    public void validateSMTPMailSenderConstructorWithProtocolsDefinedAsEmpty() {
        Map<String, String> configs = getConfigsWithHost();

        String protocols = "";
        configs.put(getConfigName(SMTPMailSender.CONFIG_ENABLED_SECURITY_PROTOCOLS), protocols);

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(protocols, smtpMailSender.sessionProps.getEnabledSecurityProtocols());
        assertNull(smtpMailSender.session.getProperties().get(enabledProtocols));
    }

    @Test
    public void validateSMTPMailSenderConstructorWithProtocolsDefinedAsBlank() {
        Map<String, String> configs = getConfigsWithHost();

        String protocols = "     ";
        configs.put(getConfigName(SMTPMailSender.CONFIG_ENABLED_SECURITY_PROTOCOLS), protocols);

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(protocols, smtpMailSender.sessionProps.getEnabledSecurityProtocols());
        assertNull(smtpMailSender.session.getProperties().get(enabledProtocols));
    }

    @Test
    public void validateSMTPMailSenderConstructorWithValidProtocol() {
        Map<String, String> configs = getConfigsWithHost();

        String protocols = "TLSv1";
        configs.put(getConfigName(SMTPMailSender.CONFIG_ENABLED_SECURITY_PROTOCOLS), protocols);

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(protocols, smtpMailSender.sessionProps.getEnabledSecurityProtocols());
        assertEquals(protocols, smtpMailSender.session.getProperties().get(enabledProtocols));
    }

    @Test
    public void validateSMTPMailSenderConstructorWithMultipleValidsProtocols() {
        Map<String, String> configs = getConfigsWithHost();

        String protocols = "TLSv1 TLSv1.2";
        configs.put(getConfigName(SMTPMailSender.CONFIG_ENABLED_SECURITY_PROTOCOLS), protocols);

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertEquals(protocols, smtpMailSender.sessionProps.getEnabledSecurityProtocols());
        assertEquals(protocols, smtpMailSender.session.getProperties().get(enabledProtocols));
    }

    @Test
    public void validateSMTPMailSenderConstructorUseAuthFalseUseStartTLSFalseStartTLSEnabledMustBeNull() {
        Map<String, String> configs = getConfigsWithHost();

        Boolean useAuth = false;
        Boolean useStartTLS = false;

        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_AUTH), String.valueOf(useAuth));
        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_STARTTLS), String.valueOf(useStartTLS));

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNull(smtpMailSender.session.getProperties().get(EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE));
    }

    @Test
    public void validateSMTPMailSenderConstructorUseAuthFalseUseStartTLSTrueStartTLSEnabledMustBeNull() {
        Map<String, String> configs = getConfigsWithHost();

        Boolean useAuth = false;
        Boolean useStartTLS = true;

        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_AUTH), String.valueOf(useAuth));
        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_STARTTLS), String.valueOf(useStartTLS));

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertNull(smtpMailSender.session.getProperties().get(EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE));
    }

    @Test
    public void validateSMTPMailSenderConstructorUseAuthTrueUseStartTLSFalseStartTLSEnabledMustBeFalse() {
        Map<String, String> configs = getConfigsWithHost();

        Boolean useAuth = true;
        Boolean useStartTLS = false;

        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_AUTH), String.valueOf(useAuth));
        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_STARTTLS), String.valueOf(useStartTLS));

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertFalse((boolean)smtpMailSender.session.getProperties().get(EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE));
    }

    @Test
    public void validateSMTPMailSenderConstructorUseAuthTrueUseStartTLSTrueStartTLSEnabledMustBeFalse() {
        Map<String, String> configs = getConfigsWithHost();

        Boolean useAuth = true;
        Boolean useStartTLS = true;

        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_AUTH), String.valueOf(useAuth));
        configs.put(getConfigName(SMTPMailSender.CONFIG_USE_STARTTLS), String.valueOf(useStartTLS));

        SMTPMailSender smtpMailSender = new SMTPMailSender(configs, namespace);

        assertTrue((boolean)smtpMailSender.session.getProperties().get(EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE));
    }

    @Test
    public void validateSMTPMailSenderCreateMessageFromDefinedAsNull() throws MessagingException, UnsupportedEncodingException {
        smtpMailSender = smtpMailSender = Mockito.spy(smtpMailSender);

        SMTPMailProperties mailProps = new SMTPMailProperties();

        mailProps.setSender(new MailAddress("test@test.com"));
        mailProps.setContent("A simple test");
        mailProps.setContentType("text/plain");

        Mockito.doReturn(true).when(smtpMailSender).setMailRecipients(Mockito.any(SMTPMessage.class), Mockito.any(), Mockito.any());

        SMTPMessage message = smtpMailSender.createMessage(mailProps);

        assertEquals("\"test@test.com\" <test@test.com>", message.getFrom()[0].toString());
    }

    @Test
    public void validateSMTPMailSenderCreateMessageFromDefined() throws MessagingException, UnsupportedEncodingException {
        smtpMailSender = smtpMailSender = Mockito.spy(smtpMailSender);

        SMTPMailProperties mailProps = new SMTPMailProperties();

        mailProps.setSender(new MailAddress("test@test.com"));
        mailProps.setFrom(new MailAddress("test2@test2.com", "TEST2"));
        mailProps.setContent("A simple test");
        mailProps.setContentType("text/plain");

        Mockito.doReturn(true).when(smtpMailSender).setMailRecipients(Mockito.any(SMTPMessage.class), Mockito.any(), Mockito.any());

        SMTPMessage message = smtpMailSender.createMessage(mailProps);

        assertEquals("TEST2 <test2@test2.com>", message.getFrom()[0].toString());
    }

    @Test
    public void validateSMTPMailSenderCreateMessageSentDateDefined() throws MessagingException, UnsupportedEncodingException {
        smtpMailSender = smtpMailSender = Mockito.spy(smtpMailSender);

        SMTPMailProperties mailProps = new SMTPMailProperties();

        mailProps.setSender(new MailAddress("test@test.com"));
        mailProps.setContent("A simple test");
        mailProps.setContentType("text/plain");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        mailProps.setSentDate(cal.getTime());

        Mockito.doReturn(true).when(smtpMailSender).setMailRecipients(Mockito.any(SMTPMessage.class), Mockito.any(), Mockito.any());

        SMTPMessage message = smtpMailSender.createMessage(mailProps);
        assertTrue(DateUtils.truncatedEquals(cal.getTime(), message.getSentDate(), Calendar.SECOND));
    }

    @Test
    public void validateSMTPMailSenderCreateMessageSubjectContentAndContentTypeDefined() throws MessagingException, UnsupportedEncodingException, IOException {
        smtpMailSender = smtpMailSender = Mockito.spy(smtpMailSender);

        SMTPMailProperties mailProps = new SMTPMailProperties();

        String subject = "A TEST";
        String content = "A simple test";
        String contentType = "text/plain;charset=utf8";

        mailProps.setSender(new MailAddress("test@test.com"));
        mailProps.setSubject(subject);
        mailProps.setContent(content);
        mailProps.setContentType(contentType);

        Mockito.doReturn(true).when(smtpMailSender).setMailRecipients(Mockito.any(SMTPMessage.class), Mockito.any(), Mockito.any());

        SMTPMessage message = smtpMailSender.createMessage(mailProps);
        assertEquals(subject, message.getSubject());
        assertEquals(content, message.getContent());
        assertEquals(contentType, message.getContentType());
    }

    @Test
    public void setMailRecipientsTest() throws UnsupportedEncodingException, MessagingException {
        SMTPMessage messageMock = new SMTPMessage(Mockito.mock(MimeMessage.class));

        Set<MailAddress> recipients = new HashSet<>();
        recipients.add(new MailAddress(null));
        recipients.add(new MailAddress(""));
        recipients.add(new MailAddress("  "));
        recipients.add(new MailAddress("smtp.acme.org"));
        recipients.add(new MailAddress("smtp.acme2.org", "Coyote"));

        boolean returnOfSetEmail = smtpMailSender.setMailRecipients(messageMock, recipients, "A simple test");

        Address[] allRecipients = messageMock.getAllRecipients();

        int expectedNumberOfValidRecipientsConfigured = 2;
        assertEquals(expectedNumberOfValidRecipientsConfigured, allRecipients.length);

        assertEquals("\"smtp.acme.org\" <smtp.acme.org>", allRecipients[0].toString());
        assertEquals("Coyote <smtp.acme2.org>", allRecipients[1].toString());

        assertTrue(returnOfSetEmail);
    }

    @Test
    public void setMailRecipientsTestOnlyInvalidEmailSettings() throws UnsupportedEncodingException, MessagingException {
        SMTPMessage messageMock = new SMTPMessage(Mockito.mock(MimeMessage.class));

        messageMock = messageMock = Mockito.spy(messageMock);
        Mockito.doReturn(new Address[0]).when(messageMock).getAllRecipients();

        Set<MailAddress> recipients = new HashSet<>();
        recipients.add(new MailAddress(null));
        recipients.add(new MailAddress(""));
        recipients.add(new MailAddress("  "));

        boolean returnOfSetEmail = smtpMailSender.setMailRecipients(messageMock, recipients, "A simple test");

        Address[] allRecipients = messageMock.getAllRecipients();

        int expectedNumberOfValidRecipientsConfigured = 0;
        assertEquals(expectedNumberOfValidRecipientsConfigured, allRecipients.length);

        assertFalse(returnOfSetEmail);
    }

    @Test
    public void validateSMTPMailSenderSendMailWithNullSession() {
        SMTPMailProperties mailProps = new SMTPMailProperties();

        boolean returnOfSendMail = smtpMailSender.sendMail(mailProps);

        assertFalse(returnOfSendMail);
    }

    @Test
    public void validateSMTPMailSenderSendMailWithValidSession() throws MessagingException, UnsupportedEncodingException {
        smtpMailSender = smtpMailSender = Mockito.spy(smtpMailSender);
        SMTPMailProperties mailProps = new SMTPMailProperties();

        smtpMailSender.session = Session.getDefaultInstance(Mockito.mock(Properties.class));

        Mockito.doReturn(Mockito.mock(SMTPMessage.class)).when(smtpMailSender).createMessage(Mockito.any(SMTPMailProperties.class));
        Mockito.doReturn(Mockito.mock(SMTPTransport.class)).when(smtpMailSender).createSmtpTransport();

        boolean returnOfSendMail = smtpMailSender.sendMail(mailProps);

        assertTrue(returnOfSendMail);
    }

    @Test
    public void validateSMTPMailSenderGetConfigPropertyUndefinedMustReturnNull() {
        smtpMailSender = smtpMailSender = Mockito.spy(smtpMailSender);

        String returnOfPropertyThatDoesNotExist = smtpMailSender.getConfig("test");

        assertNull(returnOfPropertyThatDoesNotExist);
    }

    public void validateSMTPMailSenderGetConfigPropertyDefinedMustReturnIt() {
        smtpMailSender = smtpMailSender = Mockito.spy(smtpMailSender);

        Map<String, String> configs = new HashMap<>();

        String host = "smtp.acme.org";
        configs.put(getConfigName(SMTPMailSender.CONFIG_HOST), host);

        smtpMailSender.configs = configs;

        String returnOfPropertyThatExist = smtpMailSender.getConfig(getConfigName(SMTPMailSender.CONFIG_HOST));

        assertNotNull(returnOfPropertyThatExist);
        assertNotNull(host, returnOfPropertyThatExist);
    }
}
