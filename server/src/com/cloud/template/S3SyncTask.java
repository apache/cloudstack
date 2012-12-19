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
package com.cloud.template;

import static java.lang.String.*;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.api.to.S3TO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.s3.S3Manager;

final class S3SyncTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(S3SyncTask.class);

    private final VMTemplateDao vmTemplateDao;
    private final S3Manager s3Mgr;

    S3SyncTask(final VMTemplateDao vmTemplateDao, final S3Manager s3Mgr) {

        super();

        assert vmTemplateDao != null;
        assert s3Mgr != null;

        this.vmTemplateDao = vmTemplateDao;
        this.s3Mgr = s3Mgr;

    }

    @Override
    public void run() {

        try {

            final S3TO s3 = s3Mgr.getS3TO();

            if (s3 == null) {
                LOGGER.warn("S3 sync skipped because no S3 instance is configured.");
                return;
            }

            final List<VMTemplateVO> candidateTemplates = vmTemplateDao
                    .findTemplatesToSyncToS3();

            if (candidateTemplates.isEmpty()) {
                LOGGER.debug("All templates are synced with S3.");
                return;
            }

            for (VMTemplateVO candidateTemplate : candidateTemplates) {

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(format(
                            "Uploading template %1$s (id: %2$s) to S3.",
                            candidateTemplate.getName(),
                            candidateTemplate.getId()));
                }

                s3Mgr.uploadTemplateToS3FromSecondaryStorage(candidateTemplate);

            }

            LOGGER.debug("Completed S3 template sync task.");

        } catch (Exception e) {
            LOGGER.warn(
                    "S3 Sync Task ignored exception, and will continue to execute.",
                    e);
        }

    }

}
