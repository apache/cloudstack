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
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

public class QemuImgProgressLoggerTest {

    private BufferedReader readerOf(String output) {
        return new BufferedReader(new StringReader(output));
    }

    @Test
    public void interpretTestReadsProgressSeparatedByCarriageReturns() throws IOException {
        QemuImgProgressLogger progressLogger = new QemuImgProgressLogger("test");

        String result = progressLogger.interpret(readerOf("    (0.00/100%)\r    (1.00/100%)\r    (55.50/100%)\r    (100.00/100%)\n"));

        Assert.assertNull(result);
        Assert.assertEquals(100.0, progressLogger.getLastProgress(), 0.001);
    }

    @Test
    public void processErrorTestReturnsOutputThatIsNotProgress() throws IOException {
        QemuImgProgressLogger progressLogger = new QemuImgProgressLogger("test");
        progressLogger.interpret(readerOf("    (0.00/100%)\r    (12.00/100%)\rqemu-img: error while writing at byte 1024: No space left on device\n"));

        String error = progressLogger.processError(readerOf(""));

        Assert.assertEquals("qemu-img: error while writing at byte 1024: No space left on device", error);
        Assert.assertEquals(12.0, progressLogger.getLastProgress(), 0.001);
    }

    @Test
    public void processErrorTestReturnsDefaultMessageWhenThereIsNoOutput() throws IOException {
        QemuImgProgressLogger progressLogger = new QemuImgProgressLogger("test");
        progressLogger.interpret(readerOf(""));

        Assert.assertEquals("qemu-img convert failed without an error message", progressLogger.processError(readerOf("")));
    }
}
