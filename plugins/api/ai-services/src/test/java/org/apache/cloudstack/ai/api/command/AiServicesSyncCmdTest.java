// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.api.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;

import org.apache.cloudstack.ai.AiServicesService;
import org.apache.cloudstack.ai.api.response.AiModelResponse;
import org.apache.cloudstack.ai.engine.CallerContext;
import org.apache.cloudstack.ai.engine.EngineClient;
import org.apache.cloudstack.ai.engine.EngineException;
import org.apache.cloudstack.ai.engine.EngineResponse;
import org.apache.cloudstack.ai.engine.EngineTransport;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/** Synchronous and list commands against a scripted engine. */
class AiServicesSyncCmdTest {

    private static final CallerContext CALLER =
            new CallerContext("u", "acme", "/Customers/Acme", "User");

    private static final class TestListCmd extends AiServicesListCmd<AiModelResponse> {
        @Override
        protected String enginePath() {
            return "/v1/models";
        }

        @Override
        protected String itemsKey() {
            return "models";
        }

        @Override
        protected AiModelResponse mapItem(JsonObject item) {
            return AiModelResponse.from(item);
        }
    }

    /** A list whose screen has a detail view, matching on the model's name. */
    private static final class TestFilteringListCmd extends AiServicesListCmd<AiModelResponse> {
        @Override
        protected String enginePath() {
            return "/v1/models";
        }

        @Override
        protected String itemsKey() {
            return "models";
        }

        @Override
        protected boolean filtersById() {
            return true;
        }

        @Override
        protected String identityField() {
            return "name";
        }

        @Override
        protected AiModelResponse mapItem(JsonObject item) {
            return AiModelResponse.from(item);
        }
    }

    private static void askFor(AiServicesListCmd<?> command, String id) throws Exception {
        Field field = AiServicesListCmd.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(command, id);
    }

    private static <T> T wire(T command, EngineResponse answer) throws Exception {
        AiServicesService service = mock(AiServicesService.class);
        EngineTransport transport = (method, url, headers, body) -> answer;
        when(service.getEngineClient()).thenReturn(
                new EngineClient(Collections.singletonList("https://engine"), transport));
        when(service.currentCaller()).thenReturn(CALLER);

        Field field = AiServicesSyncCmd.class.getDeclaredField("aiServices");
        field.setAccessible(true);
        field.set(command, service);
        return command;
    }

    @SuppressWarnings("unchecked")
    private static ListResponse<AiModelResponse> listOf(TestListCmd command) {
        return (ListResponse<AiModelResponse>) command.getResponseObject();
    }

    @Test
    void aListCommandMapsEveryItem() throws Exception {
        TestListCmd command = wire(new TestListCmd(), new EngineResponse(200,
                "{\"count\": 2, \"models\": ["
                        + "{\"name\": \"a\"}, {\"name\": \"b\"}]}"));

        command.execute();

        assertEquals(2, listOf(command).getCount());
    }

    @Test
    void anEmptyCollectionIsAnEmptyList() throws Exception {
        TestListCmd command =
                wire(new TestListCmd(), new EngineResponse(200, "{\"models\": []}"));

        command.execute();

        assertEquals(0, listOf(command).getCount());
    }

    @Test
    void aMissingCollectionIsAnEmptyListNotAFailure() throws Exception {
        // An engine that returns {} has nothing for this caller to see,
        // which is a legitimate answer under caller-context filtering.
        TestListCmd command = wire(new TestListCmd(), new EngineResponse(200, "{}"));

        command.execute();

        assertEquals(0, listOf(command).getCount());
    }

    @Test
    void nonObjectEntriesAreSkippedRatherThanCrashing() throws Exception {
        TestListCmd command = wire(new TestListCmd(), new EngineResponse(200,
                "{\"models\": [{\"name\": \"a\"}, \"junk\", 7]}"));

        command.execute();

        assertEquals(1, listOf(command).getCount());
    }

    @Test
    void aForbiddenEngineAnswerBecomesAnAccountError() throws Exception {
        TestListCmd command = wire(new TestListCmd(), new EngineResponse(403, "{}"));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertEquals(ApiErrorCode.ACCOUNT_ERROR, failure.getErrorCode());
    }

