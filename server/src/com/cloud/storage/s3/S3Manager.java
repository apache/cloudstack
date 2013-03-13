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
package com.cloud.storage.s3;

import java.util.List;

import com.cloud.agent.api.to.S3TO;
import org.apache.cloudstack.api.command.admin.storage.AddS3Cmd;
import org.apache.cloudstack.api.command.admin.storage.ListS3sCmd;

import com.cloud.dc.DataCenterVO;
import com.cloud.exception.DiscoveryException;
import com.cloud.storage.S3;
import com.cloud.storage.S3VO;
import com.cloud.storage.VMTemplateS3VO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.component.Manager;

public interface S3Manager extends Manager {

    S3TO getS3TO();

    S3TO getS3TO(Long s3Id);

    S3 addS3(AddS3Cmd addS3Cmd) throws DiscoveryException;

    Long chooseZoneForTemplateExtract(VMTemplateVO template);

    boolean isS3Enabled();

    boolean isTemplateInstalled(Long templateId);

    void deleteTemplate(final Long accountId, final Long templateId);

    String downloadTemplateFromS3ToSecondaryStorage(final long dcId,
            final long templateId, final int primaryStorageDownloadWait);

    List<S3VO> listS3s(ListS3sCmd listS3sCmd);

    VMTemplateS3VO findByTemplateId(Long templateId);

    void propagateTemplatesToZone(DataCenterVO zone);

    void propagateTemplateToAllZones(VMTemplateS3VO vmTemplateS3VO);

    void uploadTemplateToS3FromSecondaryStorage(final VMTemplateVO template);

}
