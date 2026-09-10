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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JwtUtilTest {

    @Test
    public void testHmacSha256_KnownVector() throws Exception {
        final byte[] actual = JwtUtil.hmacSha256("data".getBytes(StandardCharsets.UTF_8), "key".getBytes(StandardCharsets.UTF_8));
        final byte[] expected = hexToBytes("5031fe3d989c6d1537a013fa6e739da23463fdaec3b70137d828e36ace221bd0");
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testIssueHs256Jwt_BuildsValidTokenAndClaims() throws Exception {
        final long before = Instant.now().getEpochSecond();
        final String token = JwtUtil.issueHs256Jwt("sub-1", "scope-a", 120L, "very-secret");
        final long after = Instant.now().getEpochSecond();

        final String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        final JsonObject header = JsonParser.parseString(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)).getAsJsonObject();
        final JsonObject payload = JsonParser.parseString(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)).getAsJsonObject();

        assertEquals("HS256", header.get("alg").getAsString());
        assertEquals("JWT", header.get("typ").getAsString());
        assertEquals(JwtUtil.ISSUER, payload.get("iss").getAsString());
        assertEquals("sub-1", payload.get("sub").getAsString());
        assertEquals("scope-a", payload.get("scope").getAsString());

        final long iat = payload.get("iat").getAsLong();
        final long exp = payload.get("exp").getAsLong();
        assertTrue(iat >= before && iat <= after);
        assertEquals(120L, exp - iat);

        final byte[] expectedSig = JwtUtil.hmacSha256((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8), "very-secret".getBytes(StandardCharsets.UTF_8));
        final byte[] actualSig = Base64.getUrlDecoder().decode(parts[2]);
        assertArrayEquals(expectedSig, actualSig);
    }

    private static byte[] hexToBytes(final String hex) {
        final byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            final int hi = Character.digit(hex.charAt(i * 2), 16);
            final int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            out[i] = (byte)((hi << 4) + lo);
        }
        return out;
    }
}
