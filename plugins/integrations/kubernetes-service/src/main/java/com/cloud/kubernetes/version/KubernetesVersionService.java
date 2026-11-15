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
    // Detail of a Kubernetes ISO which records whether the ISO has the etcd binaries needed for dedicated etcd nodes
    static final String ETCD_INCLUDED_ISO_DETAIL = "cks.etcd.included";
    static final String ETCD_ARCHIVE_PATH_IN_ISO = "etcd/etcd-linux-amd64.tar.gz";

    /**
     * Tells whether the ISO of a Kubernetes version has the etcd binaries needed for dedicated etcd nodes. The ISO
     * is checked on the first call and the result is stored with the ISO.
     *
     * @return null if it is not known because the ISO could not be checked
     */
    Boolean isEtcdIncludedInIso(KubernetesSupportedVersion kubernetesSupportedVersion);

    ListResponse<KubernetesSupportedVersionResponse> listKubernetesSupportedVersions(ListKubernetesSupportedVersionsCmd cmd);
    KubernetesSupportedVersionResponse addKubernetesSupportedVersion(AddKubernetesSupportedVersionCmd cmd) throws CloudRuntimeException;
    GetUploadParamsResponse registerKubernetesSupportedVersionForPostUpload(GetUploadParamsForKubernetesSupportedVersionCmd cmd);
    boolean deleteKubernetesSupportedVersion(DeleteKubernetesSupportedVersionCmd cmd) throws CloudRuntimeException;
    KubernetesSupportedVersionResponse updateKubernetesSupportedVersion(UpdateKubernetesSupportedVersionCmd cmd) throws CloudRuntimeException;
}
