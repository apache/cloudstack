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
import com.sun.mail.smtp.SMTPSSLTransport;
import com.sun.mail.smtp.SMTPTransport;

import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.mail.EmailConstants;
import org.apache.log4j.Logger;

public class SMTPMailSender {

    private Logger logger = Logger.getLogger(SMTPMailSender.class);

    protected Session session = null;
    protected SMTPSessionProperties sessionProps;

    protected static final String CONFIG_HOST = "host";
    protected static final String CONFIG_PORT = "port";
    protected static final String CONFIG_USE_AUTH = "useAuth";
    protected static final String CONFIG_USERNAME = "username";
    protected static final String CONFIG_PASSWORD = "password";
    protected static final String CONFIG_DEBUG_MODE = "debug";
    protected static final String CONFIG_USE_STARTTLS = "useStartTLS";
    protected static final String CONFIG_ENABLED_SECURITY_PROTOCOLS = "enabledSecurityProtocols";
    protected static final String CONFIG_TIMEOUT = "timeout";
    protected static final String CONFIG_CONNECTION_TIMEOUT = "connectiontimeout";

    protected Map<String, String> configs;
    protected String namespace;

    public SMTPMailSender(Map<String, String> configs, String namespace) {

        if (namespace == null) {
            logger.error("Unable to configure SMTP session due to null namespace.");
            return;
        }

        this.configs = configs;
        this.namespace = namespace;
        this.sessionProps = configureSessionProperties();

        if (StringUtils.isNotBlank(sessionProps.getHost())) {
            Properties props = new Properties();

            props.put(EmailConstants.MAIL_HOST, sessionProps.getHost());
            props.put(EmailConstants.MAIL_PORT, sessionProps.getPort());
            props.put(EmailConstants.MAIL_SMTP_AUTH, sessionProps.getUseAuth());

            String username = sessionProps.getUsername();

            if (username != null) {
                props.put(EmailConstants.MAIL_SMTP_USER, username);
            }

            String protocols = sessionProps.getEnabledSecurityProtocols();
            if (StringUtils.isNotBlank(protocols)) {
                props.put("mail.smtp.ssl.protocols", protocols);
            }

            if (sessionProps.getUseAuth()) {
                props.put(EmailConstants.MAIL_TRANSPORT_STARTTLS_ENABLE, sessionProps.getUseStartTLS());
            }

            if (sessionProps.getTimeout() != null) {
                props.put(EmailConstants.MAIL_SMTP_TIMEOUT, sessionProps.getTimeout());
            }

            if (sessionProps.getConnectionTimeout() != null) {
                props.put(EmailConstants.MAIL_SMTP_CONNECTIONTIMEOUT, sessionProps.getConnectionTimeout());
            }

            String password = sessionProps.getPassword();
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            session.setDebug(sessionProps.getDebugMode());

        } else {
            logger.debug("Unable to instantiate SMTP mail session due to empty or null host.");
        }
    }

    protected String getConfig(String config) {
        return this.configs.get(String.format("%s.%s", namespace, config));
    }

    protected SMTPSessionProperties configureSessionProperties() {
        String host = getConfig(CONFIG_HOST);
        String port = getConfig(CONFIG_PORT);
        String useAuth = getConfig(CONFIG_USE_AUTH);
        String username = getConfig(CONFIG_USERNAME);
        String password = getConfig(CONFIG_PASSWORD);
        String debugMode = getConfig(CONFIG_DEBUG_MODE);
        String useStartTLS = getConfig(CONFIG_USE_STARTTLS);
        String enabledSecurityProtocols = getConfig(CONFIG_ENABLED_SECURITY_PROTOCOLS);
        String timeout = getConfig(CONFIG_TIMEOUT);
        String connectionTimeout = getConfig(CONFIG_CONNECTION_TIMEOUT);

        SMTPSessionProperties sessionProps = new SMTPSessionProperties();

        sessionProps.setHost(host);
        sessionProps.setPort(NumberUtils.toInt(port, 25));
        sessionProps.setUseAuth(BooleanUtils.toBoolean(useAuth));
        sessionProps.setUsername(username);
        sessionProps.setPassword(password);
        sessionProps.setUseStartTLS(BooleanUtils.toBoolean(useStartTLS));
        sessionProps.setEnabledSecurityProtocols(enabledSecurityProtocols);
        sessionProps.setDebugMode(BooleanUtils.toBoolean(debugMode));

        sessionProps.setTimeout(timeout == null ? null : NumberUtils.toInt(timeout));
        sessionProps.setConnectionTimeout(connectionTimeout == null ? null : NumberUtils.toInt(connectionTimeout));

        return sessionProps;
    }

    public boolean sendMail(SMTPMailProperties mailProps) {
        if (session == null) {
            logger.error("Unable to send mail due to null session.");
            return false;
        }

        try {
            SMTPMessage message = createMessage(mailProps);

            SMTPTransport smtpTrans = createSmtpTransport();

            smtpTrans.connect();
            smtpTrans.sendMessage(message, message.getAllRecipients());
            smtpTrans.close();

            return true;
        } catch (MessagingException | UnsupportedEncodingException ex) {
            logger.error(String.format("Unable to send mail [%s] to the recipcients [%s].", mailProps.getSubject(), mailProps.getRecipients().toString()), ex);
        }

        return false;

    }

    protected SMTPMessage createMessage(SMTPMailProperties mailProps) throws MessagingException, UnsupportedEncodingException {
        SMTPMessage message = new SMTPMessage(session);

        MailAddress sender = mailProps.getSender();
        MailAddress from = mailProps.getFrom();

        if (from == null) {
            from = sender;
        }

        message.setSender(new InternetAddress(sender.getAddress(), sender.getPersonal()));
        message.setFrom(new InternetAddress(from.getAddress(), from.getPersonal()));

        setMailRecipients(message, mailProps.getRecipients(), mailProps.getSubject());

        message.setSubject(mailProps.getSubject());
        message.setSentDate(mailProps.getSentDate() != null ? mailProps.getSentDate() : new Date());
        message.setContent(mailProps.getContent(), mailProps.getContentType());
        message.saveChanges();
        return message;
    }

    protected SMTPTransport createSmtpTransport() {
        URLName urlName = new URLName("smtp", sessionProps.getHost(), sessionProps.getPort(), null, sessionProps.getUsername(), sessionProps.getPassword());

        if (sessionProps.getUseAuth() && !sessionProps.getUseStartTLS()) {
            return new SMTPSSLTransport(session, urlName);
        }

        return new SMTPTransport(session, urlName);
    }

    protected boolean setMailRecipients(SMTPMessage message, Set<MailAddress> recipients, String subject) throws UnsupportedEncodingException, MessagingException {
        for (MailAddress recipient : recipients) {
            if (StringUtils.isNotBlank(recipient.getAddress())) {
                try {
                    InternetAddress address = new InternetAddress(recipient.getAddress(), recipient.getPersonal());
                    message.addRecipient(Message.RecipientType.TO, address);
                } catch (MessagingException ex) {
                    logger.error(String.format("Unable to create InternetAddres for address [%s].", recipient), ex);
                }
            }
        }

        if (recipients.isEmpty() || message.getAllRecipients().length == 0) {
            logger.error("Unable to send mail due to empty list of recipients.");
            logger.debug(String.format("Unable to send message [%s].", subject));
            return false;
        }

        return true;
    }

}
