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
package com.cloud.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.Security;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationManager;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.command.admin.router.ListRoutersCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.user.ListUsersCmd;
import org.apache.cloudstack.api.command.user.account.ListAccountsCmd;
import org.apache.cloudstack.api.command.user.account.ListProjectAccountsCmd;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.api.command.user.offering.ListDiskOfferingsCmd;
import org.apache.cloudstack.api.command.user.offering.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectInvitationsCmd;
import org.apache.cloudstack.api.command.user.project.ListProjectsCmd;
import org.apache.cloudstack.api.command.user.securitygroup.ListSecurityGroupsCmd;
import org.apache.cloudstack.api.command.user.tag.ListTagsCmd;
import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.apache.cloudstack.api.command.user.vmgroup.ListVMGroupsCmd;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesCmd;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.CreateCmdResponse;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageDispatcher;
import org.apache.cloudstack.framework.messagebus.MessageHandler;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.SocketHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;

import com.cloud.api.dispatch.DispatchChainFactory;
import com.cloud.api.dispatch.DispatchTask;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventCategory;
import com.cloud.event.EventTypes;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OriginDeniedException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.RequestLimitException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.UnavailableCommandException;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.storage.VolumeApiService;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.DomainManager;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.utils.ConstantTimeComparator;
import com.cloud.utils.DateUtil;
import com.cloud.utils.HttpUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.ReflectUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UUIDManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionProxyObject;
import com.cloud.utils.net.NetUtils;
import com.google.gson.reflect.TypeToken;

@Component
public class ApiServer extends ManagerBase implements HttpRequestHandler, ApiServerService, Configurable {
    private static final Logger s_logger = Logger.getLogger(ApiServer.class.getName());
    private static final Logger s_accessLogger = Logger.getLogger("apiserver." + ApiServer.class.getName());

    private static final String SANITIZATION_REGEX = "[\n\r]";

    private static boolean encodeApiResponse = false;

    /**
     * Non-printable ASCII characters - numbers 0 to 31 and 127 decimal
     */
    private static final String CONTROL_CHARACTERS = "[\000-\011\013-\014\016-\037\177]";

    @Inject
    private ApiDispatcher dispatcher;
    @Inject
    private DispatchChainFactory dispatchChainFactory;
    @Inject
    private AccountManager accountMgr;
    @Inject
    private DomainManager domainMgr;
    @Inject
    private DomainDao domainDao;
    @Inject
    private UUIDManager uuidMgr;
    @Inject
    private AsyncJobManager asyncMgr;
    @Inject
    private EntityManager entityMgr;
    @Inject
    private APIAuthenticationManager authManager;
    @Inject
    private ProjectDao projectDao;

    private List<PluggableService> pluggableServices;

    private List<APIChecker> apiAccessCheckers;

    @Inject
    private ApiAsyncJobDispatcher asyncDispatcher;

    private static int s_workerCount = 0;
    private static Map<String, List<Class<?>>> s_apiNameCmdClassMap = new HashMap<String, List<Class<?>>>();

    private static ExecutorService s_executor = new ThreadPoolExecutor(10, 150, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(
            "ApiServer"));

    @Inject
    private MessageBus messageBus;

    private static final ConfigKey<Integer> IntegrationAPIPort = new ConfigKey<Integer>("Advanced"
            , Integer.class
            , "integration.api.port"
            , "0"
            , "Integration (unauthenticated) API port. To disable set it to 0 or negative."
            , false
            , ConfigKey.Scope.Global);
    private static final ConfigKey<Long> ConcurrentSnapshotsThresholdPerHost = new ConfigKey<Long>("Advanced"
            , Long.class
            , "concurrent.snapshots.threshold.perhost"
            , null
            , "Limits number of snapshots that can be handled by the host concurrently; default is NULL - unlimited"
            , true // not sure if this is to be dynamic
            , ConfigKey.Scope.Global);
    private static final ConfigKey<Boolean> EncodeApiResponse = new ConfigKey<Boolean>("Advanced"
            , Boolean.class
            , "encode.api.response"
            , "false"
            , "Do URL encoding for the api response, false by default"
            , false
            , ConfigKey.Scope.Global);
    static final ConfigKey<String> JSONcontentType = new ConfigKey<String>( "Advanced"
            , String.class
            , "json.content.type"
            , "application/json; charset=UTF-8"
            , "Http response content type for .js files (default is text/javascript)"
            , false
            , ConfigKey.Scope.Global);
    static final ConfigKey<Boolean> EnableSecureSessionCookie = new ConfigKey<Boolean>("Advanced"
            , Boolean.class
            , "enable.secure.session.cookie"
            , "false"
            , "Session cookie is marked as secure if this is enabled. Secure cookies only work when HTTPS is used."
            , false
            , ConfigKey.Scope.Global);
    private static final ConfigKey<String> JSONDefaultContentType = new ConfigKey<String> ("Advanced"
            , String.class
            , "json.content.type"
            , "application/json; charset=UTF-8"
            , "Http response content type for JSON"
            , false
            , ConfigKey.Scope.Global);

