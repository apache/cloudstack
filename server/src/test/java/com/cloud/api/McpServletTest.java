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

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.zone.ListZonesCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mcp.McpToken;
import org.apache.cloudstack.mcp.McpTokenService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.projects.Project;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RunWith(MockitoJUnitRunner.Silent.class)
public class McpServletTest {
    private static final String TOKEN = "csmcp_token";
    private static final long USER_ID = 42L;
    private static final long ACCOUNT_ID = 7L;

    @Mock
    private ApiServerService apiServer;
    @Mock
    private AccountService accountService;
    @Mock
    private McpTokenService mcpTokenService;
    @Mock
    private ManagementService managementService;
    @Mock
    private ProjectDao projectDao;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private McpToken token;
    @Mock
    private User user;
    @Mock
    private Account account;

    private McpServlet servlet;
    private StringWriter responseWriter;
    private final List<Map<String, Object[]>> executedRequests = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        servlet = Mockito.spy(new McpServlet());
        servlet.apiServer = apiServer;
        servlet.accountService = accountService;
        servlet.mcpTokenService = mcpTokenService;
        servlet.managementService = managementService;
        servlet.projectDao = projectDao;
        servlet.jobPollIntervalInMillis = 1;
        Mockito.doReturn(true).when(servlet).isEnabled();
        Mockito.doReturn(0).when(servlet).getAsyncJobWait();

