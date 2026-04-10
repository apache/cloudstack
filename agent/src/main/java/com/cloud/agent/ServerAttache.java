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
package com.cloud.agent;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.nio.Link;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ServerAttache provides basic server communication commands to be implemented.
 *
 * @author mprokopchuk
 */
public class ServerAttache {
    private static final Logger logger = LogManager.getLogger(ServerAttache.class);

    private static final ScheduledExecutorService s_listenerExecutor = Executors.newScheduledThreadPool(10,
            new NamedThreadFactory("ListenerTimer"));

    protected static Comparator<Request> s_reqComparator = (o1, o2) -> {
        long seq1 = o1.getSequence();
        long seq2 = o2.getSequence();
        if (seq1 < seq2) {
            return -1;
        } else if (seq1 > seq2) {
            return 1;
        } else {
            return 0;
        }
    };

    protected static Comparator<Object> s_seqComparator = (o1, o2) -> {
        long seq1 = ((Request) o1).getSequence();
        long seq2 = (Long) o2;
        if (seq1 < seq2) {
            return -1;
        } else if (seq1 > seq2) {
            return 1;
        } else {
            return 0;
        }
    };

    private static final Random s_rand = new SecureRandom();
    protected String _name;
    private Link _link;
    protected ConcurrentHashMap<Long, ServerListener> _waitForList;
    protected LinkedList<Request> _requests;
    protected Long _currentSequence;
    protected long _nextSequence;

    protected ServerAttache(Link link) {
        _name = link.getIpAddress();
        _link = link;
        _waitForList = new ConcurrentHashMap<>();
        _requests = new LinkedList<>();
        _nextSequence = Long.valueOf(s_rand.nextInt(Short.MAX_VALUE)) << 48;
    }

