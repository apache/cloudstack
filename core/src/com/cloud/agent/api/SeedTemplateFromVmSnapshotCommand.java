/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.agent.api;

public class SeedTemplateFromVmSnapshotCommand extends Command {

    private String templateName;
    private String dsName;
    private String dsUuid;
    private String srcVolumePath;
    private String srcVmSnapshotName;
    private String srcVmSnapshotUuid;
    private String vmName;

    public SeedTemplateFromVmSnapshotCommand(String templateName, String dsName, String dsUuid, String srcVolumePath,
            String srcVmSnapshotName, String srcVmSnapshotUuid, String vmName) {
        super();
        this.templateName = templateName;
        this.dsName = dsName;
        this.dsUuid = dsUuid;
        this.srcVolumePath = srcVolumePath;
        this.srcVmSnapshotName = srcVmSnapshotName;
        this.srcVmSnapshotUuid = srcVmSnapshotUuid;
        this.vmName = vmName;
    }

    public SeedTemplateFromVmSnapshotCommand() {

    }

    public String getDsName() {
        return dsName;
    }

    public void setDsName(String dsName) {
        this.dsName = dsName;
    }

    public String getDsUuid() {
        return dsUuid;
    }

    public void setDsUuid(String dsUuid) {
        this.dsUuid = dsUuid;
    }

    public String getSrcVolumePath() {
        return srcVolumePath;
    }

    public void setSrcVolumePath(String srcVolumePath) {
        this.srcVolumePath = srcVolumePath;
    }

    public String getSrcVmSnapshotName() {
        return srcVmSnapshotName;
    }

    public void setSrcVmSnapshotName(String srcVmSnapshotName) {
        this.srcVmSnapshotName = srcVmSnapshotName;
    }

    public String getSrcVmSnapshotUuid() {
        return srcVmSnapshotUuid;
    }

    public void setSrcVmSnapshotUuid(String srcVmSnapshotUuid) {
        this.srcVmSnapshotUuid = srcVmSnapshotUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

}
