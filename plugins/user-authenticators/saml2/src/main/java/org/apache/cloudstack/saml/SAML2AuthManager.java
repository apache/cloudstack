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

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.Collection;

public interface SAML2AuthManager extends PluggableAPIAuthenticator, PluggableService {

    public static final ConfigKey<Boolean> SAMLIsPluginEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "saml2.enabled", "false",
            "Indicates whether SAML SSO plugin is enabled or not", true);

    public static final ConfigKey<String> SAMLServiceProviderID = new ConfigKey<String>("Advanced", String.class, "saml2.sp.id", "org.apache.cloudstack",
            "SAML2 Service Provider Identifier String", true);

    public static final ConfigKey<String> SAMLServiceProviderContactPersonName = new ConfigKey<String>("Advanced", String.class, "saml2.sp.contact.person", "CloudStack Developers",
            "SAML2 Service Provider Contact Person Name", true);

    public static final ConfigKey<String> SAMLServiceProviderContactEmail = new ConfigKey<String>("Advanced", String.class, "saml2.sp.contact.email", "dev@cloudstack.apache.org",
            "SAML2 Service Provider Contact Email Address", true);

    public static final ConfigKey<String> SAMLServiceProviderOrgName = new ConfigKey<String>("Advanced", String.class, "saml2.sp.org.name", "Apache CloudStack",
            "SAML2 Service Provider Organization Name", true);

    public static final ConfigKey<String> SAMLServiceProviderOrgUrl = new ConfigKey<String>("Advanced", String.class, "saml2.sp.org.url", "http://cloudstack.apache.org",
            "SAML2 Service Provider Organization URL", true);

    public static final ConfigKey<String> SAMLServiceProviderSingleSignOnURL = new ConfigKey<String>("Advanced", String.class, "saml2.sp.sso.url", "http://localhost:8080/client/api?command=samlSso",
            "SAML2 CloudStack Service Provider Single Sign On URL", true);

    public static final ConfigKey<String> SAMLServiceProviderSingleLogOutURL = new ConfigKey<String>("Advanced", String.class, "saml2.sp.slo.url", "http://localhost:8080/client/",
            "SAML2 CloudStack Service Provider Single Log Out URL", true);

    public static final ConfigKey<String> SAMLCloudStackRedirectionUrl = new ConfigKey<String>("Advanced", String.class, "saml2.redirect.url", "http://localhost:8080/client",
            "The CloudStack UI url the SSO should redirected to when successful", true);

    public static final ConfigKey<String> SAMLUserAttributeName = new ConfigKey<String>("Advanced", String.class, "saml2.user.attribute", "uid",
            "Attribute name to be looked for in SAML response that will contain the username", true);

    public static final ConfigKey<String> SAMLIdentityProviderMetadataURL = new ConfigKey<String>("Advanced", String.class, "saml2.idp.metadata.url", "https://openidp.feide.no/simplesaml/saml2/idp/metadata.php",
            "SAML2 Identity Provider Metadata XML Url", true);

    public static final ConfigKey<String> SAMLDefaultIdentityProviderId = new ConfigKey<String>("Advanced", String.class, "saml2.default.idpid", "https://openidp.feide.no",
            "The default IdP entity ID to use only in case of multiple IdPs", true);

    public static final ConfigKey<String> SAMLSignatureAlgorithm = new ConfigKey<String>("Advanced", String.class, "saml2.sigalg", "SHA1",
            "The algorithm to use to when signing a SAML request. Default is SHA1, allowed algorithms: SHA1, SHA256, SHA384, SHA512", true);

    public static final ConfigKey<Boolean> SAMLAppendDomainSuffix = new ConfigKey<Boolean>("Advanced", Boolean.class, "saml2.append.idpdomain", "false",
            "If enabled, create account/user dialog with SAML SSO enabled will append the IdP domain to the user or account name in the UI dialog", true);

    public static final ConfigKey<Integer> SAMLTimeout = new ConfigKey<Integer>("Advanced", Integer.class, "saml2.timeout", "1800",
            "SAML2 IDP Metadata refresh interval in seconds, minimum value is set to 300", true);

    public SAMLProviderMetadata getSPMetadata();
    public SAMLProviderMetadata getIdPMetadata(String entityId);
    public Collection<SAMLProviderMetadata> getAllIdPMetadata();

    public boolean isUserAuthorized(Long userId, String entityId);
    public boolean authorizeUser(Long userId, String entityId, boolean enable);

    public void saveToken(String authnId, String domain, String entity);
    public SAMLTokenVO getToken(String authnId);
    public void expireTokens();
}