    @Test
    void anUnavailableEngineBecomesAResourceError() throws Exception {
        TestListCmd command = wire(new TestListCmd(), new EngineResponse(503, "{}"));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("503"));
    }

    @Test
    void theEnginesOwnExplanationReachesTheOperator() throws Exception {
        // Found by the first end-to-end round trip. listAiGpuCapacity
        // reported "the AI Services engine returned 503 for
        // /v1/capacity" while the engine had said, in the body,
        // "capacity has not been observed yet" — which is the
        // sentence that tells an operator to run a reconcile.
        //
        // The engine answers in RFC 9457 problem JSON precisely so
        // its callers can say why. The plugin is its only caller.
        TestListCmd command = wire(
                new TestListCmd(),
                new EngineResponse(503, "{\"title\":\"unavailable\","
                        + "\"detail\":\"capacity has not been observed yet\"}"));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("capacity has not been observed yet"),
                "lost the engine's detail: " + failure.getDescription());
    }

    @Test
    void theStatusAndPathSurviveAlongsideTheDetail() throws Exception {
        // The detail is added to what was already there, not swapped
        // for it: the status is what an operator greps for.
        TestListCmd command = wire(
                new TestListCmd(),
                new EngineResponse(503, "{\"detail\":\"capacity has not been observed yet\"}"));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("503"), failure.getDescription());
    }

    @Test
    void aProblemWithNoDetailFallsBackToTheTitle() throws Exception {
        TestListCmd command =
                wire(new TestListCmd(), new EngineResponse(500, "{\"title\":\"internal error\"}"));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("internal error"), failure.getDescription());
    }

    @Test
    void anUnparseableErrorBodyStillReportsTheStatus() throws Exception {
        // A body that is not problem JSON must not turn a clean
        // failure into a parse failure: the status is still the
        // useful thing, and this path is reached when something
        // other than the engine answers — a proxy, a load balancer.
        TestListCmd command =
                wire(new TestListCmd(), new EngineResponse(502, "<html>Bad Gateway</html>"));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("502"), failure.getDescription());
    }

    @Test
    void anUnreadableBodyIsReportedClearly() throws Exception {
        TestListCmd command =
                wire(new TestListCmd(), new EngineResponse(200, "<html>nope</html>"));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("unreadable"));
    }

    @Test
    void anEmptyBodyParsesAsAnEmptyObject() {
        assertEquals(0, AiServicesSyncCmd.parse("").size());
        assertEquals(0, AiServicesSyncCmd.parse(null).size());
    }

    @Test
    void parsingRejectsNonJson() {
        assertThrows(EngineException.class, () -> AiServicesSyncCmd.parse("nope"));
    }

    @Test
    void aDetailViewGetsTheRowItAskedFor() throws Exception {
        // CloudStack opens a detail page by calling the same list command
        // with id=<the row's key>. Before this the parameter was dropped,
        // the engine answered with everything, and AutogenView drew the
        // first row — so every detail page showed the first record in its
        // list, whatever had been clicked.
        TestFilteringListCmd command = wire(new TestFilteringListCmd(), new EngineResponse(200,
                "{\"count\": 2, \"models\": ["
                        + "{\"name\": \"a\"}, {\"name\": \"b\"}]}"));
        askFor(command, "b");

        command.execute();

        ListResponse<AiModelResponse> listed =
                (ListResponse<AiModelResponse>) command.getResponseObject();
        assertEquals(1, listed.getCount());
        assertEquals("b", listed.getResponses().get(0).getName());
    }

    @Test
    void aListWithNoIdIsTheWholeList() throws Exception {
        TestFilteringListCmd command = wire(new TestFilteringListCmd(), new EngineResponse(200,
                "{\"count\": 2, \"models\": ["
                        + "{\"name\": \"a\"}, {\"name\": \"b\"}]}"));

        command.execute();

        assertEquals(2, ((ListResponse<?>) command.getResponseObject()).getCount());
    }

    @Test
    void anIdMatchingNothingIsAnEmptyList() throws Exception {
        // Not the first row, which is the failure being fixed.
        TestFilteringListCmd command = wire(new TestFilteringListCmd(), new EngineResponse(200,
                "{\"count\": 2, \"models\": ["
                        + "{\"name\": \"a\"}, {\"name\": \"b\"}]}"));
        askFor(command, "gone");

        command.execute();

        assertEquals(0, ((ListResponse<?>) command.getResponseObject()).getCount());
    }

    @Test
    void aListWithNoDetailViewIgnoresAnIdRatherThanEmptyingItself() throws Exception {
        // filtersById() is false by default. A screen with no detail page is
        // never called with an id, and filtering one that is would silently
        // empty a list that used to work.
        TestListCmd command = wire(new TestListCmd(), new EngineResponse(200,
                "{\"count\": 2, \"models\": ["
                        + "{\"name\": \"a\"}, {\"name\": \"b\"}]}"));

        command.execute();

        assertEquals(2, listOf(command).getCount());
    }
}
