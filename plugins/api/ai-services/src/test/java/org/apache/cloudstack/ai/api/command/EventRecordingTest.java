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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.ai.AiServicesManagerImpl;
import org.apache.cloudstack.ai.AiServicesService;
import org.apache.cloudstack.ai.api.response.AiModelResponse;
import org.apache.cloudstack.ai.engine.CallerContext;
import org.apache.cloudstack.ai.engine.EngineClient;
import org.apache.cloudstack.ai.engine.EngineJob;
import org.apache.cloudstack.ai.engine.EngineResponse;
import org.apache.cloudstack.ai.engine.EngineTransport;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.jupiter.api.Test;

import com.cloud.event.ActionEvent;
import com.google.gson.JsonObject;

/**
 * Every write command records whether it finished (SPEC.md section 9).
 *
 * A live management server showed six AI events and all six read
 * `Scheduled`: an operator saw work start and never learned whether it
 * succeeded. The obvious patch — `@ActionEvent` on the command's own
 * `execute()` — cannot work, and was measured not working on a real
 * server before these tests were written.
 *
 * `ActionEventInterceptor` is a Spring AOP advisor, so it wraps *beans*,
 * and a command is not one: `ComponentContext.inject` calls
 * `autowireBean` and hands back the same instance, never a proxy. The
 * annotation therefore belongs on the manager, which is a bean — the
 * same arrangement CloudStack uses for its own commands.
 *
 * The second half is as easy to get wrong: on a proxied method
 * `eventType = ""` still records nothing, because `interceptComplete`
 * returns early on an empty type and the fallback it would otherwise
 * use, `ctx.getEventType()`, is never populated by the API path. So the
 * recorder stamps the context itself, inside the proxied call, where
 * `interceptComplete` and `interceptException` both read it afterwards.
 */
class EventRecordingTest {

    private static final CallerContext CALLER =
            new CallerContext("u", "acme", "/Customers/Acme", "User");

    /** What the fake recorder was asked to record. */
    private static final class Recorded {
        final List<String> types = new ArrayList<>();
        final List<String> descriptions = new ArrayList<>();
        int runs;
    }

    private static final class TestWriteCmd extends AiServicesSyncCmd {
        boolean succeeded;

        @Override
        protected String enginePath() {
            return "/v1/profiles/guardrail";
        }

        @Override
        protected String engineMethod() {
            return "POST";
        }

        @Override
        protected void onSuccess(JsonObject body) {
            this.succeeded = true;
        }

        @Override
        public String getEventType() {
            return "AI.PROFILE.UPDATE";
        }

        @Override
        public String getEventDescription() {
            return "creating guardrail profile acme";
        }
    }

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

    private static final class TestAsyncCmd extends AiServicesAsyncCmd {
        EngineJob finished;

        @Override
        protected String enginePath() {
            return "/v1/deployments";
        }

        @Override
        protected void onSuccess(EngineJob job) {
            this.finished = job;
        }

        @Override
        public String getEventType() {
            return "AI.MODEL.DEPLOY";
        }

        @Override
        public String getEventDescription() {
            return "deploying model llama";
        }
    }

    private static AiServicesService serviceOver(
            List<EngineResponse> answers, Recorded recorded) {
        List<EngineResponse> remaining = new ArrayList<>(answers);
        EngineTransport transport = (method, url, headers, body) ->
                remaining.isEmpty() ? answers.get(answers.size() - 1) : remaining.remove(0);

        AiServicesService service = mock(AiServicesService.class);
        when(service.getEngineClient()).thenReturn(
                new EngineClient(Collections.singletonList("https://engine"), transport));
        when(service.currentCaller()).thenReturn(CALLER);
        doAnswer(invocation -> {
            recorded.types.add(invocation.getArgument(0));
            recorded.descriptions.add(invocation.getArgument(1));
            recorded.runs++;
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(service).recordEvent(anyString(), anyString(), any(Runnable.class));
        return service;
    }

    private static <T> T wire(T command, Class<?> base, AiServicesService service)
            throws Exception {
        Field field = base.getDeclaredField("aiServices");
        field.setAccessible(true);
        field.set(command, service);
        return command;
    }

    @Test
    void theRecorderOnTheManagerCarriesAnActionEvent() throws Exception {
        // The annotation has to sit on the bean's method, because that
        // is the only thing Spring proxies. Putting it on a command's
        // execute() compiles, deploys and records nothing.
        Method recorder = AiServicesManagerImpl.class.getMethod(
                "recordEvent", String.class, String.class, Runnable.class);
        ActionEvent annotation = recorder.getAnnotation(ActionEvent.class);

        assertNotNull(annotation, "recordEvent must carry @ActionEvent");
        assertFalse(annotation.async(),
                "async = true would publish a second Scheduled event beside "
                        + "the one ApiServer already created");
        assertEquals("", annotation.eventType(),
                "the type comes from the CallContext, per command");
    }

    @Test
    void aSyncWriteRoutesItsWorkThroughTheRecorder() throws Exception {
        Recorded recorded = new Recorded();
        TestWriteCmd command = wire(new TestWriteCmd(), AiServicesSyncCmd.class,
                serviceOver(Collections.singletonList(
                        new EngineResponse(200, "{\"id\": \"g-1\"}")), recorded));

        command.execute();

        assertTrue(command.succeeded, "the work still runs");
        assertEquals(1, recorded.runs);
        assertEquals("AI.PROFILE.UPDATE", recorded.types.get(0));
        assertEquals("creating guardrail profile acme", recorded.descriptions.get(0));
    }

    @Test
    void anAsyncCommandRoutesItsWorkThroughTheRecorder() throws Exception {
        Recorded recorded = new Recorded();
        TestAsyncCmd command = wire(new TestAsyncCmd(), AiServicesAsyncCmd.class,
                serviceOver(Arrays.asList(
                        new EngineResponse(202, "{\"jobid\": \"j-1\"}"),
                        new EngineResponse(200,
                                "{\"jobid\": \"j-1\", \"jobstatus\": 1,"
                                        + " \"jobresult\": {\"deployment\": {}}}")),
                        recorded));

        command.execute();

        assertNotNull(command.finished);
        assertEquals(1, recorded.runs);
        assertEquals("AI.MODEL.DEPLOY", recorded.types.get(0));
    }

    @Test
    void aListCommandRecordsNothing() throws Exception {
        // Reads are not events. A list command declares no event type,
        // and that absence is what keeps it out of the operator's log.
        Recorded recorded = new Recorded();
        TestListCmd command = wire(new TestListCmd(), AiServicesSyncCmd.class,
                serviceOver(Collections.singletonList(new EngineResponse(200,
                        "{\"count\": 1, \"models\": [{\"name\": \"a\"}]}")), recorded));

        command.execute();

        assertNull(command.getEventType());
        assertEquals(0, recorded.runs);
    }

    @Test
    void aFailedWriteStillReachesTheRecorder() throws Exception {
        // This is the half that matters most: the interceptor records
        // Failed from interceptException, which only runs if the work
        // threw *inside* the recorded call. A command that failed
        // before reaching it would stay at Scheduled forever.
        Recorded recorded = new Recorded();
        TestWriteCmd command = wire(new TestWriteCmd(), AiServicesSyncCmd.class,
                serviceOver(Collections.singletonList(
                        new EngineResponse(422, "{\"detail\": \"no such preset\"}")),
                        recorded));

        assertThrows(ServerApiException.class, command::execute);

        assertEquals(1, recorded.runs, "the failure happened inside the recorder");
        assertFalse(command.succeeded);
    }
}
