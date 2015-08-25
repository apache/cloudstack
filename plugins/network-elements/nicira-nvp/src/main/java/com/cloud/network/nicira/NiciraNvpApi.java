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

package com.cloud.network.nicira;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

import com.cloud.utils.rest.CloudstackRESTException;
import com.cloud.utils.rest.RESTServiceConnector;
import com.google.common.base.Optional;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;

@SuppressWarnings("rawtypes")
public class NiciraNvpApi {

    private static final Optional<String> ABSENT = Optional.absent();

    private static final String SWITCH_URI_PREFIX = NiciraConstants.SWITCH_URI_PREFIX;
    private static final String ROUTER_URI_PREFIX = NiciraConstants.ROUTER_URI_PREFIX;

    private static final String ATTACHMENT_PATH_SEGMENT = NiciraConstants.ATTACHMENT_PATH_SEGMENT;
    private static final String NAT_PATH_SEGMENT = NiciraConstants.NAT_PATH_SEGMENT;
    private static final String LPORT_PATH_SEGMENT = NiciraConstants.LPORT_PATH_SEGMENT;

    private static final String ATTACHMENT_GWSVC_UUID_QUERY_PARAMETER = NiciraConstants.ATTACHMENT_GWSVC_UUID_QUERY_PARAMETER;
    private static final String WILDCARD_QUERY_PARAMETER = NiciraConstants.WILDCARD_QUERY_PARAMETER;
    private static final String UUID_QUERY_PARAMETER = NiciraConstants.UUID_QUERY_PARAMETER;
    private static final String FIELDS_QUERY_PARAMETER = NiciraConstants.FIELDS_QUERY_PARAMETER;

    private static final int DEFAULT_MAX_RETRIES = 5;

    private final RESTServiceConnector restConnector;

    protected final static Map<Class, String> prefixMap;

    protected final static Map<Class, Type> listTypeMap;

    protected final static Map<String, String> defaultListParams;

    static {
        prefixMap = new HashMap<Class, String>();
        prefixMap.put(SecurityProfile.class, NiciraConstants.SEC_PROFILE_URI_PREFIX);
        prefixMap.put(Acl.class, NiciraConstants.ACL_URI_PREFIX);
        prefixMap.put(LogicalSwitch.class, SWITCH_URI_PREFIX);
        prefixMap.put(LogicalRouter.class, ROUTER_URI_PREFIX);

        listTypeMap = new HashMap<Class, Type>();
        listTypeMap.put(SecurityProfile.class, new TypeToken<NiciraNvpList<SecurityProfile>>() {
        }.getType());
        listTypeMap.put(Acl.class, new TypeToken<NiciraNvpList<Acl>>() {
        }.getType());
        listTypeMap.put(LogicalSwitch.class, new TypeToken<NiciraNvpList<LogicalSwitch>>() {
        }.getType());
        listTypeMap.put(LogicalRouter.class, new TypeToken<NiciraNvpList<LogicalRouter>>() {
        }.getType());

        defaultListParams = new HashMap<String, String>();
        defaultListParams.put(FIELDS_QUERY_PARAMETER, WILDCARD_QUERY_PARAMETER);
    }

    private NiciraNvpApi(final Builder builder) {
        final Map<Class<?>, JsonDeserializer<?>> classToDeserializerMap = new HashMap<>();
        classToDeserializerMap.put(NatRule.class, new NatRuleAdapter());
        classToDeserializerMap.put(RoutingConfig.class, new RoutingConfigAdapter());

        final NiciraRestClient niciraRestClient = NiciraRestClient.create()
            .client(builder.httpClient)
            .clientContext(builder.httpClientContext)
            .hostname(builder.host)
            .username(builder.username)
            .password(builder.password)
            .loginUrl(NiciraConstants.LOGIN_URL)
            .executionLimit(DEFAULT_MAX_RETRIES)
            .build();
        restConnector = RESTServiceConnector.create()
            .classToDeserializerMap(classToDeserializerMap)
            .client(niciraRestClient)
            .build();
    }

