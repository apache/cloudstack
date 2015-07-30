//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.quota.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "quota_sent_emails")
public class QuotaSentEmailsVO implements InternalIdentity {
    public QuotaSentEmailsVO(Long id, String fromAddress, String toAddress, String ccAddress, String bccAddress, Date sendDate, String subject, String mailText, Long version, Long updatedBy) {
        super();
        this.id = id;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.ccAddress = ccAddress;
        this.bccAddress = bccAddress;
        this.sendDate = sendDate;
        this.subject = subject;
        this.mailText = mailText;
        this.version = version;
        this.updatedBy = updatedBy;
    }

    private static final long serialVersionUID = -7117933845287653210L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "from_address")
    private String fromAddress = null;

    @Column(name = "to_address")
    private String toAddress = null;

    @Column(name = "cc_address")
    private String ccAddress = null;

    @Column(name = "bcc_address")
    private String bccAddress = null;

    @Column(name = "send_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date sendDate = null;

    @Column(name = "subject")
    private String subject = null;

    @Column(name = "mail_text")
    private String mailText = null;

    @Column(name = "version")
    private Long version;

    public QuotaSentEmailsVO() {
        super();
    }

    @Column(name = "updated_by")
    private Long updatedBy = null;

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return this.id;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getCcAddress() {
        return ccAddress;
    }

    public void setCcAddress(String ccAddress) {
        this.ccAddress = ccAddress;
    }

    public String getBccAddress() {
        return bccAddress;
    }

    public void setBccAddress(String bccAddress) {
        this.bccAddress = bccAddress;
    }

    public Date getSendDate() {
        return sendDate;
    }

    public void setSendDate(Date sendDate) {
        this.sendDate = sendDate;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMailText() {
        return mailText;
    }

    public void setMailText(String mailText) {
        this.mailText = mailText;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
