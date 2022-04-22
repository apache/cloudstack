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
package com.cloud.api;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebListener
public class ApiSessionListener implements HttpSessionListener, ServletRequestListener {
    public static final Logger LOGGER = Logger.getLogger(ApiSessionListener.class.getName());
    private static final String ATTRIBUTE_NAME = "SessionCounter";
    private static Map<HttpSession, Object> sessions = new ConcurrentHashMap<>();
    private static long sessionCount;

    public ApiSessionListener() {
        sessionCount = 0;
    }

    /**
     * @return the internal adminstered session count
     */
    public static long getSessionCount() {
        return sessionCount;
    }

    /**
     * @return the size of the internal {@see Map} of sessions
     */
    public static long getNumberOfSessions() {
        return sessions.size();
    }
    public void sessionCreated(HttpSessionEvent event) {
        LOGGER.debug("Session created by Id : " + event.getSession().getId() + " , session: " + event.getSession().toString() + " , source: " + event.getSource().toString() + " , event: " + event.toString());
        synchronized (this) {
            HttpSession session = event.getSession();
            sessions.put(session, event.getSource());
            sessionCount++;
        }
        LOGGER.debug("Sessions count: " + sessions);
    }
    public void sessionDestroyed(HttpSessionEvent event) {
        LOGGER.debug("Session destroyed by Id : " + event.getSession().getId() + " , session: " + event.getSession().toString() + " , source: " + event.getSource().toString() + " , event: " + event.toString());
        synchronized (this) {
            sessionCount--;
            sessions.remove(event.getSession());
        }
        LOGGER.debug("Sessions count: " + sessions);
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        LOGGER.debug("request destroyed");
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        LOGGER.debug("request initialized");
        HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
        HttpSession session = request.getSession();
        if (session.isNew()) {
            synchronized (this) {
                // replace the source object for the address
                sessions.put(session, request.getRemoteAddr());
            }
        }
    }
}
