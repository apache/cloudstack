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

import static com.cloud.storage.S3VO.ID_COLUMN_NAME;
import static com.cloud.utils.DateUtil.now;
import static com.cloud.utils.S3Utils.canConnect;
import static com.cloud.utils.S3Utils.canReadWriteBucket;
import static com.cloud.utils.S3Utils.checkBucketName;
import static com.cloud.utils.S3Utils.checkClientOptions;
import static com.cloud.utils.S3Utils.doesBucketExist;
import static com.cloud.utils.StringUtils.join;
import static com.cloud.utils.db.GlobalLock.executeWithNoWaitLock;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.shuffle;
import static java.util.Collections.singletonList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.storage.AddS3Cmd;
import org.apache.cloudstack.api.command.admin.storage.ListS3sCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteTemplateFromS3Command;
import com.cloud.agent.api.DownloadTemplateFromS3ToSecondaryStorageCommand;
import com.cloud.agent.api.UploadTemplateToS3FromSecondaryStorageCommand;
import com.cloud.agent.api.to.S3TO;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.S3;
import com.cloud.storage.S3VO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateS3VO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.S3Dao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateS3Dao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.S3Utils.ClientOptions;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = { S3Manager.class })
public class S3ManagerImpl extends ManagerBase implements S3Manager {

    private static final Logger LOGGER = Logger.getLogger(S3ManagerImpl.class);

    @Inject 
    private AgentManager agentManager;

    @Inject
    private S3Dao s3Dao;

    @Inject
    private VMTemplateZoneDao vmTemplateZoneDao;

    @Inject
    private VMTemplateS3Dao vmTemplateS3Dao;

    @Inject
    private VMTemplateHostDao vmTemplateHostDao;

    @Inject
    private VMTemplateDao vmTemplateDao;

    @Inject
    private ConfigurationDao configurationDao;

    @Inject
    private DataCenterDao dataCenterDao;

    @Inject
    private HostDao hostDao;

    @Inject
    private SecondaryStorageVmManager secondaryStorageVMManager;

    public S3ManagerImpl() {
    }
    
    private void verifyConnection(final S3TO s3) throws DiscoveryException {

        if (!canConnect(s3)) {
            throw new DiscoveryException(format("Unable to connect to S3 "
                    + "using access key %1$s, secret key %2$s, and endpoint, "
                    + "%3$S", s3.getAccessKey(), s3.getSecretKey(),
                    s3.getEndPoint() != null ? s3.getEndPoint() : "default"));
        }

    }

    private void verifyBuckets(S3TO s3) throws DiscoveryException {

        final List<String> errorMessages = new ArrayList<String>();

        errorMessages.addAll(verifyBucket(s3, s3.getBucketName()));

        throwDiscoveryExceptionFromErrorMessages(errorMessages);

    }

    private List<String> verifyBucket(final ClientOptions clientOptions,
            final String bucketName) {

        if (!doesBucketExist(clientOptions, bucketName)) {
            return singletonList(format("Bucket %1$s does not exist.",
                    bucketName));
        }

        if (!canReadWriteBucket(clientOptions, bucketName)) {
            return singletonList(format("Can read/write from bucket %1$s.",
                    bucketName));
        }

        return emptyList();
    }

    private void validateFields(final S3VO s3VO) {

        final List<String> errorMessages = new ArrayList<String>();

        errorMessages.addAll(checkClientOptions(s3VO.toS3TO()));

        errorMessages.addAll(checkBucketName("template", s3VO.getBucketName()));

        throwDiscoveryExceptionFromErrorMessages(errorMessages);

    }

    private void enforceS3PreConditions() throws DiscoveryException {

        if (!this.isS3Enabled()) {
            throw new DiscoveryException("S3 is not enabled.");
        }

        if (this.getS3TO() != null) {
            throw new DiscoveryException("Attempt to define multiple S3 "
                    + "instances.  Only one instance definition is supported.");
        }

    }

    private void throwDiscoveryExceptionFromErrorMessages(
            final List<String> errorMessages) {

        if (!errorMessages.isEmpty()) {
            throw new CloudRuntimeException(join(errorMessages, " "));
        }

    }

