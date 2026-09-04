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
package com.cloud.api.storage;

import com.cloud.agent.api.to.DataTO;
import org.apache.cloudstack.storage.command.CopyCommand;

public class LinstorBackupSnapshotCommand extends CopyCommand
{
    /**
     * Option holding the secondary storage install path of the parent snapshot qcow2. When set (and
     * fullSnapshot=false), the agent writes an incremental backup: a qcow2 containing only the blocks
     * that differ from the parent, with the parent as its backing file.
     */
    public static final String OPTION_PARENT_PATH = "parentPath";

    public LinstorBackupSnapshotCommand(DataTO srcData, DataTO destData, int timeout, boolean executeInSequence)
    {
        super(srcData, destData, timeout, executeInSequence);
    }
}
