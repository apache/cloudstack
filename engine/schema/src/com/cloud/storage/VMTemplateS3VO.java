/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.storage;

import com.cloud.utils.db.GenericDaoBase;
import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import java.text.DateFormat;
import java.util.Date;

@Entity
@Table(name = "template_s3_ref")
public class VMTemplateS3VO implements InternalIdentity {

    public static final String S3_ID_COLUMN_NAME = "s3_id";

    public static final String TEMPLATE_ID_COLUMN_NAME = "template_id";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = S3_ID_COLUMN_NAME)
    private long s3Id;

    @Column(name = TEMPLATE_ID_COLUMN_NAME)
    private long templateId;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    private Date created;

    @Column(name = "size")
    private Long size;

    @Column(name = "physical_size")
    private Long physicalSize;

    public VMTemplateS3VO() {
        super();
    }

    public VMTemplateS3VO(final long s3Id, final long templateId,
            final Date created, final Long size, final Long physicalSize) {

        super();

        this.s3Id = s3Id;
        this.templateId = templateId;
        this.created = created;
        this.size = size;
        this.physicalSize = physicalSize;

    }

    @Override
    public boolean equals(final Object thatObject) {

        if (this == thatObject) {
            return true;
        }

        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        final VMTemplateS3VO thatVMTemplateS3VO = (VMTemplateS3VO) thatObject;

        if (this.id != thatVMTemplateS3VO.id) {
            return false;
        }

        if (this.s3Id != thatVMTemplateS3VO.s3Id) {
            return false;
        }

        if (this.templateId != thatVMTemplateS3VO.templateId) {
            return false;
        }

        if (this.created != null ? !created.equals(thatVMTemplateS3VO.created)
                : thatVMTemplateS3VO.created != null) {
            return false;
        }

        if (this.physicalSize != null ? !physicalSize
                .equals(thatVMTemplateS3VO.physicalSize)
                : thatVMTemplateS3VO.physicalSize != null) {
            return false;
        }

        if (this.size != null ? !size.equals(thatVMTemplateS3VO.size)
                : thatVMTemplateS3VO.size != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {

        int result = (int) (this.id ^ (this.id >>> 32));

        result = 31 * result + (int) (this.s3Id ^ (this.s3Id >>> 32));
        result = 31 * result
                + (int) (this.templateId ^ (this.templateId >>> 32));
        result = 31 * result
                + (this.created != null ? this.created.hashCode() : 0);
        result = 31 * result + (this.size != null ? this.size.hashCode() : 0);
        result = 31
                * result
                + (this.physicalSize != null ? this.physicalSize.hashCode() : 0);

        return result;

    }

    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public long getS3Id() {
        return this.s3Id;
    }

    public void setS3Id(final long s3Id) {
        this.s3Id = s3Id;
    }

    public long getTemplateId() {
        return this.templateId;
    }

    public void setTemplateId(final long templateId) {
        this.templateId = templateId;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    public Long getSize() {
        return this.size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public Long getPhysicalSize() {
        return this.physicalSize;
    }

    public void setPhysicalSize(final Long physicalSize) {
        this.physicalSize = physicalSize;
    }

    @Override
    public String toString() {

        final StringBuilder stringBuilder = new StringBuilder(
                "VMTemplateS3VO [ id: ").append(id).append(", created: ")
                .append(DateFormat.getDateTimeInstance().format(created))
                .append(", physicalSize: ").append(physicalSize)
                .append(", size: ").append(size).append(", templateId: ")
                .append(templateId).append(", s3Id: ").append(s3Id)
                .append(" ]");

        return stringBuilder.toString();

    }

}
