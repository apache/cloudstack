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
package org.apache.cloudstack.saml;

import com.cloud.configuration.Config;
import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.command.SAML2LoginAPIAuthenticatorCmd;
import org.apache.cloudstack.api.command.SAML2LogoutAPIAuthenticatorCmd;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.log4j.Logger;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.BasicParserPool;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
@Local(value = {PluggableAPIAuthenticator.class, SAML2AuthManager.class})
public class SAML2AuthManagerImpl extends AdapterBase implements PluggableAPIAuthenticator, SAML2AuthManager {
    private static final Logger s_logger = Logger.getLogger(SAML2AuthManagerImpl.class);

    private String serviceProviderId;
    private String spSingleSignOnUrl;
    private String spSingleLogOutUrl;

    private String idpSingleSignOnUrl;
    private String idpSingleLogOutUrl;

    @Inject
    ConfigurationDao _configDao;

    protected SAML2AuthManagerImpl() {
        super();
    }

    @Override
    public boolean start() {
        this.serviceProviderId = _configDao.getValue(Config.SAMLServiceProviderID.key());
        this.spSingleSignOnUrl = _configDao.getValue(Config.SAMLServiceProviderSingleSignOnURL.key());
        this.spSingleLogOutUrl = _configDao.getValue(Config.SAMLServiceProviderSingleLogOutURL.key());

        String idpMetaDataUrl = _configDao.getValue(Config.SAMLIdentityProviderMetadataURL.key());

        int tolerance = 30000;
        String timeout = _configDao.getValue(Config.SAMLTimeout.key());
        if (timeout != null) {
            tolerance = Integer.parseInt(timeout);
        }

        try {
            HTTPMetadataProvider idpMetaDataProvider = new HTTPMetadataProvider(idpMetaDataUrl, tolerance);

            idpMetaDataProvider.setRequireValidMetadata(true);
            idpMetaDataProvider.setParserPool(new BasicParserPool());
            idpMetaDataProvider.initialize();

            EntityDescriptor idpEntityDescriptor = idpMetaDataProvider.getEntityDescriptor("Some entity id");
            for (SingleSignOnService ssos: idpEntityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleSignOnServices()) {
                if (ssos.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI)) {
                    this.idpSingleSignOnUrl = ssos.getLocation();
                }
            }
            for (SingleLogoutService slos: idpEntityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getSingleLogoutServices()) {
                if (slos.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI)) {
                    this.idpSingleLogOutUrl = slos.getLocation();
                }
            }

        } catch (MetadataProviderException e) {
            s_logger.error("Unable to read SAML2 IDP MetaData URL, error:" + e.getMessage());
            s_logger.error("SAML2 Authentication may be unavailable");
        }

        if (this.idpSingleLogOutUrl == null || this.idpSingleSignOnUrl == null) {
            s_logger.error("The current IDP does not support HTTP redirected authentication, SAML based authentication cannot work with this IDP");
        }

        return true;
    }

    @Override
    public List<Class<?>> getAuthCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(SAML2LoginAPIAuthenticatorCmd.class);
        cmdList.add(SAML2LogoutAPIAuthenticatorCmd.class);
        return cmdList;
    }

    public String getServiceProviderId() {
        return serviceProviderId;
    }

    public String getIdpSingleSignOnUrl() {
        return this.idpSingleSignOnUrl;
    }

    public String getIdpSingleLogOutUrl() {
        return this.idpSingleLogOutUrl;
    }

    public String getSpSingleSignOnUrl() {
        return spSingleSignOnUrl;
    }

    public String getSpSingleLogOutUrl() {
        return spSingleLogOutUrl;
    }
}
