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

package org.apache.cloudstack.utils.process;

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProcessRunner {
    public static final Logger LOG = Logger.getLogger(ProcessRunner.class);

    private static final ExecutorService processExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("ProcessRunner"));

    private static String readStream(final InputStream inputStream) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }

    public static ProcessResult executeCommands(final List<String> commands, final Duration timeOut) {
        Preconditions.checkArgument(commands != null && timeOut != null);

        int retVal = -2;
        String stdOutput = null;
        String stdError = null;

        try {
            final Process process = new ProcessBuilder().command(commands).start();
            if (timeOut.getStandardSeconds() > 0) {
                final Future<Integer> processFuture = processExecutor.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return process.waitFor();
                    }
                });
                try {
                    retVal = processFuture.get(timeOut.getStandardSeconds(), TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    retVal = -1;
                    stdError = e.getMessage();
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Failed to complete the requested command due to execution error: " + e.getMessage());
                    }
                } catch (TimeoutException e) {
                    retVal = -1;
                    stdError = "Operation timed out, aborted";
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Failed to complete the requested command within timeout: " + e.getMessage());
                    }
                } finally {
                    if (Strings.isNullOrEmpty(stdError)) {
                        stdOutput = readStream(process.getInputStream());
                        stdError = readStream(process.getErrorStream());
                    }
                    process.destroy();
                }
            } else {
                retVal = process.waitFor();
                stdOutput = readStream(process.getInputStream());
                stdError = readStream(process.getErrorStream());
            }
            if (LOG.isTraceEnabled()) {
                LOG.trace("Process standard output: " + stdOutput);
                LOG.trace("Process standard error output: " + stdError);
            }
        } catch (IOException | InterruptedException e) {
            stdError = e.getMessage();
            LOG.error("Exception caught error running commands: " + e.getMessage());
        }
        return new ProcessResult(stdOutput, stdError, retVal);
    }
}