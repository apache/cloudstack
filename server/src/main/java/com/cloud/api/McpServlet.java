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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.managed.context.ManagedContext;
import org.apache.cloudstack.mcp.McpToken;
import org.apache.cloudstack.mcp.McpTokenService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.HttpUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * MCP (Model Context Protocol) server of the management server, using the Streamable HTTP transport without streaming and without sessions:
 * each POST carries one JSON-RPC message and is answered with one JSON body. Both the protocol revisions that start with the initialize
 * handshake and the revision without handshake are served, the latter being recognized by the protocol version in the request metadata. Requests are authenticated with an MCP token
 * (see {@link McpTokenService}) and every API is executed through {@link ApiServerService}, as the calling user.
 * <p>
 * Instead of one tool per API, the server exposes a few tools to search, describe and execute any API the user has access to.
 */
public class McpServlet extends HttpServlet {
    protected static Logger LOGGER = LogManager.getLogger(McpServlet.class);
    private static final Logger ACCESSLOGGER = LogManager.getLogger("apiserver." + McpServlet.class.getName());

    // Protocol revisions based on the initialize handshake, newest first
    static final List<String> HANDSHAKE_PROTOCOL_VERSIONS = List.of("2025-11-25", "2025-06-18");
    // Protocol revision without handshake: each request informs the version, in the metadata of its parameters, and is served on its own
    static final String STATELESS_PROTOCOL_VERSION = "2026-07-28";
    static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of(STATELESS_PROTOCOL_VERSION, "2025-11-25", "2025-06-18");

    private static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    private static final String METHOD_HEADER = "Mcp-Method";
    private static final String NAME_HEADER = "Mcp-Name";
    private static final String PROTOCOL_VERSION_META = "io.modelcontextprotocol/protocolVersion";
    private static final String CLIENT_CAPABILITIES_META = "io.modelcontextprotocol/clientCapabilities";
    private static final String SERVER_INFO_META = "io.modelcontextprotocol/serverInfo";
    // The tools and the server description only change when the management server is upgraded
    private static final long CACHE_TTL_IN_MILLIS = 3600000;

    static final int PARSE_ERROR = -32700;
    static final int INVALID_REQUEST = -32600;
    static final int METHOD_NOT_FOUND = -32601;
    static final int INVALID_PARAMS = -32602;
    static final int INTERNAL_ERROR = -32603;
    static final int HEADER_MISMATCH = -32020;
    static final int UNSUPPORTED_PROTOCOL_VERSION = -32022;

    static final String TOOL_SEARCH_APIS = "cloudstack_search_apis";
    static final String TOOL_DESCRIBE_API = "cloudstack_describe_api";
    static final String TOOL_EXECUTE_API = "cloudstack_execute_api";
    static final String TOOL_QUERY_ASYNC_JOB = "cloudstack_query_async_job";

    private static final int MAX_SEARCH_RESULTS = 50;
    private static final int JOB_STATUS_PENDING = 0;
    private static final int JOB_STATUS_FAILED = 2;

    private static final String INSTRUCTIONS = "This server gives access to the Apache CloudStack API with the permissions of the user that owns the token. " +
            "Find the API for a task with " + TOOL_SEARCH_APIS + ", check its parameters with " + TOOL_DESCRIBE_API + " and run it with " + TOOL_EXECUTE_API + ". " +
            "IDs are UUIDs obtained from the list APIs. List APIs can return many entries: filter them with parameters such as keyword, name or id, " +
            "and paginate with page and pagesize.";

    @Inject
    ApiServerService apiServer;
    @Inject
    AccountService accountService;
    @Inject
    McpTokenService mcpTokenService;
    @Inject
    ManagedContext managedContext;
    @Inject
    ManagementService managementService;
    @Inject
    ProjectDao projectDao;

