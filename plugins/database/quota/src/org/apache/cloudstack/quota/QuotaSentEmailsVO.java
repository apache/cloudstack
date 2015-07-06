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
package org.apache.cloudstack.quota;

import java.math.BigDecimal;
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
/*
 *
CREATE TABLE `cloud_usage.quota_sent_emails` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `from_address` varchar(1024) NOT NULL,
  `to_address` varchar(1024) NOT NULL,
  `cc_address` varchar(1024) DEFAULT NULL,
  `bcc_address` varchar(1024) DEFAULT NULL,
  `send_date` datetime NOT NULL,
  `subject` varchar(1024) NOT NULL,
  `mail_text` longtext NOT NULL,
  `version` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
 */

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

  @Column(name = "credit")
  private BigDecimal credit;

  @Column(name = "updated_on")
  @Temporal(value = TemporalType.TIMESTAMP)
  private Date updatedOn = null;

  public QuotaSentEmailsVO() {
      super();
  }

  public BigDecimal getCredit() {
      return credit;
  }

  public void setCredit(BigDecimal credit) {
      this.credit = credit;
  }

  public Date getUpdatedOn() {
      return updatedOn;
  }

  public void setUpdatedOn(Date updatedOn) {
      this.updatedOn = updatedOn;
  }

  public Long getUpdatedBy() {
      return updatedBy;
  }

  public void setUpdatedBy(Long updatedBy) {
      this.updatedBy = updatedBy;
  }

  public void setId(Long id) {
      this.id = id;
  }

  @Column(name = "updated_by")
  private Long updatedBy = null;

  @Override
  public long getId() {
      // TODO Auto-generated method stub
      return this.id;
  }
}
