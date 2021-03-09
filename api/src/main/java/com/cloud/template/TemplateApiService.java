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
package com.cloud.template;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.cloudstack.api.BaseListTemplateOrIsoPermissionsCmd;
import org.apache.cloudstack.api.BaseUpdateTemplateOrIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.ExtractIsoCmd;
import org.apache.cloudstack.api.command.user.iso.GetUploadParamsForIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.iso.UpdateIsoCmd;
import org.apache.cloudstack.api.command.user.template.CopyTemplateCmd;
import org.apache.cloudstack.api.command.user.template.CreateTemplateCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ExtractTemplateCmd;
import org.apache.cloudstack.api.command.user.template.GetUploadParamsForTemplateCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateTemplateCmd;

import com.cloud.exception.InternalErrorException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;

public interface TemplateApiService {

    VirtualMachineTemplate registerTemplate(RegisterTemplateCmd cmd) throws URISyntaxException, ResourceAllocationException;

    GetUploadParamsResponse registerTemplateForPostUpload(GetUploadParamsForTemplateCmd cmd) throws ResourceAllocationException, MalformedURLException;

    VirtualMachineTemplate registerIso(RegisterIsoCmd cmd) throws IllegalArgumentException, ResourceAllocationException;

    GetUploadParamsResponse registerIsoForPostUpload(GetUploadParamsForIsoCmd cmd) throws ResourceAllocationException, MalformedURLException;

    VirtualMachineTemplate copyTemplate(CopyTemplateCmd cmd) throws StorageUnavailableException, ResourceAllocationException;

    VirtualMachineTemplate prepareTemplate(long templateId, long zoneId, Long storageId);

    boolean detachIso(long vmId, boolean forced);

    boolean attachIso(long isoId, long vmId, boolean forced);

    /**
     * Deletes a template
     *
     * @param cmd
     *            - the command specifying templateId
     */
    boolean deleteTemplate(DeleteTemplateCmd cmd);

    /**
     * Deletes a template
     *
     * @param cmd
     *            - the command specifying isoId
     * @return true if deletion is successful, false otherwise
     */
    boolean deleteIso(DeleteIsoCmd cmd);

    /**
     * Extracts an ISO
     *
     * @param cmd
     *            - the command specifying the mode and id of the ISO
     * @return extractUrl extract url.
     */
    String extract(ExtractIsoCmd cmd) throws InternalErrorException;

    /**
     * Extracts a Template
     *
     * @param cmd
     *            - the command specifying the mode and id of the template
     * @return extractUrl  extract url
     */
    String extract(ExtractTemplateCmd cmd) throws InternalErrorException;

    List<String> listTemplatePermissions(BaseListTemplateOrIsoPermissionsCmd cmd);

    boolean updateTemplateOrIsoPermissions(BaseUpdateTemplateOrIsoPermissionsCmd cmd);

    VirtualMachineTemplate createPrivateTemplateRecord(CreateTemplateCmd cmd, Account templateOwner) throws ResourceAllocationException;

    VirtualMachineTemplate createPrivateTemplate(CreateTemplateCmd command) throws CloudRuntimeException;

    VirtualMachineTemplate updateTemplate(UpdateIsoCmd cmd);

    VirtualMachineTemplate updateTemplate(UpdateTemplateCmd cmd);
}