    public static Builder create() {
        return new Builder();
    }

    /**
     * POST
     *
     * @param entity
     * @return
     * @throws NiciraNvpApiException
     */
    private <T> T create(final T entity) throws NiciraNvpApiException {
        final String uri = prefixMap.get(entity.getClass());
        return createWithUri(entity, uri);
    }

    /**
     * POST
     *
     * @param entity
     * @return
     * @throws NiciraNvpApiException
     */
    private <T> T createWithUri(final T entity, final String uri) throws NiciraNvpApiException {
        T createdEntity;
        try {
            createdEntity = restConnector.executeCreateObject(entity, uri, Collections.<String, String> emptyMap());
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }

        return createdEntity;
    }

    /**
     * GET list of items
     *
     * @param uuid
     *
     * @return
     * @throws NiciraNvpApiException
     */
    private <T> List<T> find(final Optional<String> uuid, final Class<T> clazz) throws NiciraNvpApiException {
        final String uri = prefixMap.get(clazz);
        Map<String, String> params = defaultListParams;
        if (uuid.isPresent()) {
            params = new HashMap<String, String>(defaultListParams);
            params.put(UUID_QUERY_PARAMETER, uuid.get());
        }

        NiciraNvpList<T> entities;
        try {
            entities = restConnector.executeRetrieveObject(listTypeMap.get(clazz), uri, params);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }

        if (entities == null) {
            throw new NiciraNvpApiException("Unexpected response from API");
        }

        return entities.getResults();
    }

    /**
     * PUT item given a UUID as key and an item object with the new data
     *
     * @param item
     * @param uuid
     * @throws NiciraNvpApiException
     */
    private <T> void update(final T item, final String uuid) throws NiciraNvpApiException {
        final String uri = prefixMap.get(item.getClass()) + "/" + uuid;
        updateWithUri(item, uri);
    }

