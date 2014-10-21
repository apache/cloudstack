/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.test;

import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class MockLocalHostEndPoint extends LocalHostEndpoint {
    @Override
    public Answer sendMessage(Command cmd) {
        if ((cmd instanceof CopyCommand) || (cmd instanceof DownloadCommand) || (cmd instanceof DeleteCommand)) {
            return resource.executeRequest(cmd);
        }
        // TODO Auto-generated method stub
        return new Answer(cmd, false, "unsupported command:" + cmd.toString());
    }
}
