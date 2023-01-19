//
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
//

package com.cloud.utils.script;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.script.OutputInterpreter.TimedOutLogger;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Script implements Callable<String> {
    protected static Logger LOGGER = LogManager.getLogger(Script.class);

    private final Logger _logger;

    public static final String ERR_EXECUTE = "execute.error";
    public static final String ERR_TIMEOUT = "timeout";
    private int _defaultTimeout = 3600 * 1000; /* 1 hour */
    private volatile boolean _isTimeOut = false;

    private boolean _passwordCommand = false;

    private static final ScheduledExecutorService s_executors = Executors.newScheduledThreadPool(10, new NamedThreadFactory("Script"));

    String _workDir;
    ArrayList<String> _command;
    long _timeout;
    Process _process;
    Thread _thread;

    public boolean isTimeout() {
        return _isTimeOut;
    }

    public int getExitValue() {
        return _process.exitValue();
    }

    public Script(String command, Duration timeout, Logger logger) {
        this(command, timeout.getMillis(), logger);
    }

    @Deprecated
    public Script(String command, long timeout, Logger logger) {
        _command = new ArrayList<String>();
        _command.add(command);
        _timeout = timeout;
        if (_timeout == 0) {
            /* always using default timeout 1 hour to avoid thread hang */
            _timeout = _defaultTimeout;
        }
        _process = null;
        _logger = logger != null ? logger : Script.LOGGER;
    }

    public Script(boolean runWithSudo, String command, Duration timeout, Logger logger) {
        this(runWithSudo, command, timeout.getMillis(), logger);
    }

    @Deprecated
    public Script(boolean runWithSudo, String command, long timeout, Logger logger) {
        this(command, timeout, logger);
        if (runWithSudo) {
            _command.add(0, "sudo");
        }
    }

    public Script(String command, Logger logger) {
        this(command, 0, logger);
    }

    public Script(String command) {
        this(command, 0, LOGGER);
    }

    public Script(String command, Duration timeout) {
        this(command, timeout.getMillis(), LOGGER);
    }

    @Deprecated
    public Script(String command, long timeout) {
        this(command, timeout, LOGGER);
    }

    public void add(String... params) {
        for (String param : params) {
            _command.add(param);
        }
    }

    public void add(String param) {
        _command.add(param);
    }

    public Script set(String name, String value) {
        _command.add(name);
        _command.add(value);
        return this;
    }

    public void setWorkDir(String workDir) {
        _workDir = workDir;
    }

    protected String buildCommandLine(String[] command) {
        StringBuilder builder = new StringBuilder();
        boolean obscureParam = false;
        for (int i = 0; i < command.length; i++) {
            String cmd = command[i];
            if (obscureParam) {
                builder.append("******").append(" ");
                obscureParam = false;
            } else {
                builder.append(command[i]).append(" ");
            }

            if ("-y".equals(cmd) || "-z".equals(cmd)) {
                obscureParam = true;
                _passwordCommand = true;
            }
        }
        return builder.toString();
    }

    protected String buildCommandLine(List<String> command) {
        StringBuilder builder = new StringBuilder();
        boolean obscureParam = false;
        for (String cmd : command) {
            if (obscureParam) {
                builder.append("******").append(" ");
                obscureParam = false;
            } else {
                builder.append(cmd).append(" ");
            }

            if ("-y".equals(cmd) || "-z".equals(cmd)) {
                obscureParam = true;
                _passwordCommand = true;
            }
        }
        return builder.toString();
    }

    public long getTimeout() {
        return _timeout;
    }

    public String execute() {
        return execute(new OutputInterpreter.OutputLogger(_logger));
    }

    @Override
    public String toString() {
        String[] command = _command.toArray(new String[_command.size()]);
        return buildCommandLine(command);
    }

    static String stackTraceAsString(Throwable throwable) {
        //TODO: a StringWriter is bit to heavy weight
        try(StringWriter out = new StringWriter(); PrintWriter writer = new PrintWriter(out);) {
            throwable.printStackTrace(writer);
            return out.toString();
        } catch (IOException e) {
            return "";
        }
    }

    public String execute(OutputInterpreter interpreter) {
        String[] command = _command.toArray(new String[_command.size()]);

        if (_logger.isDebugEnabled()) {
            _logger.debug("Executing: " + buildCommandLine(command).split(KeyStoreUtils.KS_FILENAME)[0]);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            if (_workDir != null)
                pb.directory(new File(_workDir));

            _process = pb.start();
            if (_process == null) {
                _logger.warn("Unable to execute: " + buildCommandLine(command));
                return "Unable to execute the command: " + command[0];
            }

            BufferedReader ir = new BufferedReader(new InputStreamReader(_process.getInputStream()));

            _thread = Thread.currentThread();
            ScheduledFuture<String> future = null;
            if (_timeout > 0) {
                future = s_executors.schedule(this, _timeout, TimeUnit.MILLISECONDS);
            }

            Task task = null;
            if (interpreter != null && interpreter.drain()) {
                task = new Task(interpreter, ir);
                s_executors.execute(task);
            }

            while (true) {
                _logger.debug("Executing while with timeout : " + _timeout);
                try {
                    //process execution completed within timeout period
                    if (_process.waitFor(_timeout, TimeUnit.MILLISECONDS)) {
                        //process completed successfully
                        if (_process.exitValue() == 0) {
                            _logger.debug("Execution is successful.");
                            if (interpreter != null) {
                                return interpreter.drain() ? task.getResult() : interpreter.interpret(ir);
                            } else {
                                // null return exitValue apparently
                                return String.valueOf(_process.exitValue());
                            }
                        } else { //process failed
                            break;
                        }
                    } //timeout
                } catch (InterruptedException e) {
                    if (!_isTimeOut) {
                        /*
                         * This is not timeout, we are interrupted by others,
                         * continue
                         */
                        _logger.debug("We are interrupted but it's not a timeout, just continue");
                        continue;
                    }
                } finally {
                    if (future != null) {
                        future.cancel(false);
                    }
                    Thread.interrupted();
                }

                //timeout without completing the process
                TimedOutLogger log = new TimedOutLogger(_process);
                Task timedoutTask = new Task(log, ir);

                timedoutTask.run();
                if (!_passwordCommand) {
                    _logger.warn("Timed out: " + buildCommandLine(command) + ".  Output is: " + timedoutTask.getResult());
                } else {
                    _logger.warn("Timed out: " + buildCommandLine(command));
                }

                return ERR_TIMEOUT;
            }

            _logger.debug("Exit value is " + _process.exitValue());

            BufferedReader reader = new BufferedReader(new InputStreamReader(_process.getInputStream()), 128);

            String error;
            if (interpreter != null) {
                error = interpreter.processError(reader);
            } else {
                error = String.valueOf(_process.exitValue());
            }

            if (_logger.isDebugEnabled()) {
                _logger.debug(error);
            }
            return error;
        } catch (SecurityException ex) {
            _logger.warn("Security Exception....not running as root?", ex);
            return stackTraceAsString(ex);
        } catch (Exception ex) {
            _logger.warn("Exception: " + buildCommandLine(command), ex);
            return stackTraceAsString(ex);
        } finally {
            if (_process != null) {
                IOUtils.closeQuietly(_process.getErrorStream());
                IOUtils.closeQuietly(_process.getOutputStream());
                IOUtils.closeQuietly(_process.getInputStream());
                _process.destroyForcibly();
            }
        }
    }

    @Override
    public String call() {
        try {
            _logger.trace("Checking exit value of process");
            _process.exitValue();
            _logger.trace("Script ran within the allocated time");
        } catch (IllegalThreadStateException e) {
            _logger.warn("Interrupting script.");
            _isTimeOut = true;
            _thread.interrupt();
        }
        return null;
    }

    public static class Task implements Runnable {
        OutputInterpreter interpreter;
        BufferedReader reader;
        String result;
        boolean done;

        public Task(OutputInterpreter interpreter, BufferedReader reader) {
            this.interpreter = interpreter;
            this.reader = reader;
            result = null;
        }

        @Override
        public void run() {
            synchronized(this) {
                done = false;
                try {
                    result = interpreter.interpret(reader);
                } catch (IOException ex) {
                    result = stackTraceAsString(ex);
                } catch (Exception ex) {
                    result = stackTraceAsString(ex);
                } finally {
                        done = true;
                        notifyAll();
                        IOUtils.closeQuietly(reader);
                }
            }
        }

        public synchronized String getResult() throws InterruptedException {
            if (!done) {
                wait();
            }
            return result;
        }
    }

    public static String findScript(String path, String script) {
        LOGGER.debug("Looking for " + script + " in the classpath");

        URL url = ClassLoader.getSystemResource(script);
        LOGGER.debug("System resource: " + url);
        File file = null;
        if (url != null) {
            file = new File(url.getFile());
            LOGGER.debug("Absolute path =  " + file.getAbsolutePath());
            return file.getAbsolutePath();
        }

        if (path == null) {
            LOGGER.warn("No search path specified, unable to look for " + script);
            return null;
        }
        path = path.replace("/", File.separator);

        /**
         * Look in WEB-INF/classes of the webapp
         * URI workaround the URL encoding of url.getFile
         */
        if (path.endsWith(File.separator)) {
            url = Script.class.getClassLoader().getResource(path + script);
        } else {
            url = Script.class.getClassLoader().getResource(path + File.separator + script);
        }
        LOGGER.debug("Classpath resource: " + url);
        if (url != null) {
            try {
                file = new File(new URI(url.toString()).getPath());
                LOGGER.debug("Absolute path =  " + file.getAbsolutePath());
                return file.getAbsolutePath();
            } catch (URISyntaxException e) {
                LOGGER.warn("Unable to convert " + url.toString() + " to a URI");
            }
        }

        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.lastIndexOf(File.separator));
        }

        if (path.startsWith(File.separator)) {
            // Path given was absolute so we assume the caller knows what they want.
            file = new File(path + File.separator + script);
            return file.exists() ? file.getAbsolutePath() : null;
        }

        LOGGER.debug("Looking for " + script);
        String search = null;
        for (int i = 0; i < 3; i++) {
            if (i == 0) {
                String cp = Script.class.getResource(Script.class.getSimpleName() + ".class").toExternalForm();
                int begin = cp.indexOf(File.separator);

                // work around with the inconsistency of java classpath and file separator on Windows 7
                if (begin < 0)
                    begin = cp.indexOf('/');

                int endBang = cp.lastIndexOf("!");
                int end = cp.lastIndexOf(File.separator, endBang);
                if (end < 0)
                    end = cp.lastIndexOf('/', endBang);
                if (end < 0)
                    cp = cp.substring(begin);
                else
                    cp = cp.substring(begin, end);

                LOGGER.debug("Current binaries reside at " + cp);
                search = cp;
            } else if (i == 1) {
                LOGGER.debug("Searching in environment.properties");
                try {
                    final File propsFile = PropertiesUtil.findConfigFile("environment.properties");
                    if (propsFile == null) {
                        LOGGER.debug("environment.properties could not be opened");
                    } else {
                        final Properties props = PropertiesUtil.loadFromFile(propsFile);
                        search = props.getProperty("paths.script");
                    }
                } catch (IOException e) {
                    LOGGER.debug("environment.properties could not be opened");
                    continue;
                }
                LOGGER.debug("environment.properties says scripts should be in " + search);
            } else {
                LOGGER.debug("Searching in the current directory");
                search = ".";
            }

            search += File.separatorChar + path + File.separator;
            do {
                search = search.substring(0, search.lastIndexOf(File.separator));
                file = new File(search + File.separator + script);
                LOGGER.debug("Looking for " + script + " in " + file.getAbsolutePath());
            } while (!file.exists() && search.lastIndexOf(File.separator) != -1);

            if (file.exists()) {
                return file.getAbsolutePath();
            }

        }

        search = System.getProperty("paths.script");

        search += File.separatorChar + path + File.separator;
        do {
            search = search.substring(0, search.lastIndexOf(File.separator));
            file = new File(search + File.separator + script);
            LOGGER.debug("Looking for " + script + " in " + file.getAbsolutePath());
        } while (!file.exists() && search.lastIndexOf(File.separator) != -1);

        if (file.exists()) {
            return file.getAbsolutePath();
        }

        LOGGER.warn("Unable to find script " + script);
        return null;
    }

    public static String runSimpleBashScript(String command) {
        return Script.runSimpleBashScript(command, 0);
    }

    public static String runSimpleBashScript(String command, int timeout) {

        Script s = new Script("/bin/bash", timeout);
        s.add("-c");
        s.add(command);

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        if (s.execute(parser) != null)
            return null;

        String result = parser.getLine();
        if (result == null || result.trim().isEmpty())
            return null;
        else
            return result.trim();
    }

    public static int runSimpleBashScriptForExitValue(String command) {
        return runSimpleBashScriptForExitValue(command, 0);
    }

    public static int runSimpleBashScriptForExitValue(String command, int timeout) {

        Script s = new Script("/bin/bash", timeout);
        s.add("-c");
        s.add(command);

        String result = s.execute(null);
        if (result == null || result.trim().isEmpty())
            return -1;
        else {
            try {
                return Integer.parseInt(result.trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

}