        responseWriter = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getProtocol()).thenReturn("HTTP/1.1");
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        Mockito.when(request.getServerName()).thenReturn("cloudstack.cloud.test");
        Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);

        Mockito.when(mcpTokenService.authenticate(TOKEN)).thenReturn(token);
        Mockito.when(token.getUserId()).thenReturn(USER_ID);
        Mockito.when(user.getId()).thenReturn(USER_ID);
        Mockito.when(user.getAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(account.getId()).thenReturn(ACCOUNT_ID);
        Mockito.when(apiServer.verifyUser(USER_ID)).thenReturn(true);
        Mockito.when(accountService.getActiveUser(USER_ID)).thenReturn(user);
        Mockito.when(accountService.getAccount(ACCOUNT_ID)).thenReturn(account);
        Mockito.when(apiServer.verifyApiKeyAccessAllowed(user, account)).thenReturn(true);
        Mockito.when(apiServer.verifyRequest(Mockito.anyMap(), Mockito.eq(USER_ID), Mockito.any(InetAddress.class))).thenReturn(true);
        Mockito.when(managementService.getVersion()).thenReturn("24.0.0");
    }

    private void setBody(String body) throws Exception {
        Mockito.when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
    }

    private JsonObject post(String body) throws Exception {
        setBody(body);
        servlet.processRequest(request, response);
        String written = responseWriter.toString();
        return written.isEmpty() ? null : JsonParser.parseString(written).getAsJsonObject();
    }

    private JsonObject callTool(String name, String arguments) throws Exception {
        return post(String.format("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"%s\",\"arguments\":%s}}", name, arguments))
                .getAsJsonObject("result");
    }

    private String getText(JsonObject toolResult) {
        return toolResult.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
    }

    /**
     * Answers each executed API with the next of the given responses, repeating the last one.
     */
    private void mockApiResponses(String... responses) {
        Mockito.when(apiServer.handleRequest(Mockito.anyMap(), Mockito.eq("json"), Mockito.any(StringBuilder.class))).thenAnswer(invocation -> {
            executedRequests.add(invocation.getArgument(0));
            return responses[Math.min(executedRequests.size(), responses.length) - 1];
        });
    }

    private String getParam(Map<String, Object[]> params, String name) {
        return params.get(name) == null ? null : (String)params.get(name)[0];
    }

    private void verifyStatus(int status) {
        Mockito.verify(response).setStatus(status);
    }

    @Test
    public void disabledServerAnswersNotFoundToAnyMethod() throws Exception {
        Mockito.doReturn(false).when(servlet).isEnabled();

        servlet.service(request, response);

        verifyStatus(HttpServletResponse.SC_NOT_FOUND);
        Mockito.verifyNoInteractions(mcpTokenService);
    }

    @Test
    public void getIsNotAllowed() throws Exception {
        Mockito.when(request.getMethod()).thenReturn("GET");

        servlet.service(request, response);

        Mockito.verify(response).sendError(Mockito.eq(HttpServletResponse.SC_METHOD_NOT_ALLOWED), Mockito.anyString());
    }

    @Test
    public void requestWithoutTokenIsUnauthorized() throws Exception {
        Mockito.when(request.getHeader("Authorization")).thenReturn(null);

        post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");

        verifyStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Mockito.verify(response).setHeader("WWW-Authenticate", "Bearer");
        Mockito.verify(mcpTokenService).authenticate(null);
    }

    @Test
    public void requestWithInvalidTokenIsUnauthorized() throws Exception {
        Mockito.when(mcpTokenService.authenticate(TOKEN)).thenReturn(null);

        post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");

        verifyStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void requestOfDisabledUserIsUnauthorized() throws Exception {
        Mockito.when(apiServer.verifyUser(USER_ID)).thenReturn(false);

        post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");

        verifyStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void requestOfUserWithoutApiKeyAccessIsUnauthorized() throws Exception {
        Mockito.when(apiServer.verifyApiKeyAccessAllowed(user, account)).thenReturn(false);

        post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");

        verifyStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void requestFromAnotherOriginIsForbidden() throws Exception {
        Mockito.when(request.getHeader("Origin")).thenReturn("https://evil.example.com");

        post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");

        verifyStatus(HttpServletResponse.SC_FORBIDDEN);
        Mockito.verifyNoInteractions(mcpTokenService);
    }

    @Test
    public void requestFromTheSameOriginIsAccepted() throws Exception {
        Mockito.when(request.getHeader("Origin")).thenReturn("https://cloudstack.cloud.test");

        post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}");

        verifyStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void callContextIsAlwaysUnregistered() throws Exception {
        post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}");

        Assert.assertNull(CallContext.unregister());
    }

    @Test
    public void initializeAnswersWithTheRequestedProtocolVersionWhenSupported() throws Exception {
        JsonObject result = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{}," +
                "\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}").getAsJsonObject("result");

        Assert.assertEquals("2025-06-18", result.get("protocolVersion").getAsString());
        Assert.assertTrue(result.getAsJsonObject("capabilities").has("tools"));
        Assert.assertEquals("24.0.0", result.getAsJsonObject("serverInfo").get("version").getAsString());
        verifyStatus(HttpServletResponse.SC_OK);
        Mockito.verify(response).setContentType("application/json");
    }

    @Test
    public void initializeAnswersWithTheNewestSupportedVersionWhenTheRequestedIsNot() throws Exception {
        JsonObject result = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"1999-01-01\"}}").getAsJsonObject("result");

        Assert.assertEquals(McpServlet.SUPPORTED_PROTOCOL_VERSIONS.get(0), result.get("protocolVersion").getAsString());
    }

    @Test
    public void pingAnswersWithAnEmptyResult() throws Exception {
        JsonObject answer = post("{\"jsonrpc\":\"2.0\",\"id\":\"abc\",\"method\":\"ping\"}");

        Assert.assertEquals("abc", answer.get("id").getAsString());
        Assert.assertEquals(0, answer.getAsJsonObject("result").size());
    }

    @Test
    public void toolsListReturnsTheFourTools() throws Exception {
        JsonObject result = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}").getAsJsonObject("result");

        List<String> names = new ArrayList<>();
        result.getAsJsonArray("tools").forEach(tool -> {
            names.add(tool.getAsJsonObject().get("name").getAsString());
            Assert.assertEquals("object", tool.getAsJsonObject().getAsJsonObject("inputSchema").get("type").getAsString());
        });
        Assert.assertEquals(List.of(McpServlet.TOOL_SEARCH_APIS, McpServlet.TOOL_DESCRIBE_API, McpServlet.TOOL_EXECUTE_API, McpServlet.TOOL_QUERY_ASYNC_JOB), names);
    }

    @Test
    public void notificationIsAcceptedWithoutBody() throws Exception {
        Assert.assertNull(post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));

        verifyStatus(HttpServletResponse.SC_ACCEPTED);
    }

    @Test
    public void invalidJsonIsAParseError() throws Exception {
        JsonObject answer = post("{not json");

        Assert.assertEquals(McpServlet.PARSE_ERROR, answer.getAsJsonObject("error").get("code").getAsInt());
        verifyStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void batchIsAnInvalidRequest() throws Exception {
        JsonObject answer = post("[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}]");

        Assert.assertEquals(McpServlet.INVALID_REQUEST, answer.getAsJsonObject("error").get("code").getAsInt());
        verifyStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void unknownMethodIsNotFound() throws Exception {
        JsonObject answer = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"resources/list\"}");

        Assert.assertEquals(McpServlet.METHOD_NOT_FOUND, answer.getAsJsonObject("error").get("code").getAsInt());
    }

    @Test
    public void unknownToolIsAnInvalidParamsError() throws Exception {
        JsonObject answer = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"unknown\",\"arguments\":{}}}");

        Assert.assertEquals(McpServlet.INVALID_PARAMS, answer.getAsJsonObject("error").get("code").getAsInt());
    }

    @Test
    public void unsupportedProtocolVersionInHeaderIsRejectedListingTheSupportedOnes() throws Exception {
        Mockito.when(request.getHeader("MCP-Protocol-Version")).thenReturn("1999-01-01");

        JsonObject error = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}").getAsJsonObject("error");

        Assert.assertEquals(McpServlet.UNSUPPORTED_PROTOCOL_VERSION, error.get("code").getAsInt());
        Assert.assertEquals(McpServlet.SUPPORTED_PROTOCOL_VERSIONS.size(), error.getAsJsonObject("data").getAsJsonArray("supported").size());
        verifyStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void unsupportedProtocolVersionInRequestMetadataIsRejected() throws Exception {
        JsonObject error = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{\"_meta\":" +
                "{\"io.modelcontextprotocol/protocolVersion\":\"2026-07-28\"}}}").getAsJsonObject("error");

        Assert.assertEquals(McpServlet.UNSUPPORTED_PROTOCOL_VERSION, error.get("code").getAsInt());
        Assert.assertEquals("2026-07-28", error.getAsJsonObject("data").get("requested").getAsString());
        verifyStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void executeApiVerifiesAndExecutesTheCommandWithTheEncodedParameters() throws Exception {
        Mockito.doReturn(ListZonesCmd.class).when(apiServer).getCmdClass("listZones");
        mockApiResponses("{\"listzonesresponse\":{\"count\":1,\"zone\":[{\"name\":\"z1\"}]}}");

        JsonObject result = callTool(McpServlet.TOOL_EXECUTE_API, "{\"command\":\"listZones\",\"params\":{\"available\":true,\"ids\":[\"a\",\"b\"]}}");

        Assert.assertFalse(result.get("isError").getAsBoolean());
        Assert.assertTrue(getText(result).contains("\"z1\""));

        ArgumentCaptor<Map<String, Object[]>> verified = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(apiServer).verifyRequest(verified.capture(), Mockito.eq(USER_ID), Mockito.any(InetAddress.class));
        Assert.assertEquals("listZones", getParam(verified.getValue(), "command"));

        Assert.assertEquals(1, executedRequests.size());
        Map<String, Object[]> executed = executedRequests.get(0);
        Assert.assertEquals("listZones", getParam(executed, "command"));
        Assert.assertEquals("true", getParam(executed, "available"));
        Assert.assertEquals("a,b", getParam(executed, "ids"));
        Assert.assertEquals("POST", getParam(executed, "httpmethod"));
    }

    @Test
    public void executeApiSetsTheProjectOfTheRequestInTheCallContext() throws Exception {
        ProjectVO project = Mockito.mock(ProjectVO.class);
        Mockito.when(projectDao.findByUuid("project-uuid")).thenReturn(project);
        List<Project> projectsDuringVerification = new ArrayList<>();
        Mockito.when(apiServer.verifyRequest(Mockito.anyMap(), Mockito.eq(USER_ID), Mockito.any(InetAddress.class))).thenAnswer(invocation -> {
            projectsDuringVerification.add(CallContext.current().getProject());
            return true;
        });
        mockApiResponses("{\"listvirtualmachinesresponse\":{}}");

        callTool(McpServlet.TOOL_EXECUTE_API, "{\"command\":\"listVirtualMachines\",\"params\":{\"projectid\":\"project-uuid\"}}");

        Assert.assertEquals(List.of(project), projectsDuringVerification);
    }

    @Test
    public void executeApiReportsApiErrorsInTheToolResult() throws Exception {
        Mockito.when(apiServer.handleRequest(Mockito.anyMap(), Mockito.eq("json"), Mockito.any(StringBuilder.class)))
                .thenThrow(new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find zone"));

        JsonObject answer = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"" + McpServlet.TOOL_EXECUTE_API +
                "\",\"arguments\":{\"command\":\"listZones\"}}}");

        Assert.assertFalse(answer.has("error"));
        JsonObject result = answer.getAsJsonObject("result");
        Assert.assertTrue(result.get("isError").getAsBoolean());
        Assert.assertTrue(getText(result).contains("Unable to find zone"));
        verifyStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void executeApiReportsDeniedCommandsInTheToolResultWithoutExecutingThem() throws Exception {
        Mockito.when(apiServer.verifyRequest(Mockito.anyMap(), Mockito.eq(USER_ID), Mockito.any(InetAddress.class)))
                .thenThrow(new ServerApiException(ApiErrorCode.UNAUTHORIZED, "not available for user"));

        JsonObject result = callTool(McpServlet.TOOL_EXECUTE_API, "{\"command\":\"listHosts\"}");

        Assert.assertTrue(result.get("isError").getAsBoolean());
        Mockito.verify(apiServer, Mockito.never()).handleRequest(Mockito.anyMap(), Mockito.anyString(), Mockito.any(StringBuilder.class));
    }

    @Test
    public void executeApiReportsRequestsFromDisallowedAddressesInTheToolResult() throws Exception {
        Mockito.when(apiServer.verifyRequest(Mockito.anyMap(), Mockito.eq(USER_ID), Mockito.any(InetAddress.class))).thenReturn(false);

        JsonObject result = callTool(McpServlet.TOOL_EXECUTE_API, "{\"command\":\"listZones\"}");

        Assert.assertTrue(result.get("isError").getAsBoolean());
        Mockito.verify(apiServer, Mockito.never()).handleRequest(Mockito.anyMap(), Mockito.anyString(), Mockito.any(StringBuilder.class));
    }

    @Test
    public void executeApiReportsInvalidArgumentsInTheToolResult() throws Exception {
        Assert.assertTrue(callTool(McpServlet.TOOL_EXECUTE_API, "{}").get("isError").getAsBoolean());

        responseWriter.getBuffer().setLength(0);
        Assert.assertTrue(callTool(McpServlet.TOOL_EXECUTE_API, "{\"command\":\"listZones\",\"params\":{\"apikey\":\"x\"}}").get("isError").getAsBoolean());

        Mockito.verify(apiServer, Mockito.never()).handleRequest(Mockito.anyMap(), Mockito.anyString(), Mockito.any(StringBuilder.class));
    }

    @Test
    public void executeApiReportsCommandsNotHandledByTheApiServerInTheToolResult() throws Exception {
        mockApiResponses((String)null);

        Assert.assertTrue(callTool(McpServlet.TOOL_EXECUTE_API, "{\"command\":\"login\"}").get("isError").getAsBoolean());
    }

    @Test
    public void executeApiWaitsForTheJobOfAnAsynchronousCommand() throws Exception {
        Mockito.doReturn(5).when(servlet).getAsyncJobWait();
        Mockito.doReturn(DeployVMCmd.class).when(apiServer).getCmdClass("deployVirtualMachine");
        mockApiResponses("{\"deployvirtualmachineresponse\":{\"id\":\"vm-uuid\",\"jobid\":\"job-uuid\"}}",
                "{\"queryasyncjobresultresponse\":{\"jobid\":\"job-uuid\",\"jobstatus\":0}}",
                "{\"queryasyncjobresultresponse\":{\"jobid\":\"job-uuid\",\"jobstatus\":1,\"jobresult\":{\"virtualmachine\":{\"name\":\"vm-1\"}}}}");

        JsonObject result = callTool(McpServlet.TOOL_EXECUTE_API, "{\"command\":\"deployVirtualMachine\",\"params\":{\"details\":{\"cpuNumber\":2}}}");

        Assert.assertFalse(result.get("isError").getAsBoolean());
        Assert.assertTrue(getText(result).contains("vm-1"));
        Assert.assertEquals(1, result.getAsJsonArray("content").size());

        Assert.assertEquals(3, executedRequests.size());
        Assert.assertEquals("2", getParam(executedRequests.get(0), "details[0].cpuNumber"));
        Assert.assertEquals("queryAsyncJobResult", getParam(executedRequests.get(1), "command"));
        Assert.assertEquals("job-uuid", getParam(executedRequests.get(1), "jobid"));
        // Only the command itself goes through the access checks and the rate limit, not the polling
        Mockito.verify(apiServer, Mockito.times(1)).verifyRequest(Mockito.anyMap(), Mockito.eq(USER_ID), Mockito.any(InetAddress.class));
    }

    @Test
    public void executeApiReturnsThePendingJobWhenItDoesNotFinishInTime() throws Exception {
        Mockito.doReturn(DeployVMCmd.class).when(apiServer).getCmdClass("deployVirtualMachine");
        mockApiResponses("{\"deployvirtualmachineresponse\":{\"id\":\"vm-uuid\",\"jobid\":\"job-uuid\"}}",
                "{\"queryasyncjobresultresponse\":{\"jobid\":\"job-uuid\",\"jobstatus\":0}}");

        JsonObject result = callTool(McpServlet.TOOL_EXECUTE_API, "{\"command\":\"deployVirtualMachine\"}");

        Assert.assertFalse(result.get("isError").getAsBoolean());
        Assert.assertTrue(getText(result).contains("job-uuid"));
        Assert.assertEquals(2, result.getAsJsonArray("content").size());
        Assert.assertEquals(2, executedRequests.size());
    }

    @Test
    public void queryAsyncJobReportsAFailedJobAsError() throws Exception {
        mockApiResponses("{\"queryasyncjobresultresponse\":{\"jobid\":\"job-uuid\",\"jobstatus\":2,\"jobresult\":{\"errortext\":\"No capacity\"}}}");

        JsonObject result = callTool(McpServlet.TOOL_QUERY_ASYNC_JOB, "{\"jobid\":\"job-uuid\"}");

        Assert.assertTrue(result.get("isError").getAsBoolean());
        Assert.assertTrue(getText(result).contains("No capacity"));
        Mockito.verify(apiServer).verifyRequest(Mockito.anyMap(), Mockito.eq(USER_ID), Mockito.any(InetAddress.class));
    }

    @Test
    public void searchApisReturnsTheApisMatchingAllTheWords() throws Exception {
        mockApiResponses("{\"listapisresponse\":{\"count\":3,\"api\":[" +
                "{\"name\":\"listVirtualMachines\",\"description\":\"List the virtual machines owned by the account.\",\"isasync\":false}," +
                "{\"name\":\"deployVirtualMachine\",\"description\":\"Creates and automatically starts a virtual machine.\",\"isasync\":true}," +
                "{\"name\":\"listZones\",\"description\":\"Lists zones\",\"isasync\":false}]}}");

        JsonObject result = callTool(McpServlet.TOOL_SEARCH_APIS, "{\"keyword\":\"Virtual  DEPLOY\"}");

        String text = getText(result);
        Assert.assertTrue(text.contains("deployVirtualMachine [async]: Creates"));
        Assert.assertFalse(text.contains("listVirtualMachines"));
        Assert.assertFalse(text.contains("listZones"));
        Assert.assertEquals("listApis", getParam(executedRequests.get(0), "command"));
    }

    @Test
    public void describeApiReturnsTheParametersWithoutTheResponseDescription() throws Exception {
        mockApiResponses("{\"listapisresponse\":{\"count\":1,\"api\":[{\"name\":\"listZones\",\"description\":\"Lists zones\",\"isasync\":false," +
                "\"params\":[{\"name\":\"id\",\"type\":\"uuid\",\"required\":false,\"description\":\"the ID of the zone\",\"since\":\"4.0\"}]," +
                "\"response\":[{\"name\":\"name\",\"description\":\"Zone name\",\"type\":\"string\"}]}]}}");

        JsonObject description = JsonParser.parseString(getText(callTool(McpServlet.TOOL_DESCRIBE_API, "{\"name\":\"listZones\"}"))).getAsJsonObject();

        Assert.assertEquals("listZones", description.get("name").getAsString());
        Assert.assertFalse(description.has("response"));
        JsonObject param = description.getAsJsonArray("params").get(0).getAsJsonObject();
        Assert.assertEquals("uuid", param.get("type").getAsString());
        Assert.assertFalse(param.has("since"));
        Assert.assertEquals("listZones", getParam(executedRequests.get(0), "name"));
    }

    @Test
    public void describeApiOfAnUnknownApiIsReportedInTheToolResult() throws Exception {
        mockApiResponses("{\"listapisresponse\":{}}");

        Assert.assertTrue(callTool(McpServlet.TOOL_DESCRIBE_API, "{\"name\":\"doesNotExist\"}").get("isError").getAsBoolean());
    }
}
