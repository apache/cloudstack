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
import java.util.Stack;
import java.util.UUID;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.projects.Project;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * CallContext records information about the environment the call is made.  This
 * class must be always be available in all CloudStack code.  Every thread
 * entry point must set the context and remove it when the thread finishes.
 */
public class CallContext {
    private static final Logger s_logger = Logger.getLogger(CallContext.class);
    private static ManagedThreadLocal<CallContext> s_currentContext = new ManagedThreadLocal<CallContext>();
    private static ManagedThreadLocal<Stack<CallContext>> s_currentContextStack = new ManagedThreadLocal<Stack<CallContext>>() {
        @Override
        protected Stack<CallContext> initialValue() {
            return new Stack<CallContext>();
        }
    };

    private String contextId;
    private Account account;
    private long accountId;
    private long startEventId = 0;
    private String eventDescription;
    private String eventDetails;
    private String eventType;
    private boolean isEventDisplayEnabled = true; // default to true unless specifically set
    private User user;
    private long userId;
    private final Map<Object, Object> context = new HashMap<Object, Object>();
    private Project project;
    private String apiName;

    static EntityManager s_entityMgr;

    public static void init(EntityManager entityMgr) {
        s_entityMgr = entityMgr;
    }

    protected CallContext() {
    }

    protected CallContext(long userId, long accountId, String contextId) {
        this.userId = userId;
        this.accountId = accountId;
        this.contextId = contextId;
    }

    protected CallContext(User user, Account account, String contextId) {
        this.user = user;
        userId = user.getId();
        this.account = account;
        accountId = account.getId();
        this.contextId = contextId;
    }

    public void putContextParameter(Object key, Object value) {
        context.put(key, value);
    }

    /**
     * @param key any not null key object
     * @return the value of the key from context map
     * @throws NullPointerException if the specified key is nul
     */
    public Object getContextParameter(Object key) {
        Object value = context.get(key);
        //check if the value is present in the toString value of the key
        //due to a bug in the way we update the key by serializing and deserializing, it sometimes gets toString value of the key. @see com.cloud.api.ApiAsyncJobDispatcher#runJob
        if(value == null ) {
            value = context.get(key.toString());
        }
        return value;
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

    public String getContextId() {
        return contextId;
    }

    public Account getCallingAccount() {
        if (account == null) {
            account = s_entityMgr.findById(Account.class, accountId);
        }
        return account;
    }

    public static CallContext current() {
        CallContext context = s_currentContext.get();

        // TODO other than async job and api dispatches, there are many system background running threads
        // that do not setup CallContext at all, however, many places in code that are touched by these background tasks
        // assume not-null CallContext. Following is a fix to address therefore caused NPE problems
        //
        // There are security implications with this. It assumes that all system background running threads are
        // indeed have no problem in running under system context.
        //
        if (context == null) {
            context = registerSystemCallContextOnceOnly();
        }

        return context;
    }

    /**
     * This method should only be called if you can propagate the context id
     * from another CallContext.
     *
     * @param callingUser calling user
     * @param callingAccount calling account
     * @param contextId context id propagated from another call context
     * @return CallContext
     */
    public static CallContext register(User callingUser, Account callingAccount, String contextId) {
        return register(callingUser, callingAccount, null, null, contextId);
    }

    protected static CallContext register(User callingUser, Account callingAccount, Long userId, Long accountId, String contextId) {
        /*
                Unit tests will have multiple times of setup/tear-down call to this, remove assertions to all unit test to run
                assert s_currentContext.get() == null : "There's a context already so what does this new register context mean? " + s_currentContext.get().toString();
                if (s_currentContext.get() != null) { // FIXME: This should be removed soon.  I added this check only to surface all the places that have this problem.
                    throw new CloudRuntimeException("There's a context already so what does this new register context mean? " + s_currentContext.get().toString());
                }
        */
        CallContext callingContext = null;
        if (userId == null || accountId == null) {
            callingContext = new CallContext(callingUser, callingAccount, contextId);
        } else {
            callingContext = new CallContext(userId, accountId, contextId);
        }
        s_currentContext.set(callingContext);
        NDC.push("ctx-" + UuidUtils.first(contextId));
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Registered: " + callingContext);
        }

        s_currentContextStack.get().push(callingContext);

        return callingContext;
    }

    public static CallContext registerPlaceHolderContext() {
        CallContext context = new CallContext(0, 0, UUID.randomUUID().toString());
        s_currentContext.set(context);

        s_currentContextStack.get().push(context);
        return context;
    }

    public static CallContext register(User callingUser, Account callingAccount) {
        return register(callingUser, callingAccount, UUID.randomUUID().toString());
    }

    public static CallContext registerSystemCallContextOnceOnly() {
        try {
            CallContext context = s_currentContext.get();
            if (context == null) {
                return register(null, null, User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, UUID.randomUUID().toString());
            }
            assert context.getCallingUserId() == User.UID_SYSTEM : "You are calling a very specific method that registers a one time system context.  This method is meant for background threads that does processing.";
            return context;
        } catch (Exception e) {
            s_logger.error("Failed to register the system call context.", e);
            throw new CloudRuntimeException("Failed to register system call context", e);
        }
    }

    public static CallContext register(String callingUserUuid, String callingAccountUuid) {
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

    public static CallContext register(long callingUserId, long callingAccountId) throws CloudAuthenticationException {
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

    public static CallContext register(long callingUserId, long callingAccountId, String contextId) throws CloudAuthenticationException {
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

    public static void unregisterAll() {
        while (unregister() != null) {
            // NOOP
        }
    }

    public static CallContext unregister() {
        CallContext context = s_currentContext.get();
        if (context == null) {
            return null;
        }
        s_currentContext.remove();
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Unregistered: " + context);
        }
        String contextId = context.getContextId();
        String sessionIdOnStack = null;
        String sessionIdPushedToNDC = "ctx-" + UuidUtils.first(contextId);
        while ((sessionIdOnStack = NDC.pop()) != null) {
            if (sessionIdOnStack.isEmpty() || sessionIdPushedToNDC.equals(sessionIdOnStack)) {
                break;
            }
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Popping from NDC: " + contextId);
            }
        }

        Stack<CallContext> stack = s_currentContextStack.get();
        stack.pop();

        if (!stack.isEmpty()) {
            s_currentContext.set(stack.peek());
        } else {
            s_currentContext.set(null);
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

    public Project getProject() {
        return this.project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
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

    public Map<Object, Object> getContextParameters() {
        return context;
    }

    public void putContextParameters(Map<Object, Object> details){
        if (details == null) return;
        for(Map.Entry<Object,Object>entry : details.entrySet()){
            putContextParameter(entry.getKey(), entry.getValue());
        }
    }

    public static void setActionEventInfo(String eventType, String description) {
        CallContext context = CallContext.current();
        if (context != null) {
            context.setEventType(eventType);
            context.setEventDescription(description);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("CCtxt[acct=").append(getCallingAccountId())
            .append("; user=")
            .append(getCallingUserId())
            .append("; id=")
            .append(contextId)
            .append("]")
            .toString();
    }
}
