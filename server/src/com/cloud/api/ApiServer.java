/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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

import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.domain.DomainVO;
import com.cloud.maid.StackMaid;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.encoding.Base64;


public class ApiServer implements HttpRequestHandler {
    private static final Logger s_logger = Logger.getLogger(ApiServer.class.getName());
    private static final Logger s_accessLogger = Logger.getLogger("apiserver." + ApiServer.class.getName());

    private static final short ADMIN_COMMAND = 1;
    private static final short DOMAIN_ADMIN_COMMAND = 2;
    private static final short READ_ONLY_ADMIN_COMMAND = 4;
    private static final short USER_COMMAND = 8;
    private Properties _apiCommands = null;
    private ManagementServer _ms = null;
    
    private static int _workerCount = 0;

    private static ApiServer s_instance = null;
    private static List<String> s_userCommands = null;
    private static List<String> s_resellerCommands = null; // AKA domain-admin
    private static List<String> s_adminCommands = null;
    private static List<String> s_readOnlyAdminCommands = null;
    
    private static ExecutorService _executor = new ThreadPoolExecutor(10, 150, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("ApiServer"));

    static {
        s_userCommands = new ArrayList<String>();
        s_resellerCommands = new ArrayList<String>();
        s_adminCommands = new ArrayList<String>();
        s_readOnlyAdminCommands = new ArrayList<String>();
    }

    private ApiServer() { }

    public static void initApiServer(String[] apiConfig) {
        if (s_instance == null) {
            s_instance = new ApiServer();
            s_instance.init(apiConfig);
        }
    }

    public static ApiServer getInstance() {
        //initApiServer();
        return s_instance;
    }

    public void init(String[] apiConfig) {
        try {
            _apiCommands = new Properties();
            Properties preProcessedCommands = new Properties();
            if (apiConfig != null) {
                for (String configFile : apiConfig) {
                    File commandsFile = PropertiesUtil.findConfigFile(configFile);
                    preProcessedCommands.load(new FileInputStream(commandsFile));
                }
                for (Object key : preProcessedCommands.keySet()) {
                    String preProcessedCommand = preProcessedCommands.getProperty((String)key);
                    String[] commandParts = preProcessedCommand.split(";");
                    _apiCommands.put(key, commandParts[0]);
                    if (commandParts.length > 1) {
                        try {
                            short cmdPermissions = Short.parseShort(commandParts[1]);
                            if ((cmdPermissions & ADMIN_COMMAND) != 0) {
                                s_adminCommands.add((String)key);
                            }
                            if ((cmdPermissions & DOMAIN_ADMIN_COMMAND) != 0) {
                                s_resellerCommands.add((String)key);
                            }
                            if ((cmdPermissions & READ_ONLY_ADMIN_COMMAND) != 0) {
                                s_readOnlyAdminCommands.add((String)key);
                            }
                            if ((cmdPermissions & USER_COMMAND) != 0) {
                                s_userCommands.add((String)key);
                            }
                        } catch (NumberFormatException nfe) {
                            s_logger.info("Malformed command.properties permissions value, key = " + key + ", value = " + preProcessedCommand);
                        }
                    }
                }
            }
        } catch (FileNotFoundException fnfex) {
            s_logger.error("Unable to find properites file", fnfex);
        } catch (IOException ioex) {
            s_logger.error("Exception loading properties file", ioex);
        }

        _ms = (ManagementServer)ComponentLocator.getComponent(ManagementServer.Name);

        int apiPort = 8096; // default port
        ConfigurationDao configDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(ConfigurationDao.class);
        SearchCriteria sc = configDao.createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, "integration.api.port");
        List<ConfigurationVO> values = configDao.search(sc, null);
        if ((values != null) && (values.size() > 0)) {
            ConfigurationVO apiPortConfig = values.get(0);
            apiPort = Integer.parseInt(apiPortConfig.getValue());
        }

        ListenerThread listenerThread = new ListenerThread(this, apiPort);
        listenerThread.start();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        // get some information for the access log...
        StringBuffer sb = new StringBuffer();
        HttpServerConnection connObj = (HttpServerConnection)context.getAttribute("http.connection");
        if (connObj instanceof SocketHttpServerConnection) {
            InetAddress remoteAddr = ((SocketHttpServerConnection)connObj).getRemoteAddress();
            sb.append(remoteAddr.toString() + " -- ");
        }
        sb.append(request.getRequestLine());

