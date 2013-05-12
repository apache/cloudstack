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
package com.cloud.hypervisor.kvm.storage;

import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.command.CreatePrimaryDataStoreCmd;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;

public class KVMStorageResource {
    private LibvirtComputingResource resource;
    public KVMStorageResource(LibvirtComputingResource resource) {
        this.resource = resource;
    }
    
    public Answer handleStorageCommands(StorageSubSystemCommand command) {
        if (command instanceof CopyCommand) {
            return this.execute((CopyCommand)command);
        } else if (command instanceof AttachPrimaryDataStoreCmd) {
            return this.execute((AttachPrimaryDataStoreCmd)command);
        } else if (command instanceof CreatePrimaryDataStoreCmd) {
            return execute((CreatePrimaryDataStoreCmd) command);
        } else if (command instanceof CreateObjectCommand) {
            return execute((CreateObjectCommand) command);
        } else if (command instanceof DeleteCommand) {
            return execute((DeleteCommand)command);
        } else if (command instanceof AttachCommand) {
            return execute((AttachCommand)command);
        } else if (command instanceof DettachCommand) {
            return execute((DettachCommand)command);
        }
        return new Answer((Command)command, false, "not implemented yet");
    }
    
    protected Answer execute(CopyCommand cmd) {
        return new Answer((Command)cmd, false, "not implemented yet");
    }
    
    protected Answer execute(AttachPrimaryDataStoreCmd cmd) {
        return new Answer((Command)cmd, false, "not implemented yet");
    }
    
    protected Answer execute(CreatePrimaryDataStoreCmd cmd) {
        return new Answer((Command)cmd, false, "not implemented yet");
    }
    
    protected Answer execute(CreateObjectCommand cmd) {
        return new Answer((Command)cmd, false, "not implemented yet");
    }
    
    protected Answer execute(DeleteCommand cmd) {
        return new Answer((Command)cmd, false, "not implemented yet");
    }
    
    protected Answer execute(AttachCommand cmd) {
        return new Answer((Command)cmd, false, "not implemented yet");
    }
    
    protected Answer execute(DettachCommand cmd) {
        return new Answer((Command)cmd, false, "not implemented yet");
    }
    
}
