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

    ConfigKey<Boolean> SAMLIsPluginEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "saml2.enabled", "false",
            "Indicates whether SAML SSO plugin is enabled or not", true);

    ConfigKey<String> SAMLServiceProviderID = new ConfigKey<String>("Advanced", String.class, "saml2.sp.id", "org.apache.cloudstack",
            "SAML2 Service Provider Identifier String", true);

    ConfigKey<String> SAMLServiceProviderContactPersonName = new ConfigKey<String>("Advanced", String.class, "saml2.sp.contact.person", "CloudStack Developers",
            "SAML2 Service Provider Contact Person Name", true);

    ConfigKey<String> SAMLServiceProviderContactEmail = new ConfigKey<String>("Advanced", String.class, "saml2.sp.contact.email", "dev@cloudstack.apache.org",
            "SAML2 Service Provider Contact Email Address", true);

    ConfigKey<String> SAMLServiceProviderOrgName = new ConfigKey<String>("Advanced", String.class, "saml2.sp.org.name", "Apache CloudStack",
            "SAML2 Service Provider Organization Name", true);

    ConfigKey<String> SAMLServiceProviderOrgUrl = new ConfigKey<String>("Advanced", String.class, "saml2.sp.org.url", "http://cloudstack.apache.org",
            "SAML2 Service Provider Organization URL", true);

    ConfigKey<String> SAMLServiceProviderSingleSignOnURL = new ConfigKey<String>("Advanced", String.class, "saml2.sp.sso.url", "http://localhost:8080/client/api?command=samlSso",
            "SAML2 CloudStack Service Provider Single Sign On URL", true);

    ConfigKey<String> SAMLServiceProviderSingleLogOutURL = new ConfigKey<String>("Advanced", String.class, "saml2.sp.slo.url", "http://localhost:8080/client/",
            "SAML2 CloudStack Service Provider Single Log Out URL", true);

    ConfigKey<String> SAMLCloudStackRedirectionUrl = new ConfigKey<String>("Advanced", String.class, "saml2.redirect.url", "http://localhost:8080/client",
            "The CloudStack UI url the SSO should redirected to when successful", true);

    ConfigKey<String> SAMLUserAttributeName = new ConfigKey<String>("Advanced", String.class, "saml2.user.attribute", "uid",
            "Attribute name to be looked for in SAML response that will contain the username", true);

    ConfigKey<String> SAMLIdentityProviderMetadataURL = new ConfigKey<String>("Advanced", String.class, "saml2.idp.metadata.url", "https://openidp.feide.no/simplesaml/saml2/idp/metadata.php",
            "SAML2 Identity Provider Metadata XML Url", true);

    ConfigKey<String> SAMLDefaultIdentityProviderId = new ConfigKey<String>("Advanced", String.class, "saml2.default.idpid", "https://openidp.feide.no",
            "The default IdP entity ID to use only in case of multiple IdPs", true);

    ConfigKey<String> SAMLSignatureAlgorithm = new ConfigKey<>(String.class, "saml2.sigalg", "Advanced", "SHA1",
            "The algorithm to use to when signing a SAML request. Default is SHA1, allowed algorithms: SHA1, SHA256, SHA384, SHA512", true, ConfigKey.Scope.Global, null, null, null, null, null, ConfigKey.Kind.Select, "SHA1,SHA256,SHA384,SHA512");

    ConfigKey<Boolean> SAMLAppendDomainSuffix = new ConfigKey<Boolean>("Advanced", Boolean.class, "saml2.append.idpdomain", "false",
            "If enabled, create account/user dialog with SAML SSO enabled will append the IdP domain to the user or account name in the UI dialog", true);

    ConfigKey<Integer> SAMLTimeout = new ConfigKey<Integer>("Advanced", Integer.class, "saml2.timeout", "1800",
            "SAML2 IDP Metadata refresh interval in seconds, minimum value is set to 300", true);

    ConfigKey<Boolean> SAMLCheckSignature = new ConfigKey<Boolean>("Advanced", Boolean.class, "saml2.check.signature", "true",
            "When enabled (default and recommended), SAML2 signature checks are enforced and lack of signature in the SAML SSO response will cause login exception. Disabling this is not advisable but provided for backward compatibility for users who are able to accept the risks.", false);

    ConfigKey<Boolean> SAMLForceAuthn = new ConfigKey<Boolean>("Advanced", Boolean.class, "saml2.force.authn", "false",
            "When enabled (default false), SAML2 will force a new authentication. This can be useful if multiple application use different saml logins from the same application (I.E. browser)", true);

    ConfigKey<String> SAMLUserSessionKeyPathAttribute = new ConfigKey<String>("Advanced", String.class, "saml2.user.sessionkey.path", "",
            "The Path attribute of sessionkey cookie when SAML users have logged in. If not set, it will be set to the path of SAML redirection URL (saml2.redirect.url).", true);

    SAMLProviderMetadata getSPMetadata();
    SAMLProviderMetadata getIdPMetadata(String entityId);
    Collection<SAMLProviderMetadata> getAllIdPMetadata();

    boolean isUserAuthorized(Long userId, String entityId);
    boolean authorizeUser(Long userId, String entityId, boolean enable);

    void saveToken(String authnId, String domain, String entity);
    SAMLTokenVO getToken(String authnId);
    void purgeToken(SAMLTokenVO token);
    void expireTokens();
}
