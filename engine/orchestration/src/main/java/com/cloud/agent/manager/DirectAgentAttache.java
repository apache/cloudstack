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
package com.cloud.agent.manager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CronCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.Status;
import com.cloud.resource.ServerResource;
import org.apache.logging.log4j.ThreadContext;

public class DirectAgentAttache extends AgentAttache {

    protected final ConfigKey<Integer> _HostPingRetryCount = new ConfigKey<Integer>("Advanced", Integer.class, "host.ping.retry.count", "0",
            "Number of times retrying a host ping while waiting for check results", true);
    protected final ConfigKey<Integer> _HostPingRetryTimer = new ConfigKey<Integer>("Advanced", Integer.class, "host.ping.retry.timer", "5",
            "Interval to wait before retrying a host ping while waiting for check results", true);
    ServerResource _resource;
    List<ScheduledFuture<?>> _futures = new ArrayList<ScheduledFuture<?>>();
    long _seq = 0;
    LinkedList<Task> tasks = new LinkedList<Task>();
    AtomicInteger _outstandingTaskCount;
    AtomicInteger _outstandingCronTaskCount;

    public DirectAgentAttache(AgentManagerImpl agentMgr, long id, String name, ServerResource resource, boolean maintenance) {
        super(agentMgr, id, name, maintenance);
        _resource = resource;
        _outstandingTaskCount = new AtomicInteger(0);
        _outstandingCronTaskCount = new AtomicInteger(0);
    }

    @Override
    public void disconnect(Status state) {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing disconnect " + _id + "(" + _name + ")");
        }

        for (ScheduledFuture<?> future : _futures) {
            future.cancel(false);
        }

        synchronized (this) {
            if (_resource != null) {
                _resource.disconnected();
                _resource = null;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DirectAgentAttache)) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public synchronized boolean isClosed() {
        return _resource == null;
    }

    @Override
    public void send(Request req) throws AgentUnavailableException {
        req.logD("Executing: ", true);
        if (req instanceof Response) {
            Response resp = (Response)req;
            Answer[] answers = resp.getAnswers();
            if (answers != null && answers[0] instanceof StartupAnswer) {
                StartupAnswer startup = (StartupAnswer)answers[0];
                int interval = startup.getPingInterval();
                _futures.add(_agentMgr.getCronJobPool().scheduleAtFixedRate(new PingTask(), interval, interval, TimeUnit.SECONDS));
            }
        } else {
            Command[] cmds = req.getCommands();
            if (cmds.length > 0 && !(cmds[0] instanceof CronCommand)) {
                queueTask(new Task(req));
                scheduleFromQueue();
            } else {
                CronCommand cmd = (CronCommand)cmds[0];
                _futures.add(_agentMgr.getCronJobPool().scheduleAtFixedRate(new CronTask(req), cmd.getInterval(), cmd.getInterval(), TimeUnit.SECONDS));
            }
        }
    }

    @Override
    public void process(Answer[] answers) {
        if (answers != null && answers[0] instanceof StartupAnswer) {
            StartupAnswer startup = (StartupAnswer)answers[0];
            int interval = startup.getPingInterval();
            logger.info("StartupAnswer received " + startup.getHostId() + " Interval = " + interval);
            _futures.add(_agentMgr.getCronJobPool().scheduleAtFixedRate(new PingTask(), interval, interval, TimeUnit.SECONDS));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            assert _resource == null : "Come on now....If you're going to dabble in agent code, you better know how to close out our resources. Ever considered why there's a method called disconnect()?";
            synchronized (this) {
                if (_resource != null) {
                    logger.warn("Lost attache for " + _id + "(" + _name + ")");
                    disconnect(Status.Alert);
                }
            }
        } finally {
            super.finalize();
        }
    }

    private synchronized void queueTask(Task task) {
        tasks.add(task);
    }

    private synchronized void scheduleFromQueue() {
        if (logger.isTraceEnabled()) {
            logger.trace("Agent attache=" + _id + ", task queue size=" + tasks.size() + ", outstanding tasks=" + _outstandingTaskCount.get());
        }
        while (!tasks.isEmpty() && _outstandingTaskCount.get() < _agentMgr.getDirectAgentThreadCap()) {
            _outstandingTaskCount.incrementAndGet();
            _agentMgr.getDirectAgentPool().execute(tasks.remove());
        }
    }

    protected class PingTask extends ManagedContextRunnable {
        @Override
        protected synchronized void runInContext() {
            try {
                if (_outstandingCronTaskCount.incrementAndGet() >= _agentMgr.getDirectAgentThreadCap()) {
                    logger.warn("PingTask execution for direct attache(" + _id + ") has reached maximum outstanding limit(" + _agentMgr.getDirectAgentThreadCap() + "), bailing out");
                    return;
                }

                ServerResource resource = _resource;

                if (resource != null) {
                    PingCommand cmd = resource.getCurrentStatus(_id);
                    int retried = 0;
                    while (cmd == null && ++retried <= _HostPingRetryCount.value()) {
                        Thread.sleep(1000*_HostPingRetryTimer.value());
                        cmd = resource.getCurrentStatus(_id);
                    }

                    if (cmd == null) {
                        logger.warn("Unable to get current status on " + _id + "(" + _name + ")");
                        return;
                    }

                    if (cmd.getContextParam("logid") != null) {
                        ThreadContext.put("logcontextid", cmd.getContextParam("logid"));
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Ping from " + _id + "(" + _name + ")");
                    }
                    long seq = _seq++;

                    if (logger.isTraceEnabled()) {
                        logger.trace("SeqA " + _id + "-" + seq + ": " + new Request(_id, -1, cmd, false).toString());
                    }

                    _agentMgr.handleCommands(DirectAgentAttache.this, seq, new Command[] {cmd});
                } else {
                    logger.debug("Unable to send ping because agent is disconnected " + _id + "(" + _name + ")");
                }
            } catch (Exception e) {
                logger.warn("Unable to complete the ping task", e);
            } finally {
                _outstandingCronTaskCount.decrementAndGet();
            }
        }
    }

