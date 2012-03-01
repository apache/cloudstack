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
package com.cloud.agent.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CronCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.resource.ServerResource;
import com.cloud.server.ManagementService;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;

public class DirectAgentAttache extends AgentAttache {
    private final static Logger s_logger = Logger.getLogger(DirectAgentAttache.class);

    ServerResource _resource;
    static ScheduledExecutorService s_executor;
    List<ScheduledFuture<?>> _futures = new ArrayList<ScheduledFuture<?>>();
    AgentManagerImpl _mgr;
    long _seq = 0;

    static {
    	ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
    	ConfigurationDao _configDao = locator.getDao(ConfigurationDao.class);
    	Integer size = Integer.valueOf(_configDao.getValue(Config.DirectAgentPoolSize.key()));
    	s_executor = new ScheduledThreadPoolExecutor(size, new NamedThreadFactory("DirectAgent"));
    	s_logger.debug("Create DirectAgentAttache tool with size: " + size);
    }
    
    public DirectAgentAttache(AgentManager agentMgr, long id, ServerResource resource, boolean maintenance, AgentManagerImpl mgr) {
        super(agentMgr, id, maintenance);
        _resource = resource;
        _mgr = mgr;
    }

    @Override
    public void disconnect(Status state) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Processing disconnect " + _id);
        }

        for (ScheduledFuture<?> future : _futures) {
            future.cancel(false);
        }

        synchronized(this) {
            if( _resource != null ) {
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
                _futures.add(s_executor.scheduleAtFixedRate(new PingTask(), interval, interval, TimeUnit.SECONDS));
            }
        } else {
            Command[] cmds = req.getCommands();
            if (cmds.length > 0 && !(cmds[0] instanceof CronCommand)) {
                s_executor.execute(new Task(req));
            } else {
                CronCommand cmd = (CronCommand)cmds[0];
                _futures.add(s_executor.scheduleAtFixedRate(new Task(req), cmd.getInterval(), cmd.getInterval(), TimeUnit.SECONDS));
            }
        }
    }

    @Override
    public void process(Answer[] answers) {
        if (answers != null && answers[0] instanceof StartupAnswer) {
            StartupAnswer startup = (StartupAnswer)answers[0];
            int interval = startup.getPingInterval();
            s_logger.info("StartupAnswer received " + startup.getHostId() + " Interval = " + interval );
            _futures.add(s_executor.scheduleAtFixedRate(new PingTask(), interval, interval, TimeUnit.SECONDS));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            assert _resource == null : "Come on now....If you're going to dabble in agent code, you better know how to close out our resources. Ever considered why there's a method called disconnect()?";
            synchronized (this) {
                if (_resource != null) {
                    s_logger.warn("Lost attache for " + _id);
                    disconnect(Status.Alert);
                }
            }
        } finally {
            super.finalize();
        }
    }

    protected class PingTask implements Runnable {
        @Override
        public synchronized void run() {
            try {
                ServerResource resource = _resource;

                if (resource != null) {
                    PingCommand cmd = resource.getCurrentStatus(_id);
                    if (cmd == null) {
                        s_logger.warn("Unable to get current status on " + _id);
                        _mgr.disconnect(DirectAgentAttache.this, Event.AgentDisconnected, true);
                        return;
                    }
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Ping from " + _id);
                    }
                    long seq = _seq++;

                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("SeqA " + _id + "-" + seq + ": " + new Request(_id, -1, cmd, false).toString());
                    }

                    _mgr.handleCommands(DirectAgentAttache.this, seq, new Command[]{cmd});
                } else {
                    s_logger.debug("Unable to send ping because agent is disconnected " + _id);
                }
            } catch (Exception e) {
                s_logger.warn("Unable to complete the ping task", e);
            }
        }
    }


    protected class Task implements Runnable {
        Request _req;

        public Task(Request req) {
            _req = req;
        }

        @Override
        public void run() {
            long seq = _req.getSequence();
            try {
                ServerResource resource = _resource;
                Command[] cmds = _req.getCommands();
                boolean stopOnError = _req.stopOnError();

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(log(seq, "Executing request"));
                }
                ArrayList<Answer> answers = new ArrayList<Answer>(cmds.length);
                for (int i = 0; i < cmds.length; i++) {
                    Answer answer = null;
                    try {
                        if (resource != null) {
                            answer = resource.executeRequest(cmds[i]);
                        } else {
                            answer = new Answer(cmds[i], false, "Agent is disconnected");
                        }
                    } catch (Exception e) {
                        s_logger.warn(log(seq, "Exception Caught while executing command"), e);
                        answer = new Answer(cmds[i], false, e.toString());
                    }
                    answers.add(answer);
                    if (!answer.getResult() && stopOnError) {
                        if (i < cmds.length - 1 && s_logger.isDebugEnabled()) {
                            s_logger.debug(log(seq, "Cancelling because one of the answers is false and it is stop on error."));
                        }
                        break;
                    }
                }

                Response resp = new Response(_req, answers.toArray(new Answer[answers.size()]));
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(log(seq, "Response Received: "));
                }

                processAnswers(seq, resp);
            } catch (Exception e) {
                s_logger.warn(log(seq, "Exception caught "), e);
            }
        }
    }


    @Override
    public void updatePassword(Command new_password) {
        _resource.executeRequest(new_password);
    }
}
