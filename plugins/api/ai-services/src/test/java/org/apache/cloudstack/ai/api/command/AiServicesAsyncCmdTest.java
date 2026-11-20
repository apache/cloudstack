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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.ai.AiServicesService;
import org.apache.cloudstack.ai.engine.CallerContext;
import org.apache.cloudstack.ai.engine.EngineClient;
import org.apache.cloudstack.ai.engine.EngineJob;
import org.apache.cloudstack.ai.engine.EngineResponse;
import org.apache.cloudstack.ai.engine.EngineTransport;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.jupiter.api.Test;

/**
 * The async bridge (SPEC.md section 9).
 *
 * A 202 is the contract: anything else means the engine did not accept
 * the command, and treating a 200 as success would report work that
 * never started.
 */
class AiServicesAsyncCmdTest {

    private static final CallerContext CALLER =
            new CallerContext("u", "acme", "/Customers/Acme", "User");

    /** A minimal async command that records what it was given. */
    private static final class TestCmd extends AiServicesAsyncCmd {
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
            return "AI.TEST";
        }

        @Override
        public String getEventDescription() {
            return "a test command";
        }
    }

    /** Answers the POST, then the job polls, from a script. */
    private static EngineClient clientReturning(List<EngineResponse> answers) {
        List<EngineResponse> remaining = new ArrayList<>(answers);
        EngineTransport transport = (method, url, headers, body) ->
                remaining.isEmpty() ? answers.get(answers.size() - 1) : remaining.remove(0);
        return new EngineClient(Collections.singletonList("https://engine"), transport);
    }

    private static TestCmd commandOver(List<EngineResponse> answers) throws Exception {
        TestCmd command = new TestCmd();
        AiServicesService service = mock(AiServicesService.class);
        when(service.getEngineClient()).thenReturn(clientReturning(answers));
        when(service.currentCaller()).thenReturn(CALLER);
        // Every async command now runs its work inside the manager, so
        // CloudStack records whether it finished; the real one is a
        // Spring proxy carrying @ActionEvent. Here it only has to let
        // the work through — EventRecordingTest is what checks that
        // the command routes through it at all.
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(service).recordEvent(anyString(), anyString(), any(Runnable.class));

        Field field = AiServicesAsyncCmd.class.getDeclaredField("aiServices");
        field.setAccessible(true);
        field.set(command, service);
        return command;
    }

    @Test
    void anAcceptedCommandIsPolledToCompletion() throws Exception {
        TestCmd command = commandOver(java.util.Arrays.asList(
                new EngineResponse(202, "{\"jobid\": \"j-1\"}"),
                new EngineResponse(200,
                        "{\"jobid\": \"j-1\", \"jobstatus\": 1,"
                                + " \"jobresult\": {\"deployment\": {\"id\": \"d-1\"}}}")));

        command.execute();

        assertTrue(command.finished.isSuccess());
        assertEquals("j-1", command.finished.getJobId());
    }

    @Test
    void a200IsNotTreatedAsAcceptance() throws Exception {
        // The engine answered without starting a job; reporting success
        // would claim work that never happened.
        TestCmd command = commandOver(Collections.singletonList(
                new EngineResponse(200, "{\"jobid\": \"j-1\"}")));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("did not accept"));
    }

    @Test
    void aRefusedCommandSaysWhyTheEngineRefused() throws Exception {
        // The sync path learned this from a live round trip; the async
        // path had the same defect from the same cause. A batch the
        // engine refused reported only
        //
        //   the AI Services engine did not accept AI.BATCH.CREATE: HTTP 400
        //
        // while the body said which field was wrong. The engine speaks
        // RFC 9457 so its caller can explain itself, and both paths
        // now ask EngineResponse the same question.
        TestCmd command = commandOver(Collections.singletonList(
                new EngineResponse(400,
                        "{\"title\":\"bad request\",\"detail\":\"model is not deployed\"}")));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("model is not deployed"),
                "lost the engine's detail: " + failure.getDescription());
    }

    @Test
    void aRefusalKeepsItsStatusBesideTheDetail() throws Exception {
        TestCmd command = commandOver(Collections.singletonList(
                new EngineResponse(400, "{\"detail\":\"model is not deployed\"}")));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("400"), failure.getDescription());
    }

    @Test
    void aRefusedCommandSurfacesItsStatus() throws Exception {
        TestCmd command = commandOver(Collections.singletonList(
                new EngineResponse(403, "{}")));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("403"));
    }

    @Test
    void aFailedEngineJobBecomesAServerApiException() throws Exception {
        TestCmd command = commandOver(java.util.Arrays.asList(
                new EngineResponse(202, "{\"jobid\": \"j-1\"}"),
                new EngineResponse(200,
                        "{\"jobid\": \"j-1\", \"jobstatus\": 2, \"jobresult\":"
                                + " {\"errortext\": \"Insufficient capacity\","
                                + " \"errorcode\": 533}}")));

        ServerApiException failure =
                assertThrows(ServerApiException.class, command::execute);

        assertTrue(failure.getDescription().contains("Insufficient capacity"));
    }

    @Test
    void theDefaultVerbIsPost() {
        assertEquals("POST", new TestCmd().engineMethod());
    }

    @Test
    void thereIsNoBodyByDefault() {
        org.junit.jupiter.api.Assertions.assertNull(new TestCmd().engineBody());
    }
}