    protected class CronTask extends ManagedContextRunnable {
        Request _req;

        public CronTask(Request req) {
            _req = req;
        }

        private void bailout() {
            long seq = _req.getSequence();
            try {
                Command[] cmds = _req.getCommands();
                ArrayList<Answer> answers = new ArrayList<Answer>(cmds.length);
                for (Command cmd : cmds) {
                    Answer answer = new Answer(cmd, false, "Bailed out as maximum outstanding task limit reached");
                    answers.add(answer);
                }
                Response resp = new Response(_req, answers.toArray(new Answer[answers.size()]));
                processAnswers(seq, resp);
            } catch (Exception e) {
                logger.warn(log(seq, "Exception caught in bailout "), e);
            }
        }

        @Override
        protected void runInContext() {
            long seq = _req.getSequence();
            try {
                if (_outstandingCronTaskCount.incrementAndGet() >= _agentMgr.getDirectAgentThreadCap()) {
                    logger.warn("CronTask execution for direct attache(" + _id + ") has reached maximum outstanding limit(" + _agentMgr.getDirectAgentThreadCap() + "), bailing out");
                    bailout();
                    return;
                }

                ServerResource resource = _resource;
                Command[] cmds = _req.getCommands();
                boolean stopOnError = _req.stopOnError();

                if (logger.isDebugEnabled()) {
                    logger.debug(log(seq, "Executing request"));
                }
                ArrayList<Answer> answers = new ArrayList<Answer>(cmds.length);
                for (int i = 0; i < cmds.length; i++) {
                    Answer answer = null;
                    Command currentCmd = cmds[i];
                    if (currentCmd.getContextParam("logid") != null) {
                        ThreadContext.put("logcontextid", currentCmd.getContextParam("logid"));
                    }
                    try {
                        if (resource != null) {
                            answer = resource.executeRequest(cmds[i]);
                            if (answer == null) {
                                logger.warn("Resource returned null answer!");
                                answer = new Answer(cmds[i], false, "Resource returned null answer");
                            }
                        } else {
                            answer = new Answer(cmds[i], false, "Agent is disconnected");
                        }
                    } catch (Exception e) {
                        logger.warn(log(seq, "Exception Caught while executing command"), e);
                        answer = new Answer(cmds[i], false, e.toString());
                    }
                    answers.add(answer);
                    if (!answer.getResult() && stopOnError) {
                        if (i < cmds.length - 1 && logger.isDebugEnabled()) {
                            logger.debug(log(seq, "Cancelling because one of the answers is false and it is stop on error."));
                        }
                        break;
                    }
                }

                Response resp = new Response(_req, answers.toArray(new Answer[answers.size()]));
                if (logger.isDebugEnabled()) {
                    logger.debug(log(seq, "Response Received: "));
                }

                processAnswers(seq, resp);
            } catch (Exception e) {
                logger.warn(log(seq, "Exception caught "), e);
            } finally {
                _outstandingCronTaskCount.decrementAndGet();
            }
        }
    }

    protected class Task extends ManagedContextRunnable {
        Request _req;

        public Task(Request req) {
            _req = req;
        }

        @Override
        protected void runInContext() {
            long seq = _req.getSequence();
            try {
                ServerResource resource = _resource;
                Command[] cmds = _req.getCommands();
                boolean stopOnError = _req.stopOnError();

                if (logger.isDebugEnabled()) {
                    logger.debug(log(seq, "Executing request"));
                }
                ArrayList<Answer> answers = new ArrayList<Answer>(cmds.length);
                for (int i = 0; i < cmds.length; i++) {
                    Answer answer = null;
                    Command currentCmd = cmds[i];
                    if (currentCmd.getContextParam("logid") != null) {
                        ThreadContext.put("logcontextid", currentCmd.getContextParam("logid"));
                    }
                    try {
                        if (resource != null) {
                            answer = resource.executeRequest(cmds[i]);
                            if (answer == null) {
                                logger.warn("Resource returned null answer!");
                                answer = new Answer(cmds[i], false, "Resource returned null answer");
                            }
                        } else {
                            answer = new Answer(cmds[i], false, "Agent is disconnected");
                        }
                    } catch (Throwable t) {
                        // Catch Throwable as all exceptions will otherwise be eaten by the executor framework
                        logger.warn(log(seq, "Throwable caught while executing command"), t);
                        answer = new Answer(cmds[i], false, t.toString());
                    }
                    answers.add(answer);
                    if (!answer.getResult() && stopOnError) {
                        if (i < cmds.length - 1 && logger.isDebugEnabled()) {
                            logger.debug(log(seq, "Cancelling because one of the answers is false and it is stop on error."));
                        }
                        break;
                    }
                }

                Response resp = new Response(_req, answers.toArray(new Answer[answers.size()]));
                if (logger.isDebugEnabled()) {
                    logger.debug(log(seq, "Response Received: "));
                }

                processAnswers(seq, resp);
            } catch (Throwable t) {
                // This is pretty serious as processAnswers might not be called and the calling process is stuck waiting for the full timeout
                logger.error(log(seq, "Throwable caught in runInContext, this will cause the management to become unpredictable"), t);
            } finally {
                _outstandingTaskCount.decrementAndGet();
                scheduleFromQueue();
            }
        }
    }
}