        try {
            String uri = request.getRequestLine().getUri();
            int requestParamsStartIndex = uri.indexOf('?');
            if (requestParamsStartIndex >= 0) {
                uri = uri.substring(requestParamsStartIndex+1);
            }

            String[] paramArray = uri.split("&");
            if (paramArray.length < 1) {
                s_logger.info("no parameters received for request: " + uri + ", aborting...");
                return;
            }

            Map parameterMap = new HashMap<String, String[]>();

            String responseType = BaseCmd.RESPONSE_TYPE_XML;
            for (String paramEntry : paramArray) {
                String[] paramValue = paramEntry.split("=");
                if (paramValue.length != 2) {
                    s_logger.info("malformed parameter: " + paramEntry + ", skipping");
                    continue;
                }
                if ("response".equalsIgnoreCase(paramValue[0])) {
                    responseType = paramValue[1];
                } else {
                    // according to the servlet spec, the parameter map should be in the form (name=String, value=String[]), so parameter values will be stored in an array
                    parameterMap.put(/*name*/paramValue[0], /*value*/new String[] {paramValue[1]});
                }
            }
            try {
            	// always trust commands from API port, user context will always be UID_SYSTEM/ACCOUNT_ID_SYSTEM
            	UserContext.registerContext(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, null, true);
            	
                String responseText = handleRequest(parameterMap, true, responseType);
                sb.append(" 200 " + ((responseText == null) ? 0 : responseText.length()));
                writeResponse(response, responseText, false, responseType);
            } catch (ServerApiException se) {
                try {
                    response.setStatusCode(se.getErrorCode());
                    response.setReasonPhrase(se.getDescription());
                    BasicHttpEntity body = new BasicHttpEntity();
                    body.setContentType("text/xml; charset=UTF-8");
                    String responseStr = "<error>"+se.getErrorCode()+" : "+se.getDescription()+"</error>";
                    body.setContent(new ByteArrayInputStream(responseStr.getBytes()));
                    response.setEntity(body);

                    sb.append(" " + se.getErrorCode() + " " + responseStr.length());
                } catch (Exception e) {
                    s_logger.error("IO Exception responding to http request", e);
                }
            } catch(RuntimeException e) {
            	// log runtime exception like NullPointerException to help identify the source easier
                s_logger.error("Unhandled exception, ", e);
                throw e;
            }
        } finally {
            s_accessLogger.info(sb.toString());
            
            UserContext.unregisterContext();
        }
    }

    public String handleRequest(Map params, boolean decode, String responseType) throws ServerApiException {
        String response = null;
        try {
            String[] command = (String[])params.get("command");
            if (command == null) {
                s_logger.error("invalid request, no command sent");
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("dumping request parameters");
                    for (Object key : params.keySet()) {
                        String keyStr = (String)key;
                        String[] value = (String[])params.get(key);
                        s_logger.trace("   key: " + keyStr + ", value: " + ((value == null) ? "'null'" : value[0]));
                    }
                }
                response = buildErrorResponse("invalid request, no command sent", responseType);
            } else {
                Map<String, Object> paramMap = new HashMap<String, Object>();
                Set keys = params.keySet();
                Iterator keysIter = keys.iterator();
                while (keysIter.hasNext()) {
                    String key = (String)keysIter.next();
                    if ("command".equalsIgnoreCase(key)) {
                        continue;
                    }
                    Object[] value = (Object[])params.get(key);
                    paramMap.put(key, value[0]);
                }
                String cmdClassName = _apiCommands.getProperty(command[0]);
                if (cmdClassName != null) {
                    Class<?> cmdClass = Class.forName(cmdClassName);
                    BaseCmd cmdObj = (BaseCmd)cmdClass.newInstance();
                    cmdObj.setManagementServer(_ms);
                    Map<String, Object> validatedParams = cmdObj.validateParams(paramMap, decode);
                    
                    List<Pair<String, Object>> resultValues = cmdObj.execute(validatedParams);
                    response = cmdObj.buildResponse(resultValues, responseType);
                } else {
                    s_logger.warn("unknown API command: " + ((command == null) ? "null" : command[0]));
                    response = buildErrorResponse("unknown API command: " + ((command == null) ? "null" : command[0]), responseType);
                }
            }
        } catch (Exception ex) {
            if (ex instanceof ServerApiException) {
            	throw (ServerApiException)ex;
            } else {
                s_logger.error("error executing api command", ex);
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal server error, unable to execute request.");
            }
        }
        return response;
    }

    public boolean verifyRequest(Map<String, Object[]> requestParameters, String userId) {
        try {
            String apiKey = null;
            String secretKey = null;
            String signature = null;
            String unsignedRequest = null;

            String[] command = (String[])requestParameters.get("command");
            if (command == null) {
                s_logger.info("missing command, ignoring request...");
                return false;
            }

            String commandName = command[0];
            
            //if userId not null, that mean that user is logged in
            if (userId != null) {
            	Long accountId = _ms.findUserById(Long.valueOf(userId)).getAccountId();
            	Account userAccount = _ms.findAccountById(accountId);
            	short accountType = userAccount.getType();
            	
            	if (!isCommandAvailable(accountType, commandName)) {
            		return false;
            	}
            	return true;
            }

            // - build a request string with sorted params, make sure it's all lowercase
            // - sign the request, verify the signature is the same
            List<String> parameterNames = new ArrayList<String>();

            for (Object paramNameObj : requestParameters.keySet()) {
                parameterNames.add((String)paramNameObj); // put the name in a list that we'll sort later
            }

            Collections.sort(parameterNames);

            for (String paramName : parameterNames) {
                // parameters come as name/value pairs in the form String/String[]
                String paramValue = ((String[])requestParameters.get(paramName))[0];
                
                if ("signature".equalsIgnoreCase(paramName)) {
                    signature = paramValue;
                } else {
                    if ("apikey".equalsIgnoreCase(paramName)) {
                        apiKey = paramValue;
                    }

                    if (unsignedRequest == null) {
                        unsignedRequest = paramName + "=" + URLEncoder.encode(paramValue, "UTF-8").replaceAll("\\+", "%20");
                    } else {
                        unsignedRequest = unsignedRequest + "&" + paramName + "=" + URLEncoder.encode(paramValue, "UTF-8").replaceAll("\\+", "%20");
                    }
                }
            }
            

            // if api/secret key are passed to the parameters
            if ((signature == null) || (apiKey == null)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.info("expired session, missing signature, or missing apiKey -- ignoring request...sig: " + signature + ", apiKey: " + apiKey);
                }
                return false; // no signature, bad request
            }

            Transaction txn = Transaction.open(Transaction.CLOUD_DB);
            txn.close();
            User user = null;
            // verify there is a user with this api key
            Pair<User, Account> userAcctPair = _ms.findUserByApiKey(apiKey);
            if (userAcctPair == null) {
                s_logger.info("apiKey does not map to a valid user -- ignoring request, apiKey: " + apiKey);
                return false;
            }

            user = userAcctPair.first();
            Account account = userAcctPair.second();

            if (!user.getState().equals(Account.ACCOUNT_STATE_ENABLED) || !account.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
                s_logger.info("disabled or locked user accessing the api, userid = " + user.getId() + "; name = " + user.getUsername() + "; state: " + user.getState() + "; accountState: " + account.getState());
                return false;
            }

            if (account.getType() == Account.ACCOUNT_TYPE_NORMAL) {
    			requestParameters.put(BaseCmd.Properties.USER_ID.getName(), new String[] { user.getId().toString() });
                requestParameters.put(BaseCmd.Properties.ACCOUNT.getName(), new String[] { account.getAccountName() });
                requestParameters.put(BaseCmd.Properties.DOMAIN_ID.getName(), new String[] { account.getDomainId().toString() });
        		requestParameters.put(BaseCmd.Properties.ACCOUNT_OBJ.getName(), new Object[] { account });
    		} else {
    			requestParameters.put(BaseCmd.Properties.USER_ID.getName(), new String[] { user.getId().toString() });
    			requestParameters.put(BaseCmd.Properties.ACCOUNT_OBJ.getName(), new Object[] { account });
    		}           

            if (!isCommandAvailable(account.getType(), commandName)) {
        		return false;
        	}

            // verify secret key exists
            secretKey = user.getSecretKey();
            if (secretKey == null) {
                s_logger.info("User does not have a secret key associated with the account -- ignoring request, username: " + user.getUsername());
                return false;
            }

            unsignedRequest = unsignedRequest.toLowerCase();

            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
            mac.init(keySpec);
            mac.update(unsignedRequest.getBytes());
            byte[] encryptedBytes = mac.doFinal();
            String computedSignature = Base64.encodeBytes(encryptedBytes);
            boolean equalSig = signature.equals(computedSignature);
            if (!equalSig) {
            	s_logger.info("User signature: " + signature + " is not equaled to computed signature: " + computedSignature);
            }
            return equalSig;
        } catch (Exception ex) {
            s_logger.error("unable to verifty request signature", ex);
        }
        return false;
    }
    
    public List<Pair<String, Object>> loginUser(String username, String password, Long domainId, String domainPath, Map<String, Object[]> requestParameters) {
    	// We will always use domainId first.  If that does not exist, we will use domain name.  If THAT doesn't exist
    	// we will default to ROOT
        if (domainId == null) {
        	if (domainPath == null || domainPath.trim().length() == 0) {
        		domainId = DomainVO.ROOT_DOMAIN;
        	} else {
                DomainVO domainObj = _ms.findDomainByPath(domainPath);
        		if (domainObj != null) {
        			domainId = domainObj.getId();
        		} else { // if an unknown path is passed in, fail the login call
        			return null;
        		}
        	}
        }

        UserAccount userAcct = _ms.authenticateUser(username, password, domainId, requestParameters);
        if (userAcct != null) 
        {
        	String timezone = userAcct.getTimezone();
        	float offsetInHrs = 0f;
        	if (timezone!=null) {
	    		TimeZone t = TimeZone.getTimeZone(timezone);
	    		s_logger.info("Current user logged in under "+timezone+" timezone");
	    		
	            java.util.Date date = new java.util.Date();
	            long longDate = date.getTime();
	            float offsetInMs = (float)(t.getOffset(longDate));
	            offsetInHrs = offsetInMs/ (1000*60*60);
	            s_logger.info("Timezone offset from UTC is: "+offsetInHrs);
        	}
        	
        	Account account = _ms.findAccountById(userAcct.getAccountId());
            List<Pair<String, Object>> loginParams = new ArrayList<Pair<String, Object>>();

            String networkType = _ms.getConfigurationValue("network.type");
            if (networkType == null) 
            	networkType = "vnet";
            
            String hypervisorType = _ms.getConfigurationValue("hypervisor.type");
            if (hypervisorType == null) 
            	hypervisorType = "kvm";
            
            String directAttachNetworkGroupsEnabled = _ms.getConfigurationValue("direct.attach.network.groups.enabled");
            if(directAttachNetworkGroupsEnabled == null) 
            	directAttachNetworkGroupsEnabled = "false";     
            
            String directAttachedUntaggedEnabled = _ms.getConfigurationValue("direct.attach.untagged.vlan.enabled");
            if (directAttachedUntaggedEnabled == null) 
            	directAttachedUntaggedEnabled = "false";
            
            // set the userId and account object for everyone
            loginParams.add(new Pair<String, Object>("userId", userAcct.getId().toString()));
            loginParams.add(new Pair<String, Object>("userName", userAcct.getUsername()));
            loginParams.add(new Pair<String, Object>("firstName", userAcct.getFirstname()));
            loginParams.add(new Pair<String, Object>("lastName", userAcct.getLastname()));
            loginParams.add(new Pair<String, Object>("accountobj", account));
            loginParams.add(new Pair<String, Object>("account", account.getAccountName()));
            loginParams.add(new Pair<String, Object>("domainId", account.getDomainId().toString()));           
            loginParams.add(new Pair<String, Object>("type", new Short(account.getType())));
            loginParams.add(new Pair<String, Object>("networkType", networkType));
            loginParams.add(new Pair<String, Object>("hypervisorType", hypervisorType));
            loginParams.add(new Pair<String, Object>("directAttachNetworkGroupsEnabled", directAttachNetworkGroupsEnabled));
            loginParams.add(new Pair<String, Object>("directAttachedUntaggedEnabled", directAttachedUntaggedEnabled));
            if (timezone!=null) {
            	loginParams.add(new Pair<String, Object>("timezone", timezone));
            	loginParams.add(new Pair<String, Object>("timezoneOffset", offsetInHrs));
            }

            return loginParams;
        }
        return null;
    }

    public void logoutUser(long userId) {
        _ms.logoutUser(Long.valueOf(userId));
        return;
    }

    public boolean verifyUser(Long userId) {
    	User user = _ms.findUserById(userId);
    	Account account = null;
    	if (user != null) {
    	    account = _ms.findAccountById(user.getAccountId());
    	}

    	if ((user == null) || (user.getRemoved() != null) || !user.getState().equals(Account.ACCOUNT_STATE_ENABLED) || (account == null) || !account.getState().equals(Account.ACCOUNT_STATE_ENABLED)) {
    		s_logger.warn("Deleted/Disabled/Locked user with id=" + userId + " attempting to access public API");
    		return false;
    	}
    	return true;
    }

    public static boolean isCommandAvailable(short accountType, String commandName) {
        boolean isCommandAvailable = false;
        switch (accountType) {
        case Account.ACCOUNT_TYPE_ADMIN:
            isCommandAvailable = s_adminCommands.contains(commandName);
            break;
        case Account.ACCOUNT_TYPE_DOMAIN_ADMIN:
            isCommandAvailable = s_resellerCommands.contains(commandName);
            break;
        case Account.ACCOUNT_TYPE_READ_ONLY_ADMIN:
            isCommandAvailable = s_readOnlyAdminCommands.contains(commandName);
            break;
        case Account.ACCOUNT_TYPE_NORMAL:
            isCommandAvailable = s_userCommands.contains(commandName);
            break;
        }
        return isCommandAvailable;
    }

    // FIXME: rather than isError, we might was to pass in the status code to give more flexibility
    private void writeResponse(HttpResponse resp, final String responseText, final boolean isError, String responseType) {
        try {
            resp.setStatusCode(isError? HttpStatus.SC_INTERNAL_SERVER_ERROR : HttpStatus.SC_OK);

            BasicHttpEntity body = new BasicHttpEntity();
            if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                // JSON response
                body.setContentType("text/javascript; charset=UTF-8");
                if (responseText == null) {
                    body.setContent(new ByteArrayInputStream("{ \"error\" : { \"description\" : \"Internal Server Error\" } }".getBytes()));
                }
            } else {
                body.setContentType("text/xml; charset=UTF-8");
                if (responseText == null) {
                    body.setContent(new ByteArrayInputStream("<error>Internal Server Error</error>".getBytes()));
                }
            }

            if (responseText != null) {
                body.setContent(new ByteArrayInputStream(responseText.getBytes()));
            }
            resp.setEntity(body);
        } catch (Exception ex) {
            s_logger.error("error!", ex);
        }
    }

    private String buildErrorResponse(String errorStr, String responseType) {
        StringBuffer sb = new StringBuffer();
        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            // JSON response
            sb.append("{ \"error\" : { \"description\" : \"" + errorStr + "\" } }");
        } else {
            sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            sb.append("<error><description>");
            sb.append(errorStr);
            sb.append("</description></error>");
        }
        return sb.toString();
    }

    // FIXME:  the following two threads are copied from http://svn.apache.org/repos/asf/httpcomponents/httpcore/trunk/httpcore/src/examples/org/apache/http/examples/ElementalHttpServer.java
    //         we have to cite a license if we are using this code directly, so we need to add the appropriate citation or modify the code to be very specific to our needs
    static class ListenerThread extends Thread {
        private HttpService _httpService = null;
        private ServerSocket _serverSocket = null;
        private HttpParams _params = null;

        public ListenerThread(ApiServer requestHandler, int port) {
            try {
                _serverSocket = new ServerSocket(port);
            } catch (IOException ioex) {
                s_logger.error("error initializing api server", ioex);
                return;
            }

            _params = new BasicHttpParams();
            _params
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

            // Set up the HTTP protocol processor
            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new ResponseDate());
            httpproc.addInterceptor(new ResponseServer());
            httpproc.addInterceptor(new ResponseContent());
            httpproc.addInterceptor(new ResponseConnControl());

            // Set up request handlers
            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
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
                    Socket socket = _serverSocket.accept();
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, _params);

                    // Execute a new worker task to handle the request
                    _executor.execute(new WorkerTask(_httpService, conn, _workerCount++));
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    s_logger.error("I/O error initializing connection thread", e);
                    break;
                }
            }
        }
    }

    static class WorkerTask implements Runnable {
        private final HttpService _httpService;
        private final HttpServerConnection _conn;

        public WorkerTask(
                final HttpService httpService,
                final HttpServerConnection conn,
                final int count) {
            _httpService = httpService;
            _conn = conn;
        }

        @Override
		public void run() {
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && _conn.isOpen()) {
                	try {
	                    _httpService.handleRequest(_conn, context);
	                    _conn.close();
                	} finally {
                		StackMaid.current().exitCleanup();
                	}
                }
            } catch (ConnectionClosedException ex) {
                if (s_logger.isTraceEnabled()) s_logger.trace("ApiServer:  Client closed connection");
            } catch (IOException ex) {
                if (s_logger.isTraceEnabled()) s_logger.trace("ApiServer:  IOException - " + ex);
            } catch (HttpException ex) {
                s_logger.warn("ApiServer:  Unrecoverable HTTP protocol violation" + ex);
            } finally {
                try {
                    _conn.shutdown();
                } catch (IOException ignore) {}
            }
        }
    }
}
