/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.feign.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import feign.Feign;
import feign.Request;
import feign.Response;
import feign.jackson.JacksonDecoder;

class SANFeignClientTest {

    @Test
    void getLunByUUIDSendsFieldsQueryParameter() {
        AtomicReference<Request> capturedRequest = new AtomicReference<>();
        SANFeignClient client = Feign.builder()
                .client((request, options) -> {
                    capturedRequest.set(request);
                    return Response.builder()
                            .status(200)
                            .reason("OK")
                            .headers(Collections.emptyMap())
                            .body("{\"uuid\":\"lun-uuid\"}", StandardCharsets.UTF_8)
                            .request(request)
                            .build();
                })
                .decoder(new JacksonDecoder())
                .target(SANFeignClient.class, "http://localhost");

        client.getLunByUUID("Basic credentials", "lun-uuid", Map.of("fields", "copy,name,uuid"));

        assertEquals("http://localhost/api/storage/luns/lun-uuid?fields=copy%2Cname%2Cuuid",
                capturedRequest.get().url());
    }
}
