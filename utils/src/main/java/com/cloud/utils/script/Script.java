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
import org.apache.log4j.Logger;
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
    private static final Logger s_logger = Logger.getLogger(Script.class);

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
        _logger = logger != null ? logger : s_logger;
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
        this(command, 0, s_logger);
    }

    public Script(String command, Duration timeout) {
        this(command, timeout.getMillis(), s_logger);
    }

    @Deprecated
    public Script(String command, long timeout) {
        this(command, timeout, s_logger);
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

        String commandLine = buildCommandLine(command);
        _logger.debug(String.format("Executing command [%s].", commandLine.split(KeyStoreUtils.KS_FILENAME)[0]));

        try {
            _logger.trace(String.format("Creating process for command [%s].", commandLine));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            if (_workDir != null)
                pb.directory(new File(_workDir));

            _logger.trace(String.format("Starting process for command [%s].", commandLine));
            _process = pb.start();
            if (_process == null) {
                _logger.warn(String.format("Unable to execute command [%s] because no process was created.", commandLine));
                return "Unable to execute the command: " + command[0];
            }

            BufferedReader ir = new BufferedReader(new InputStreamReader(_process.getInputStream()));

            _thread = Thread.currentThread();
            ScheduledFuture<String> future = null;
            if (_timeout > 0) {
                _logger.trace(String.format("Scheduling the execution of command [%s] with a timeout of [%s] milliseconds.", commandLine, _timeout));
                future = s_executors.schedule(this, _timeout, TimeUnit.MILLISECONDS);
            }

            long processPid = _process.pid();
            Task task = null;
            if (interpreter != null && interpreter.drain()) {
                _logger.trace(String.format("Executing interpreting task of process [%s] for command [%s].", processPid, commandLine));
                task = new Task(interpreter, ir);
                s_executors.execute(task);
            }

            while (true) {
                _logger.trace(String.format("Attempting process [%s] execution for command [%s] with timeout [%s].", processPid, commandLine, _timeout));
                try {
                    if (_process.waitFor(_timeout, TimeUnit.MILLISECONDS)) {
                        _logger.trace(String.format("Process [%s] execution for command [%s] completed within timeout period [%s].", processPid, commandLine,
                                _timeout));
                        if (_process.exitValue() == 0) {
                            _logger.debug(String.format("Successfully executed process [%s] for command [%s].", processPid, commandLine));
                            if (interpreter != null) {
                                if (interpreter.drain()) {
                                    _logger.trace(String.format("Returning task result of process [%s] for command [%s].", processPid, commandLine));
                                    return task.getResult();
                                }
                                _logger.trace(String.format("Returning interpretation of process [%s] for command [%s].", processPid, commandLine));
                                return interpreter.interpret(ir);
                            } else {
                                // null return exitValue apparently
                                _logger.trace(String.format("Process [%s] for command [%s] exited with value [%s].", processPid, commandLine,
                                        _process.exitValue()));
                                return String.valueOf(_process.exitValue());
                            }
                        } else {
                            _logger.warn(String.format("Execution of process [%s] for command [%s] failed.", processPid, commandLine));
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    if (!_isTimeOut) {
                        _logger.debug(String.format("Exception [%s] occurred; however, it was not a timeout. Therefore, proceeding with the execution of process [%s] for command "
                                + "[%s].", e.getMessage(), processPid, commandLine), e);
                        continue;
                    }
                } finally {
                    if (future != null) {
                        future.cancel(false);
                    }
                    Thread.interrupted();
                }

                TimedOutLogger log = new TimedOutLogger(_process);
                Task timedoutTask = new Task(log, ir);

                _logger.trace(String.format("Running timed out task of process [%s] for command [%s].", processPid, commandLine));
                timedoutTask.run();
                if (!_passwordCommand) {
                    _logger.warn(String.format("Process [%s] for command [%s] timed out. Output is [%s].", processPid, commandLine, timedoutTask.getResult()));
                } else {
                    _logger.warn(String.format("Process [%s] for command [%s] timed out.", processPid, commandLine));
                }

                return ERR_TIMEOUT;
            }

            _logger.debug(String.format("Exit value of process [%s] for command [%s] is [%s].", processPid, commandLine, _process.exitValue()));

            BufferedReader reader = new BufferedReader(new InputStreamReader(_process.getInputStream()), 128);

            String error;
            if (interpreter != null) {
                error = interpreter.processError(reader);
            } else {
                error = String.valueOf(_process.exitValue());
            }

            _logger.warn(String.format("Process [%s] for command [%s] encountered the error: [%s].", processPid, commandLine, error));

            return error;
        } catch (SecurityException ex) {
            _logger.warn(String.format("Exception [%s] occurred. This may be due to an attempt of executing command [%s] as non root.", ex.getMessage(), commandLine),
                    ex);
            return stackTraceAsString(ex);
        } catch (Exception ex) {
            _logger.warn(String.format("Exception [%s] occurred when attempting to run command [%s].", ex.getMessage(), commandLine), ex);
            return stackTraceAsString(ex);
        } finally {
            if (_process != null) {
                _logger.trace(String.format("Destroying process [%s] for command [%s].", _process.pid(), commandLine));
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
        s_logger.debug("Looking for " + script + " in the classpath");

        URL url = ClassLoader.getSystemResource(script);
        s_logger.debug("System resource: " + url);
        File file = null;
        if (url != null) {
            file = new File(url.getFile());
            s_logger.debug("Absolute path =  " + file.getAbsolutePath());
            return file.getAbsolutePath();
        }

        if (path == null) {
            s_logger.warn("No search path specified, unable to look for " + script);
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
        s_logger.debug("Classpath resource: " + url);
        if (url != null) {
            try {
                file = new File(new URI(url.toString()).getPath());
                s_logger.debug("Absolute path =  " + file.getAbsolutePath());
                return file.getAbsolutePath();
            } catch (URISyntaxException e) {
                s_logger.warn("Unable to convert " + url.toString() + " to a URI");
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

        s_logger.debug("Looking for " + script);
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

                s_logger.debug("Current binaries reside at " + cp);
                search = cp;
            } else if (i == 1) {
                s_logger.debug("Searching in environment.properties");
                try {
                    final File propsFile = PropertiesUtil.findConfigFile("environment.properties");
                    if (propsFile == null) {
                        s_logger.debug("environment.properties could not be opened");
                    } else {
                        final Properties props = PropertiesUtil.loadFromFile(propsFile);
                        search = props.getProperty("paths.script");
                    }
                } catch (IOException e) {
                    s_logger.debug("environment.properties could not be opened");
                    continue;
                }
                s_logger.debug("environment.properties says scripts should be in " + search);
            } else {
                s_logger.debug("Searching in the current directory");
                search = ".";
            }

            search += File.separatorChar + path + File.separator;
            do {
                search = search.substring(0, search.lastIndexOf(File.separator));
                file = new File(search + File.separator + script);
                s_logger.debug("Looking for " + script + " in " + file.getAbsolutePath());
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
            s_logger.debug("Looking for " + script + " in " + file.getAbsolutePath());
        } while (!file.exists() && search.lastIndexOf(File.separator) != -1);

        if (file.exists()) {
            return file.getAbsolutePath();
        }

        s_logger.warn("Unable to find script " + script);
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
