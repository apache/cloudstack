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

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.cloud.dao.EntityManager;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * CallContext records information about the environment the call is made.  This
 * class must be always be available in all CloudStack code.  Every thread
 * entry point must set the context and remove it when the thread finishes.
 */
public class CallContext {
    private static final Logger s_logger = Logger.getLogger(CallContext.class);
    private static ThreadLocal<CallContext> s_currentContext = new ThreadLocal<CallContext>();

    private String sessionId;
    private Account account;
    private long startEventId = 0;
    private String eventDetails;
    private User user;
    private final Map<String, Object> context = new HashMap<String, Object>();

    private static EntityManager s_entityMgr;

    public static void init(EntityManager entityMgr) {
        s_entityMgr = entityMgr;
    }

    public CallContext() {
    }

    protected CallContext(User user, Account account, String sessionId) {
        this.user = user;
        this.account = account;
        this.sessionId = sessionId;
    }
    
    public void putContextParameter(String key, Object value) {
        context.put(key, value);
    }

    public Object getContextParameter(String key) {
        return context.get(key);
    }

    public long getCallingUserId() {
        return user.getId();
    }

    public User getCallingUser() {
        return user;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Account getCallingAccount() {
        return account;
    }

    public static CallContext current() {
        return s_currentContext.get();
    }

    public static CallContext register(User callingUser, Account callingAccount, String sessionId) {
        assert s_currentContext.get() == null : "There's a context already so what does this new register context mean? " + s_currentContext.get().toString();
        if (s_currentContext.get() != null) { // FIXME: This should be removed soon.  I added this check only to surface all the places that have this problem.
            throw new CloudRuntimeException("There's a context already so what does this new register context mean? " + s_currentContext.get().toString());
        }
        CallContext callingContext = new CallContext(callingUser, callingAccount, sessionId);
        s_currentContext.set(callingContext);
        if (sessionId != null) {
            NDC.push("job-" + sessionId);
        }
        s_logger.debug("Setting calling context: " + s_currentContext.get());
        return callingContext;
    }

    public static CallContext registerOnceOnly() {
        CallContext context = s_currentContext.get();
        if (context == null) {
            return register(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, null);
        }

        assert context.getCallingUserId() == User.UID_SYSTEM : "You are calling a very specific method that registers a one time system context.  This method is meant for background threads that does processing.";
        return context;
    }

    public static CallContext register(String callingUserUuid, String callingAccountUuid, String sessionId) {
        Account account = s_entityMgr.findByUuid(Account.class, callingAccountUuid);
        if (account == null) {
            throw new CloudAuthenticationException("The account is no longer current.").add(Account.class, callingAccountUuid);
        }
        
        User user = s_entityMgr.findByUuid(User.class, callingUserUuid);
        if (user == null) {
            throw new CloudAuthenticationException("The user is no longer current.").add(User.class, callingUserUuid);
        }
        return register(user, account, sessionId);
    }

    public static CallContext register(long callingUserId, long callingAccountId, String sessionId) throws CloudAuthenticationException {
        Account account = s_entityMgr.findById(Account.class, callingAccountId);
        if (account == null) {
            throw new CloudAuthenticationException("The account is no longer current.").add(Account.class, Long.toString(callingAccountId));
        }
        User user = s_entityMgr.findById(User.class, callingUserId);
        if (user == null) {
            throw new CloudAuthenticationException("The user is no longer current.").add(User.class, Long.toString(callingUserId));
        }
        return register(user, account, sessionId);
    }

    public static CallContext register(long callingUserId, Account callingAccount, String sessionId, boolean apiServer) {
        User user = s_entityMgr.findById(User.class, callingUserId);
        if (user == null) {
            throw new CloudAuthenticationException("The user is no longer current.").add(User.class, Long.toString(callingUserId));
        }
        return register(user, callingAccount, sessionId);
    }

    public static CallContext unregister() {
        assert s_currentContext.get() != null : "Removing the context when we don't need to " + s_currentContext.get().toString();
        CallContext context = s_currentContext.get();
        if (context == null) {
            s_logger.trace("No context to remove");
            return null;
        }
        s_currentContext.remove();
        s_logger.debug("Context removed " + context);
        String sessionId = context.getSessionId();
        if (sessionId != null) {
            String sessionIdOnStack = null;
            while ((sessionIdOnStack = NDC.pop()) != null) {
                if (sessionId.equals(sessionIdOnStack)) {
                    break;
                }
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Popping from NDC: " + sessionId);
                }
            }
        }
        return context;
    }

    public void setStartEventId(long startEventId) {
        this.startEventId = startEventId;
    }

    public long getStartEventId() {
        return startEventId;
    }

    public long getCallingAccountId() {
        return account.getId();
    }

    public String getCallingAccountUuid() {
        return account.getUuid();
    }

    public String getCallingUserUuid() {
        return user.getUuid();
    }

    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    @Override
    public String toString() {
        return new StringBuffer("CallContext[acct=").append(account.getId())
                .append("; user=").append(user.getId())
                .append("; session=").append(sessionId)
                .append("]").toString();
    }
}
