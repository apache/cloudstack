/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.user;

import org.apache.log4j.Logger;

import com.cloud.utils.ProcessUtil;

public class UserContext {
    private static final Logger s_logger = Logger.getLogger(UserContext.class);

    private static ThreadLocal<UserContext> s_currentContext = new ThreadLocal<UserContext>();

    private Long userId;
    private String accountName;
    private Long accountId;
    private Long domainId;
    private String sessionId;
    private Object accountObject;

    private boolean apiServer;

    private static UserContext s_nullContext = new UserContext(); 

    public UserContext() {
    }

    public UserContext(Long userId, Object accountObject, String accountName, Long accountId, Long domainId, String sessionId, boolean apiServer) {
        this.userId = userId;
        this.accountObject = accountObject;
        this.accountId = accountId;
        this.domainId = domainId;
        this.sessionId = sessionId;
        this.apiServer = apiServer;
    }

    public Long getUserId() {
        if (userId != null) {
            return userId;
        }

        if (!apiServer) { 
            s_logger.warn("Null user id in UserContext " + ProcessUtil.dumpStack());
        }

        return null;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getAccountId() {
        if (accountId != null) {
            return accountId;
        }

        if (!apiServer) {
            s_logger.warn("Null account id in UserContext " + ProcessUtil.dumpStack());
        }

        return null;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Object getAccountObject() {
        return accountObject;
    }

    public void setAccountObject(Object accountObject) {
        this.accountObject = accountObject;
    }

    public void setSessionKey(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isApiServer() {
        return apiServer;
    }

    public void setApiServer(boolean apiServer) {
        this.apiServer = apiServer;
    }

    public static UserContext current() {
        UserContext context = s_currentContext.get();
        if (context == null) {
            return s_nullContext;
        }
        return context;
    }

	public static void updateContext(Long userId, Object accountObject, String accountName, Long accountId, Long domainId, String sessionId) {
	    UserContext context = current();
	    assert(context != null) : "Context should be already setup before you can call this one";

	    context.setUserId(userId);
	    context.setAccountObject(accountObject);
	    context.setAccountName(accountName);
	    context.setAccountId(accountId);
	    context.setDomainId(domainId);
	    context.setSessionKey(sessionId);
	}

	public static void registerContext(Long userId, Object accountObject, String accountName, Long accountId, Long domainId, String sessionId, boolean apiServer) {
	    s_currentContext.set(new UserContext(userId, accountObject, accountName, accountId, domainId, sessionId, apiServer));
	}

	public static void unregisterContext() {
	    s_currentContext.set(null);
	}
}
