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

import feign.Feign;

public class FeignClientFactory {

    private final FeignConfiguration feignConfiguration;

    public FeignClientFactory() {
        this.feignConfiguration = new FeignConfiguration();
    }

    public FeignClientFactory(FeignConfiguration feignConfiguration) {
        this.feignConfiguration = feignConfiguration;
    }

    public <T> T createClient(Class<T> clientClass, String baseURL) {
        return Feign.builder()
                .client(feignConfiguration.createClient())
                .encoder(feignConfiguration.createEncoder())
                .decoder(feignConfiguration.createDecoder())
                .retryer(feignConfiguration.createRetryer())
                .requestInterceptor(feignConfiguration.createRequestInterceptor())
                .target(clientClass, baseURL);
    }
}
