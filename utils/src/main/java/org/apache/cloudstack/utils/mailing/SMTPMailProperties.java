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

import java.util.Date;
import java.util.Set;

public class SMTPMailProperties {

    private MailAddress sender;
    private MailAddress from;
    private Set<MailAddress> recipients;
    private String subject;
    private Date sentDate;
    private Object content;
    private String contentType;

    public SMTPMailProperties() {
    }

    public MailAddress getSender() {
        return sender;
    }

    public void setSender(MailAddress sender) {
        this.sender = sender;
    }

    public MailAddress getFrom() {
        return from;
    }

    public void setFrom(MailAddress from) {
        this.from = from;
    }

    public Set<MailAddress> getRecipients() {
        return recipients;
    }

    public void setRecipients(Set<MailAddress> recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}