    /**
     * PUT item given a UUID as key and an item object with the new data
     *
     * @param item
     * @param uuid
     * @throws NiciraNvpApiException
     */
    private <T> void updateWithUri(final T item, final String uri) throws NiciraNvpApiException {
        try {
            restConnector.executeUpdateObject(item, uri, Collections.<String, String> emptyMap());
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    /**
     * DELETE Security Profile given a UUID as key
     *
     * @param securityProfileUuid
     * @throws NiciraNvpApiException
     */
    private <T> void delete(final String uuid, final Class<T> clazz) throws NiciraNvpApiException {
        final String uri = prefixMap.get(clazz) + "/" + uuid;
        deleteWithUri(uri);
    }

    /**
     * DELETE Security Profile given a UUID as key
     *
     * @param securityProfileUuid
     * @throws NiciraNvpApiException
     */
    private void deleteWithUri(final String uri) throws NiciraNvpApiException {
        try {
            restConnector.executeDeleteObject(uri);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    /**
     * POST {@link SecurityProfile}
     *
     * @param securityProfile
     * @return
     * @throws NiciraNvpApiException
     */
    public SecurityProfile createSecurityProfile(final SecurityProfile securityProfile) throws NiciraNvpApiException {
        return create(securityProfile);
    }

    /**
     * GET list of {@link SecurityProfile}
     *
     * @return
     * @throws NiciraNvpApiException
     */
    public List<SecurityProfile> findSecurityProfile() throws NiciraNvpApiException {
        return find(ABSENT, SecurityProfile.class);
    }

    /**
     * GET list of {@link SecurityProfile} filtered by UUID
     *
     * We could have invoked the service: SEC_PROFILE_URI_PREFIX + "/" + securityProfileUuid but it is not working currently
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public List<SecurityProfile> findSecurityProfile(final String uuid) throws NiciraNvpApiException {
        return find(Optional.fromNullable(uuid), SecurityProfile.class);
    }

    /**
     * PUT {@link SecurityProfile} given a UUID as key and a {@link SecurityProfile} with the new data
     *
     * @param securityProfile
     * @param securityProfileUuid
     * @throws NiciraNvpApiException
     */
    public void updateSecurityProfile(final SecurityProfile securityProfile, final String securityProfileUuid) throws NiciraNvpApiException {
        update(securityProfile, securityProfileUuid);
    }

    /**
     * DELETE Security Profile given a UUID as key
     *
     * @param securityProfileUuid
     * @throws NiciraNvpApiException
     */
    public void deleteSecurityProfile(final String securityProfileUuid) throws NiciraNvpApiException {
        delete(securityProfileUuid, SecurityProfile.class);
    }

    /**
     * POST {@link Acl}
     *
     * @param acl
     * @return
     * @throws NiciraNvpApiException
     */
    public Acl createAcl(final Acl acl) throws NiciraNvpApiException {
        return create(acl);
    }

    /**
     * GET list of {@link Acl}
     *
     * @return
     * @throws NiciraNvpApiException
     */
    public List<Acl> findAcl() throws NiciraNvpApiException {
        return findAcl(null);
    }

    /**
     * GET list of {@link Acl} filtered by UUID
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public List<Acl> findAcl(final String uuid) throws NiciraNvpApiException {
        return find(Optional.fromNullable(uuid), Acl.class);
    }

    /**
     * PUT {@link Acl} given a UUID as key and a {@link Acl} with the new data
     *
     * @param acl
     * @param aclUuid
     * @throws NiciraNvpApiException
     */
    public void updateAcl(final Acl acl, final String aclUuid) throws NiciraNvpApiException {
        update(acl, aclUuid);
    }

    /**
     * DELETE Acl given a UUID as key
     *
     * @param acl
     * @throws NiciraNvpApiException
     */
    public void deleteAcl(final String aclUuid) throws NiciraNvpApiException {
        delete(aclUuid, Acl.class);
    }

    public LogicalSwitch createLogicalSwitch(final LogicalSwitch logicalSwitch) throws NiciraNvpApiException {
        return create(logicalSwitch);
    }

    /**
     * GET list of {@link LogicalSwitch}
     *
     * @return
     * @throws NiciraNvpApiException
     */
    public List<LogicalSwitch> findLogicalSwitch() throws NiciraNvpApiException {
        return findLogicalSwitch(null);
    }

    /**
     * GET list of {@link LogicalSwitch} filtered by UUID
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public List<LogicalSwitch> findLogicalSwitch(final String uuid) throws NiciraNvpApiException {
        return find(Optional.fromNullable(uuid), LogicalSwitch.class);
    }

    /**
     * PUT {@link LogicalSwitch} given a UUID as key and a {@link LogicalSwitch} with the new data
     *
     * @param logicalSwitch
     * @param logicalSwitchUuid
     * @throws NiciraNvpApiException
     */
    public void updateLogicalSwitch(final LogicalSwitch logicalSwitch, final String logicalSwitchUuid) throws NiciraNvpApiException {
        update(logicalSwitch, logicalSwitchUuid);
    }

    public void deleteLogicalSwitch(final String uuid) throws NiciraNvpApiException {
        delete(uuid, LogicalSwitch.class);
    }

    public LogicalSwitchPort createLogicalSwitchPort(final String logicalSwitchUuid, final LogicalSwitchPort logicalSwitchPort) throws NiciraNvpApiException {
        return createWithUri(logicalSwitchPort, buildLogicalSwitchElementUri(logicalSwitchUuid, LPORT_PATH_SEGMENT));
    }

    public void updateLogicalSwitchPort(final String logicalSwitchUuid, final LogicalSwitchPort logicalSwitchPort) throws NiciraNvpApiException {
        updateWithUri(logicalSwitchPort, buildLogicalSwitchElementUri(logicalSwitchUuid, LPORT_PATH_SEGMENT, logicalSwitchPort.getUuid().toString()));
    }

    public void updateLogicalSwitchPortAttachment(final String logicalSwitchUuid, final String logicalSwitchPortUuid, final Attachment attachment) throws NiciraNvpApiException {
        updateWithUri(attachment, buildLogicalSwitchElementUri(logicalSwitchUuid, LPORT_PATH_SEGMENT, logicalSwitchPortUuid) + ATTACHMENT_PATH_SEGMENT);
    }

    public void deleteLogicalSwitchPort(final String logicalSwitchUuid, final String logicalSwitchPortUuid) throws NiciraNvpApiException {
        deleteWithUri(buildLogicalSwitchElementUri(logicalSwitchUuid, LPORT_PATH_SEGMENT, logicalSwitchPortUuid));
    }

    public String findLogicalSwitchPortUuidByVifAttachmentUuid(final String logicalSwitchUuid, final String vifAttachmentUuid) throws NiciraNvpApiException {
        final String uri = buildLogicalSwitchElementUri(logicalSwitchUuid, LPORT_PATH_SEGMENT);
        final Map<String, String> params = buildBasicParametersMap(UUID_QUERY_PARAMETER);
        params.put(NiciraConstants.ATTACHMENT_VIF_UUID_QUERY_PARAMETER_NAME, vifAttachmentUuid);

        NiciraNvpList<LogicalSwitchPort> niciraList;
        try {
            final Type niciraListType = new TypeToken<NiciraNvpList<LogicalSwitchPort>>() {
            }.getType();
            niciraList = restConnector.executeRetrieveObject(niciraListType, uri, params);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }

        final List<LogicalSwitchPort> lspl = niciraList.getResults();

        final int listSize = lspl.size();
        if (listSize != 1) {
            throw new NiciraNvpApiException("Expected 1 LogicalSwitchPort, but got " + listSize);
        }

        final LogicalSwitchPort lsp = lspl.get(0);
        return lsp.getUuid();
    }

    public ControlClusterStatus getControlClusterStatus() throws NiciraNvpApiException {
        final String uri = NiciraConstants.CONTROL_CLUSTER_STATUS_URL;
        try {
            return restConnector.executeRetrieveObject(ControlClusterStatus.class, uri, new HashMap<String, String>());
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    public List<LogicalSwitchPort> findLogicalSwitchPortsByUuid(final String logicalSwitchUuid, final String logicalSwitchPortUuid) throws NiciraNvpApiException {
        final String uri = buildLogicalSwitchElementUri(logicalSwitchUuid, LPORT_PATH_SEGMENT);
        final Map<String, String> params = buildBasicParametersMap(UUID_QUERY_PARAMETER);
        params.put(UUID_QUERY_PARAMETER, logicalSwitchPortUuid);

        try {
            final Type niciraListType = new TypeToken<NiciraNvpList<LogicalSwitchPort>>() {
            }.getType();
            return restConnector.<NiciraNvpList<LogicalSwitchPort>> executeRetrieveObject(niciraListType, uri, params).getResults();
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    public List<LogicalRouterPort> findLogicalRouterPortsByUuid(final String logicalRouterUuid, final String logicalRouterPortUuid) throws NiciraNvpApiException {
        final String uri = buildLogicalRouterElementUri(logicalRouterUuid, LPORT_PATH_SEGMENT);
        final Map<String, String> params = buildBasicParametersMap(UUID_QUERY_PARAMETER);
        params.put(UUID_QUERY_PARAMETER, logicalRouterPortUuid);

        try {
            final Type niciraListType = new TypeToken<NiciraNvpList<LogicalRouterPort>>() {
            }.getType();
            return restConnector.<NiciraNvpList<LogicalRouterPort>> executeRetrieveObject(niciraListType, uri, params).getResults();
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    public LogicalRouter createLogicalRouter(final LogicalRouter logicalRouter) throws NiciraNvpApiException {
        return create(logicalRouter);
    }

    /**
     * GET list of {@link LogicalRouter}
     *
     * @return
     * @throws NiciraNvpApiException
     */
    public List<LogicalRouter> findLogicalRouter() throws NiciraNvpApiException {
        return findLogicalRouter(null);
    }

    /**
     * GET list of {@link LogicalRouter} filtered by UUID
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public List<LogicalRouter> findLogicalRouter(final String uuid) throws NiciraNvpApiException {
        return find(Optional.fromNullable(uuid), LogicalRouter.class);
    }

    public LogicalRouter findOneLogicalRouterByUuid(final String logicalRouterUuid) throws NiciraNvpApiException {
        return findLogicalRouter(logicalRouterUuid).get(0);
    }

    public void updateLogicalRouter(final LogicalRouter logicalRouter, final String logicalRouterUuid) throws NiciraNvpApiException {
        update(logicalRouter, logicalRouterUuid);
    }

    public void deleteLogicalRouter(final String logicalRouterUuid) throws NiciraNvpApiException {
        deleteWithUri(buildLogicalRouterUri(logicalRouterUuid));
    }

    public LogicalRouterPort createLogicalRouterPort(final String logicalRouterUuid, final LogicalRouterPort logicalRouterPort) throws NiciraNvpApiException {
        return createWithUri(logicalRouterPort, buildLogicalRouterElementUri(logicalRouterUuid, LPORT_PATH_SEGMENT));
    }

    public void deleteLogicalRouterPort(final String logicalRouterUuid, final String logicalRouterPortUuid) throws NiciraNvpApiException {
        deleteWithUri(buildLogicalRouterElementUri(logicalRouterUuid, LPORT_PATH_SEGMENT, logicalRouterPortUuid));
    }

    public void updateLogicalRouterPort(final String logicalRouterUuid, final LogicalRouterPort logicalRouterPort) throws NiciraNvpApiException {
        updateWithUri(logicalRouterPort, buildLogicalRouterElementUri(logicalRouterUuid, LPORT_PATH_SEGMENT, logicalRouterPort.getUuid().toString()));
    }

    public void updateLogicalRouterPortAttachment(final String logicalRouterUuid, final String logicalRouterPortUuid, final Attachment attachment) throws NiciraNvpApiException {
        updateWithUri(attachment, buildLogicalRouterElementUri(logicalRouterUuid, LPORT_PATH_SEGMENT, logicalRouterPortUuid) + ATTACHMENT_PATH_SEGMENT);
    }

    public NatRule createLogicalRouterNatRule(final String logicalRouterUuid, final NatRule natRule) throws NiciraNvpApiException {
        return createWithUri(natRule, buildLogicalRouterElementUri(logicalRouterUuid, NAT_PATH_SEGMENT));
    }

    public void updateLogicalRouterNatRule(final String logicalRouterUuid, final NatRule natRule) throws NiciraNvpApiException {
        updateWithUri(natRule, buildLogicalRouterElementUri(logicalRouterUuid, NAT_PATH_SEGMENT, natRule.getUuid().toString()));
    }

    public void deleteLogicalRouterNatRule(final String logicalRouterUuid, final UUID natRuleUuid) throws NiciraNvpApiException {
        deleteWithUri(buildLogicalRouterElementUri(logicalRouterUuid, NAT_PATH_SEGMENT, natRuleUuid.toString()));
    }

    public List<LogicalRouterPort> findLogicalRouterPortByGatewayServiceAndVlanId(final String logicalRouterUuid, final String gatewayServiceUuid, final long vlanId)
                    throws NiciraNvpApiException {
        final String uri = buildLogicalRouterElementUri(logicalRouterUuid, LPORT_PATH_SEGMENT);
        final Map<String, String> params = buildBasicParametersMap(WILDCARD_QUERY_PARAMETER);
        params.put(ATTACHMENT_GWSVC_UUID_QUERY_PARAMETER, gatewayServiceUuid);
        params.put(NiciraConstants.ATTACHMENT_VLAN_PARAMETER, Long.toString(vlanId));

        try {
            final Type niciraListType = new TypeToken<NiciraNvpList<LogicalRouterPort>>() {
            }.getType();
            return restConnector.<NiciraNvpList<LogicalRouterPort>> executeRetrieveObject(niciraListType, uri, params).getResults();
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    public List<NatRule> findNatRulesByLogicalRouterUuid(final String logicalRouterUuid) throws NiciraNvpApiException {
        final String uri = buildLogicalRouterElementUri(logicalRouterUuid, NAT_PATH_SEGMENT);
        final Map<String, String> params = buildBasicParametersMap(WILDCARD_QUERY_PARAMETER);

        try {
            final Type niciraListType = new TypeToken<NiciraNvpList<NatRule>>() {
            }.getType();
            return restConnector.<NiciraNvpList<NatRule>> executeRetrieveObject(niciraListType, uri, params).getResults();
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    public List<LogicalRouterPort> findLogicalRouterPortByGatewayServiceUuid(final String logicalRouterUuid, final String l3GatewayServiceUuid)
                    throws NiciraNvpApiException {
        final String uri = buildLogicalRouterElementUri(logicalRouterUuid, LPORT_PATH_SEGMENT);
        final Map<String, String> params = buildBasicParametersMap(WILDCARD_QUERY_PARAMETER);
        params.put(ATTACHMENT_GWSVC_UUID_QUERY_PARAMETER, l3GatewayServiceUuid);

        try {
            final Type niciraListType = new TypeToken<NiciraNvpList<LogicalRouterPort>>() {
            }.getType();
            return restConnector.<NiciraNvpList<LogicalRouterPort>> executeRetrieveObject(niciraListType, uri, params).getResults();
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    private static Map<String, String> buildBasicParametersMap(final String fieldsQueryValue) {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(FIELDS_QUERY_PARAMETER, fieldsQueryValue);
        return params;
    }

    private static String buildUri(final String uriPrefix, final String uuid) {
        return uriPrefix + "/" + uuid;
    }

    private static String buildLogicalSwitchUri(final String logicalSwitchUuid) {
        return buildUri(SWITCH_URI_PREFIX, logicalSwitchUuid);
    }

    private static String buildLogicalSwitchElementUri(final String logicalSwitchUuid, final String logicalElementType) {
        return buildLogicalSwitchUri(logicalSwitchUuid) + logicalElementType;
    }

    private static String buildLogicalSwitchElementUri(final String logicalSwitchUuid, final String logicalElementType, final String elementUuid) {
        return buildLogicalSwitchElementUri(logicalSwitchUuid, logicalElementType) + "/" + elementUuid.toString();
    }

    private static String buildLogicalRouterUri(final String logicalRouterUuid) {
        return buildUri(ROUTER_URI_PREFIX, logicalRouterUuid);
    }

    private static String buildLogicalRouterElementUri(final String logicalRouterUuid, final String logicalElementType) {
        return buildLogicalRouterUri(logicalRouterUuid) + logicalElementType;
    }

    private static String buildLogicalRouterElementUri(final String logicalRouterUuid, final String logicalRouterElementType, final String elementUuid) {
        return buildLogicalRouterElementUri(logicalRouterUuid, logicalRouterElementType) + "/" + elementUuid.toString();
    }

    public static class Builder {
        private String host;
        private String username;
        private String password;
        private CloseableHttpClient httpClient;
        private HttpClientContext httpClientContext = HttpClientContext.create();

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder httpClient(final CloseableHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder httpClientContext(final HttpClientContext httpClientContext) {
            this.httpClientContext = httpClientContext;
            return this;
        }

        public NiciraNvpApi build() {
            return new NiciraNvpApi(this);
        }
    }
}
