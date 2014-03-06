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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import com.cloud.utils.rest.CloudstackRESTException;
import com.cloud.utils.rest.RESTServiceConnector;
import com.cloud.utils.rest.RESTValidationStrategy;

@SuppressWarnings("rawtypes")
public class NiciraNvpApi {
    protected static final String GET_METHOD_TYPE = "get";
    protected static final String DELETE_METHOD_TYPE = "delete";
    protected static final String PUT_METHOD_TYPE = "put";
    protected static final String POST_METHOD_TYPE = "post";

    protected static final String SEC_PROFILE_URI_PREFIX = "/ws.v1/security-profile";
    protected static final String ACL_URI_PREFIX = "/ws.v1/acl";
    private static final String SWITCH_URI_PREFIX = "/ws.v1/lswitch";
    private static final String ROUTER_URI_PREFIX = "/ws.v1/lrouter";
    private static final String LOGIN_URL = "/ws.v1/login";

    protected RESTServiceConnector restConnector;

    protected final static Map<Class, String> prefixMap;

    protected final static Map<Class, Type> listTypeMap;

    protected final static Map<String, String> defaultListParams;

    static {
        prefixMap = new HashMap<Class, String>();
        prefixMap.put(SecurityProfile.class, SEC_PROFILE_URI_PREFIX);
        prefixMap.put(Acl.class, ACL_URI_PREFIX);
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
        defaultListParams.put("fields", "*");
    }

    public NiciraNvpApi() {
        final List<Class<?>> classList = new ArrayList<Class<?>>();
        classList.add(NatRule.class);
        classList.add(RoutingConfig.class);
        final List<JsonDeserializer<?>> deserializerList = new ArrayList<JsonDeserializer<?>>();
        deserializerList.add(new NatRuleAdapter());
        deserializerList.add(new RoutingConfigAdapter());

        restConnector = new RESTServiceConnector(new RESTValidationStrategy(LOGIN_URL), classList, deserializerList);
    }

    public NiciraNvpApi(final String address, final String username, final String password) {
        this();
        restConnector.setControllerAddress(address);
        restConnector.setAdminCredentials(username, password);
    }

    public void setControllerAddress(final String address) {
        restConnector.setControllerAddress(address);
    }

    public void setAdminCredentials(final String username, final String password) {
        restConnector.setAdminCredentials(username, password);
    }