    @Override
    public String toString() {
        return String.format("ServerAttache %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this,
                "_name"));
    }

    public synchronized long getNextSequence() {
        return ++_nextSequence;
    }

    protected synchronized void addRequest(Request req) {
        int index = findRequest(req);
        assert (index < 0) : "How can we get index again? " + index + ":" + req.toString();
        _requests.add(-index - 1, req);
    }

    protected void cancel(Request req) {
        cancel(req.getSequence());
    }

    protected synchronized void cancel(long seq) {
        logger.debug(log(seq, "Cancelling."));

        ServerListener listener = _waitForList.remove(seq);
        if (listener != null) {
            listener.processDisconnect();
        }
        int index = findRequest(seq);
        if (index >= 0) {
            _requests.remove(index);
        }
    }

    protected synchronized int findRequest(Request req) {
        return Collections.binarySearch(_requests, req, s_reqComparator);
    }

    protected synchronized int findRequest(long seq) {
        return Collections.binarySearch(_requests, seq, s_seqComparator);
    }

    protected String log(long seq, String msg) {
        return "Seq " + _name + "-" + seq + ": " + msg;
    }

    protected void registerListener(long seq, ServerListener listener) {
        if (logger.isTraceEnabled()) {
            logger.trace(log(seq, "Registering listener"));
        }
        if (listener.getTimeout() != -1) {
            s_listenerExecutor.schedule(new Alarm(seq), listener.getTimeout(), TimeUnit.SECONDS);
        }
        _waitForList.put(seq, listener);
    }

    protected ServerListener unregisterListener(long sequence) {
        if (logger.isTraceEnabled()) {
            logger.trace(log(sequence, "Unregistering listener"));
        }
        return _waitForList.remove(sequence);
    }

    protected ServerListener getListener(long sequence) {
        return _waitForList.get(sequence);
    }

    public String getName() {
        return _name;
    }

    public int getQueueSize() {
        return _requests.size();
    }

    public boolean processAnswers(long seq, Response resp) {
        resp.logD("Processing: ", true);
        boolean processed = false;
        Answer[] answers = resp.getAnswers();
        try {
            ServerListener monitor = getListener(seq);
            if (monitor == null) {
                if (answers[0] != null && answers[0].getResult()) {
                    processed = true;
                }
                logger.debug(log(seq, "Unable to find listener."));
            } else {
                processed = monitor.processAnswers(seq, answers);
                logger.trace(log(seq, (processed ? "" : " did not ") + " processed "));
                if (!monitor.isRecurring()) {
                    unregisterListener(seq);
                }
            }
        } finally {
            // we should always trigger next command execution, even in failure cases - otherwise in exception case
            // all the remaining will be stuck in the sync queue forever
            if (resp.executeInSequence()) {
                sendNext(seq);
            }
        }
        return processed;
    }

    protected void cancelAllCommands() {
        for (Iterator<Map.Entry<Long, ServerListener>> it = _waitForList.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, ServerListener> entry = it.next();
            it.remove();

            ServerListener monitor = entry.getValue();
            logger.debug(log(entry.getKey(), "Sending disconnect to " + monitor.getClass()));
            monitor.processDisconnect();
        }
    }

    public void cleanup() {
        cancelAllCommands();
        _requests.clear();
    }

    @Override
    public boolean equals(Object obj) {
        // Return false straight away.
        if (obj == null) {
            return false;
        }
        // No need to handle a ClassCastException. If the classes are different, then equals can return false
        // straight ahead.
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ServerAttache that = (ServerAttache) obj;
        return _name.equals(that.getName());
    }

    public void send(Request req, ServerListener listener) throws CloudException {
        long seq = req.getSequence();
        if (listener != null) {
            registerListener(seq, listener);
        }

        synchronized (this) {
            try {
                if (isClosed()) {
                    throw new CloudException("The link to the server " + _name + " has been closed", new ClosedChannelException());
                }

                if (req.executeInSequence() && _currentSequence != null) {
                    req.logD("Waiting for Seq " + _currentSequence + " Scheduling: ", true);
                    addRequest(req);
                    return;
                }

                // If we got to here either we're not supposed to set the _currentSequence or it is null already.
                req.logD("Sending ", true);
                send(req);

                if (req.executeInSequence() && _currentSequence == null) {
                    _currentSequence = seq;
                    logger.trace(log(seq, " is current sequence"));
                }
            } catch (CloudException e) {
                logger.info(log(seq, "Unable to send due to " + e.getMessage()), e);
                cancel(seq);
                throw e;
            } catch (Exception e) {
                logger.warn(log(seq, "Unable to send due to " + e.getMessage()), e);
                cancel(seq);
                throw new CloudException("Problem due to other exception " + e.getMessage(), e);
            }
        }
    }

    public Answer[] send(Request req, int wait) throws CloudException {
        if (req.getSequence() <= 0) {
            req.setSequence(getNextSequence());
        }
        SynchronousListener sl = new SynchronousListener();
        sl.setTimeout(wait + 5);
        send(req, sl);

        long seq = req.getSequence();
        try {
            for (int i = 0; i < 2; i++) {
                Answer[] answers = null;
                try {
                    answers = sl.waitFor(wait);
                } catch (InterruptedException e) {
                    logger.debug(log(seq, "Interrupted"));
                }
                if (answers != null) {
                    new Response(req, answers).logD("Received: ", false);
                    return answers;
                }

                answers = sl.getAnswers(); // Try it again.
                if (answers != null) {
                    new Response(req, answers).logD("Received after timeout: ", true);
                    return answers;
                }

                Long current = _currentSequence;
                if (current != null && seq != current) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(log(seq, "Waited too long."));
                    }
                    throw new OperationTimedoutException(req.getCommands(), -1, seq, wait, false);
                }
                logger.debug(log(seq, "Waiting some more time because this is the current command"));
            }
            throw new OperationTimedoutException(req.getCommands(), -1, seq, wait * 2, true);
        } catch (OperationTimedoutException e) {
            logger.warn(log(seq, "Timed out on " + req));
            cancel(seq);
            Long current = _currentSequence;
            if (req.executeInSequence() && (current != null && current == seq)) {
                sendNext(seq);
            }
            throw e;
        } catch (Exception e) {
            logger.warn(log(seq, "Exception while waiting for answer"), e);
            cancel(seq);
            Long current = _currentSequence;
            if (req.executeInSequence() && (current != null && current == seq)) {
                sendNext(seq);
            }
            throw new OperationTimedoutException(req.getCommands(), -1, seq, wait, false);
        } finally {
            unregisterListener(seq);
        }
    }

    protected synchronized void sendNext(long seq) {
        _currentSequence = null;
        if (_requests.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(log(seq, "No more commands found"));
            }
            return;
        }

        Request req = _requests.pop();
        if (logger.isDebugEnabled()) {
            logger.debug(log(req.getSequence(), "Sending now.  is current sequence."));
        }
        try {
            send(req);
        } catch (CloudException e) {
            logger.debug(log(req.getSequence(), "Unable to send the next sequence"));
            cancel(req.getSequence());
        }
        _currentSequence = req.getSequence();
    }

    public void process(Answer[] answers) {
        //do nothing
    }

    /**
     * sends the request asynchronously.
     *
     * @param req
     * @throws AgentUnavailableException
     */
    public synchronized void send(final Request req) throws CloudException {
        try {
            _link.send(req.toBytes());
        } catch (ClosedChannelException e) {
            throw new CloudException("Channel is closed", e);
        }
    }

    /**
     * Process disconnect.
     */
    public void disconnect() {
        synchronized (this) {
            logger.debug("Processing Disconnect.");
            if (_link != null) {
                _link.close();
                _link.terminated();
            }
            _link = null;
        }
        cancelAllCommands();
        _requests.clear();
    }

    /**
     * Is the agent closed for more commands?
     *
     * @return true if unable to reach agent or false if reachable.
     */
    protected boolean isClosed() {
        return _link == null;
    }

    public Link getLink() {
        return _link;
    }

    public <T> T send(Long agentId, Command[] commands, Class<T> answerType, int asyncCommandTimeoutSec) throws IOException {
        String logId = Optional.ofNullable(ThreadContext.get("logcontextid"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse(null);
        if (logId != null) {
            for (Command command : commands) {
                if (command.getContextParam("logid") == null) {
                    command.setContextParam("logid", logId);
                }
            }
        }
        Link link = getLink();
        String commandName = commands[0].getClass().getSimpleName();

        Long id = Optional.ofNullable(agentId).orElse(-1L);
        Request request = new Request(id, -1, commands, true, false);
        Answer[] answers;
        try {
            answers = send(request, asyncCommandTimeoutSec);
        } catch (OperationTimedoutException e) {
            String msg = String.format("Failed to retrieve answer for command %s to %s", commandName, link);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (CloudException e) {
            String msg = String.format("Failed to send command %s to %s", commandName, link);
            logger.error(msg, e);
            Throwable cause = e.getCause();
            if (cause instanceof ClosedChannelException) {
                throw (ClosedChannelException) cause;
            }
            throw new RuntimeException(msg, e);
        }
        if (ArrayUtils.isEmpty(answers)) {
            String msg = String.format("Received empty %s response from %s", commandName, link);
            logger.warn(msg);
            throw new IllegalArgumentException(msg);
        }
        Answer answer = answers[0];
        String details = answer.getDetails();
        if (!answer.getResult()) {
            String msg = String.format("Received unsuccessful %s response status from %s: %s", commandName, link,
                    details);
            logger.warn(msg);
            throw new IllegalArgumentException(msg);
        }
        String answerName = answer.getClass().getSimpleName();
        if (!answerType.isInstance(answer)) {
            String msg = String.format("Received unexpected %s response type %s from %s: %s", commandName,
                    answerName, link, details);
            logger.warn(msg);
            throw new IllegalArgumentException(msg);
        }
        return answerType.cast(answer);
    }

    protected class Alarm extends ManagedContextRunnable {
        long _seq;

        public Alarm(long seq) {
            _seq = seq;
        }

        @Override
        protected void runInContext() {
            try {
                ServerListener listener = unregisterListener(_seq);
                if (listener != null) {
                    cancel(_seq);
                    listener.processTimeout(_seq);
                }
            } catch (Exception e) {
                ServerAttache.logger.warn("Exception ", e);
            }
        }
    }
}
