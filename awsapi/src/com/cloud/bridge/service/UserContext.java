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
package com.cloud.bridge.service;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.cloud.bridge.service.exception.InternalErrorException;
import com.cloud.bridge.util.StringHelper;

public class UserContext {
    protected final static Logger logger = Logger.getLogger(UserContext.class);

    private static ThreadLocal<UserContext> threadUserContext = new ThreadLocal<UserContext>();

    private boolean annonymous = false;
    private String accessKey;
    private String secretKey;
    private String canonicalUserId;  // In our design, we re-use the accessKey to provide the canonicalUserId  -- TODO loPri - reconsider?
    private String description;
    private HttpServletRequest request = null;

    public UserContext() {
    }

    public static UserContext current() {
        UserContext context = threadUserContext.get();
        if (context == null) {
            logger.debug("initializing a new [anonymous] UserContext!");
            context = new UserContext();
            threadUserContext.set(context);
        }
        return context;
    }

    public void initContext() {
        annonymous = true;
    }

    public void initContext(String accessKey, String secretKey, String canonicalUserId, String description, HttpServletRequest request) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.canonicalUserId = canonicalUserId;
        this.description = description;
        this.annonymous = false;
        this.request = request;
    }

    public HttpServletRequest getHttp() {
        return request;
    }

    public String getAccessKey() {
        if (annonymous)
            return StringHelper.EMPTY_STRING;

        if (accessKey == null) {
            logger.error("Fatal - UserContext has not been correctly setup");
            throw new InternalErrorException("Uninitalized user context");
        }
        return accessKey;
    }

    public String getSecretKey() {
        if (annonymous)
            return StringHelper.EMPTY_STRING;

        if (secretKey == null) {
            logger.error("Fatal - UserContext has not been correctly setup");
            throw new InternalErrorException("Uninitalized user context");
        }

        return secretKey;
    }

    public String getCanonicalUserId() {
        if (annonymous)
            return StringHelper.EMPTY_STRING;

        if (canonicalUserId == null) {
            logger.error("Fatal - UserContext has not been correctly setup");
            throw new InternalErrorException("Uninitalized user context");
        }

        return canonicalUserId;
    }

    public String getDescription() {
        if (description != null)
            return description;

        return StringHelper.EMPTY_STRING;
    }
}
