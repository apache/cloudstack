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
package org.apache.cloudstack.context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * LogContext records information about the environment the API call is made.  This
 * class must be always be available in all CloudStack code.
 */
public class LogContext {
    private static final Logger s_logger = Logger.getLogger(LogContext.class);
    private static ManagedThreadLocal<LogContext> s_currentContext = new ManagedThreadLocal<LogContext>();

    private String logContextId;
    private Account account;
    private long accountId;
    private long startEventId = 0;
    private String eventDescription;
    private String eventDetails;
    private String eventType;
    private boolean isEventDisplayEnabled = true; // default to true unless specifically set
    private User user;
    private long userId;
    private final Map<String, String> context = new HashMap<String, String>();

    static EntityManager s_entityMgr;

    public static void init(EntityManager entityMgr) {
        s_entityMgr = entityMgr;
    }

    protected LogContext() {
    }

    protected LogContext(long userId, long accountId, String logContextId) {
        this.userId = userId;
        this.accountId = accountId;
        this.logContextId = logContextId;
    }

    protected LogContext(User user, Account account, String logContextId) {
        this.user = user;
        userId = user.getId();
        this.account = account;
        accountId = account.getId();
        this.logContextId = logContextId;
    }

    public void putContextParameter(String key, String value) {
        context.put(key, value);
    }

    public String getContextParameter(String key) {
        return context.get(key);
    }

    public long getCallingUserId() {
        return userId;
    }

    public User getCallingUser() {
        if (user == null) {
            user = s_entityMgr.findById(User.class, userId);
        }
        return user;
    }

    public String getLogContextId() {
        return logContextId;
    }

    public Account getCallingAccount() {
        if (account == null) {
            account = s_entityMgr.findById(Account.class, accountId);
        }
        return account;
    }

    public static LogContext current() {
        LogContext context = s_currentContext.get();
        if (context == null) {
            context = registerSystemLogContextOnceOnly();
        }
        return context;
    }

    /**
     * This method should only be called if you can propagate the context id
     * from another LogContext.
     *
     * @param callingUser calling user
     * @param callingAccount calling account
     * @param contextId context id propagated from another call context
     * @return LogContext
     */
    public static LogContext register(User callingUser, Account callingAccount, String contextId) {
        return register(callingUser, callingAccount, null, null, contextId);
    }

    protected static LogContext register(User callingUser, Account callingAccount, Long userId, Long accountId, String contextId) {
        LogContext callingContext = null;
        if (userId == null || accountId == null) {
            callingContext = new LogContext(callingUser, callingAccount, contextId);
        } else {
            callingContext = new LogContext(userId, accountId, contextId);
        }
        s_currentContext.set(callingContext);
        MDC.put("logcontextid", UuidUtils.first(contextId));
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Registered for log: " + callingContext);
        }
        return callingContext;
    }

    public static LogContext registerPlaceHolderContext() {
        LogContext context = new LogContext(0, 0, UUID.randomUUID().toString());
        s_currentContext.set(context);
        return context;
    }

    public static LogContext register(User callingUser, Account callingAccount) {
        return register(callingUser, callingAccount, UUID.randomUUID().toString());
    }

    public static LogContext registerSystemLogContextOnceOnly() {
        try {
            LogContext context = s_currentContext.get();
            if (context == null) {
                return register(null, null, User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, UUID.randomUUID().toString());
            }
            assert context.getCallingUserId() == User.UID_SYSTEM : "You are calling a very specific method that registers a one time system context.  This method is meant for background threads that does processing.";
            return context;
        } catch (Exception e) {
            s_logger.error("Failed to register the system log context.", e);
            throw new CloudRuntimeException("Failed to register system log context", e);
        }
    }

    public static LogContext register(String callingUserUuid, String callingAccountUuid) {
        Account account = s_entityMgr.findByUuid(Account.class, callingAccountUuid);
        if (account == null) {
            throw new CloudAuthenticationException("The account is no longer current.").add(Account.class, callingAccountUuid);
        }

        User user = s_entityMgr.findByUuid(User.class, callingUserUuid);
        if (user == null) {
            throw new CloudAuthenticationException("The user is no longer current.").add(User.class, callingUserUuid);
        }
        return register(user, account);
    }

    public static LogContext register(long callingUserId, long callingAccountId) throws CloudAuthenticationException {
        Account account = s_entityMgr.findById(Account.class, callingAccountId);
        if (account == null) {
            throw new CloudAuthenticationException("The account is no longer current.").add(Account.class, Long.toString(callingAccountId));
        }
        User user = s_entityMgr.findById(User.class, callingUserId);
        if (user == null) {
            throw new CloudAuthenticationException("The user is no longer current.").add(User.class, Long.toString(callingUserId));
        }
        return register(user, account);
    }

    public static LogContext register(long callingUserId, long callingAccountId, String contextId) throws CloudAuthenticationException {
        Account account = s_entityMgr.findById(Account.class, callingAccountId);
        if (account == null) {
            throw new CloudAuthenticationException("The account is no longer current.").add(Account.class, Long.toString(callingAccountId));
        }
        User user = s_entityMgr.findById(User.class, callingUserId);
        if (user == null) {
            throw new CloudAuthenticationException("The user is no longer current.").add(User.class, Long.toString(callingUserId));
        }
        return register(user, account, contextId);
    }

    public static void unregister() {
        LogContext context = s_currentContext.get();
        if (context != null) {
            s_currentContext.remove();
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Unregistered: " + context);
            }
        }
        MDC.clear();
    }

    public void setStartEventId(long startEventId) {
        this.startEventId = startEventId;
    }

    public long getStartEventId() {
        return startEventId;
    }

    public long getCallingAccountId() {
        return accountId;
    }

    public String getCallingAccountUuid() {
        return getCallingAccount().getUuid();
    }

    public String getCallingUserUuid() {
        return getCallingUser().getUuid();
    }

    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    /**
     * Whether to display the event to the end user.
     * @return true - if the event is to be displayed to the end user, false otherwise.
     */
    public boolean isEventDisplayEnabled() {
        return isEventDisplayEnabled;
    }

    public void setEventDisplayEnabled(boolean eventDisplayEnabled) {
        isEventDisplayEnabled = eventDisplayEnabled;
    }

    public Map<String, String> getContextParameters() {
        return context;
    }

    public void putContextParameters(Map<String, String> details) {
        if (details == null)
            return;
        for (Map.Entry<String, String> entry : details.entrySet()) {
            putContextParameter(entry.getKey(), entry.getValue());
        }
    }

    public static void setActionEventInfo(String eventType, String description) {
        LogContext context = LogContext.current();
        if (context != null) {
            context.setEventType(eventType);
            context.setEventDescription(description);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("LogCtxt[acct=").append(getCallingAccountId())
                .append("; user=")
                .append(getCallingUserId())
                .append("; id=")
                .append(logContextId)
                .append("]")
                .toString();
    }
}