    @SuppressWarnings("unchecked")
    private String determineLockId(final long accountId, final long templateId) {

        // TBD The lock scope may be too coarse grained. Deletes need to lock
        // the template across all zones where upload and download could
        // probably safely scoped to the zone ...
        return join(asList("S3_TEMPLATE", accountId, templateId), "_");

    }

    @Override
    public S3TO getS3TO(final Long s3Id) {
        return this.s3Dao.getS3TO(s3Id);
    }

    @Override
    public S3TO getS3TO() {

        final List<S3VO> s3s = this.s3Dao.listAll();

        if (s3s == null || (s3s != null && s3s.isEmpty())) {
            return null;
        }

        if (s3s.size() == 1) {
            return s3s.get(0).toS3TO();
        }

        throw new CloudRuntimeException("Multiple S3 instances have been "
                + "defined.  Only one instance configuration is supported.");

    }

    @Override
    public S3 addS3(final AddS3Cmd addS3Cmd) throws DiscoveryException {

        this.enforceS3PreConditions();

        final S3VO s3VO = new S3VO(UUID.randomUUID().toString(),
                addS3Cmd.getAccessKey(), addS3Cmd.getSecretKey(),
                addS3Cmd.getEndPoint(), addS3Cmd.getBucketName(),
                addS3Cmd.getHttpsFlag(), addS3Cmd.getConnectionTimeout(),
                addS3Cmd.getMaxErrorRetry(), addS3Cmd.getSocketTimeout(), now());

        this.validateFields(s3VO);

        final S3TO s3 = s3VO.toS3TO();
        this.verifyConnection(s3);
        this.verifyBuckets(s3);

        return this.s3Dao.persist(s3VO);

    }

    @Override
    public boolean isS3Enabled() {
        return Boolean
                .valueOf(configurationDao.getValue(Config.S3Enable.key()));
    }

    @Override
    public boolean isTemplateInstalled(final Long templateId) {
        throw new UnsupportedOperationException(
                "S3Manager#isTemplateInstalled (DeleteIsoCmd) has not yet "
                        + "been implemented");
    }

