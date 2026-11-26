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
package org.apache.cloudstack.ai.api.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * The one moment an API key's secret exists.
 *
 * The engine returns it as `apikey`, with a warning saying it cannot
 * be retrieved again. This class read `secret`, so it was always null
 * — a tenant could create keys and never learn any of them, through
 * CloudMonkey, Terraform or the UI alike. A live 4.23.0.0 answered
 * `createAiApiKey` with `{"id": …, "name": …}` and nothing else.
 */
class AiApiKeyResponseTest {

    private static JsonObject minted() {
        JsonObject body = new JsonObject();
        body.addProperty("id", "884f7894b5e58aa6");
        body.addProperty("name", "tf-probe");
        body.addProperty("apikey", "sk-live-not-a-real-key");
        body.addProperty("warning", "This key is shown once and cannot be retrieved again.");
        return body;
    }

    @Test
    void theSecretSurvivesTheOnlyMomentItExists() {
        AiApiKeyResponse mapped = AiApiKeyResponse.from(minted());

        assertEquals("sk-live-not-a-real-key", mapped.getApiKey());
    }

    @Test
    void theIdentityComesThroughWithIt() {
        AiApiKeyResponse mapped = AiApiKeyResponse.from(minted());

        assertEquals("884f7894b5e58aa6", mapped.getId());
        assertEquals("tf-probe", mapped.getName());
    }

    @Test
    void aListedKeyHasNoSecretAndSaysSo() {
        // `listAiApiKeys` returns metadata only; nothing should
        // invent an empty string where there is no secret.
        JsonObject listed = new JsonObject();
        listed.addProperty("id", "884f7894b5e58aa6");
        listed.addProperty("name", "tf-probe");

        assertNull(AiApiKeyResponse.from(listed).getApiKey());
    }
}