    private static final ConfigKey<Boolean> UseEventAccountInfo = new ConfigKey<Boolean>( "advanced"
            , Boolean.class
            , "event.accountinfo"
            , "false"
            , "use account info in event logging"
            , true
            , ConfigKey.Scope.Global);

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        messageBus.subscribe(AsyncJob.Topics.JOB_EVENT_PUBLISH, MessageDispatcher.getDispatcher(this));
        return true;
    }

    @MessageHandler(topic = AsyncJob.Topics.JOB_EVENT_PUBLISH)
    public void handleAsyncJobPublishEvent(String subject, String senderAddress, Object args) {
        assert (args != null);

        @SuppressWarnings("unchecked")
        Pair<AsyncJob, String> eventInfo = (Pair<AsyncJob, String>)args;
        AsyncJob job = eventInfo.first();
        String jobEvent = eventInfo.second();

        if (s_logger.isTraceEnabled())
            s_logger.trace("Handle asyjob publish event " + jobEvent);

        EventBus eventBus = null;
        try {
            eventBus = ComponentContext.getComponent(EventBus.class);
        } catch (NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        if (!job.getDispatcher().equalsIgnoreCase("ApiAsyncJobDispatcher")) {
            return;
        }

        User userJobOwner = accountMgr.getUserIncludingRemoved(job.getUserId());
        Account jobOwner = accountMgr.getAccount(userJobOwner.getAccountId());

        // Get the event type from the cmdInfo json string
        String info = job.getCmdInfo();
        String cmdEventType = "unknown";
        Map<String, Object> cmdInfoObj = new HashMap<String, Object>();
        if (info != null) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> cmdInfo = ApiGsonHelper.getBuilder().create().fromJson(info, type);
            cmdInfoObj.putAll(cmdInfo);
            String eventTypeObj = cmdInfo.get("cmdEventType");
            if (eventTypeObj != null) {
                cmdEventType = eventTypeObj;

                if (s_logger.isDebugEnabled())
                    s_logger.debug("Retrieved cmdEventType from job info: " + cmdEventType);
            } else {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("Unable to locate cmdEventType marker in job info. publish as unknown event");
            }
            String contextDetails = cmdInfo.get("ctxDetails");
            if(contextDetails != null) {
                Type objectMapType = new TypeToken<Map<Object, Object>>() {}.getType();
                Map<Object, Object> ctxDetails = ApiGsonHelper.getBuilder().create().fromJson(contextDetails, objectMapType);
                cmdInfoObj.put("ctxDetails", ctxDetails);
            }
        }
        // For some reason, the instanceType / instanceId are not abstract, which means we may get null values.
        String instanceType = job.getInstanceType() != null ? job.getInstanceType() : "unknown";
        String instanceUuid = job.getInstanceId() != null ? ApiDBUtils.findJobInstanceUuid(job) : "";
        org.apache.cloudstack.framework.events.Event event = new org.apache.cloudstack.framework.events.Event("management-server", EventCategory.ASYNC_JOB_CHANGE_EVENT.getName(),
                jobEvent, instanceType, instanceUuid);

        Map<String, Object> eventDescription = new HashMap<String, Object>();
        eventDescription.put("command", job.getCmd());
        eventDescription.put("user", userJobOwner.getUuid());
        eventDescription.put("account", jobOwner.getUuid());
        eventDescription.put("processStatus", "" + job.getProcessStatus());
        eventDescription.put("resultCode", "" + job.getResultCode());
        eventDescription.put("instanceUuid", instanceUuid);
        eventDescription.put("instanceType", instanceType);
        eventDescription.put("commandEventType", cmdEventType);
        eventDescription.put("jobId", job.getUuid());
        eventDescription.put("jobResult", ApiSerializerHelper.fromSerializedStringToMap(job.getResult()));
        eventDescription.put("cmdInfo", cmdInfoObj);
        eventDescription.put("status", "" + job.getStatus());
        // If the event.accountinfo boolean value is set, get the human readable value for the username / domainname
        if (UseEventAccountInfo.value()) {
            DomainVO domain = domainDao.findById(jobOwner.getDomainId());
            eventDescription.put("username", userJobOwner.getUsername());
            eventDescription.put("accountname", jobOwner.getAccountName());
            eventDescription.put("domainname", domain.getName());
        }
        event.setDescription(eventDescription);

        try {
            eventBus.publish(event);
        } catch (EventBusException evx) {
            String errMsg = "Failed to publish async job event on the the event bus.";
            s_logger.warn(errMsg, evx);
        }
    }

    @Override
    public boolean start() {
        Security.addProvider(new BouncyCastleProvider());
        Integer apiPort = IntegrationAPIPort.value(); // api port, null by default

        final Long snapshotLimit = ConcurrentSnapshotsThresholdPerHost.value();
        if (snapshotLimit == null || snapshotLimit.longValue() <= 0) {
            s_logger.debug("Global concurrent snapshot config parameter " + ConcurrentSnapshotsThresholdPerHost.value() + " is less or equal 0; defaulting to unlimited");
        } else {
            dispatcher.setCreateSnapshotQueueSizeLimit(snapshotLimit);
        }

        final Long migrationLimit = VolumeApiService.ConcurrentMigrationsThresholdPerDatastore.value();
        if (migrationLimit == null || migrationLimit.longValue() <= 0) {
            s_logger.debug("Global concurrent migration config parameter " + VolumeApiService.ConcurrentMigrationsThresholdPerDatastore.value() + " is less or equal 0; defaulting to unlimited");
        } else {
            dispatcher.setMigrateQueueSizeLimit(migrationLimit);
        }

        final Set<Class<?>> cmdClasses = new HashSet<Class<?>>();
        for (final PluggableService pluggableService : pluggableServices) {
            cmdClasses.addAll(pluggableService.getCommands());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Discovered plugin " + pluggableService.getClass().getSimpleName());
            }
        }

        for (final Class<?> cmdClass : cmdClasses) {
            final APICommand at = cmdClass.getAnnotation(APICommand.class);
            if (at == null) {
                throw new CloudRuntimeException(String.format("%s is claimed as a API command, but it doesn't have @APICommand annotation", cmdClass.getName()));
            }

            String apiName = at.name();
            List<Class<?>> apiCmdList = s_apiNameCmdClassMap.get(apiName);
            if (apiCmdList == null) {
                apiCmdList = new ArrayList<Class<?>>();
                s_apiNameCmdClassMap.put(apiName, apiCmdList);
            }
            apiCmdList.add(cmdClass);

        }

        setEncodeApiResponse(EncodeApiResponse.value());

        if (apiPort != null) {
            final ListenerThread listenerThread = new ListenerThread(this, apiPort);
            listenerThread.start();
        }

        return true;
    }

    // NOTE: handle() only handles over the wire (OTW) requests from integration.api.port 8096
    // If integration api port is not configured, actual OTW requests will be received by ApiServlet
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException {

        // Create StringBuffer to log information in access log
        final StringBuilder sb = new StringBuilder();
        final HttpServerConnection connObj = (HttpServerConnection)context.getAttribute("http.connection");
        if (connObj instanceof SocketHttpServerConnection) {
            final InetAddress remoteAddr = ((SocketHttpServerConnection)connObj).getRemoteAddress();
            sb.append(remoteAddr.toString() + " -- ");
        }
        sb.append(StringUtils.cleanString(request.getRequestLine().toString()));

        try {
            List<NameValuePair> paramList = null;
            try {
                paramList = URLEncodedUtils.parse(new URI(request.getRequestLine().getUri()), HttpUtils.UTF_8);
            } catch (final URISyntaxException e) {
                s_logger.error("Error parsing url request", e);
            }

            // Use Multimap as the parameter map should be in the form (name=String, value=String[])
            // So parameter values are stored in a list for the same name key
            // APITODO: Use Guava's (import com.google.common.collect.Multimap;)
            // (Immutable)Multimap<String, String> paramMultiMap = HashMultimap.create();
            // Map<String, Collection<String>> parameterMap = paramMultiMap.asMap();
            final Map parameterMap = new HashMap<String, String[]>();
            String responseType = HttpUtils.RESPONSE_TYPE_XML;
            if(paramList != null) {
                for (final NameValuePair param : paramList) {
                    if (param.getName().equalsIgnoreCase("response")) {
                        responseType = param.getValue();
                        continue;
                    }
                    if(parameterMap.putIfAbsent(param.getName(), new String[]{param.getValue()}) != null) {
                        String message = String.format("Query parameter '%s' has multiple values [%s, %s]. Only the last value will be respected." +
                            "It is advised to pass only a single parameter", param.getName(), param.getValue(), parameterMap.get(param.getName()));
                        s_logger.warn(message);
                    }
                }
            }

            // Get the type of http method being used.
            parameterMap.put("httpmethod", new String[] {request.getRequestLine().getMethod()});

            // Check responseType, if not among valid types, fallback to JSON
            if (!(responseType.equals(HttpUtils.RESPONSE_TYPE_JSON) || responseType.equals(HttpUtils.RESPONSE_TYPE_XML))) {
                responseType = HttpUtils.RESPONSE_TYPE_XML;
            }
            try {
                //verify that parameter is legit for passing via admin port
                String[] command = (String[]) parameterMap.get("command");
                if (command != null) {
                    Class<?> cmdClass = getCmdClass(command[0]);
                    if (cmdClass != null) {
                        List<Field> fields = ReflectUtil.getAllFieldsForClass(cmdClass, BaseCmd.class);
                        for (Field field : fields) {
                            Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
                            if ((parameterAnnotation == null) || !parameterAnnotation.expose()) {
                                continue;
                            }
                            Object paramObj = parameterMap.get(parameterAnnotation.name());
                            if (paramObj != null) {
                                if (!parameterAnnotation.acceptedOnAdminPort()) {
                                    throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, "Parameter " + parameterAnnotation.name() + " can't be passed through the API integration port");
                                }
                            }
                        }
                    }
                }
                // always trust commands from API port, user context will always be UID_SYSTEM/ACCOUNT_ID_SYSTEM
                CallContext.register(accountMgr.getSystemUser(), accountMgr.getSystemAccount());
                sb.insert(0, "(userId=" + User.UID_SYSTEM + " accountId=" + Account.ACCOUNT_ID_SYSTEM + " sessionId=" + null + ") ");
                final String responseText = handleRequest(parameterMap, responseType, sb);
                sb.append(" 200 " + ((responseText == null) ? 0 : responseText.length()));

                writeResponse(response, responseText, HttpStatus.SC_OK, responseType, null);
            } catch (final ServerApiException se) {
                final String responseText = getSerializedApiError(se, parameterMap, responseType);
                writeResponse(response, responseText, se.getErrorCode().getHttpCode(), responseType, se.getDescription());
                sb.append(" " + se.getErrorCode() + " " + se.getDescription());
            } catch (final RuntimeException e) {
                // log runtime exception like NullPointerException to help identify the source easier
                s_logger.error("Unhandled exception, ", e);
                throw e;
            }
        } finally {
            s_accessLogger.info(sb.toString());
            CallContext.unregister();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void checkCharacterInkParams(final Map params) {
        final Map<String, String> stringMap = new HashMap<String, String>();
        final Set keys = params.keySet();
        final Iterator keysIter = keys.iterator();
        while (keysIter.hasNext()) {
            final String key = (String)keysIter.next();
            final String[] value = (String[])params.get(key);
            // fail if parameter value contains ASCII control (non-printable) characters
            if (value[0] != null && !ApiConstants.ACTIVATION_RULE.equals(key)) {
                final Pattern pattern = Pattern.compile(CONTROL_CHARACTERS);
                final Matcher matcher = pattern.matcher(value[0]);
                if (matcher.find()) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Received value containing illegal ASCII non-printable characters for parameter " + key);
                }
            }
            stringMap.put(key, value[0]);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public String handleRequest(final Map params, final String responseType, final StringBuilder auditTrailSb) throws ServerApiException {
        checkCharacterInkParams(params);

        String response = null;
        String[] command = null;

        try {
            command = (String[])params.get("command");
            if (command == null) {
                s_logger.error("invalid request, no command sent");
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("dumping request parameters");
                    for (final  Object key : params.keySet()) {
                        final String keyStr = (String)key;
                        final String[] value = (String[])params.get(key);
                        s_logger.trace("   key: " + keyStr + ", value: " + ((value == null) ? "'null'" : value[0]));
                    }
                }
                throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR, "Invalid request, no command sent");
            } else {
                // Don't allow Login/Logout APIs to go past this point
                if (authManager.getAPIAuthenticator(command[0]) != null) {
                    return null;
                }
                final Map<String, String> paramMap = new HashMap<String, String>();
                final Set keys = params.keySet();
                final Iterator keysIter = keys.iterator();
                while (keysIter.hasNext()) {
                    final String key = (String)keysIter.next();
                    if ("command".equalsIgnoreCase(key)) {
                        continue;
                    }
                    final String[] value = (String[])params.get(key);
                    paramMap.put(key, value[0]);
                }

                Class<?> cmdClass = getCmdClass(command[0]);
                if (cmdClass != null) {
                    APICommand annotation = cmdClass.getAnnotation(APICommand.class);
                    if (annotation == null) {
                        s_logger.error("No APICommand annotation found for class " + cmdClass.getCanonicalName());
                        throw new CloudRuntimeException("No APICommand annotation found for class " + cmdClass.getCanonicalName());
                    }

                    BaseCmd cmdObj = (BaseCmd)cmdClass.newInstance();
                    cmdObj = ComponentContext.inject(cmdObj);
                    cmdObj.configure();
                    cmdObj.setFullUrlParams(paramMap);
                    cmdObj.setResponseType(responseType);
                    cmdObj.setHttpMethod(paramMap.get(ApiConstants.HTTPMETHOD).toString());

                    // This is where the command is either serialized, or directly dispatched
                    StringBuilder log = new StringBuilder();
                    response = queueCommand(cmdObj, paramMap, log);
                    buildAuditTrail(auditTrailSb, command[0], log.toString());
                } else {
                    final String errorString = "Unknown API command: " + command[0];
                    s_logger.warn(errorString);
                    auditTrailSb.append(" " + errorString);
                    throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR, errorString);
                }
            }
        } catch (final InvalidParameterValueException ex) {
            s_logger.info(ex.getMessage());
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, ex.getMessage(), ex);
        } catch (final IllegalArgumentException ex) {
            s_logger.info(ex.getMessage());
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, ex.getMessage(), ex);
        } catch (final PermissionDeniedException ex) {
            final ArrayList<ExceptionProxyObject> idList = ex.getIdProxyList();
            if (idList != null) {
                final StringBuffer buf = new StringBuffer();
                for (final ExceptionProxyObject obj : idList) {
                    buf.append(obj.getDescription());
                    buf.append(":");
                    buf.append(obj.getUuid());
                    buf.append(" ");
                }
                s_logger.info("PermissionDenied: " + ex.getMessage() + " on objs: [" + buf.toString() + "]");
            } else {
                s_logger.info("PermissionDenied: " + ex.getMessage());
            }
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, ex.getMessage(), ex);
        } catch (final AccountLimitException ex) {
            s_logger.info(ex.getMessage());
            throw new ServerApiException(ApiErrorCode.ACCOUNT_RESOURCE_LIMIT_ERROR, ex.getMessage(), ex);
        } catch (final InsufficientCapacityException ex) {
            s_logger.info(ex.getMessage());
            String errorMsg = ex.getMessage();
            if (!accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())) {
                // hide internal details to non-admin user for security reason
                errorMsg = BaseCmd.USER_ERROR_MESSAGE;
            }
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, errorMsg, ex);
        } catch (final ResourceAllocationException ex) {
            s_logger.info(ex.getMessage());
            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage(), ex);
        } catch (final ResourceUnavailableException ex) {
            s_logger.info(ex.getMessage());
            String errorMsg = ex.getMessage();
            if (!accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())) {
                // hide internal details to non-admin user for security reason
                errorMsg = BaseCmd.USER_ERROR_MESSAGE;
            }
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, errorMsg, ex);
        } catch (final ServerApiException ex) {
            s_logger.info(ex.getDescription());
            throw ex;
        } catch (final Exception ex) {
            s_logger.error("unhandled exception executing api command: " + ((command == null) ? "null" : command), ex);
            String errorMsg = ex.getMessage();
            if (!accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())) {
                // hide internal details to non-admin user for security reason
                errorMsg = BaseCmd.USER_ERROR_MESSAGE;
            }
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, errorMsg, ex);
        }

        return response;
    }

    private String getBaseAsyncResponse(final long jobId, final BaseAsyncCmd cmd) {
        final AsyncJobResponse response = new AsyncJobResponse();

        final AsyncJob job = entityMgr.findByIdIncludingRemoved(AsyncJob.class, jobId);
        response.setJobId(job.getUuid());
        response.setResponseName(cmd.getCommandName());
        return ApiResponseSerializer.toSerializedString(response, cmd.getResponseType());
    }

    private String getBaseAsyncCreateResponse(final long jobId, final BaseAsyncCreateCmd cmd, final String objectUuid) {
        final CreateCmdResponse response = new CreateCmdResponse();
        final AsyncJob job = entityMgr.findByIdIncludingRemoved(AsyncJob.class, jobId);
        response.setJobId(job.getUuid());
        response.setId(objectUuid);
        response.setResponseName(cmd.getCommandName());
        return ApiResponseSerializer.toSerializedString(response, cmd.getResponseType());
    }

    private String queueCommand(final BaseCmd cmdObj, final Map<String, String> params, StringBuilder log) throws Exception {
        final CallContext ctx = CallContext.current();
        final Long callerUserId = ctx.getCallingUserId();
        final Account caller = ctx.getCallingAccount();

        // Queue command based on Cmd super class:
        // BaseCmd: cmd is dispatched to ApiDispatcher, executed, serialized and returned.
        // BaseAsyncCreateCmd: cmd params are processed and create() is called, then same workflow as BaseAsyncCmd.
        // BaseAsyncCmd: cmd is processed and submitted as an AsyncJob, job related info is serialized and returned.
        if (cmdObj instanceof BaseAsyncCmd) {
            Long objectId = null;
            String objectUuid = null;
            if (cmdObj instanceof BaseAsyncCreateCmd) {
                final BaseAsyncCreateCmd createCmd = (BaseAsyncCreateCmd)cmdObj;
                dispatcher.dispatchCreateCmd(createCmd, params);
                objectId = createCmd.getEntityId();
                objectUuid = createCmd.getEntityUuid();
                params.put("id", objectId.toString());
                Class entityClass = EventTypes.getEntityClassForEvent(createCmd.getEventType());
                if (entityClass != null)
                    ctx.putContextParameter(entityClass, objectUuid);
            } else {
                // Extract the uuid before params are processed and id reflects internal db id
                objectUuid = params.get(ApiConstants.ID);
                dispatchChainFactory.getStandardDispatchChain().dispatch(new DispatchTask(cmdObj, params));
            }

            final BaseAsyncCmd asyncCmd = (BaseAsyncCmd)cmdObj;

            if (callerUserId != null) {
                params.put("ctxUserId", callerUserId.toString());
            }
            if (caller != null) {
                params.put("ctxAccountId", String.valueOf(caller.getId()));
            }
            if (objectUuid != null) {
                params.put("uuid", objectUuid);
            }

            long startEventId = ctx.getStartEventId();
            asyncCmd.setStartEventId(startEventId);

            // save the scheduled event
            final Long eventId =
                    ActionEventUtils.onScheduledActionEvent((callerUserId == null) ? (Long)User.UID_SYSTEM : callerUserId, asyncCmd.getEntityOwnerId(), asyncCmd.getEventType(),
                            asyncCmd.getEventDescription(), asyncCmd.getApiResourceId(), asyncCmd.getApiResourceType().toString(), asyncCmd.isDisplay(), startEventId);
            if (startEventId == 0) {
                // There was no create event before, set current event id as start eventId
                startEventId = eventId;
            }

            params.put("ctxStartEventId", String.valueOf(startEventId));
            params.put("cmdEventType", asyncCmd.getEventType().toString());
            params.put("ctxDetails", ApiGsonHelper.getBuilder().create().toJson(ctx.getContextParameters()));

            Long instanceId = (objectId == null) ? asyncCmd.getApiResourceId() : objectId;

            // users can provide the job id they want to use, so log as it is a uuid and is unique
            String injectedJobId = asyncCmd.getInjectedJobId();
            uuidMgr.checkUuidSimple(injectedJobId, AsyncJob.class);

            AsyncJobVO job = new AsyncJobVO("", callerUserId, caller.getId(), cmdObj.getClass().getName(),
                    ApiGsonHelper.getBuilder().create().toJson(params), instanceId,
                    asyncCmd.getApiResourceType() != null ? asyncCmd.getApiResourceType().toString() : null,
                            injectedJobId);
            job.setDispatcher(asyncDispatcher.getName());

            final long jobId = asyncMgr.submitAsyncJob(job);

            if (jobId == 0L) {
                final String errorMsg = "Unable to schedule async job for command " + job.getCmd();
                s_logger.warn(errorMsg);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, errorMsg);
            }
            final String response;
            if (objectId != null) {
                final String objUuid = (objectUuid == null) ? objectId.toString() : objectUuid;
                response = getBaseAsyncCreateResponse(jobId, (BaseAsyncCreateCmd)asyncCmd, objUuid);
            } else {
                SerializationContext.current().setUuidTranslation(true);
                response = getBaseAsyncResponse(jobId, asyncCmd);
            }
            // Always log response for async for now, I don't think any sensitive data will be in here.
            // It might be nice to send this through scrubbing similar to how
            // ApiResponseSerializer.toSerializedStringWithSecureLogs works. For now, this gets jobid's
            // in the api logs.
            log.append(response);
            return response;

        } else {
            dispatcher.dispatch(cmdObj, params, false);

            // if the command is of the listXXXCommand, we will need to also return the
            // the job id and status if possible
            // For those listXXXCommand which we have already created DB views, this step is not needed since async job is joined in their db views.
            if (cmdObj instanceof BaseListCmd && !(cmdObj instanceof ListVMsCmd) && !(cmdObj instanceof ListRoutersCmd)
                    && !(cmdObj instanceof ListSecurityGroupsCmd) &&
                    !(cmdObj instanceof ListTagsCmd) && !(cmdObj instanceof ListEventsCmd) && !(cmdObj instanceof ListVMGroupsCmd) && !(cmdObj instanceof ListProjectsCmd) &&
                    !(cmdObj instanceof ListProjectAccountsCmd) && !(cmdObj instanceof ListProjectInvitationsCmd) && !(cmdObj instanceof ListHostsCmd) &&
                    !(cmdObj instanceof ListVolumesCmd) && !(cmdObj instanceof ListUsersCmd) && !(cmdObj instanceof ListAccountsCmd)
                    && !(cmdObj instanceof ListStoragePoolsCmd) && !(cmdObj instanceof ListDiskOfferingsCmd) && !(cmdObj instanceof ListServiceOfferingsCmd) &&
                    !(cmdObj instanceof ListZonesCmd)) {
                buildAsyncListResponse((BaseListCmd)cmdObj, caller);
            }

            SerializationContext.current().setUuidTranslation(true);
            return ApiResponseSerializer.toSerializedStringWithSecureLogs((ResponseObject)cmdObj.getResponseObject(), cmdObj.getResponseType(), log);
        }
    }

    @SuppressWarnings("unchecked")
    private void buildAsyncListResponse(final BaseListCmd command, final Account account) {
        final List<ResponseObject> responses = ((ListResponse)command.getResponseObject()).getResponses();
        if (responses != null && responses.size() > 0) {
            List<? extends AsyncJob> jobs = null;

            // list all jobs for ROOT admin
            if (accountMgr.isRootAdmin(account.getId())) {
                jobs = asyncMgr.findInstancePendingAsyncJobs(command.getApiResourceType().toString(), null);
            } else {
                jobs = asyncMgr.findInstancePendingAsyncJobs(command.getApiResourceType().toString(), account.getId());
            }

            if (jobs.size() == 0) {
                return;
            }

            final Map<String, AsyncJob> objectJobMap = new HashMap<String, AsyncJob>();
            for (final AsyncJob job : jobs) {
                if (job.getInstanceId() == null) {
                    continue;
                }
                final String instanceUuid = ApiDBUtils.findJobInstanceUuid(job);
                objectJobMap.put(instanceUuid, job);
            }

            for (final ResponseObject response : responses) {
                if (response.getObjectId() != null && objectJobMap.containsKey(response.getObjectId())) {
                    final AsyncJob job = objectJobMap.get(response.getObjectId());
                    response.setJobId(job.getUuid());
                    response.setJobStatus(job.getStatus().ordinal());
                }
            }
        }
    }

    private void buildAuditTrail(final StringBuilder auditTrailSb, final String command, final String result) {
        if (result == null) {
            return;
        }
        auditTrailSb.append(" " + HttpServletResponse.SC_OK + " ");
        if (command.equals("createSSHKeyPair")) {
            auditTrailSb.append("This result was not logged because it contains sensitive data.");
        } else {
            auditTrailSb.append(result);
        }
    }

    @Override
    public boolean verifyRequest(final Map<String, Object[]> requestParameters, final Long userId, InetAddress remoteAddress) throws ServerApiException {
        try {
            String apiKey = null;
            String secretKey = null;
            String signature = null;
            String unsignedRequest = null;

            final String[] command = (String[])requestParameters.get(ApiConstants.COMMAND);
            if (command == null) {
                s_logger.info("missing command, ignoring request...");
                return false;
            }

            final String commandName = command[0];

            // if userId not null, that mean that user is logged in
            if (userId != null) {
                final User user = ApiDBUtils.findUserById(userId);
                return commandAvailable(remoteAddress, commandName, user);
            } else {
                // check against every available command to see if the command exists or not
                if (!s_apiNameCmdClassMap.containsKey(commandName) && !commandName.equals("login") && !commandName.equals("logout")) {
                    final String errorMessage = "The given command " + commandName + " either does not exist, is not available" +
                            " for user, or not available from ip address '" + remoteAddress.getHostAddress() + "'.";
                    s_logger.debug(errorMessage);
                    return false;
                }
            }

            // - build a request string with sorted params, make sure it's all lowercase
            // - sign the request, verify the signature is the same
            final List<String> parameterNames = new ArrayList<String>();

            for (final Object paramNameObj : requestParameters.keySet()) {
                parameterNames.add((String)paramNameObj); // put the name in a list that we'll sort later
            }

            Collections.sort(parameterNames);

            String signatureVersion = null;
            String expires = null;

            for (final String paramName : parameterNames) {
                // parameters come as name/value pairs in the form String/String[]
                final String paramValue = ((String[])requestParameters.get(paramName))[0];

                if (ApiConstants.SIGNATURE.equalsIgnoreCase(paramName)) {
                    signature = paramValue;
                } else {
                    if (ApiConstants.API_KEY.equalsIgnoreCase(paramName)) {
                        apiKey = paramValue;
                    } else if (ApiConstants.SIGNATURE_VERSION.equalsIgnoreCase(paramName)) {
                        signatureVersion = paramValue;
                    } else if (ApiConstants.EXPIRES.equalsIgnoreCase(paramName)) {
                        expires = paramValue;
                    }

                    if (unsignedRequest == null) {
                        unsignedRequest = paramName + "=" + URLEncoder.encode(paramValue, HttpUtils.UTF_8).replaceAll("\\+", "%20");
                    } else {
                        unsignedRequest = unsignedRequest + "&" + paramName + "=" + URLEncoder.encode(paramValue, HttpUtils.UTF_8).replaceAll("\\+", "%20");
                    }
                }
            }

            // if api/secret key are passed to the parameters
            if ((signature == null) || (apiKey == null)) {
                s_logger.debug("Expired session, missing signature, or missing apiKey -- ignoring request. Signature: " + signature + ", apiKey: " + apiKey);
                return false; // no signature, bad request
            }

            Date expiresTS = null;
            // FIXME: Hard coded signature, why not have an enum
            if ("3".equals(signatureVersion)) {
                // New signature authentication. Check for expire parameter and its validity
                if (expires == null) {
                    s_logger.debug("Missing Expires parameter -- ignoring request.");
                    return false;
                }

                try {
                    expiresTS = DateUtil.parseTZDateString(expires);
                } catch (final ParseException pe) {
                    s_logger.debug("Incorrect date format for Expires parameter", pe);
                    return false;
                }

                final Date now = new Date(System.currentTimeMillis());
                if (expiresTS.before(now)) {
                    signature = signature.replaceAll(SANITIZATION_REGEX, "_");
                    apiKey = apiKey.replaceAll(SANITIZATION_REGEX, "_");
                    s_logger.debug(String.format("Request expired -- ignoring ...sig [%s], apiKey [%s].", signature, apiKey));
                    return false;
                }
            }

            final TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
            User user = null;
            // verify there is a user with this api key
            final Pair<User, Account> userAcctPair = accountMgr.findUserByApiKey(apiKey);
            if (userAcctPair == null) {
                s_logger.debug("apiKey does not map to a valid user -- ignoring request, apiKey: " + apiKey);
                return false;
            }

            user = userAcctPair.first();
            final Account account = userAcctPair.second();

            if (user.getState() != Account.State.ENABLED || !account.getState().equals(Account.State.ENABLED)) {
                s_logger.info("disabled or locked user accessing the api, userid = " + user.getId() + "; name = " + user.getUsername() + "; state: " + user.getState() +
                        "; accountState: " + account.getState());
                return false;
            }

            if (!commandAvailable(remoteAddress, commandName, user)) {
                return false;
            }

            // verify secret key exists
            secretKey = user.getSecretKey();
            if (secretKey == null) {
                s_logger.info("User does not have a secret key associated with the account -- ignoring request, username: " + user.getUsername());
                return false;
            }

            unsignedRequest = unsignedRequest.toLowerCase();

            final Mac mac = Mac.getInstance("HmacSHA1");
            final SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
            mac.init(keySpec);
            mac.update(unsignedRequest.getBytes());

            final byte[] encryptedBytes = mac.doFinal();
            final String computedSignature = Base64.encodeBase64String(encryptedBytes);
            final boolean equalSig = ConstantTimeComparator.compareStrings(signature, computedSignature);

            if (!equalSig) {
                signature = signature.replaceAll(SANITIZATION_REGEX, "_");
                s_logger.info(String.format("User signature [%s] is not equaled to computed signature [%s].", signature, computedSignature));
            } else {
                CallContext.register(user, account);
            }
            return equalSig;
        } catch (final ServerApiException ex) {
            throw ex;
        } catch (final Exception ex) {
            s_logger.error("unable to verify request signature");
        }
        return false;
    }

    private boolean commandAvailable(final InetAddress remoteAddress, final String commandName, final User user) {
        try {
            checkCommandAvailable(user, commandName, remoteAddress);
        } catch (final RequestLimitException ex) {
            s_logger.debug(ex.getMessage());
            throw new ServerApiException(ApiErrorCode.API_LIMIT_EXCEED, ex.getMessage());
        }  catch (final UnavailableCommandException ex) {
            s_logger.debug(ex.getMessage());
            throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR, ex.getMessage());
        } catch (final PermissionDeniedException ex) {
            final String errorMessage = "The given command '" + commandName + "' either does not exist, is not available" +
                    " for user.";
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED , errorMessage);
        } catch (final OriginDeniedException ex) {
            // in this case we can remove the session with extreme prejudice
            final String errorMessage = "The user '" + user.getUsername() + "' is not allowed to execute commands from ip address '" + remoteAddress.getHostName() + "'.";
            s_logger.debug(errorMessage);
            return false;
        }
        return true;
    }

    @Override
    public Long fetchDomainId(final String domainUUID) {
        final Domain domain = domainMgr.getDomain(domainUUID);
        if (domain != null)
            return domain.getId();
        else
            return null;
    }

    private ResponseObject createLoginResponse(HttpSession session) {
        LoginCmdResponse response = new LoginCmdResponse();
        response.setTimeout(session.getMaxInactiveInterval());

        final String user_UUID = (String)session.getAttribute("user_UUID");
        response.setUserId(user_UUID);

        final String domain_UUID = (String)session.getAttribute("domain_UUID");
        response.setDomainId(domain_UUID);

        synchronized (session) {
            session.removeAttribute("user_UUID");
            session.removeAttribute("domain_UUID");
        }

        final Enumeration attrNames = session.getAttributeNames();
        if (attrNames != null) {
            while (attrNames.hasMoreElements()) {
                final String attrName = (String) attrNames.nextElement();
                final Object attrObj = session.getAttribute(attrName);
                if (ApiConstants.USERNAME.equalsIgnoreCase(attrName)) {
                    response.setUsername(attrObj.toString());
                }
                if (ApiConstants.ACCOUNT.equalsIgnoreCase(attrName)) {
                    response.setAccount(attrObj.toString());
                }
                if (ApiConstants.FIRSTNAME.equalsIgnoreCase(attrName)) {
                    response.setFirstName(attrObj.toString());
                }
                if (ApiConstants.LASTNAME.equalsIgnoreCase(attrName)) {
                    response.setLastName(attrObj.toString());
                }
                if (ApiConstants.TYPE.equalsIgnoreCase(attrName)) {
                    response.setType((attrObj.toString()));
                }
                if (ApiConstants.TIMEZONE.equalsIgnoreCase(attrName)) {
                    response.setTimeZone(attrObj.toString());
                }
                if (ApiConstants.TIMEZONEOFFSET.equalsIgnoreCase(attrName)) {
                    response.setTimeZoneOffset(attrObj.toString());
                }
                if (ApiConstants.REGISTERED.equalsIgnoreCase(attrName)) {
                    response.setRegistered(attrObj.toString());
                }
                if (ApiConstants.SESSIONKEY.equalsIgnoreCase(attrName)) {
                    response.setSessionKey(attrObj.toString());
                }
                if (ApiConstants.IS_2FA_ENABLED.equalsIgnoreCase(attrName)) {
                    response.set2FAenabled(attrObj.toString());
                }
                if (ApiConstants.IS_2FA_VERIFIED.equalsIgnoreCase(attrName)) {
                    response.set2FAverfied(attrObj.toString());
                }
                if (ApiConstants.PROVIDER_FOR_2FA.equalsIgnoreCase(attrName)) {
                    response.setProviderFor2FA(attrObj.toString());
                }
                if (ApiConstants.ISSUER_FOR_2FA.equalsIgnoreCase(attrName)) {
                    response.setIssuerFor2FA(attrObj.toString());
                }
            }
        }
        response.setResponseName("loginresponse");
        return response;
    }

    @Override
    public ResponseObject loginUser(final HttpSession session, final String username, final String password, Long domainId, final String domainPath, final InetAddress loginIpAddress,
            final Map<String, Object[]> requestParameters) throws CloudAuthenticationException {
        // We will always use domainId first. If that does not exist, we will use domain name. If THAT doesn't exist
        // we will default to ROOT
        final Domain userDomain = domainMgr.findDomainByIdOrPath(domainId, domainPath);
        if (userDomain == null || userDomain.getId() < 1L) {
            throw new CloudAuthenticationException("Unable to find the domain from the path " + domainPath);
        } else {
            domainId = userDomain.getId();
        }

        final UserAccount userAcct = accountMgr.authenticateUser(username, password, domainId, loginIpAddress, requestParameters);
        if (userAcct != null) {
            final String timezone = userAcct.getTimezone();
            float offsetInHrs = 0f;
            if (timezone != null) {
                final TimeZone t = TimeZone.getTimeZone(timezone);
                s_logger.info("Current user logged in under " + timezone + " timezone");

                final java.util.Date date = new java.util.Date();
                final long longDate = date.getTime();
                final float offsetInMs = (t.getOffset(longDate));
                offsetInHrs = offsetInMs / (1000 * 60 * 60);
                s_logger.info("Timezone offset from UTC is: " + offsetInHrs);
            }

            final Account account = accountMgr.getAccount(userAcct.getAccountId());

            // set the userId and account object for everyone
            session.setAttribute("userid", userAcct.getId());
            final UserVO user = (UserVO)accountMgr.getActiveUser(userAcct.getId());
            if (user.getUuid() != null) {
                session.setAttribute("user_UUID", user.getUuid());
            }

            session.setAttribute("username", userAcct.getUsername());
            session.setAttribute("firstname", userAcct.getFirstname());
            session.setAttribute("lastname", userAcct.getLastname());
            session.setAttribute("accountobj", account);
            session.setAttribute("account", account.getAccountName());

            session.setAttribute("domainid", account.getDomainId());
            final DomainVO domain = (DomainVO)domainMgr.getDomain(account.getDomainId());
            if (domain.getUuid() != null) {
                session.setAttribute("domain_UUID", domain.getUuid());
            }

            session.setAttribute("type", account.getType().ordinal());
            session.setAttribute("registrationtoken", userAcct.getRegistrationToken());
            session.setAttribute("registered", Boolean.toString(userAcct.isRegistered()));

            if (timezone != null) {
                session.setAttribute("timezone", timezone);
                session.setAttribute("timezoneoffset", Float.valueOf(offsetInHrs).toString());
            }

            boolean is2faEnabled = false;
            if (userAcct.isUser2faEnabled() || (Boolean.TRUE.equals(AccountManagerImpl.enableUserTwoFactorAuthentication.valueIn(userAcct.getDomainId())) && Boolean.TRUE.equals(AccountManagerImpl.mandateUserTwoFactorAuthentication.valueIn(userAcct.getDomainId())))) {
                is2faEnabled = true;
            }
            String issuerFor2FA = AccountManagerImpl.userTwoFactorAuthenticationIssuer.valueIn(userAcct.getDomainId());
            session.setAttribute(ApiConstants.IS_2FA_ENABLED, Boolean.toString(is2faEnabled));
            if (!is2faEnabled) {
                session.setAttribute(ApiConstants.IS_2FA_VERIFIED, true);
            } else {
                session.setAttribute(ApiConstants.IS_2FA_VERIFIED, false);
            }
            session.setAttribute(ApiConstants.PROVIDER_FOR_2FA, userAcct.getUser2faProvider());
            session.setAttribute(ApiConstants.ISSUER_FOR_2FA, issuerFor2FA);

            // (bug 5483) generate a session key that the user must submit on every request to prevent CSRF, add that
            // to the login response so that session-based authenticators know to send the key back
            final SecureRandom sesssionKeyRandom = new SecureRandom();
            final byte sessionKeyBytes[] = new byte[20];
            sesssionKeyRandom.nextBytes(sessionKeyBytes);
            final String sessionKey = Base64.encodeBase64URLSafeString(sessionKeyBytes);
            session.setAttribute(ApiConstants.SESSIONKEY, sessionKey);

            return createLoginResponse(session);
        }
        throw new CloudAuthenticationException("Failed to authenticate user " + username + " in domain " + domainId + "; please provide valid credentials");
    }

    @Override
    public void logoutUser(final long userId) {
        accountMgr.logoutUser(userId);
        return;
    }

    @Override
    public boolean verifyUser(final Long userId) {
        final User user = accountMgr.getUserIncludingRemoved(userId);
        Account account = null;
        if (user != null) {
            account = accountMgr.getAccount(user.getAccountId());
        }

        if ((user == null) || (user.getRemoved() != null) || !user.getState().equals(Account.State.ENABLED) || (account == null) ||
                !account.getState().equals(Account.State.ENABLED)) {
            s_logger.warn("Deleted/Disabled/Locked user with id=" + userId + " attempting to access public API");
            return false;
        }
        return true;
    }

    private void checkCommandAvailable(final User user, final String commandName, final InetAddress remoteAddress) throws PermissionDeniedException {
        if (user == null) {
            throw new PermissionDeniedException("User is null for role based API access check for command" + commandName);
        }

        final Account account = accountMgr.getAccount(user.getAccountId());
        final String accessAllowedCidrs = ApiServiceConfiguration.ApiAllowedSourceCidrList.valueIn(account.getId()).replaceAll("\\s","");
        final Boolean apiSourceCidrChecksEnabled = ApiServiceConfiguration.ApiSourceCidrChecksEnabled.value();

        if (apiSourceCidrChecksEnabled) {
            s_logger.debug("CIDRs from which account '" + account.toString() + "' is allowed to perform API calls: " + accessAllowedCidrs);
            if (!NetUtils.isIpInCidrList(remoteAddress, accessAllowedCidrs.split(","))) {
                s_logger.warn("Request by account '" + account.toString() + "' was denied since " + remoteAddress + " does not match " + accessAllowedCidrs);
                throw new OriginDeniedException("Calls from disallowed origin", account, remoteAddress);
                }
        }


        for (final APIChecker apiChecker : apiAccessCheckers) {
            apiChecker.checkAccess(user, commandName);
        }
    }

    @Override
    public Class<?> getCmdClass(String cmdName) {
        List<Class<?>> cmdList = s_apiNameCmdClassMap.get(cmdName);
        if (cmdList == null || cmdList.size() == 0)
            return null;
        else if (cmdList.size() == 1)
            return cmdList.get(0);
        else {
            // determine the cmd class based on calling context
            ResponseView view = ResponseView.Restricted;
            if (CallContext.current() != null
                    && accountMgr.isRootAdmin(CallContext.current().getCallingAccount().getId())) {
                view = ResponseView.Full;
            }
            for (Class<?> cmdClass : cmdList) {
                APICommand at = cmdClass.getAnnotation(APICommand.class);
                if (at == null) {
                    throw new CloudRuntimeException(String.format("%s is claimed as a API command, but it doesn't have @APICommand annotation", cmdClass.getName()));
                }
                if (at.responseView() == null) {
                    throw new CloudRuntimeException(String.format(
                            "%s @APICommand annotation should specify responseView attribute to distinguish multiple command classes for a single api name", cmdClass.getName()));
                } else if (at.responseView() == view) {
                    return cmdClass;
                }
            }
            return null;
        }
    }

    // FIXME: rather than isError, we might was to pass in the status code to give more flexibility
    private void writeResponse(final HttpResponse resp, final String responseText, final int statusCode, final String responseType, final String reasonPhrase) {
        try {
            resp.setStatusCode(statusCode);
            resp.setReasonPhrase(reasonPhrase);

            final BasicHttpEntity body = new BasicHttpEntity();
            if (HttpUtils.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                // JSON response
                body.setContentType(JSONcontentType.value());
                if (responseText == null) {
                    body.setContent(new ByteArrayInputStream("{ \"error\" : { \"description\" : \"Internal Server Error\" } }".getBytes(HttpUtils.UTF_8)));
                }
            } else {
                body.setContentType("text/xml");
                if (responseText == null) {
                    body.setContent(new ByteArrayInputStream("<error>Internal Server Error</error>".getBytes(HttpUtils.UTF_8)));
                }
            }

            if (responseText != null) {
                body.setContent(new ByteArrayInputStream(responseText.getBytes(HttpUtils.UTF_8)));
            }
            resp.setEntity(body);
        } catch (final Exception ex) {
            s_logger.error("error!", ex);
        }
    }

    // FIXME: the following two threads are copied from
    // http://svn.apache.org/repos/asf/httpcomponents/httpcore/trunk/httpcore/src/examples/org/apache/http/examples/ElementalHttpServer.java
    // we have to cite a license if we are using this code directly, so we need to add the appropriate citation or
    // modify the
    // code to be very specific to our needs
    static class ListenerThread extends Thread {
        private HttpService _httpService = null;
        private ServerSocket _serverSocket = null;
        private HttpParams _params = null;

        public ListenerThread(final ApiServer requestHandler, final int port) {
            try {
                _serverSocket = new ServerSocket(port);
            } catch (final IOException ioex) {
                s_logger.error("error initializing api server", ioex);
                return;
            }

            _params = new BasicHttpParams();
            _params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

            // Set up the HTTP protocol processor
            final BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new ResponseDate());
            httpproc.addInterceptor(new ResponseServer());
            httpproc.addInterceptor(new ResponseContent());
            httpproc.addInterceptor(new ResponseConnControl());

            // Set up request handlers
            final HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("*", requestHandler);

            // Set up the HTTP service
            _httpService = new HttpService(httpproc, new NoConnectionReuseStrategy(), new DefaultHttpResponseFactory());
            _httpService.setParams(_params);
            _httpService.setHandlerResolver(reqistry);
        }

        @Override
        public void run() {
            s_logger.info("ApiServer listening on port " + _serverSocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    final Socket socket = _serverSocket.accept();
                    final DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, _params);

                    // Execute a new worker task to handle the request
                    s_executor.execute(new WorkerTask(_httpService, conn, s_workerCount++));
                } catch (final InterruptedIOException ex) {
                    break;
                } catch (final IOException e) {
                    s_logger.error("I/O error initializing connection thread", e);
                    break;
                }
            }
        }
    }

    static class WorkerTask extends ManagedContextRunnable {
        private final HttpService _httpService;
        private final HttpServerConnection _conn;

        public WorkerTask(final HttpService httpService, final HttpServerConnection conn, final int count) {
            _httpService = httpService;
            _conn = conn;
        }

        @Override
        protected void runInContext() {
            final HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && _conn.isOpen()) {
                    _httpService.handleRequest(_conn, context);
                    _conn.close();
                }
            } catch (final ConnectionClosedException ex) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("ApiServer:  Client closed connection");
                }
            } catch (final IOException ex) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("ApiServer:  IOException - " + ex);
                }
            } catch (final HttpException ex) {
                s_logger.warn("ApiServer:  Unrecoverable HTTP protocol violation" + ex);
            } finally {
                try {
                    _conn.shutdown();
                } catch (final IOException ignore) {
                }
            }
        }
    }

    @Override
    public String getSerializedApiError(final int errorCode, final String errorText, final Map<String, Object[]> apiCommandParams, final String responseType) {
        String responseName = null;
        Class<?> cmdClass = null;
        String responseText = null;

        try {
            if (apiCommandParams == null || apiCommandParams.isEmpty()) {
                responseName = "errorresponse";
            } else {
                final Object cmdObj = apiCommandParams.get(ApiConstants.COMMAND);
                // cmd name can be null when "command" parameter is missing in the request
                if (cmdObj != null) {
                    final String cmdName = ((String[])cmdObj)[0];
                    cmdClass = getCmdClass(cmdName);
                    if (cmdClass != null) {
                        responseName = ((BaseCmd)cmdClass.newInstance()).getCommandName();
                    } else {
                        responseName = "errorresponse";
                    }
                }
            }
            final ExceptionResponse apiResponse = new ExceptionResponse();
            apiResponse.setErrorCode(errorCode);
            apiResponse.setErrorText(errorText);
            apiResponse.setResponseName(responseName);
            SerializationContext.current().setUuidTranslation(true);
            responseText = ApiResponseSerializer.toSerializedString(apiResponse, responseType);

        } catch (final Exception e) {
            s_logger.error("Exception responding to http request", e);
        }
        return responseText;
    }

    @Override
    public String getSerializedApiError(final ServerApiException ex, final Map<String, Object[]> apiCommandParams, final String responseType) {
        String responseName = null;
        Class<?> cmdClass = null;
        String responseText = null;

        if (ex == null) {
            // this call should not be invoked with null exception
            return getSerializedApiError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Some internal error happened", apiCommandParams, responseType);
        }
        try {
            if (ex.getErrorCode() == ApiErrorCode.UNSUPPORTED_ACTION_ERROR || apiCommandParams == null || apiCommandParams.isEmpty()) {
                responseName = "errorresponse";
            } else {
                final Object cmdObj = apiCommandParams.get(ApiConstants.COMMAND);
                // cmd name can be null when "command" parameter is missing in
                // the request
                if (cmdObj != null) {
                    final String cmdName = ((String[])cmdObj)[0];
                    cmdClass = getCmdClass(cmdName);
                    if (cmdClass != null) {
                        responseName = ((BaseCmd)cmdClass.newInstance()).getCommandName();
                    } else {
                        responseName = "errorresponse";
                    }
                }
            }
            final ExceptionResponse apiResponse = new ExceptionResponse();
            apiResponse.setErrorCode(ex.getErrorCode().getHttpCode());
            apiResponse.setErrorText(ex.getDescription());
            apiResponse.setResponseName(responseName);
            final ArrayList<ExceptionProxyObject> idList = ex.getIdProxyList();
            if (idList != null) {
                for (int i = 0; i < idList.size(); i++) {
                    apiResponse.addProxyObject(idList.get(i));
                }
            }
            // Also copy over the cserror code and the function/layer in which
            // it was thrown.
            apiResponse.setCSErrorCode(ex.getCSErrorCode());

            SerializationContext.current().setUuidTranslation(true);
            responseText = ApiResponseSerializer.toSerializedString(apiResponse, responseType);

        } catch (final Exception e) {
            s_logger.error("Exception responding to http request", e);
        }
        return responseText;
    }

    @Inject
    public void setPluggableServices(final List<PluggableService> pluggableServices) {
        this.pluggableServices = pluggableServices;
    }

    @Inject
    public void setApiAccessCheckers(final List<APIChecker> apiAccessCheckers) {
        this.apiAccessCheckers = apiAccessCheckers;
    }

    public static boolean isEncodeApiResponse() {
        return ApiServer.encodeApiResponse;
    }

    private static void setEncodeApiResponse(final boolean encodeApiResponse) {
        ApiServer.encodeApiResponse = encodeApiResponse;
    }

    @Override
    public String getConfigComponentName() {
        return ApiServer.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                IntegrationAPIPort,
                ConcurrentSnapshotsThresholdPerHost,
                EncodeApiResponse,
                EnableSecureSessionCookie,
                JSONDefaultContentType
        };
    }
}