    @Override
    public void deleteTemplate(final Long templateId, final Long accountId) {

        final S3TO s3 = getS3TO();

        if (s3 == null) {
            final String errorMessage = "Delete Template Failed: No S3 configuration defined.";
            LOGGER.error(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }

        final VMTemplateS3VO vmTemplateS3VO = vmTemplateS3Dao
                .findOneByS3Template(s3.getId(), templateId);
        if (vmTemplateS3VO == null) {
            final String errorMessage = format(
                    "Delete Template Failed: Unable to find Template %1$s in S3.",
                    templateId);
            LOGGER.error(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }

        try {

            executeWithNoWaitLock(determineLockId(accountId, templateId),
                    new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    final Answer answer = agentManager.sendToSSVM(null,
                            new DeleteTemplateFromS3Command(s3,
                                    accountId, templateId));
                    if (answer == null || !answer.getResult()) {
                        final String errorMessage = format(
                                "Delete Template Failed: Unable to delete template id %1$s from S3 due to following error: %2$s",
                                templateId,
                                ((answer == null) ? "answer is null"
                                        : answer.getDetails()));
                        LOGGER.error(errorMessage);
                        throw new CloudRuntimeException(errorMessage);
                    }

                    vmTemplateS3Dao.remove(vmTemplateS3VO.getId());
                    LOGGER.debug(format(
                            "Deleted template %1$s from S3.",
                            templateId));

                    return null;

                }

            });

        } catch (Exception e) {

            final String errorMessage = format(
                    "Delete Template Failed: Unable to delete template id %1$s from S3 due to the following error: %2$s.",
                    templateId, e.getMessage());
            LOGGER.error(errorMessage);
            throw new CloudRuntimeException(errorMessage, e);

        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public String downloadTemplateFromS3ToSecondaryStorage(
            final long dataCenterId, final long templateId,
            final int primaryStorageDownloadWait) {

        if (!isS3Enabled()) {
            return null;
        }

        final VMTemplateVO template = vmTemplateDao.findById(templateId);
        if (template == null) {
            final String errorMessage = String
                    .format("Failed to download template id %1$s from S3 because the template definition was not found.",
                            templateId);
            LOGGER.error(errorMessage);
            return errorMessage;
        }

        final VMTemplateS3VO templateS3VO = findByTemplateId(templateId);
        if (templateS3VO == null) {
            final String errorMessage = format(
                    "Failed to download template id %1$s from S3 because it does not exist in S3.",
                    templateId);
            LOGGER.error(errorMessage);
            return errorMessage;
        }

        final S3TO s3 = getS3TO(templateS3VO.getS3Id());
        if (s3 == null) {
            final String errorMessage = format(
                    "Failed to download template id %1$s from S3 because S3 id %2$s does not exist.",
                    templateId, templateS3VO);
            LOGGER.error(errorMessage);
            return errorMessage;
        }

        final HostVO secondaryStorageHost = secondaryStorageVMManager
                .findSecondaryStorageHost(dataCenterId);
        if (secondaryStorageHost == null) {
            final String errorMessage = format(
                    "Unable to find secondary storage host for zone id %1$s.",
                    dataCenterId);
            LOGGER.error(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }

        final long accountId = template.getAccountId();
        final DownloadTemplateFromS3ToSecondaryStorageCommand cmd = new DownloadTemplateFromS3ToSecondaryStorageCommand(
                s3, accountId, templateId, secondaryStorageHost.getName(),
                primaryStorageDownloadWait);

        try {

            executeWithNoWaitLock(determineLockId(accountId, templateId),
                    new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    final Answer answer = agentManager.sendToSSVM(
                            dataCenterId, cmd);

                    if (answer == null || !answer.getResult()) {
                        final String errMsg = String
                                .format("Failed to download template from S3 to secondary storage due to %1$s",
                                        (answer == null ? "answer is null"
                                                : answer.getDetails()));
                        LOGGER.error(errMsg);
                        throw new CloudRuntimeException(errMsg);
                    }

                    final String installPath = join(
                            asList("template", "tmpl", accountId,
                                    templateId), File.separator);
                    final VMTemplateHostVO tmpltHost = new VMTemplateHostVO(
                            secondaryStorageHost.getId(), templateId,
                            now(), 100, Status.DOWNLOADED, null, null,
                            null, installPath, template.getUrl());
                    tmpltHost.setSize(templateS3VO.getSize());
                    tmpltHost.setPhysicalSize(templateS3VO
                            .getPhysicalSize());
                    vmTemplateHostDao.persist(tmpltHost);

                    return null;

                }

            });

        } catch (Exception e) {
            final String errMsg = "Failed to download template from S3 to secondary storage due to "
                    + e.toString();
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        return null;

    }

    @Override
    public List<S3VO> listS3s(final ListS3sCmd cmd) {

        final Filter filter = new Filter(S3VO.class, ID_COLUMN_NAME, TRUE,
                cmd.getStartIndex(), cmd.getPageSizeVal());
        final SearchCriteria<S3VO> criteria = this.s3Dao.createSearchCriteria();

        return this.s3Dao.search(criteria, filter);

    }

    @Override
    public VMTemplateS3VO findByTemplateId(final Long templateId) {
        throw new UnsupportedOperationException(
                "S3Manager#findByTemplateId(Long) has not yet "
                        + "been implemented");
    }

    @Override
    public void propagateTemplatesToZone(final DataCenterVO zone) {

        if (!isS3Enabled()) {
            return;
        }

        final List<VMTemplateS3VO> s3VMTemplateRefs = this.vmTemplateS3Dao
                .listAll();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format("Propagating %1$s templates to zone %2$s.",
                    s3VMTemplateRefs.size(), zone.getName()));
        }

        for (final VMTemplateS3VO templateS3VO : s3VMTemplateRefs) {
            this.vmTemplateZoneDao.persist(new VMTemplateZoneVO(zone.getId(),
                    templateS3VO.getTemplateId(), now()));
        }

    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params)
            throws ConfigurationException {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format("Configuring S3 Manager %1$s", name));
        }

        return true;
    }

    @Override
    public boolean start() {
        LOGGER.info("Starting S3 Manager");
        return true;
    }

    @Override
    public boolean stop() {
        LOGGER.info("Stopping S3 Manager");
        return true;
    }

    @Override
    public void propagateTemplateToAllZones(final VMTemplateS3VO vmTemplateS3VO) {

        final long templateId = vmTemplateS3VO.getId();

        if (!isS3Enabled()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(format(
                        "Attempt to propogate template id %1$s across all zones.  However, S3 is not enabled.",
                        templateId));
            }
            return;

        }

        final S3TO s3 = getS3TO();

        if (s3 == null) {
            LOGGER.warn(format(
                    "Unable to propagate template id %1$s across all zones because S3 is enabled, but not configured.",
                    templateId));
            return;
        }

        if (vmTemplateS3VO != null) {
            final List<DataCenterVO> dataCenters = dataCenterDao.listAll();
            for (DataCenterVO dataCenter : dataCenters) {
                final VMTemplateZoneVO tmpltZoneVO = new VMTemplateZoneVO(
                        dataCenter.getId(), templateId, now());
                vmTemplateZoneDao.persist(tmpltZoneVO);
            }
        }

    }

    @Override
    public Long chooseZoneForTemplateExtract(VMTemplateVO template) {

        final S3TO s3 = getS3TO();

        if (s3 == null) {
            return null;
        }

        final List<VMTemplateHostVO> templateHosts = vmTemplateHostDao
                .listByOnlyTemplateId(template.getId());
        if (templateHosts != null) {
            shuffle(templateHosts);
            for (VMTemplateHostVO vmTemplateHostVO : templateHosts) {
                final HostVO host = hostDao.findById(vmTemplateHostVO
                        .getHostId());
                if (host != null) {
                    return host.getDataCenterId();
                }
                throw new CloudRuntimeException(
                        format("Unable to find secondary storage host for template id %1$s.",
                                template.getId()));
            }
        }

        final List<DataCenterVO> dataCenters = dataCenterDao.listAll();
        shuffle(dataCenters);
        return dataCenters.get(0).getId();

    }

    @Override
    public void uploadTemplateToS3FromSecondaryStorage(
            final VMTemplateVO template) {

        final Long templateId = template.getId();

        final List<VMTemplateHostVO> templateHostRefs = vmTemplateHostDao
                .listByTemplateId(templateId);

        if (templateHostRefs == null
                || (templateHostRefs != null && templateHostRefs.isEmpty())) {
            throw new CloudRuntimeException(
                    format("Attempt to sync template id %1$s that is not attached to a host.",
                            templateId));
        }

        final VMTemplateHostVO templateHostRef = templateHostRefs.get(0);

        if (!isS3Enabled()) {
            return;
        }

        final S3TO s3 = getS3TO();
        if (s3 == null) {
            LOGGER.warn("S3 Template Sync Failed: Attempt to sync templates with S3, but no S3 instance defined.");
            return;
        }

        final HostVO secondaryHost = this.hostDao.findById(templateHostRef
                .getHostId());
        if (secondaryHost == null) {
            throw new CloudRuntimeException(format(
                    "Unable to find secondary storage host id %1$s.",
                    templateHostRef.getHostId()));
        }

        final Long dataCenterId = secondaryHost.getDataCenterId();
        final Long accountId = template.getAccountId();

        try {

            executeWithNoWaitLock(determineLockId(accountId, templateId),
                    new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    final UploadTemplateToS3FromSecondaryStorageCommand cmd = new UploadTemplateToS3FromSecondaryStorageCommand(
                            s3, secondaryHost.getStorageUrl(),
                            dataCenterId, accountId, templateId);

                    final Answer answer = agentManager.sendToSSVM(
                            dataCenterId, cmd);
                    if (answer == null || !answer.getResult()) {

                        final String reason = answer != null ? answer
                                .getDetails()
                                : "S3 template sync failed due to an unspecified error.";
                                throw new CloudRuntimeException(
                                        format("Failed to upload template id %1$s to S3 from secondary storage due to %2$s.",
                                                templateId, reason));

                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(format(
                                "Creating VMTemplateS3VO instance using template id %1s.",
                                templateId));
                    }

                    final VMTemplateS3VO vmTemplateS3VO = new VMTemplateS3VO(
                            s3.getId(), templateId, now(),
                            templateHostRef.getSize(), templateHostRef
                            .getPhysicalSize());

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(format("Persisting %1$s",
                                vmTemplateS3VO));
                    }

                    vmTemplateS3Dao.persist(vmTemplateS3VO);
                    propagateTemplateToAllZones(vmTemplateS3VO);

                    return null;

                }

            });

        } catch (Exception e) {

            final String errorMessage = format(
                    "Failed to upload template id %1$s for zone id %2$s to S3.",
                    templateId, dataCenterId);
            LOGGER.error(errorMessage, e);

        }

    }

}
