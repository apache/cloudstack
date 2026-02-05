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

package org.apache.cloudstack.storage.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.Client;
import feign.httpclient.ApacheHttpClient;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.EncodeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class FeignConfiguration {
    private static final Logger logger = LogManager.getLogger(FeignConfiguration.class);

    private final int retryMaxAttempt = 3;
    private final int retryMaxInterval = 5;
    private final String ontapFeignMaxConnection = "80";
    private final String ontapFeignMaxConnectionPerRoute = "20";
    private final ObjectMapper objectMapper;

    public FeignConfiguration() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Client createClient() {
        int maxConn;
        int maxConnPerRoute;
        try {
            maxConn = Integer.parseInt(this.ontapFeignMaxConnection);
        } catch (Exception e) {
            logger.error("ontapFeignClient: parse max connection failed, using default");
            maxConn = 20;
        }
        try {
            maxConnPerRoute = Integer.parseInt(this.ontapFeignMaxConnectionPerRoute);
        } catch (Exception e) {
            logger.error("ontapFeignClient: parse max connection per route failed, using default");
            maxConnPerRoute = 2;
        }
        logger.debug("ontapFeignClient: maxConn={}, maxConnPerRoute={}", maxConn, maxConnPerRoute);
        ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> 0;
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(maxConn)
                .setMaxConnPerRoute(maxConnPerRoute)
                .setKeepAliveStrategy(keepAliveStrategy)
                .setSSLSocketFactory(getSSLSocketFactory())
                .setConnectionTimeToLive(60, TimeUnit.SECONDS)
                .build();
        return new ApacheHttpClient(httpClient);
    }

    private SSLConnectionSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustAllStrategy()).build();
            return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public RequestInterceptor createRequestInterceptor() {
        return template -> {
            logger.info("Feign Request URL: {}", template.url());
            logger.info("HTTP Method: {}", template.method());
            logger.info("Headers: {}", template.headers());
            if (template.body() != null) {
                logger.info("Body: {}", new String(template.body(), StandardCharsets.UTF_8));
            }
        };
    }

    public Retryer createRetryer() {
        return new Retryer.Default(1000L, retryMaxInterval * 1000L, retryMaxAttempt);
    }

    public Encoder createEncoder() {
        return new Encoder() {
            @Override
            public void encode(Object object, Type bodyType, feign.RequestTemplate template) throws EncodeException {
                if (object == null) {
                    template.body(null, StandardCharsets.UTF_8);
                    return;
                }
                try {
                    byte[] jsonBytes = objectMapper.writeValueAsBytes(object);
                    template.body(jsonBytes, StandardCharsets.UTF_8);
                    template.header("Content-Type", "application/json");
                } catch (JsonProcessingException e) {
                    throw new EncodeException("Error encoding object to JSON", e);
                }
            }
        };
    }

    public Decoder createDecoder() {
        return new Decoder() {
            @Override
            public Object decode(Response response, Type type) throws IOException, DecodeException {
                if (response.body() == null) {
                    logger.debug("Response body is null, returning null");
                    return null;
                }
                String json = null;
                try (InputStream bodyStream = response.body().asInputStream()) {
                    json = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
                    logger.debug("Decoding JSON response: {}", json);
                    return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type));
                } catch (IOException e) {
                    logger.error("IOException during decoding. Status: {}, Raw body: {}", response.status(), json, e);
                    throw new DecodeException(response.status(), "Error decoding JSON response", response.request(), e);
                } catch (Exception e) {
                    logger.error("Unexpected error during decoding. Status: {}, Type: {}, Raw body: {}", response.status(), type, json, e);
                    throw new DecodeException(response.status(), "Unexpected error during decoding", response.request(), e);
                }
            }
        };
    }
}
