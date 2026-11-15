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

package org.apache.cloudstack.veeam.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonObject;

public class JwtUtil {
    public static final String ALGORITHM = "HmacSHA256";
    public static final String ISSUER = "veeam-control";

    public static String issueHs256Jwt(String subject, String scope, long ttlSeconds, String secret) throws Exception {
        long now = Instant.now().getEpochSecond();
        long exp = now + ttlSeconds;

        JsonObject headerObject = new JsonObject();
        headerObject.addProperty("alg", "HS256");
        headerObject.addProperty("typ", "JWT");
        String headerJson = headerObject.toString();
        JsonObject payloadObject = new JsonObject();
        payloadObject.addProperty("iss", ISSUER);
        payloadObject.addProperty("sub", subject);
        payloadObject.addProperty("scope", scope);
        payloadObject.addProperty("iat", now);
        payloadObject.addProperty("exp", exp);
        String payloadJson = payloadObject.toString();

        String header = DataUtil.b64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = DataUtil.b64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        byte[] sig = hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + DataUtil.b64Url(sig);
    }

    public static byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
        final Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(key, ALGORITHM));
        return mac.doFinal(data);
    }
}
