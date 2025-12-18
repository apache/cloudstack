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
package com.cloud.api.dispatch;

import com.cloud.exception.ResourceAllocationException;
import org.apache.cloudstack.api.BaseCmd;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SpecificCmdValidationWorkerTest {

    @Test
    public void testHandle() throws ResourceAllocationException {
        // Prepare
        final BaseCmd cmd = mock(BaseCmd.class);
        final Map<String, String> params = new HashMap<String, String>();

        // Execute
        final SpecificCmdValidationWorker worker = new SpecificCmdValidationWorker();

        worker.handle(new DispatchTask(cmd, params));

        // Assert
        verify(cmd, times(1)).validateSpecificParameters(params);
    }
}
