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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CronCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.ShutdownCommand;
import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.exception.AgentControlChannelException;
import com.cloud.resource.ServerResource;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.backoff.BackoffAlgorithm;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.HandlerFactory;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.NioClient;
import com.cloud.utils.nio.NioConnection;
import com.cloud.utils.nio.Task;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

/**
 * @config
 *         {@table
 *         || Param Name | Description | Values | Default ||
 *         || type | Type of server | Storage / Computing / Routing | No Default ||
 *         || workers | # of workers to process the requests | int | 1 ||
 *         || host | host to connect to | ip address | localhost ||
 *         || port | port to connect to | port number | 8250 ||
 *         || instance | Used to allow multiple agents running on the same host | String | none || * }
 *
 *         For more configuration options, see the individual types.
 *
 **/
public class Agent implements HandlerFactory, IAgentControl {
    private static final Logger s_logger = Logger.getLogger(Agent.class.getName());

    public enum ExitStatus {
        Normal(0), // Normal status = 0.
        Upgrade(65), // Exiting for upgrade.
        Configuration(66), // Exiting due to configuration problems.
        Error(67); // Exiting because of error.

        int value;

        ExitStatus(final int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    List<IAgentControlListener> _controlListeners     = new ArrayList<IAgentControlListener>();

    IAgentShell                 _shell;
    NioConnection               _connection;
    ServerResource              _resource;
    Link                        _link;
    Long                        _id;

    Timer                       _timer                = new Timer("Agent Timer");

    List<WatchTask>             _watchList            = new ArrayList<WatchTask>();
    long                        _sequence             = 0;
    long                        _lastPingResponseTime = 0;
    long                        _pingInterval         = 0;
    AtomicInteger               _inProgress           = new AtomicInteger();

    StartupTask                 _startup              = null;
    long  _startupWaitDefault = 180000;
    long  _startupWait = _startupWaitDefault;
    boolean                     _reconnectAllowed     = true;
    //For time sentitive task, e.g. PingTask
    private ThreadPoolExecutor     _ugentTaskPool;
    ExecutorService _executor;

    // for simulator use only
    public Agent(IAgentShell shell) {
        _shell = shell;
        _link = null;

        _connection = new NioClient("Agent", _shell.getHost(), _shell.getPort(), _shell.getWorkers(), this);

        Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));

        _ugentTaskPool = new ThreadPoolExecutor(shell.getPingRetries(), 2 * shell.getPingRetries(), 10, TimeUnit.MINUTES,
                                                new SynchronousQueue<Runnable>(), new NamedThreadFactory("UgentTask")
                                                );
        
        _executor = new ThreadPoolExecutor(_shell.getWorkers(), 5 * _shell.getWorkers(), 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("agentRequest-Handler"));
    }

