//
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
//

package com.cloud.util;

import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.StringUtils;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.commons.codec.binary.Base64;

public class NuageVspUtil {

    public static String getPreConfiguredDomainTemplateName(ConfigurationDao configDao, NetworkDetailsDao networkDetailsDao, Network network, NetworkOffering networkOffering) {
        NetworkDetailVO domainTemplateNetworkDetail = networkDetailsDao.findDetail(network.getId(), NuageVspManager.nuageDomainTemplateDetailName);
        if (domainTemplateNetworkDetail != null) {
            return domainTemplateNetworkDetail.getValue();
        }

        String configKey;
        if (network.getVpcId() != null) {
            configKey = NuageVspManager.NuageVspVpcDomainTemplateName.key();
        } else if (networkOffering.getGuestType() == Network.GuestType.Shared) {
            configKey = NuageVspManager.NuageVspSharedNetworkDomainTemplateName.key();
        } else {
            configKey = NuageVspManager.NuageVspIsolatedNetworkDomainTemplateName.key();
        }
        return configDao.getValue(configKey);
    }

    public static String encodePassword(String originalPassword) {
        byte[] passwordBytes = originalPassword.getBytes(StringUtils.getPreferredCharset());
        byte[] encodedPasswordBytes = Base64.encodeBase64(passwordBytes);
        return new String(encodedPasswordBytes, StringUtils.getPreferredCharset());
    }

    public static String decodePassword(String encodedPassword) {
        byte[] encodedPasswordBytes = encodedPassword.getBytes(StringUtils.getPreferredCharset());
        byte[] passwordBytes = Base64.decodeBase64(encodedPasswordBytes);
        return new String(passwordBytes, StringUtils.getPreferredCharset());
    }
}
