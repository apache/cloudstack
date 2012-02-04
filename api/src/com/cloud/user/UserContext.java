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

import com.cloud.server.ManagementService;
import com.cloud.utils.component.ComponentLocator;

public class UserContext {

    private static ThreadLocal<UserContext> s_currentContext = new ThreadLocal<UserContext>();
    private static final ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
    private static final AccountService _accountMgr = locator.getManager(AccountService.class);

    private long userId;
    private String sessionId;
    private Account account;
    private long startEventId = 0;
    private long accountId;
    private String eventDetails;

    private boolean apiServer;

    private static UserContext s_adminContext = new UserContext(_accountMgr.getSystemUser().getId(), _accountMgr.getSystemAccount(), null, false);

    public UserContext() {
    }

    public UserContext(long userId, Account accountObject, String sessionId, boolean apiServer) {
        this.userId = userId;
        this.account = accountObject;
        this.sessionId = sessionId;
        this.apiServer = apiServer;
    }

    public long getCallerUserId() {
        return userId;
    }

    public void setCallerUserId(long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Account getCaller() {
        return account;
    }

    public void setCaller(Account accountObject) {
        this.account = accountObject;
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
            //
            // TODO: we should enforce explicit UserContext setup at major entry-points for security concerns,
            // however, there are many places that run background jobs assume the system context.
            //
            // If there is a security concern, all entry points from user (including the front end that takes HTTP
// request in and
            // the core async-job manager that runs commands from user) have explicitly setup the UserContext.
            //
            return s_adminContext;
        }
        return context;
    }

    public static void updateContext(long userId, Account accountObject, String sessionId) {
        UserContext context = current();
        assert (context != null) : "Context should be already setup before you can call this one";

        context.setCallerUserId(userId);
        context.setCaller(accountObject);
        context.setSessionKey(sessionId);
    }

    public static void registerContext(long userId, Account accountObject, String sessionId, boolean apiServer) {
        s_currentContext.set(new UserContext(userId, accountObject, sessionId, apiServer));
    }

    public static void unregisterContext() {
        s_currentContext.set(null);
    }

    public void setStartEventId(long startEventId) {
        this.startEventId = startEventId;
    }

    public long getStartEventId() {
        return startEventId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    public String getEventDetails() {
        return eventDetails;
    }
}