    public Agent(IAgentShell shell, int localAgentId, ServerResource resource) throws ConfigurationException {
        _shell = shell;
        _resource = resource;
        _link = null;

        resource.setAgentControl(this);

        String value = _shell.getPersistentProperty(getResourceName(), "id");
        _id = value != null ? Long.parseLong(value) : null;
        s_logger.info("id is " + ((_id != null) ? _id : ""));

        final Map<String, Object> params = PropertiesUtil.toMap(_shell.getProperties());

        // merge with properties from command line to let resource access command line parameters
        for (Map.Entry<String, Object> cmdLineProp : _shell.getCmdLineProperties().entrySet()) {
            params.put(cmdLineProp.getKey(), cmdLineProp.getValue());
        }

        if (!_resource.configure(getResourceName(), params)) {
            throw new ConfigurationException("Unable to configure " + _resource.getName());
        }

        _connection = new NioClient("Agent", _shell.getHost(), _shell.getPort(), _shell.getWorkers(), this);

        // ((NioClient)_connection).setBindAddress(_shell.getPrivateIp());

        s_logger.debug("Adding shutdown hook");
        Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));

        _ugentTaskPool = new ThreadPoolExecutor(shell.getPingRetries(), 2 * shell.getPingRetries(), 10, TimeUnit.MINUTES,
                                                new SynchronousQueue<Runnable>(), new NamedThreadFactory("UgentTask")
                                                );

        
        _executor = new ThreadPoolExecutor(_shell.getWorkers(), 5 * _shell.getWorkers(), 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("agentRequest-Handler"));
        

        s_logger.info("Agent [id = " + (_id != null ? _id : "new") + " : type = " + getResourceName() + " : zone = " + _shell.getZone() + " : pod = " + _shell.getPod() + " : workers = "
                + _shell.getWorkers() + " : host = " + _shell.getHost() + " : port = " + _shell.getPort());
    }

    public String getVersion() {
        return _shell.getVersion();
    }

    public String getResourceGuid() {
        String guid = _shell.getGuid();
        return guid + "-" + getResourceName();
    }

    public String getZone() {
        return _shell.getZone();
    }

    public String getPod() {
        return _shell.getPod();
    }

    protected void setLink(final Link link) {
        _link = link;
    }

    public ServerResource getResource() {
        return _resource;
    }

    public BackoffAlgorithm getBackoffAlgorithm() {
        return _shell.getBackoffAlgorithm();
    }

    public String getResourceName() {
        return _resource.getClass().getSimpleName();
    }

    public void start() {
        if (!_resource.start()) {
            s_logger.error("Unable to start the resource: " + _resource.getName());
            throw new CloudRuntimeException("Unable to start the resource: " + _resource.getName());
        }
   
        _connection.start();
       while (!_connection.isStartup()) {
           _shell.getBackoffAlgorithm().waitBeforeRetry();
           _connection = new NioClient("Agent", _shell.getHost(), _shell.getPort(), _shell.getWorkers(), this);
           _connection.start();
       }
    }

    public void stop(final String reason, final String detail) {
        s_logger.info("Stopping the agent: Reason = " + reason + (detail != null ? ": Detail = " + detail : ""));
        if (_connection != null) {
            final ShutdownCommand cmd = new ShutdownCommand(reason, detail);
            try {
                if (_link != null) {
                    Request req = new Request((_id != null ? _id : -1), -1, cmd, false);
                    _link.send(req.toBytes());
                }
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to send: " + cmd.toString());
            } catch (Exception e) {
                s_logger.warn("Unable to send: " + cmd.toString() + " due to exception: ", e);
            }
            s_logger.debug("Sending shutdown to management server");
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                s_logger.debug("Who the heck interrupted me here?");
            }
            _connection.stop();
            _connection = null;
        }

        if (_resource != null) {
            _resource.stop();
            _resource = null;
        }

        _ugentTaskPool.shutdownNow();
    }

    public Long getId() {
        return _id;
    }

    public void setId(final Long id) {
        s_logger.info("Set agent id " + id);
        _id = id;
        _shell.setPersistentProperty(getResourceName(), "id", Long.toString(id));
    }

    public void scheduleWatch(final Link link, final Request request, final long delay, final long period) {
        synchronized (_watchList) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Adding a watch list");
            }
            final WatchTask task = new WatchTask(link, request, this);
            _timer.schedule(task, 0, period);
            _watchList.add(task);
        }
    }

    protected void cancelTasks() {
        synchronized (_watchList) {
            for (final WatchTask task : _watchList) {
                task.cancel();
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Clearing watch list: " + _watchList.size());
            }
            _watchList.clear();
        }
    }

    public void sendStartup(Link link) {
        final StartupCommand[] startup = _resource.initialize();
        final Command[] commands = new Command[startup.length];
        for (int i = 0; i < startup.length; i++) {
            setupStartupCommand(startup[i]);
            commands[i] = startup[i];
        }

        final Request request = new Request(_id != null ? _id : -1, -1, commands, false, false);
        request.setSequence(getNextSequence());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Sending Startup: " + request.toString());
        }
        synchronized (this) {
            _startup = new StartupTask(link);
            _timer.schedule(_startup, _startupWait);
        }
        try {
            link.send(request.toBytes());
        } catch (final ClosedChannelException e) {
            s_logger.warn("Unable to send reques: " + request.toString());
        }
    }

    protected void setupStartupCommand(StartupCommand startup) {
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            s_logger.warn("unknow host? ", e);
            throw new CloudRuntimeException("Cannot get local IP address");
        }

        final Script command = new Script("hostname", 500, s_logger);
        final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        final String result = command.execute(parser);
        final String hostname = result == null ? parser.getLine() : addr.toString();

        startup.setId(getId());
        if (startup.getName() == null) {
            startup.setName(hostname);
        }
        startup.setDataCenter(getZone());
        startup.setPod(getPod());
        startup.setGuid(getResourceGuid());
        startup.setResourceName(getResourceName());
        startup.setVersion(getVersion());
    }

    @Override
    public Task create(Task.Type type, Link link, byte[] data) {
        return new ServerHandler(type, link, data);
    }

    protected void reconnect(final Link link) {
        if (!_reconnectAllowed) {
            return;
        }
        synchronized (this) {
            if (_startup != null) {
                _startup.cancel();
                _startup = null;
            }
        }

        link.close();
        link.terminated();

        setLink(null);
        cancelTasks();

        _resource.disconnected();

        int inProgress = 0;
        do {
            _shell.getBackoffAlgorithm().waitBeforeRetry();

            s_logger.info("Lost connection to the server. Dealing with the remaining commands...");

            inProgress = _inProgress.get();
            if (inProgress > 0) {
                s_logger.info("Cannot connect because we still have " + inProgress + " commands in progress.");
            }
        } while (inProgress > 0);

        _connection.stop();
        while (_connection.isStartup()) {
            _shell.getBackoffAlgorithm().waitBeforeRetry();
        }

        try {
            _connection.cleanUp();
        } catch (IOException e) {
            s_logger.warn("Fail to clean up old connection. " + e);
        }
        _connection = new NioClient("Agent", _shell.getHost(), _shell.getPort(), _shell.getWorkers(), this);
        do {
            s_logger.info("Reconnecting...");
            _connection.start();
            _shell.getBackoffAlgorithm().waitBeforeRetry();
        } while (!_connection.isStartup());
        s_logger.info("Connected to the server");
    }

    public void processStartupAnswer(Answer answer, Response response, Link link) {
        boolean cancelled = false;
        synchronized (this) {
            if (_startup != null) {
                _startup.cancel();
                _startup = null;
            } else {
                cancelled = true;
            }
        }

        final StartupAnswer startup = (StartupAnswer) answer;
        if (!startup.getResult()) {
            s_logger.error("Not allowed to connect to the server: " + answer.getDetails());
            System.exit(1);
        }
        if (cancelled) {
            s_logger.warn("Threw away a startup answer because we're reconnecting.");
            return;
        }

        s_logger.info("Proccess agent startup answer, agent id = " + startup.getHostId());

        setId(startup.getHostId());
        _pingInterval = startup.getPingInterval() * 1000; // change to ms.

        setLastPingResponseTime();
        scheduleWatch(link, response, _pingInterval, _pingInterval);

        _ugentTaskPool.setKeepAliveTime(2* _pingInterval, TimeUnit.MILLISECONDS);

        s_logger.info("Startup Response Received: agent id = " + getId());
    }

    protected void processRequest(final Request request, final Link link) {
        boolean requestLogged = false;
        Response response = null;
        try {
            final Command[] cmds = request.getCommands();
            final Answer[] answers = new Answer[cmds.length];

            for (int i = 0; i < cmds.length; i++) {
                final Command cmd = cmds[i];
                Answer answer;
                try {
                	if (s_logger.isDebugEnabled()) {
                		if (!requestLogged) // ensures request is logged only once per method call
                		{
                			String requestMsg = request.toString();
                			if (requestMsg != null) {
                				s_logger.debug("Request:" + requestMsg);
                			}
                			requestLogged = true;
                		}
                		s_logger.debug("Processing command: " + cmd.toString());
                	}

                    if (cmd instanceof CronCommand) {
                        final CronCommand watch = (CronCommand) cmd;
                        scheduleWatch(link, request, watch.getInterval() * 1000, watch.getInterval() * 1000);
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ShutdownCommand) {
                        ShutdownCommand shutdown = (ShutdownCommand) cmd;
                        s_logger.debug("Received shutdownCommand, due to: " + shutdown.getReason());
                        cancelTasks();
                        _reconnectAllowed = false;
                        answer = new Answer(cmd, true, null);
                    } else if (cmd instanceof ReadyCommand && ((ReadyCommand)cmd).getDetails() != null) {
                		s_logger.debug("Not ready to connect to mgt server: " + ((ReadyCommand)cmd).getDetails());
                		System.exit(1);
                    	return;
                    } else if (cmd instanceof MaintainCommand) {
                          s_logger.debug("Received maintainCommand" );
                          cancelTasks();
                          _reconnectAllowed = false;
                          answer = new MaintainAnswer((MaintainCommand)cmd);
                    } else if (cmd instanceof AgentControlCommand) {
                        answer = null;
                        synchronized (_controlListeners) {
                            for (IAgentControlListener listener : _controlListeners) {
                                answer = listener.processControlRequest(request, (AgentControlCommand) cmd);
                                if (answer != null) {
                                    break;
                                }
                            }
                        }

                        if (answer == null) {
                            s_logger.warn("No handler found to process cmd: " + cmd.toString());
                            answer = new AgentControlAnswer(cmd);
                        }

                    } else {
                        if (cmd instanceof ReadyCommand) {
                            processReadyCommand((ReadyCommand)cmd);
                        }
                        _inProgress.incrementAndGet();
                        try {
                            answer = _resource.executeRequest(cmd);
                        } finally {
                            _inProgress.decrementAndGet();
                        }
                        if (answer == null) {
                            s_logger.debug("Response: unsupported command" + cmd.toString());
                            answer = Answer.createUnsupportedCommandAnswer(cmd);
                        }
                    }
                } catch (final Throwable th) {
                    s_logger.warn("Caught: ", th);
                    final StringWriter writer = new StringWriter();
                    th.printStackTrace(new PrintWriter(writer));
                    answer = new Answer(cmd, false, writer.toString());
                }

                answers[i] = answer;
                if (!answer.getResult() && request.stopOnError()) {
                    for (i++; i < cmds.length; i++) {
                        answers[i] = new Answer(cmds[i], false, "Stopped by previous failure");
                    }
                    break;
                }
            }
            response = new Response(request, answers);
        } finally {
            if (s_logger.isDebugEnabled()) {
            	String responseMsg = response.toString();
            	if (responseMsg != null) {
            		s_logger.debug(response.toString());
            	}
            }

            if (response != null) {
                try {
                    link.send(response.toBytes());
                } catch (final ClosedChannelException e) {
                    s_logger.warn("Unable to send response: " + response.toString());
                }
            }
        }
    }

    public void processResponse(final Response response, final Link link) {
        final Answer answer = response.getAnswer();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Received response: " + response.toString());
        }
        if (answer instanceof StartupAnswer) {
            processStartupAnswer(answer, response, link);
        } else if (answer instanceof AgentControlAnswer) {
            // Notice, we are doing callback while holding a lock!
            synchronized (_controlListeners) {
                for (IAgentControlListener listener : _controlListeners) {
                    listener.processControlResponse(response, (AgentControlAnswer) answer);
                }
            }
        } else {
            setLastPingResponseTime();
        }
    }
    
    
    public void processReadyCommand(Command cmd) {

        final ReadyCommand ready = (ReadyCommand) cmd;
        
        s_logger.info("Proccess agent ready command, agent id = " + ready.getHostId());
        if (ready.getHostId() != null) {
            setId(ready.getHostId());
        }
        s_logger.info("Ready command is processed: agent id = " + getId());

    }

    public void processOtherTask(Task task) {
        final Object obj = task.get();
        if (obj instanceof Response) {
            if ((System.currentTimeMillis() - _lastPingResponseTime) > _pingInterval * _shell.getPingRetries()) {
                s_logger.error("Ping Interval has gone past " + _pingInterval * _shell.getPingRetries() + ".  Attempting to reconnect.");
                final Link link = task.getLink();
                reconnect(link);
                return;
            }

            final PingCommand ping = _resource.getCurrentStatus(getId());
            final Request request = new Request(_id, -1, ping, false);
            request.setSequence(getNextSequence());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending ping: " + request.toString());
            }

            try {
                task.getLink().send(request.toBytes());
                //if i can send pingcommand out, means the link is ok
                setLastPingResponseTime();
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to send request: " + request.toString());
            }
            
        } else if (obj instanceof Request) {
            final Request req = (Request) obj;
            final Command command = req.getCommand();
            Answer answer = null;
            _inProgress.incrementAndGet();
            try {
                answer = _resource.executeRequest(command);
            } finally {
                _inProgress.decrementAndGet();
            }
            if (answer != null) {
                final Response response = new Response(req, answer);

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Watch Sent: " + response.toString());
                }
                try {
                    task.getLink().send(response.toBytes());
                } catch (final ClosedChannelException e) {
                    s_logger.warn("Unable to send response: " + response.toString());
                }
            }
        } else {
            s_logger.warn("Ignoring an unknown task");
        }
    }

    public synchronized void setLastPingResponseTime() {
        _lastPingResponseTime = System.currentTimeMillis();
    }

    protected synchronized long getNextSequence() {
        return _sequence++;
    }

    @Override
    public void registerControlListener(IAgentControlListener listener) {
        synchronized (_controlListeners) {
            _controlListeners.add(listener);
        }
    }

    @Override
    public void unregisterControlListener(IAgentControlListener listener) {
        synchronized (_controlListeners) {
            _controlListeners.remove(listener);
        }
    }

    @Override
    public AgentControlAnswer sendRequest(AgentControlCommand cmd, int timeoutInMilliseconds) throws AgentControlChannelException {
        Request request = new Request(this.getId(), -1, new Command[] { cmd } , true, false);
        request.setSequence(getNextSequence());

        AgentControlListener listener = new AgentControlListener(request);

        registerControlListener(listener);
        try {
            postRequest(request);
            synchronized (listener) {
                try {
                    listener.wait(timeoutInMilliseconds);
                } catch (InterruptedException e) {
                    s_logger.warn("sendRequest is interrupted, exit waiting");
                }
            }

            return listener.getAnswer();
        } finally {
            unregisterControlListener(listener);
        }
    }

    @Override
    public void postRequest(AgentControlCommand cmd) throws AgentControlChannelException {
        Request request = new Request(this.getId(), -1, new Command[] { cmd } , true, false);
        request.setSequence(getNextSequence());
        postRequest(request);
    }

    private void postRequest(Request request) throws AgentControlChannelException {
        if (_link != null) {
            try {
                _link.send(request.toBytes());
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to post agent control reques: " + request.toString());
                throw new AgentControlChannelException("Unable to post agent control request due to " + e.getMessage());
            }
        } else {
            throw new AgentControlChannelException("Unable to post agent control request as link is not available");
        }
    }

    public class AgentControlListener implements IAgentControlListener {
        private AgentControlAnswer _answer;
        private final Request      _request;

        public AgentControlListener(Request request) {
            _request = request;
        }

        public AgentControlAnswer getAnswer() {
            return _answer;
        }

        @Override
        public Answer processControlRequest(Request request, AgentControlCommand cmd) {
            return null;
        }

        @Override
        public void processControlResponse(Response response, AgentControlAnswer answer) {
            if (_request.getSequence() == response.getSequence()) {
                _answer = answer;
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

    protected class ShutdownThread extends Thread {
        Agent _agent;

        public ShutdownThread(final Agent agent) {
            super("AgentShutdownThread");
            _agent = agent;
        }

        @Override
        public void run() {
            _agent.stop(ShutdownCommand.Requested, null);
        }
    }

    public class WatchTask extends TimerTask {
        protected Request _request;
        protected Agent   _agent;
        protected Link    _link;

        public WatchTask(final Link link, final Request request, final Agent agent) {
            super();
            _request = request;
            _link = link;
            _agent = agent;
        }

        @Override
        public void run() {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Scheduling " + (_request instanceof Response ? "Ping" : "Watch Task"));
            }
            try {
                if (_request instanceof Response) {
                    _ugentTaskPool.submit(new ServerHandler(Task.Type.OTHER, _link, _request));
                } else {
                    _link.schedule(new ServerHandler(Task.Type.OTHER, _link, _request));
                }
            } catch (final ClosedChannelException e) {
                s_logger.warn("Unable to schedule task because channel is closed");
            }
        }
    }

    public class StartupTask extends TimerTask {
        protected Link             _link;
        protected volatile boolean cancelled = false;

        public StartupTask(final Link link) {
            s_logger.debug("Startup task created");
            _link = link;
        }

        @Override
        public synchronized boolean cancel() {
            // TimerTask.cancel may fail depends on the calling context
            if (!cancelled) {
                cancelled = true;
                _startupWait = _startupWaitDefault;
                s_logger.debug("Startup task cancelled");
                return super.cancel();
            }
            return true;
        }

        @Override
        public synchronized void run() {
            if (!cancelled) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("The startup command is now cancelled");
                }
                cancelled = true;
                _startup = null;
                _startupWait = _startupWaitDefault *2;
                reconnect(_link);
            }
        }
    }
    
    public class AgentRequestHandler extends Task {
          public AgentRequestHandler(Task.Type type, Link link, Request req) {
              super(type, link, req);
          }

        @Override
        protected void doTask(Task task) throws Exception {
            Request req = (Request)this.get();
            if (!(req instanceof Response)) {
                processRequest(req, task.getLink());
            }
        }
    }

    public class ServerHandler extends Task {
        public ServerHandler(Task.Type type, Link link, byte[] data) {
            super(type, link, data);
        }

        public ServerHandler(Task.Type type, Link link, Request req) {
            super(type, link, req);
        }

        @Override
        public void doTask(final Task task) {
            if (task.getType() == Task.Type.CONNECT) {
                _shell.getBackoffAlgorithm().reset();
                setLink(task.getLink());
                sendStartup(task.getLink());
            } else if (task.getType() == Task.Type.DATA) {
                Request request;
                try {
                    request = Request.parse(task.getData());
                    if (request instanceof Response) {
                        //It's for pinganswer etc, should be processed immediately.
                        processResponse((Response) request, task.getLink());
                    } else {
                        //put the requests from mgt server into another thread pool, as the request may take a longer time to finish. Don't block the NIO main thread pool
                        //processRequest(request, task.getLink());
                        _executor.execute(new AgentRequestHandler(this.getType(), this.getLink(), request));
                    }
                } catch (final ClassNotFoundException e) {
                    s_logger.error("Unable to find this request ");
                } catch (final Exception e) {
                    s_logger.error("Error parsing task", e);
                }
            } else if (task.getType() == Task.Type.DISCONNECT) {
                reconnect(task.getLink());
                return;
            } else if (task.getType() == Task.Type.OTHER) {
                processOtherTask(task);
            }
        }
    }
}
