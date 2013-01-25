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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
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

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.cluster.StackMaid;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CSExceptionErrorCode;

public abstract class MockApiServer implements HttpRequestHandler {
//    private static final Logger s_logger = Logger.getLogger(MockApiServer.class.getName());
//
//    public static final short ADMIN_COMMAND = 1;
//    public static final short DOMAIN_ADMIN_COMMAND = 4;
//    public static final short RESOURCE_DOMAIN_ADMIN_COMMAND = 2;
//    public static final short USER_COMMAND = 8;
//    public static boolean encodeApiResponse = false;
//    public static String jsonContentType = "text/javascript";
//    private Properties _apiCommands = null;
//    private ApiDispatcher _dispatcher;
//    private AccountManager _accountMgr = null;
//    private Account _systemAccount = null;
//    private User _systemUser = null;
//
//    private static int _workerCount = 0;
//
//    private static MockApiServer s_instance = null;
//    private static List<String> s_userCommands = null;
//    private static List<String> s_resellerCommands = null; // AKA domain-admin
//    private static List<String> s_adminCommands = null;
//    private static List<String> s_resourceDomainAdminCommands = null;
//    private static List<String> s_allCommands = null;
//    private static List<String> s_pluggableServiceCommands = null;
//
//    private static ExecutorService _executor = new ThreadPoolExecutor(10, 150, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("ApiServer"));
//
//    static {
//        s_userCommands = new ArrayList<String>();
//        s_resellerCommands = new ArrayList<String>();
//        s_adminCommands = new ArrayList<String>();
//        s_resourceDomainAdminCommands = new ArrayList<String>();
//        s_allCommands = new ArrayList<String>();
//        s_pluggableServiceCommands = new ArrayList<String>();
//    }
//
//    private MockApiServer() {
//    }
//
//    public static void initApiServer(String[] apiConfig) {
//        if (s_instance == null) {
//            s_instance = new MockApiServer();
//            s_instance.init(apiConfig);
//        }
//    }
//
//    public static MockApiServer getInstance() {
//        // initApiServer();
//        return s_instance;
//    }
//
//    public Properties get_apiCommands() {
//        return _apiCommands;
//    }
//
//    private void processConfigFiles(String[] apiConfig, boolean pluggableServicesConfig) {
//        try {
//            if (_apiCommands == null) {
//                _apiCommands = new Properties();
//            }
//            Properties preProcessedCommands = new Properties();
//            if (apiConfig != null) {
//                for (String configFile : apiConfig) {
//                    File commandsFile = PropertiesUtil.findConfigFile(configFile);
//                    if (commandsFile != null) {
//                        try {
//                            preProcessedCommands.load(new FileInputStream(commandsFile));
//                        } catch (FileNotFoundException fnfex) {
//                            // in case of a file within a jar in classpath, try to open stream using url
//                            InputStream stream = PropertiesUtil.openStreamFromURL(configFile);
//                            if (stream != null) {
//                                preProcessedCommands.load(stream);
//                            } else {
//                                s_logger.error("Unable to find properites file", fnfex);
//                            }
//                        }
//                    }
//                }
//                for (Object key : preProcessedCommands.keySet()) {
//                    String preProcessedCommand = preProcessedCommands.getProperty((String) key);
//                    String[] commandParts = preProcessedCommand.split(";");
//                    _apiCommands.put(key, commandParts[0]);
//
//                    if (pluggableServicesConfig) {
//                        s_pluggableServiceCommands.add(commandParts[0]);
//                    }
//
//                    if (commandParts.length > 1) {
//                        try {
//                            short cmdPermissions = Short.parseShort(commandParts[1]);
//                            if ((cmdPermissions & ADMIN_COMMAND) != 0) {
//                                s_adminCommands.add((String) key);
//                            }
//                            if ((cmdPermissions & RESOURCE_DOMAIN_ADMIN_COMMAND) != 0) {
//                                s_resourceDomainAdminCommands.add((String) key);
//                            }
//                            if ((cmdPermissions & DOMAIN_ADMIN_COMMAND) != 0) {
//                                s_resellerCommands.add((String) key);
//                            }
//                            if ((cmdPermissions & USER_COMMAND) != 0) {
//                                s_userCommands.add((String) key);
//                            }
//                        } catch (NumberFormatException nfe) {
//                            s_logger.info("Malformed command.properties permissions value, key = " + key + ", value = " + preProcessedCommand);
//                        }
//                    }
//                }
//
//                s_allCommands.addAll(s_adminCommands);
//                s_allCommands.addAll(s_resourceDomainAdminCommands);
//                s_allCommands.addAll(s_userCommands);
//                s_allCommands.addAll(s_resellerCommands);
//            }
//        } catch (FileNotFoundException fnfex) {
//            s_logger.error("Unable to find properites file", fnfex);
//        } catch (IOException ioex) {
//            s_logger.error("Exception loading properties file", ioex);
//        }
//    }
//
//    public void init(String[] apiConfig) {
//        BaseCmd.setComponents(new ApiResponseHelper());
//        BaseListCmd.configure();
//        processConfigFiles(apiConfig, false);
//
//        ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
//        _accountMgr = locator.getManager(AccountManager.class);
//        _systemAccount = _accountMgr.getSystemAccount();
//        _systemUser = _accountMgr.getSystemUser();
//        _dispatcher = ApiDispatcher.getInstance();
//
//        Integer apiPort = null; // api port, null by default
//        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
//        SearchCriteria<ConfigurationVO> sc = configDao.createSearchCriteria();
//        sc.addAnd("name", SearchCriteria.Op.EQ, "integration.api.port");
//        List<ConfigurationVO> values = configDao.search(sc, null);
//        if ((values != null) && (values.size() > 0)) {
//            ConfigurationVO apiPortConfig = values.get(0);
//            if (apiPortConfig.getValue() != null) {
//                apiPort = Integer.parseInt(apiPortConfig.getValue());
//            }
//        }
//
//        encodeApiResponse = Boolean.valueOf(configDao.getValue(Config.EncodeApiResponse.key()));
//
//        String jsonType = configDao.getValue(Config.JavaScriptDefaultContentType.key());
//        if (jsonType != null) {
//            jsonContentType = jsonType;
//        }
//
//        if (apiPort != null) {
//            ListenerThread listenerThread = new ListenerThread(this, apiPort);
//            listenerThread.start();
//        }
//    }
//
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    @Override
//    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
//        // get some information for the access log...
//        StringBuffer sb = new StringBuffer();
//        HttpServerConnection connObj = (HttpServerConnection) context.getAttribute("http.connection");
//        if (connObj instanceof SocketHttpServerConnection) {
//            InetAddress remoteAddr = ((SocketHttpServerConnection) connObj).getRemoteAddress();
//            sb.append(remoteAddr.toString() + " -- ");
//        }
//        sb.append(request.getRequestLine());
//
//        try {
//            String uri = request.getRequestLine().getUri();
//            int requestParamsStartIndex = uri.indexOf('?');
//            if (requestParamsStartIndex >= 0) {
//                uri = uri.substring(requestParamsStartIndex + 1);
//            }
//
//            String[] paramArray = uri.split("&");
//            if (paramArray.length < 1) {
//                s_logger.info("no parameters received for request: " + uri + ", aborting...");
//                return;
//            }
//
//            Map parameterMap = new HashMap<String, String[]>();
//
//            String responseType = BaseCmd.RESPONSE_TYPE_XML;
//            for (String paramEntry : paramArray) {
//                String[] paramValue = paramEntry.split("=");
//                if (paramValue.length != 2) {
//                    s_logger.info("malformed parameter: " + paramEntry + ", skipping");
//                    continue;
//                }
//                if ("response".equalsIgnoreCase(paramValue[0])) {
//                    responseType = paramValue[1];
//                } else {
//                    // according to the servlet spec, the parameter map should be in the form (name=String,
//                    // value=String[]), so
//                    // parameter values will be stored in an array
//                    parameterMap.put(/* name */paramValue[0], /* value */new String[] { paramValue[1] });
//                }
//            }
//            try {
//                // always trust commands from API port, user context will always be UID_SYSTEM/ACCOUNT_ID_SYSTEM
//                UserContext.registerContext(_systemUser.getId(), _systemAccount, null, true);
//                sb.insert(0, "(userId=" + User.UID_SYSTEM + " accountId=" + Account.ACCOUNT_ID_SYSTEM + " sessionId=" + null + ") ");
//                String responseText = handleRequest(parameterMap, true, responseType, sb);
//                sb.append(" 200 " + ((responseText == null) ? 0 : responseText.length()));
//
//                writeResponse(response, responseText, HttpStatus.SC_OK, responseType, null);
//            } catch (ServerApiException se) {
//                String responseText = getSerializedApiError(se.getErrorCode(), se.getDescription(), parameterMap, responseType, se);                
//                writeResponse(response, responseText, se.getErrorCode(), responseType, se.getDescription());
//                sb.append(" " + se.getErrorCode() + " " + se.getDescription());
//            } catch (RuntimeException e) {
//                // log runtime exception like NullPointerException to help identify the source easier
//                s_logger.error("Unhandled exception, ", e);
//                throw e;
//            } catch (Exception e){
//            	s_logger.info("Error: "+e.getMessage());
//            }
//        } finally {
//            UserContext.unregisterContext();
//        }
//    }
//
//    @SuppressWarnings("rawtypes")
//    public String handleRequest(Map params, boolean decode, String responseType, StringBuffer auditTrailSb) throws ServerApiException {
//        String response = null;
//        String[] command = null;
//        try {
//            command = (String[]) params.get("command");
//            if (command == null) {
//                s_logger.error("invalid request, no command sent");
//                if (s_logger.isTraceEnabled()) {
//                    s_logger.trace("dumping request parameters");
//                    for (Object key : params.keySet()) {
//                        String keyStr = (String) key;
//                        String[] value = (String[]) params.get(key);
//                        s_logger.trace("   key: " + keyStr + ", value: " + ((value == null) ? "'null'" : value[0]));
//                    }
//                }
//                throw new ServerApiException(BaseCmd.UNSUPPORTED_ACTION_ERROR, "Invalid request, no command sent");
//            } else {
//                Map<String, String> paramMap = new HashMap<String, String>();
//                Set keys = params.keySet();
//                Iterator keysIter = keys.iterator();
//                while (keysIter.hasNext()) {
//                    String key = (String) keysIter.next();
//                    if ("command".equalsIgnoreCase(key)) {
//                        continue;
//                    }
//                    String[] value = (String[]) params.get(key);
//
//                    String decodedValue = null;
//                    if (decode) {
//                        try {
//                            decodedValue = URLDecoder.decode(value[0], "UTF-8");
//                        } catch (UnsupportedEncodingException usex) {
//                            s_logger.warn(key + " could not be decoded, value = " + value[0]);
//                            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, key + " could not be decoded, received value " + value[0]);
//                        } catch (IllegalArgumentException iae) {
//                            s_logger.warn(key + " could not be decoded, value = " + value[0]);
//                            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, key + " could not be decoded, received value " + value[0] + " which contains illegal characters eg.%");
//                        }
//                    } else {
//                        decodedValue = value[0];
//                    }
//                    paramMap.put(key, decodedValue);
//                }
//                String cmdClassName = _apiCommands.getProperty(command[0]);
//                if (cmdClassName != null) {
//                    Class<?> cmdClass = Class.forName(cmdClassName);
//                    BaseCmd cmdObj = (BaseCmd) cmdClass.newInstance();
//                    cmdObj.setFullUrlParams(paramMap);
//                    cmdObj.setResponseType(responseType);
//                    // This is where the command is either serialized, or directly dispatched
//                    response = queueCommand(cmdObj, paramMap);
//                } else {
//                    if (!command[0].equalsIgnoreCase("login") && !command[0].equalsIgnoreCase("logout")) {
//                        String errorString = "Unknown API command: " + ((command == null) ? "null" : command[0]);
//                        s_logger.warn(errorString);
//                        auditTrailSb.append(" " + errorString);
//                        throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR, errorString);
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            if (ex instanceof InvalidParameterValueException) {
//            	InvalidParameterValueException ref = (InvalidParameterValueException)ex;
//            	ServerApiException e = new ServerApiException(ApiErrorCode.PARAM_ERROR, ex.getMessage());            	
//                // copy over the IdentityProxy information as well and throw the serverapiexception.
//                ArrayList<IdentityProxy> idList = ref.getIdProxyList();
//                if (idList != null) {
//                	// Iterate through entire arraylist and copy over each proxy id.
//                	for (int i = 0 ; i < idList.size(); i++) {
//                		IdentityProxy obj = idList.get(i);
//                		e.addProxyObject(obj.getTableName(), obj.getValue(), obj.getidFieldName());
//                	}
//                }
//                // Also copy over the cserror code and the function/layer in which it was thrown.
//            	e.setCSErrorCode(ref.getCSErrorCode());
//                throw e;
//            } else if (ex instanceof PermissionDeniedException) {
//            	PermissionDeniedException ref = (PermissionDeniedException)ex;
//            	ServerApiException e = new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, ex.getMessage());
//                // copy over the IdentityProxy information as well and throw the serverapiexception.
//            	ArrayList<IdentityProxy> idList = ref.getIdProxyList();
//                if (idList != null) {
//                	// Iterate through entire arraylist and copy over each proxy id.
//                	for (int i = 0 ; i < idList.size(); i++) {
//                		IdentityProxy obj = idList.get(i);
//                		e.addProxyObject(obj.getTableName(), obj.getValue(), obj.getidFieldName());
//                	}
//                }
//                e.setCSErrorCode(ref.getCSErrorCode());
//                throw e;
//            } else if (ex instanceof ServerApiException) {
//                throw (ServerApiException) ex;
//            } else {
//                s_logger.error("unhandled exception executing api command: " + ((command == null) ? "null" : command[0]), ex);
//                ServerApiException e = new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Internal server error, unable to execute request.");
//                e.setCSErrorCode(CSExceptionErrorCode.getCSErrCode("ServerApiException"));
//                throw e;
//            }
//        }
//        return response;
//    }
//
//    private String queueCommand(BaseCmd cmdObj, Map<String, String> params) {
//    	params.put("ctxStartEventId", String.valueOf(0L));
//    	_dispatcher.dispatch(cmdObj, params);
//    	SerializationContext.current().setUuidTranslation(true);
//    	return ApiResponseSerializer.toSerializedString((ResponseObject) cmdObj.getResponseObject(), cmdObj.getResponseType());
//    }
//
//    // FIXME: rather than isError, we might was to pass in the status code to give more flexibility
//    private void writeResponse(HttpResponse resp, final String responseText, final int statusCode, String responseType, String reasonPhrase) {
//        try {
//            resp.setStatusCode(statusCode);
//            resp.setReasonPhrase(reasonPhrase);
//
//            BasicHttpEntity body = new BasicHttpEntity();
//            if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
//                // JSON response
//                body.setContentType(jsonContentType);
//                if (responseText == null) {
//                    body.setContent(new ByteArrayInputStream("{ \"error\" : { \"description\" : \"Internal Server Error\" } }".getBytes("UTF-8")));
//                }
//            } else {
//                body.setContentType("text/xml");
//                if (responseText == null) {
//                    body.setContent(new ByteArrayInputStream("<error>Internal Server Error</error>".getBytes("UTF-8")));
//                }
//            }
//
//            if (responseText != null) {
//                body.setContent(new ByteArrayInputStream(responseText.getBytes("UTF-8")));
//            }
//            resp.setEntity(body);
//        } catch (Exception ex) {
//            s_logger.error("error!", ex);
//        }
//    }
//
//    // FIXME: the following two threads are copied from
//    // http://svn.apache.org/repos/asf/httpcomponents/httpcore/trunk/httpcore/src/examples/org/apache/http/examples/ElementalHttpServer.java
//    // we have to cite a license if we are using this code directly, so we need to add the appropriate citation or
//    // modify the
//    // code to be very specific to our needs
//    static class ListenerThread extends Thread {
//        private HttpService _httpService = null;
//        private ServerSocket _serverSocket = null;
//        private HttpParams _params = null;
//
//        public ListenerThread(MockApiServer requestHandler, int port) {
//            try {
//                _serverSocket = new ServerSocket(port);
//            } catch (IOException ioex) {
//                s_logger.error("error initializing api server", ioex);
//                return;
//            }
//
//            _params = new BasicHttpParams();
//            _params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000).setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
//                    .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
//                    .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");
//
//            // Set up the HTTP protocol processor
//            BasicHttpProcessor httpproc = new BasicHttpProcessor();
//            httpproc.addInterceptor(new ResponseDate());
//            httpproc.addInterceptor(new ResponseServer());
//            httpproc.addInterceptor(new ResponseContent());
//            httpproc.addInterceptor(new ResponseConnControl());
//
//            // Set up request handlers
//            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
//            reqistry.register("*", requestHandler);
//
//            // Set up the HTTP service
//            _httpService = new HttpService(httpproc, new NoConnectionReuseStrategy(), new DefaultHttpResponseFactory());
//            _httpService.setParams(_params);
//            _httpService.setHandlerResolver(reqistry);
//        }
//
//        @Override
//        public void run() {
//            s_logger.info("ApiServer listening on port " + _serverSocket.getLocalPort());
//            while (!Thread.interrupted()) {
//                try {
//                    // Set up HTTP connection
//                    Socket socket = _serverSocket.accept();
//                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
//                    conn.bind(socket, _params);
//
//                    // Execute a new worker task to handle the request
//                    _executor.execute(new WorkerTask(_httpService, conn, _workerCount++));
//                } catch (InterruptedIOException ex) {
//                    break;
//                } catch (IOException e) {
//                    s_logger.error("I/O error initializing connection thread", e);
//                    break;
//                }
//            }
//        }
//    }
//
//    static class WorkerTask implements Runnable {
//        private final HttpService _httpService;
//        private final HttpServerConnection _conn;
//
//        public WorkerTask(final HttpService httpService, final HttpServerConnection conn, final int count) {
//            _httpService = httpService;
//            _conn = conn;
//        }
//
//        @Override
//        public void run() {
//            HttpContext context = new BasicHttpContext(null);
//            try {
//                while (!Thread.interrupted() && _conn.isOpen()) {
//                    try {
//                        _httpService.handleRequest(_conn, context);
//                        _conn.close();
//                    } finally {
//                        StackMaid.current().exitCleanup();
//                    }
//                }
//            } catch (ConnectionClosedException ex) {
//                if (s_logger.isTraceEnabled()) {
//                    s_logger.trace("ApiServer:  Client closed connection");
//                }
//            } catch (IOException ex) {
//                if (s_logger.isTraceEnabled()) {
//                    s_logger.trace("ApiServer:  IOException - " + ex);
//                }
//            } catch (HttpException ex) {
//                s_logger.warn("ApiServer:  Unrecoverable HTTP protocol violation" + ex);
//            } finally {
//                try {
//                    _conn.shutdown();
//                } catch (IOException ignore) {
//                }
//            }
//        }
//    }
//
//    public String getSerializedApiError(int errorCode, String errorText, Map<String, Object[]> apiCommandParams, String responseType, Exception ex) {
//        String responseName = null;
//        String cmdClassName = null;
//
//        String responseText = null;
//
//        try {
//            if (errorCode == ApiErrorCode.UNSUPPORTED_ACTION_ERROR.ordinal() || apiCommandParams == null || apiCommandParams.isEmpty()) {
//                responseName = "errorresponse";
//            } else {
//                Object cmdObj = apiCommandParams.get("command");
//                // cmd name can be null when "command" parameter is missing in the request
//                if (cmdObj != null) {
//                    String cmdName = ((String[]) cmdObj)[0];
//                    cmdClassName = _apiCommands.getProperty(cmdName);
//                    if (cmdClassName != null) {
//                        Class<?> claz = Class.forName(cmdClassName);
//                        responseName = ((BaseCmd) claz.newInstance()).getCommandName();
//                    } else {
//                        responseName = "errorresponse";
//                    }
//                }
//            }
//            ExceptionResponse apiResponse = new ExceptionResponse();
//            apiResponse.setErrorCode(errorCode);
//            apiResponse.setErrorText(errorText);
//            apiResponse.setResponseName(responseName);
//            // Also copy over the IdentityProxy object List into this new apiResponse, from
//            // the exception caught. When invoked from handle(), the exception here can
//            // be either ServerApiException, PermissionDeniedException or InvalidParameterValue
//            // Exception. When invoked from ApiServlet's processRequest(), this can be
//            // a standard exception like NumberFormatException. We'll leave the standard ones alone.
////            if (ex != null) {
////            	if (ex instanceof ServerApiException || ex instanceof PermissionDeniedException
////            			|| ex instanceof InvalidParameterValueException) {
////            		// Cast the exception appropriately and retrieve the IdentityProxy
////            		if (ex instanceof ServerApiException) {
////            			ServerApiException ref = (ServerApiException) ex;
////            			ArrayList<IdentityProxy> idList = ref.getIdProxyList();
////            			if (idList != null) {
////            				for (int i=0; i < idList.size(); i++) {
////            					IdentityProxy id = idList.get(i);
////            					apiResponse.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
////            				}            				
////            			}
////            			// Also copy over the cserror code and the function/layer in which it was thrown.
////            			apiResponse.setCSErrorCode(ref.getCSErrorCode());
////            		} else if (ex instanceof PermissionDeniedException) {
////            			PermissionDeniedException ref = (PermissionDeniedException) ex;
////            			ArrayList<IdentityProxy> idList = ref.getIdProxyList();
////            			if (idList != null) {
////            				for (int i=0; i < idList.size(); i++) {
////            					IdentityProxy id = idList.get(i);
////            					apiResponse.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
////            				}            				
////            			}
////            			// Also copy over the cserror code and the function/layer in which it was thrown.
////            			apiResponse.setCSErrorCode(ref.getCSErrorCode());
////            		} else if (ex instanceof InvalidParameterValueException) {
////            			InvalidParameterValueException ref = (InvalidParameterValueException) ex;
////            			ArrayList<IdentityProxy> idList = ref.getIdProxyList();
////            			if (idList != null) {
////            				for (int i=0; i < idList.size(); i++) {
////            					IdentityProxy id = idList.get(i);
////            					apiResponse.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
////            				}            				
////            			}
////            			// Also copy over the cserror code and the function/layer in which it was thrown.
////            			apiResponse.setCSErrorCode(ref.getCSErrorCode());
////            		}
////            	}
////            }
//            SerializationContext.current().setUuidTranslation(true);
//            responseText = ApiResponseSerializer.toSerializedString(apiResponse, responseType);
//
//        } catch (Exception e) {
//            s_logger.error("Exception responding to http request", e);            
//        }
//        return responseText;
//    }
//    
}
