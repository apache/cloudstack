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

package com.cloud.kubernetes.version;

import org.apache.cloudstack.api.command.admin.kubernetes.version.AddKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetes.version.DeleteKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetes.version.GetUploadParamsForKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.admin.kubernetes.version.UpdateKubernetesSupportedVersionCmd;
import org.apache.cloudstack.api.command.user.kubernetes.version.ListKubernetesSupportedVersionsCmd;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.api.response.KubernetesSupportedVersionResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;

public interface KubernetesVersionService extends PluggableService {
    static final String MIN_KUBERNETES_VERSION = "1.11.0";
    ListResponse<KubernetesSupportedVersionResponse> listKubernetesSupportedVersions(ListKubernetesSupportedVersionsCmd cmd);
    KubernetesSupportedVersionResponse addKubernetesSupportedVersion(AddKubernetesSupportedVersionCmd cmd) throws CloudRuntimeException;
    GetUploadParamsResponse registerKubernetesSupportedVersionForPostUpload(GetUploadParamsForKubernetesSupportedVersionCmd cmd);
    boolean deleteKubernetesSupportedVersion(DeleteKubernetesSupportedVersionCmd cmd) throws CloudRuntimeException;
    KubernetesSupportedVersionResponse updateKubernetesSupportedVersion(UpdateKubernetesSupportedVersionCmd cmd) throws CloudRuntimeException;
}
