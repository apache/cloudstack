// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.qemu;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cloud.utils.script.OutputInterpreter;

/**
 * Reads the output of "qemu-img convert -p" while the command runs and writes the progress to the Agent log, so that
 * an operator can follow a long volume copy or migration. Any output that is not a progress line is kept and returned
 * as the error message when the command fails.
 */
public class QemuImgProgressLogger extends OutputInterpreter {
    protected static final Pattern PROGRESS_PATTERN = Pattern.compile("\\((\\d{1,3}(?:\\.\\d+)?)/100%\\)");
    protected static final int PROGRESS_STEP_PERCENT = 10;
    protected static final long PROGRESS_MAX_SILENCE_MS = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_KEPT_OUTPUT_CHARS = 4096;
    private static final long WAIT_FOR_OUTPUT_END_SECONDS = 5;

    private final String description;
    private final StringBuilder otherOutput = new StringBuilder();
    private final CountDownLatch outputEnded = new CountDownLatch(1);
    private int lastLoggedStep = -1;
    private long lastLoggedTime;
    private double lastProgress = -1;

    public QemuImgProgressLogger(String description) {
        this.description = description;
    }

    @Override
    public boolean drain() {
        return true;
    }

    @Override
    public String interpret(BufferedReader reader) throws IOException {
        lastLoggedTime = System.currentTimeMillis();
        try {
            String line;
            // qemu-img ends each progress report with a carriage return, which readLine() also treats as a line end
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
        } finally {
            outputEnded.countDown();
        }
        return null;
    }

    protected void processLine(String line) {
        Matcher matcher = PROGRESS_PATTERN.matcher(line);
        if (!matcher.find()) {
            keepOutput(line);
            return;
        }
        lastProgress = Double.parseDouble(matcher.group(1));
        int step = (int) (lastProgress / PROGRESS_STEP_PERCENT);
        long now = System.currentTimeMillis();
        if (step > lastLoggedStep || now - lastLoggedTime >= PROGRESS_MAX_SILENCE_MS) {
            lastLoggedStep = Math.max(step, lastLoggedStep);
            lastLoggedTime = now;
            logger.info("Progress of {}: {}%.", description, String.format("%.2f", lastProgress));
        }
    }

    private synchronized void keepOutput(String line) {
        if (line.trim().isEmpty() || otherOutput.length() >= MAX_KEPT_OUTPUT_CHARS) {
            return;
        }
        otherOutput.append(line.trim()).append(' ');
    }

    @Override
    public String processError(BufferedReader reader) throws IOException {
        try {
            if (!outputEnded.await(WAIT_FOR_OUTPUT_END_SECONDS, TimeUnit.SECONDS)) {
                logger.debug("Still reading the output of {} after it failed.", description);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String kept;
        synchronized (this) {
            kept = otherOutput.toString().trim();
        }
        if (lastProgress >= 0) {
            logger.warn("{} failed at {}%.", description, String.format("%.2f", lastProgress));
        }
        return kept.isEmpty() ? "qemu-img convert failed without an error message" : kept;
    }

    public double getLastProgress() {
        return lastProgress;
    }
}