    /**
     * POST
     *
     * @param entity
     * @return
     * @throws NiciraNvpApiException
     */
    protected <T> T create(final T entity) throws NiciraNvpApiException {
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
    protected <T> T createWithUri(final T entity, final String uri) throws NiciraNvpApiException {
        T createdEntity;
        try {
            createdEntity = restConnector.executeCreateObject(entity, new TypeToken<T>() {
            }.getType(), uri, Collections.<String, String> emptyMap());
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }

        return createdEntity;
    }

    /**
     * GET list of items
     *
     * @return
     * @throws NiciraNvpApiException
     */
    protected <T> NiciraNvpList<T> find(final Class<T> clazz) throws NiciraNvpApiException {
        return find(null, clazz);
    }

    /**
     * GET list of items
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public <T> NiciraNvpList<T> find(final String uuid, final Class<T> clazz) throws NiciraNvpApiException {
        final String uri = prefixMap.get(clazz);
        Map<String, String> params = defaultListParams;
        if (uuid != null) {
            params = new HashMap<String, String>(defaultListParams);
            params.put("uuid", uuid);
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

        return entities;
    }

    /**
     * PUT item given a UUID as key and an item object
     * with the new data
     *
     * @param item
     * @param uuid
     * @throws NiciraNvpApiException
     */
    public <T> void update(final T item, final String uuid)
                    throws NiciraNvpApiException {
        final String uri = prefixMap.get(item.getClass()) + "/" + uuid;
        updateWithUri(item, uri);
    }

    /**
     * PUT item given a UUID as key and an item object
     * with the new data
     *
     * @param item
     * @param uuid
     * @throws NiciraNvpApiException
     */
    public <T> void updateWithUri(final T item, final String uri)
                    throws NiciraNvpApiException {
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
    public <T> void delete(final String uuid, final Class<T> clazz)
            throws NiciraNvpApiException {
        final String uri = prefixMap.get(clazz) + "/" + uuid;
        deleteWithUri(uri);
    }

    /**
     * DELETE Security Profile given a UUID as key
     *
     * @param securityProfileUuid
     * @throws NiciraNvpApiException
     */
    public <T> void deleteWithUri(final String uri)
            throws NiciraNvpApiException {
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
    public NiciraNvpList<SecurityProfile> findSecurityProfile() throws NiciraNvpApiException {
        return findSecurityProfile(null);
    }

    /**
     * GET list of {@link SecurityProfile} filtered by UUID
     *
     * We could have invoked the service:
     * SEC_PROFILE_URI_PREFIX + "/" + securityProfileUuid
     * but it is not working currently
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public NiciraNvpList<SecurityProfile> findSecurityProfile(final String uuid) throws NiciraNvpApiException {
        return find(uuid, SecurityProfile.class);
    }

    /**
     * PUT {@link SecurityProfile} given a UUID as key and a {@link SecurityProfile}
     * with the new data
     *
     * @param securityProfile
     * @param securityProfileUuid
     * @throws NiciraNvpApiException
     */
    public void updateSecurityProfile(final SecurityProfile securityProfile,
            final String securityProfileUuid)
                    throws NiciraNvpApiException {
        update(securityProfile, securityProfileUuid);
    }

    /**
     * DELETE Security Profile given a UUID as key
     *
     * @param securityProfileUuid
     * @throws NiciraNvpApiException
     */
    public void deleteSecurityProfile(final String securityProfileUuid)
            throws NiciraNvpApiException {
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
    public NiciraNvpList<Acl> findAcl() throws NiciraNvpApiException {
        return findAcl(null);
    }

    /**
     * GET list of {@link Acl} filtered by UUID
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public NiciraNvpList<Acl> findAcl(final String uuid) throws NiciraNvpApiException {
        return find(uuid, Acl.class);
    }

    /**
     * PUT {@link Acl} given a UUID as key and a {@link Acl}
     * with the new data
     *
     * @param acl
     * @param aclUuid
     * @throws NiciraNvpApiException
     */
    public void updateAcl(final Acl acl,
            final String aclUuid)
                    throws NiciraNvpApiException {
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
    public NiciraNvpList<LogicalSwitch> findLogicalSwitch() throws NiciraNvpApiException {
        return findLogicalSwitch(null);
    }

    /**
     * GET list of {@link LogicalSwitch} filtered by UUID
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public NiciraNvpList<LogicalSwitch> findLogicalSwitch(final String uuid) throws NiciraNvpApiException {
        return find(uuid, LogicalSwitch.class);
    }

    /**
     * PUT {@link LogicalSwitch} given a UUID as key and a {@link LogicalSwitch}
     * with the new data
     *
     * @param logicalSwitch
     * @param logicalSwitchUuid
     * @throws NiciraNvpApiException
     */
    public void updateLogicalSwitch(final LogicalSwitch logicalSwitch,
            final String logicalSwitchUuid)
                    throws NiciraNvpApiException {
        update(logicalSwitch, logicalSwitchUuid);
    }

    public void deleteLogicalSwitch(final String uuid) throws NiciraNvpApiException {
        delete(uuid, LogicalSwitch.class);
    }

    public LogicalSwitchPort createLogicalSwitchPort(final String logicalSwitchUuid, final LogicalSwitchPort logicalSwitchPort) throws NiciraNvpApiException {
        final String uri = SWITCH_URI_PREFIX + "/" + logicalSwitchUuid + "/lport";
        return createWithUri(logicalSwitchPort, uri);
    }

    public void updateLogicalSwitchPort(final String logicalSwitchUuid, final LogicalSwitchPort logicalSwitchPort) throws NiciraNvpApiException {
        final String uri = SWITCH_URI_PREFIX + "/" + logicalSwitchUuid + "/lport/" + logicalSwitchPort.getUuid();
        updateWithUri(logicalSwitchPort, uri);
    }

    public void updateLogicalSwitchPortAttachment(final String logicalSwitchUuid, final String logicalSwitchPortUuid,
            final Attachment attachment) throws NiciraNvpApiException {
        final String uri = SWITCH_URI_PREFIX + "/" + logicalSwitchUuid + "/lport/" + logicalSwitchPortUuid + "/attachment";
        updateWithUri(attachment, uri);
    }

    public void deleteLogicalSwitchPort(final String logicalSwitchUuid, final String logicalSwitchPortUuid) throws NiciraNvpApiException {
        final String uri = SWITCH_URI_PREFIX + "/" + logicalSwitchUuid + "/lport/" + logicalSwitchPortUuid;
        deleteWithUri(uri);
    }

    public String findLogicalSwitchPortUuidByVifAttachmentUuid(final String logicalSwitchUuid, final String vifAttachmentUuid) throws NiciraNvpApiException {
        final String uri = SWITCH_URI_PREFIX + "/" + logicalSwitchUuid + "/lport";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("attachment_vif_uuid", vifAttachmentUuid);
        params.put("fields", "uuid");

        NiciraNvpList<LogicalSwitchPort> lspl;
        try {
            lspl = restConnector.executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalSwitchPort>>() {
            }.getType(), uri, params);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }

        if (lspl == null || lspl.getResultCount() != 1) {
            throw new NiciraNvpApiException("Unexpected response from API");
        }

        final LogicalSwitchPort lsp = lspl.getResults().get(0);
        return lsp.getUuid();
    }

    public ControlClusterStatus getControlClusterStatus() throws NiciraNvpApiException {
        final String uri = "/ws.v1/control-cluster/status";
        ControlClusterStatus ccs;
        try {
            ccs = restConnector.executeRetrieveObject(new TypeToken<ControlClusterStatus>() {
            }.getType(), uri, null);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }

        return ccs;
    }

    public NiciraNvpList<LogicalSwitchPort> findLogicalSwitchPortsByUuid(final String logicalSwitchUuid, final String logicalSwitchPortUuid) throws NiciraNvpApiException {
        final String uri = SWITCH_URI_PREFIX + "/" + logicalSwitchUuid + "/lport";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("uuid", logicalSwitchPortUuid);
        params.put("fields", "uuid");

        NiciraNvpList<LogicalSwitchPort> lspl;
        try {
            lspl = restConnector.executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalSwitchPort>>() {
            }.getType(), uri, params);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }

        if (lspl == null) {
            throw new NiciraNvpApiException("Unexpected response from API");
        }

        return lspl;
    }

    public NiciraNvpList<LogicalRouterPort> findLogicalRouterPortsByUuid(final String logicalRouterUuid, final String logicalRouterPortUuid) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/lport";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("uuid", logicalRouterPortUuid);
        params.put("fields", "uuid");

        NiciraNvpList<LogicalRouterPort> lrpl;
        try {
            lrpl = restConnector.executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalRouterPort>>() {
            }.getType(), uri, params);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }

        if (lrpl == null) {
            throw new NiciraNvpApiException("Unexpected response from API");
        }

        return lrpl;
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
    public NiciraNvpList<LogicalRouter> findLogicalRouter() throws NiciraNvpApiException {
        return findLogicalRouter(null);
    }

    /**
     * GET list of {@link LogicalRouter} filtered by UUID
     *
     * @param uuid
     * @return
     * @throws NiciraNvpApiException
     */
    public NiciraNvpList<LogicalRouter> findLogicalRouter(final String uuid) throws NiciraNvpApiException {
        return find(uuid, LogicalRouter.class);
    }

    public LogicalRouter findOneLogicalRouterByUuid(final String logicalRouterUuid) throws NiciraNvpApiException {
        return findLogicalRouter(logicalRouterUuid).getResults().get(0);
    }

    public void updateLogicalRouter(final LogicalRouter logicalRouter,
            final String logicalRouterUuid)
                    throws NiciraNvpApiException {
        update(logicalRouter, logicalRouterUuid);
    }

    public void deleteLogicalRouter(final String logicalRouterUuid) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid;
        deleteWithUri(uri);
    }

    public LogicalRouterPort createLogicalRouterPort(final String logicalRouterUuid, final LogicalRouterPort logicalRouterPort) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/lport";
        return createWithUri(logicalRouterPort, uri);
    }

