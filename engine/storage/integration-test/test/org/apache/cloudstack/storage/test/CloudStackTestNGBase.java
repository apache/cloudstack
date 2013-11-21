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
package org.apache.cloudstack.storage.test;

import java.lang.reflect.Method;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.TransactionLegacy;

public class CloudStackTestNGBase extends AbstractTestNGSpringContextTests {
    private String hostGateway;
    private String hostCidr;
    private String hostIp;
    private String hostGuid;
    private String templateUrl;
    private String localStorageUuid;
    private String primaryStorageUrl;
    private String secondaryStorage;
    private String imageInstallPath;
    private String scriptPath;
    private HypervisorType hypervisor;
    private TransactionLegacy txn;

    private String s3AccessKey;
    private String s3SecretKey;
    private String s3EndPoint;
    private String s3TemplateBucket;
    private String primaryStorageUuid;
    private boolean s3UseHttps;

    protected void injectMockito() {

    }

    @BeforeMethod(alwaysRun = true)
    protected void injectDB(Method testMethod) throws Exception {
        txn = TransactionLegacy.open(testMethod.getName());
    }

    @Test
    protected void injectMockitoTest() {
        injectMockito();
    }

    @AfterMethod(alwaysRun = true)
    protected void closeDB(Method testMethod) throws Exception {
        if (txn != null) {
            txn.close();
        }
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters({"devcloud-host-uuid", "devcloud-host-gateway", "devcloud-host-cidr", "devcloud-host-ip", "template-url", "devcloud-local-storage-uuid",
        "primary-storage-want-to-add", "devcloud-secondary-storage", "s3-accesskey", "s3-secretkey", "s3-endpoint", "s3-template-bucket", "s3-usehttps",
        "image-install-path", "primary-storage-uuid-want-to-add", "script-path", "hypervisor"})
    protected void setup(String hostuuid, String gateway, String cidr, String hostIp, String templateUrl, String localStorageUuid, String primaryStorage,
        String secondaryStorage, String s3_accessKey, String s3_secretKey, String s3_endpoint, String s3_template_bucket, String s3_usehttps, String imageInstallPath,
        String primaryStorageUuid, String scriptPath, String hypervisor) {
        this.hostGuid = hostuuid;
        this.hostGateway = gateway;
        this.hostCidr = cidr;
        this.hostIp = hostIp;
        this.templateUrl = templateUrl;
        this.localStorageUuid = localStorageUuid;
        this.primaryStorageUrl = primaryStorage;
        this.primaryStorageUuid = primaryStorageUuid;
        this.imageInstallPath = imageInstallPath;
        this.hypervisor = HypervisorType.getType(hypervisor);
        this.setSecondaryStorage(secondaryStorage);
        // set S3 parameters
        this.s3AccessKey = s3_accessKey;
        this.s3SecretKey = s3_secretKey;
        this.s3EndPoint = s3_endpoint;
        this.s3TemplateBucket = s3_template_bucket;
        this.s3UseHttps = Boolean.parseBoolean(s3_usehttps);
        this.scriptPath = scriptPath;
        if (this.scriptPath != null) {
            System.setProperty("paths.script", this.getScriptPath());
        }
    }

    protected String getHostGuid() {
        return this.hostGuid;
    }

    protected String getHostGateway() {
        return this.hostGateway;
    }

    protected String getHostCidr() {
        return this.hostCidr;
    }

    protected String getHostIp() {
        return this.hostIp;
    }

    protected String getTemplateUrl() {
        return this.templateUrl;
    }

    protected String getLocalStorageUuid() {
        return this.localStorageUuid;
    }

    protected String getPrimaryStorageUrl() {
        return this.primaryStorageUrl;
    }

    public String getSecondaryStorage() {
        return secondaryStorage;
    }

    public void setSecondaryStorage(String secondaryStorage) {
        this.secondaryStorage = secondaryStorage;
    }

    public String getS3AccessKey() {
        return s3AccessKey;
    }

    public String getS3SecretKey() {
        return s3SecretKey;
    }

    public String getS3EndPoint() {
        return s3EndPoint;
    }

    public String getS3TemplateBucket() {
        return s3TemplateBucket;
    }

    public boolean isS3UseHttps() {
        return s3UseHttps;
    }

    public String getImageInstallPath() {
        return imageInstallPath;
    }

    public void setImageInstallPath(String imageInstallPath) {
        this.imageInstallPath = imageInstallPath;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public HypervisorType getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(HypervisorType hypervisor) {
        this.hypervisor = hypervisor;
    }

}
