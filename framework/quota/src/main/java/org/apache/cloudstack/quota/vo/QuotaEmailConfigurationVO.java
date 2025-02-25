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
package org.apache.cloudstack.quota.vo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "quota_email_configuration")
public class QuotaEmailConfigurationVO {

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "email_template_id")
    private long emailTemplateId;

    @Column(name = "enabled")
    private boolean enabled;

    public QuotaEmailConfigurationVO() {
    }

    public QuotaEmailConfigurationVO(long accountId, long emailTemplateId, boolean enable) {
        this.accountId = accountId;
        this.emailTemplateId = emailTemplateId;
        this.enabled = enable;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getEmailTemplateId() {
        return emailTemplateId;
    }

    public void setEmailTemplateId(long emailTemplateId) {
        this.emailTemplateId = emailTemplateId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