    public void deleteLogicalRouterPort(final String logicalRouterUuid, final String logicalRouterPortUuid) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/lport/" + logicalRouterPortUuid;
        deleteWithUri(uri);
    }

    public void updateLogicalRouterPort(final String logicalRouterUuid, final LogicalRouterPort logicalRouterPort) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/lport/" + logicalRouterPort.getUuid();
        updateWithUri(logicalRouterPort, uri);
    }

    public void updateLogicalRouterPortAttachment(final String logicalRouterUuid, final String logicalRouterPortUuid, final Attachment attachment)
            throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/lport/" + logicalRouterPortUuid + "/attachment";
        updateWithUri(attachment, uri);
    }

    public NatRule createLogicalRouterNatRule(final String logicalRouterUuid, final NatRule natRule) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/nat";
        return createWithUri(natRule, uri);
    }

    public void updateLogicalRouterNatRule(final String logicalRouterUuid, final NatRule natRule) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/nat/" + natRule.getUuid();
        updateWithUri(natRule, uri);
    }

    public void deleteLogicalRouterNatRule(final String logicalRouterUuid, final UUID natRuleUuid) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/nat/" + natRuleUuid.toString();
        deleteWithUri(uri);
    }

    public NiciraNvpList<LogicalRouterPort> findLogicalRouterPortByGatewayServiceAndVlanId(final String logicalRouterUuid, final String gatewayServiceUuid,
            final long vlanId) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/lport";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("attachment_gwsvc_uuid", gatewayServiceUuid);
        params.put("attachment_vlan", "0");
        params.put("fields", "*");

        try {
            return restConnector.executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalRouterPort>>() {
            }.getType(), uri, params);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    public NiciraNvpList<NatRule> findNatRulesByLogicalRouterUuid(final String logicalRouterUuid) throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/nat";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("fields", "*");

        try {
            return restConnector.executeRetrieveObject(new TypeToken<NiciraNvpList<NatRule>>() {
            }.getType(), uri, params);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    public NiciraNvpList<LogicalRouterPort> findLogicalRouterPortByGatewayServiceUuid(final String logicalRouterUuid, final String l3GatewayServiceUuid)
            throws NiciraNvpApiException {
        final String uri = ROUTER_URI_PREFIX + "/" + logicalRouterUuid + "/lport";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("fields", "*");
        params.put("attachment_gwsvc_uuid", l3GatewayServiceUuid);

        try {
            return restConnector.executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalRouterPort>>() {
            }.getType(), uri, params);
        } catch (final CloudstackRESTException e) {
            throw new NiciraNvpApiException(e);
        }
    }

    public static class NatRuleAdapter implements JsonDeserializer<NatRule> {

        @Override
        public NatRule deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Deserializing as a NatRule, but no type present in the json object");
            }

            final String natRuleType = jsonObject.get("type").getAsString();
            if ("SourceNatRule".equals(natRuleType)) {
                return context.deserialize(jsonElement, SourceNatRule.class);
            } else if ("DestinationNatRule".equals(natRuleType)) {
                return context.deserialize(jsonElement, DestinationNatRule.class);
            }

            throw new JsonParseException("Failed to deserialize type \"" + natRuleType + "\"");
        }
    }

    public static class RoutingConfigAdapter implements JsonDeserializer<RoutingConfig> {

        private static final String ROUTING_TABLE_ROUTING_CONFIG = "RoutingTableRoutingConfig";
        private static final String SINGLE_DEFAULT_ROUTE_IMPLICIT_ROUTING_CONFIG = "SingleDefaultRouteImplicitRoutingConfig";

        @Override
        public RoutingConfig deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (!jsonObject.has("type")) {
                throw new JsonParseException("Deserializing as a RoutingConfig, but no type present in the json object");
            }

            final String routingConfigType = jsonObject.get("type").getAsString();
            if (SINGLE_DEFAULT_ROUTE_IMPLICIT_ROUTING_CONFIG.equals(routingConfigType)) {
                return context.deserialize(jsonElement, SingleDefaultRouteImplicitRoutingConfig.class);
            } else if (ROUTING_TABLE_ROUTING_CONFIG.equals(routingConfigType)) {
                return context.deserialize(jsonElement, RoutingTableRoutingConfig.class);
            }

            throw new JsonParseException("Failed to deserialize type \"" + routingConfigType + "\"");
        }
    }
}