    long jobPollIntervalInMillis = 1000;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
    }

    boolean isEnabled() {
        return McpTokenService.McpEnabled.value();
    }

    int getAsyncJobWait() {
        return McpTokenService.McpAsyncJobWait.value();
    }

    /**
     * Only POST is implemented: the other methods, used by the transport for streams and sessions, get the 405 of {@link HttpServlet}.
     */
    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (!isEnabled()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        super.service(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) {
        managedContext.runWithContext(() -> processRequest(req, resp));
    }

    void processRequest(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            if (!isOriginAllowed(req)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            McpToken token = mcpTokenService.authenticate(getBearerToken(req));
            User user = token != null && apiServer.verifyUser(token.getUserId()) ? accountService.getActiveUser(token.getUserId()) : null;
            Account account = user != null ? accountService.getAccount(user.getAccountId()) : null;
            if (account == null || !apiServer.verifyApiKeyAccessAllowed(user, account)) {
                resp.setHeader("WWW-Authenticate", "Bearer");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            InetAddress remoteAddress = ApiServlet.getClientAddress(req);
            CallContext.register(user, account);
            try {
                processMessage(req, resp, user.getId(), remoteAddress);
            } finally {
                CallContext.unregister();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Unexpected error while processing an MCP request", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Protection against DNS rebinding required by the transport: a browser page of another origin must not reach this endpoint.
     */
    private boolean isOriginAllowed(HttpServletRequest req) {
        String origin = req.getHeader("Origin");
        if (origin == null) {
            return true;
        }
        try {
            return req.getServerName().equalsIgnoreCase(new URI(origin).getHost());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String getBearerToken(HttpServletRequest req) {
        String authorization = req.getHeader("Authorization");
        if (!StringUtils.startsWithIgnoreCase(authorization, "Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private void processMessage(HttpServletRequest req, HttpServletResponse resp, long userId, InetAddress remoteAddress) throws IOException {
        JsonElement body;
        try {
            req.setCharacterEncoding("UTF-8");
            body = JsonParser.parseReader(req.getReader());
        } catch (JsonParseException e) {
            writeResponse(resp, HttpServletResponse.SC_BAD_REQUEST, createError(null, PARSE_ERROR, "Parse error", null));
            return;
        }

        JsonObject message = body.isJsonObject() ? body.getAsJsonObject() : new JsonObject();
        JsonElement id = message.get("id");
        String method = getString(message, "method");
        if (method == null) {
            if (message.has("result") || message.has("error")) {
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            } else {
                writeResponse(resp, HttpServletResponse.SC_BAD_REQUEST, createError(id, INVALID_REQUEST, "Invalid Request", null));
            }
            return;
        }

        JsonObject params = message.get("params") instanceof JsonObject ? message.getAsJsonObject("params") : new JsonObject();
        JsonObject meta = params.get("_meta") instanceof JsonObject ? params.getAsJsonObject("_meta") : new JsonObject();
        String metaProtocolVersion = getString(meta, PROTOCOL_VERSION_META);
        boolean isStateless = metaProtocolVersion != null;
        String protocolVersion = isStateless ? metaProtocolVersion : req.getHeader(PROTOCOL_VERSION_HEADER);
        if (isStateless ? !STATELESS_PROTOCOL_VERSION.equals(protocolVersion) : protocolVersion != null && !SUPPORTED_PROTOCOL_VERSIONS.contains(protocolVersion)) {
            JsonObject data = new JsonObject();
            JsonArray supported = new JsonArray();
            SUPPORTED_PROTOCOL_VERSIONS.forEach(supported::add);
            data.add("supported", supported);
            data.addProperty("requested", protocolVersion);
            writeResponse(resp, HttpServletResponse.SC_BAD_REQUEST, createError(id, UNSUPPORTED_PROTOCOL_VERSION, "Unsupported protocol version", data));
            return;
        }

        if (id == null || id.isJsonNull()) {
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            return;
        }

        int status = HttpServletResponse.SC_OK;
        JsonObject response;
        try {
            if (isStateless) {
                validateStatelessRequest(req, method, params, meta);
                response = createResult(id, processStatelessMethod(method, params, userId, remoteAddress));
            } else {
                response = createResult(id, processMethod(method, params, userId, remoteAddress));
            }
        } catch (McpException e) {
            status = e.status;
            response = createError(id, e.code, e.getMessage(), null);
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected error while processing the MCP method [{}]", method, e);
            response = createError(id, INTERNAL_ERROR, "Internal error", null);
        }
        writeResponse(resp, status, response);
    }

    /**
     * Methods of the protocol revisions based on the initialize handshake.
     */
    private JsonObject processMethod(String method, JsonObject params, long userId, InetAddress remoteAddress) throws McpException {
        switch (method) {
            case "initialize":
                return initialize(params);
            case "ping":
                return new JsonObject();
            case "tools/list":
                JsonObject tools = new JsonObject();
                tools.add("tools", createTools());
                return tools;
            case "tools/call":
                return callTool(params, userId, remoteAddress);
            default:
                throw new McpException(HttpServletResponse.SC_OK, METHOD_NOT_FOUND, "Method not found: " + method);
        }
    }

    private JsonObject initialize(JsonObject params) {
        String requestedVersion = getString(params, "protocolVersion");

        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", HANDSHAKE_PROTOCOL_VERSIONS.contains(requestedVersion) ? requestedVersion : HANDSHAKE_PROTOCOL_VERSIONS.get(0));
        result.add("capabilities", createCapabilities());
        result.add("serverInfo", createServerInfo());
        result.addProperty("instructions", INSTRUCTIONS);
        return result;
    }

    /**
     * In the protocol revision without handshake, the routing headers of the request must be present and match its body.
     */
    private void validateStatelessRequest(HttpServletRequest req, String method, JsonObject params, JsonObject meta) throws McpException {
        validateHeader(req, PROTOCOL_VERSION_HEADER, STATELESS_PROTOCOL_VERSION);
        validateHeader(req, METHOD_HEADER, method);
        if ("tools/call".equals(method)) {
            validateHeader(req, NAME_HEADER, getString(params, "name"));
        }
        if (!(meta.get(CLIENT_CAPABILITIES_META) instanceof JsonObject)) {
            throw new McpException(HttpServletResponse.SC_BAD_REQUEST, INVALID_PARAMS, "Missing required metadata: " + CLIENT_CAPABILITIES_META);
        }
    }

    private void validateHeader(HttpServletRequest req, String header, String bodyValue) throws McpException {
        String value = req.getHeader(header);
        // Values that are not plain ASCII are sent encoded in Base64
        if (value != null && value.startsWith("=?base64?") && value.endsWith("?=")) {
            value = new String(Base64.getDecoder().decode(value.substring("=?base64?".length(), value.length() - "?=".length())), StandardCharsets.UTF_8);
        }
        if (value == null || !value.equals(bodyValue)) {
            throw new McpException(HttpServletResponse.SC_BAD_REQUEST, HEADER_MISMATCH,
                    String.format("Header mismatch: %s header value '%s' does not match body value '%s'", header, value, bodyValue));
        }
    }

    /**
     * Methods of the protocol revision without handshake.
     */
    private JsonObject processStatelessMethod(String method, JsonObject params, long userId, InetAddress remoteAddress) throws McpException {
        JsonObject result;
        switch (method) {
            case "server/discover":
                JsonArray supportedVersions = new JsonArray();
                SUPPORTED_PROTOCOL_VERSIONS.forEach(supportedVersions::add);
                result = new JsonObject();
                result.add("supportedVersions", supportedVersions);
                result.add("capabilities", createCapabilities());
                result.addProperty("instructions", INSTRUCTIONS);
                addCacheHints(result);
                break;
            case "tools/list":
                result = new JsonObject();
                result.add("tools", createTools());
                addCacheHints(result);
                break;
            case "tools/call":
                result = callTool(params, userId, remoteAddress);
                break;
            default:
                throw new McpException(HttpServletResponse.SC_NOT_FOUND, METHOD_NOT_FOUND, "Method not found: " + method);
        }

        JsonObject meta = new JsonObject();
        meta.add(SERVER_INFO_META, createServerInfo());
        result.add("_meta", meta);
        result.addProperty("resultType", "complete");
        return result;
    }

    private void addCacheHints(JsonObject result) {
        result.addProperty("ttlMs", CACHE_TTL_IN_MILLIS);
        // The same for every user
        result.addProperty("cacheScope", "public");
    }

    private JsonObject createCapabilities() {
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        return capabilities;
    }

    private JsonObject createServerInfo() {
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "cloudstack");
        serverInfo.addProperty("title", "Apache CloudStack");
        serverInfo.addProperty("version", managementService.getVersion());
        return serverInfo;
    }

    private JsonArray createTools() {
        JsonArray tools = new JsonArray();
        tools.add(createTool(TOOL_SEARCH_APIS, "Search APIs", true,
                "Searches the CloudStack APIs available to the user by name and description. Returns up to " + MAX_SEARCH_RESULTS +
                        " API names with their descriptions. API names follow the pattern verbResource, such as listVirtualMachines or createNetwork.",
                Map.of("keyword", createProperty("string", "Words that must all be present in the name or in the description of the API, e.g. \"list virtualmachine\".")),
                "keyword"));
        tools.add(createTool(TOOL_DESCRIBE_API, "Describe API", true,
                "Returns the description and the parameters of a CloudStack API: name, type, whether it is required and what it means. " +
                        "Use it before executing an API for the first time.",
                Map.of("name", createProperty("string", "Exact name of the API, e.g. deployVirtualMachine.")),
                "name"));
        tools.add(createTool(TOOL_EXECUTE_API, "Execute API", false,
                "Executes a CloudStack API as the user and returns its JSON response. This can create, change and destroy resources. " +
                        "For asynchronous APIs, waits for the job to finish for a limited time: if the job is still running, returns its jobid, " +
                        "to be used with " + TOOL_QUERY_ASYNC_JOB + ".",
                Map.of("command", createProperty("string", "Exact name of the API, e.g. listVirtualMachines."),
                        "params", createProperty("object", "Parameters of the API, as described by " + TOOL_DESCRIBE_API + ". " +
                                "Parameters of type list take a JSON array of values. Parameters of type map take a JSON object, " +
                                "or an array of objects when the map has several entries, " +
                                "e.g. {\"details\": {\"cpuNumber\": 2}} or {\"iptonetworklist\": [{\"networkid\": \"...\", \"ip\": \"...\"}]}.")),
                "command"));
        tools.add(createTool(TOOL_QUERY_ASYNC_JOB, "Query asynchronous job", true,
                "Waits for a limited time for an asynchronous job to finish and returns its status and result. " +
                        "The jobstatus is 0 while the job is running, 1 when it succeeded and 2 when it failed.",
                Map.of("jobid", createProperty("string", "ID of the job, as returned by " + TOOL_EXECUTE_API + ".")),
                "jobid"));
        return tools;
    }

    private JsonObject createProperty(String type, String description) {
        JsonObject property = new JsonObject();
        property.addProperty("type", type);
        property.addProperty("description", description);
        return property;
    }

    private JsonObject createTool(String name, String title, boolean readOnly, String description, Map<String, JsonObject> properties, String required) {
        JsonObject schemaProperties = new JsonObject();
        properties.forEach(schemaProperties::add);
        JsonArray schemaRequired = new JsonArray();
        schemaRequired.add(required);
        JsonObject inputSchema = new JsonObject();
        inputSchema.addProperty("type", "object");
        inputSchema.add("properties", schemaProperties);
        inputSchema.add("required", schemaRequired);

        JsonObject annotations = new JsonObject();
        annotations.addProperty("readOnlyHint", readOnly);
        if (!readOnly) {
            annotations.addProperty("destructiveHint", true);
        }
        annotations.addProperty("openWorldHint", false);

        JsonObject tool = new JsonObject();
        tool.addProperty("name", name);
        tool.addProperty("title", title);
        tool.addProperty("description", description);
        tool.add("inputSchema", inputSchema);
        tool.add("annotations", annotations);
        return tool;
    }

    /**
     * Failures of the tool itself, such as invalid arguments or an error returned by the API, are reported in the tool result,
     * where the model can see them and correct the call. Only an unknown tool is a protocol error.
     */
    private JsonObject callTool(JsonObject params, long userId, InetAddress remoteAddress) throws McpException {
        String name = getString(params, "name");
        JsonObject arguments = params.get("arguments") instanceof JsonObject ? params.getAsJsonObject("arguments") : new JsonObject();
        try {
            switch (StringUtils.defaultString(name)) {
                case TOOL_SEARCH_APIS:
                    return createToolResult(searchApis(getRequiredArgument(arguments, "keyword"), userId, remoteAddress), false);
                case TOOL_DESCRIBE_API:
                    return createToolResult(describeApi(getRequiredArgument(arguments, "name"), userId, remoteAddress), false);
                case TOOL_EXECUTE_API:
                    return executeApi(arguments, userId, remoteAddress);
                case TOOL_QUERY_ASYNC_JOB:
                    return createJobResult(waitForJob(getRequiredArgument(arguments, "jobid"), userId, remoteAddress, true));
                default:
                    throw new McpException(HttpServletResponse.SC_OK, INVALID_PARAMS, "Unknown tool: " + name);
            }
        } catch (ServerApiException e) {
            JsonObject error = new JsonObject();
            error.addProperty("errorcode", e.getErrorCode().getHttpCode());
            error.addProperty("errortext", e.getDescription());
            return createToolResult(error.toString(), true);
        } catch (IllegalArgumentException e) {
            return createToolResult(e.getMessage(), true);
        }
    }

    private String getRequiredArgument(JsonObject arguments, String name) {
        String value = getString(arguments, name);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(String.format("The argument [%s] is required and must be a string.", name));
        }
        return value;
    }

    private String searchApis(String keyword, long userId, InetAddress remoteAddress) {
        String[] terms = keyword.toLowerCase().trim().split("\\s+");
        List<String> matches = new ArrayList<>();
        for (JsonElement element : listApis(null, userId, remoteAddress)) {
            JsonObject api = element.getAsJsonObject();
            String name = getString(api, "name");
            String description = StringUtils.defaultString(getString(api, "description"));
            String searchable = (name + " " + description).toLowerCase();
            if (Arrays.stream(terms).allMatch(searchable::contains)) {
                boolean isAsync = api.has("isasync") && api.get("isasync").getAsBoolean();
                matches.add(String.format("%s%s: %s", name, isAsync ? " [async]" : "", description));
            }
        }

        if (matches.isEmpty()) {
            return String.format("No API found for [%s]. Try fewer or different words.", keyword);
        }
        int total = matches.size();
        List<String> shown = matches.subList(0, Math.min(total, MAX_SEARCH_RESULTS));
        String header = total > MAX_SEARCH_RESULTS ? String.format("%d APIs found, showing the first %d. Refine the search to see the others.", total, MAX_SEARCH_RESULTS)
                : String.format("%d APIs found.", total);
        return header + "\n" + String.join("\n", shown);
    }

    private String describeApi(String name, long userId, InetAddress remoteAddress) {
        JsonArray apis = listApis(name, userId, remoteAddress);
        if (apis.size() == 0) {
            throw new IllegalArgumentException(String.format("The API [%s] does not exist or is not available to the user.", name));
        }

        // The description of the response of the API is left out: it is large and the response describes itself
        JsonObject api = apis.get(0).getAsJsonObject();
        JsonObject description = new JsonObject();
        for (String field : List.of("name", "description", "isasync", "since")) {
            if (api.has(field)) {
                description.add(field, api.get(field));
            }
        }
        JsonArray parameters = new JsonArray();
        if (api.get("params") instanceof JsonArray) {
            for (JsonElement element : api.getAsJsonArray("params")) {
                JsonObject param = element.getAsJsonObject();
                JsonObject parameter = new JsonObject();
                for (String field : List.of("name", "type", "required", "description", "length", "related")) {
                    if (param.has(field)) {
                        parameter.add(field, param.get(field));
                    }
                }
                parameters.add(parameter);
            }
        }
        description.add("params", parameters);
        return description.toString();
    }

    private JsonArray listApis(String name, long userId, InetAddress remoteAddress) {
        Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.COMMAND, "listApis");
        if (name != null) {
            params.put(ApiConstants.NAME, name);
        }
        JsonObject response = getResponseObject(callApi(params, userId, remoteAddress, true));
        return response.get("api") instanceof JsonArray ? response.getAsJsonArray("api") : new JsonArray();
    }

    private JsonObject executeApi(JsonObject arguments, long userId, InetAddress remoteAddress) {
        String command = getRequiredArgument(arguments, "command");
        JsonElement apiParams = arguments.get("params");
        if (apiParams != null && !apiParams.isJsonNull() && !apiParams.isJsonObject()) {
            throw new IllegalArgumentException("The argument [params] must be an object.");
        }

        Map<String, String> params = McpParamEncoder.encode(apiParams instanceof JsonObject ? apiParams.getAsJsonObject() : new JsonObject());
        params.put(ApiConstants.COMMAND, command);
        String response = callApi(params, userId, remoteAddress, true);

        Class<?> cmdClass = apiServer.getCmdClass(command);
        if (cmdClass != null && BaseAsyncCmd.class.isAssignableFrom(cmdClass)) {
            String jobId = getString(getResponseObject(response), ApiConstants.JOB_ID);
            if (jobId != null) {
                return createJobResult(waitForJob(jobId, userId, remoteAddress, false));
            }
        }
        return createToolResult(response, false);
    }

    /**
     * Polls the job until it finishes or the configured wait time is over, so the request does not outlive the timeouts of the HTTP
     * connection. Only the first query is subject to the API access checks and to the API rate limit.
     *
     * @return the response of queryAsyncJobResult
     */
    private String waitForJob(String jobId, long userId, InetAddress remoteAddress, boolean verifyFirstQuery) {
        Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.COMMAND, "queryAsyncJobResult");
        params.put(ApiConstants.JOB_ID, jobId);

        long deadline = System.currentTimeMillis() + getAsyncJobWait() * 1000L;
        String response = callApi(params, userId, remoteAddress, verifyFirstQuery);
        while (getJobStatus(response) == JOB_STATUS_PENDING && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(jobPollIntervalInMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            response = callApi(params, userId, remoteAddress, false);
        }
        return response;
    }

    private int getJobStatus(String queryAsyncJobResultResponse) {
        return getResponseObject(queryAsyncJobResultResponse).get(ApiConstants.JOB_STATUS).getAsInt();
    }

    private JsonObject createJobResult(String queryAsyncJobResultResponse) {
        int status = getJobStatus(queryAsyncJobResultResponse);
        JsonObject result = createToolResult(queryAsyncJobResultResponse, status == JOB_STATUS_FAILED);
        if (status == JOB_STATUS_PENDING) {
            result.getAsJsonArray("content").add(createTextContent("The job is still running. Call " + TOOL_QUERY_ASYNC_JOB + " with its jobid to wait for the result."));
        }
        return result;
    }

    /**
     * Executes an API as the calling user, with the same checks of a request to the API endpoint authenticated by a session.
     *
     * @return the response of the API, serialized as JSON
     */
    private String callApi(Map<String, String> apiParams, long userId, InetAddress remoteAddress, boolean verify) {
        String command = apiParams.get(ApiConstants.COMMAND);
        Map<String, Object[]> params = new HashMap<>();
        apiParams.forEach((name, value) -> params.put(name, new String[] {value}));
        params.put(ApiConstants.HTTPMETHOD, new String[] {"POST"});

        ApiServlet.setProjectContext(params, projectDao);
        if (verify && !apiServer.verifyRequest(params, userId, remoteAddress)) {
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, String.format("The command [%s] is not allowed from the address of the request.", command));
        }

        StringBuilder auditTrail = new StringBuilder();
        try {
            String response = apiServer.handleRequest(params, HttpUtils.RESPONSE_TYPE_JSON, auditTrail);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR, String.format("The command [%s] is not available through MCP.", command));
            }
            return response;
        } catch (ServerApiException e) {
            auditTrail.append(" ").append(e.getErrorCode()).append(" ").append(e.getDescription());
            throw e;
        } finally {
            if (verify) {
                // Parameter values are not logged, as they may hold sensitive data
                ACCESSLOGGER.info("(userId={} accountId={}) {} -- MCP command={} params={}{}", userId, CallContext.current().getCallingAccountId(),
                        remoteAddress.getHostAddress(), command, apiParams.keySet(), auditTrail);
            }
        }
    }

    /**
     * @return the object that wraps the content of an API response, e.g. the value of "listzonesresponse"
     */
    private JsonObject getResponseObject(String apiResponse) {
        return JsonParser.parseString(apiResponse).getAsJsonObject().entrySet().iterator().next().getValue().getAsJsonObject();
    }

    private String getString(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private JsonObject createTextContent(String text) {
        JsonObject content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", text);
        return content;
    }

    private JsonObject createToolResult(String text, boolean isError) {
        JsonArray content = new JsonArray();
        content.add(createTextContent(text));
        JsonObject result = new JsonObject();
        result.add("content", content);
        result.addProperty("isError", isError);
        return result;
    }

    private JsonObject createResult(JsonElement id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        response.add("result", result);
        return response;
    }

    private JsonObject createError(JsonElement id, int code, String message, JsonObject data) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        if (data != null) {
            error.add("data", data);
        }
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id == null ? JsonNull.INSTANCE : id);
        response.add("error", error);
        return response;
    }

    private void writeResponse(HttpServletResponse resp, int status, JsonObject response) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().print(response.toString());
    }

    private static class McpException extends Exception {
        private final int status;
        private final int code;

        McpException(int status, int code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }
}
