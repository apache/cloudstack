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

package com.cloud.utils.ssh;

import org.junit.Assert;
import org.junit.Test;

public class SSHCmdHelperTest {

    @Test
    public void getCmdForLoggingReturnsMaskedCmdWhenProvided() {
        String cmd = "python /usr/bin/prepare_tftp_bootfile.py restore tftp mac server share dir template user SuperSecretPassword ip mask gw";
        String maskedCmd = "python /usr/bin/prepare_tftp_bootfile.py restore tftp mac server share dir template user ***** ip mask gw";

        String result = SSHCmdHelper.getCmdForLogging(cmd, maskedCmd);

        Assert.assertEquals(maskedCmd, result);
        Assert.assertFalse(result.contains("SuperSecretPassword"));
    }

    @Test
    public void getCmdForLoggingFallsBackToKeystoreSplitWhenNoMaskProvided() {
        String cmd = "setup.sh /etc/cloudstack/agent/agent.properties cloud.jks SuperSecretPassword 825";

        String result = SSHCmdHelper.getCmdForLogging(cmd, null);

        Assert.assertFalse(result.contains("SuperSecretPassword"));
        Assert.assertEquals("setup.sh /etc/cloudstack/agent/agent.properties ", result);
    }
}
