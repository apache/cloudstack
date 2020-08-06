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

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.GetUploadParamsForIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ExtractTemplateCmd;
import org.apache.cloudstack.api.command.user.template.GetUploadParamsForTemplateCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;
import org.apache.cloudstack.storage.command.TemplateOrVolumePostUploadCommand;

public interface TemplateAdapter extends Adapter {
    public static class TemplateAdapterType {
        String _name;

        public static final TemplateAdapterType Hypervisor = new TemplateAdapterType("HypervisorAdapter");
        public static final TemplateAdapterType BareMetal = new TemplateAdapterType("BareMetalAdapter");

        public TemplateAdapterType(String name) {
            _name = name;
        }

        public String getName() {
            return _name;
        }
    }

    TemplateProfile prepare(RegisterTemplateCmd cmd) throws ResourceAllocationException;

    TemplateProfile prepare(GetUploadParamsForTemplateCmd cmd) throws ResourceAllocationException;

    TemplateProfile prepare(RegisterIsoCmd cmd) throws ResourceAllocationException;

    TemplateProfile prepare(GetUploadParamsForIsoCmd cmd) throws ResourceAllocationException;

    VMTemplateVO create(TemplateProfile profile);

    List<TemplateOrVolumePostUploadCommand> createTemplateForPostUpload(TemplateProfile profile);

    TemplateProfile prepareDelete(DeleteTemplateCmd cmd);

    TemplateProfile prepareDelete(DeleteIsoCmd cmd);

    TemplateProfile prepareExtractTemplate(ExtractTemplateCmd cmd);

    boolean delete(TemplateProfile profile);

    TemplateProfile prepare(boolean isIso, Long userId, String name, String displayText, Integer bits, Boolean passwordEnabled, Boolean requiresHVM, String url, Boolean isPublic,
                            Boolean featured, Boolean isExtractable, String format, Long guestOSId, List<Long> zoneId, HypervisorType hypervisorType, String accountName, Long domainId, String chksum, Boolean bootable, Map details, boolean directDownload,
                            boolean deployAsIs) throws ResourceAllocationException;

    TemplateProfile prepare(boolean isIso, long userId, String name, String displayText, Integer bits, Boolean passwordEnabled, Boolean requiresHVM, String url, Boolean isPublic,
                            Boolean featured, Boolean isExtractable, String format, Long guestOSId, List<Long> zoneId, HypervisorType hypervisorType, String chksum, Boolean bootable, String templateTag, Account templateOwner, Map details, Boolean sshKeyEnabled, String imageStoreUuid, Boolean isDynamicallyScalable,
                            TemplateType templateType, boolean directDownload, boolean deployAsIs) throws ResourceAllocationException;

}
