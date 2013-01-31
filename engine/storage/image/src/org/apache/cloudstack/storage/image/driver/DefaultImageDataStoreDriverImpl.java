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
package org.apache.cloudstack.storage.image.driver;

import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.command.CreateObjectCommand;
import org.apache.cloudstack.storage.endpoint.EndPointSelector;
import org.apache.cloudstack.storage.image.ImageDataStoreDriver;

import com.cloud.storage.dao.VMTemplateDao;

//http-read-only based image store
public class DefaultImageDataStoreDriverImpl implements ImageDataStoreDriver {
    @Inject
    EndPointSelector selector;
    @Inject
    VMTemplateDao imageDataDao;
    public DefaultImageDataStoreDriverImpl() {
    }

    @Override
    public String grantAccess(DataObject data, EndPoint ep) {
        return data.getUri();
    }

    @Override
    public boolean revokeAccess(DataObject data, EndPoint ep) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Set<DataObject> listObjects(DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createAsync(DataObject data,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        //for default http data store, can create http based template/iso
        CreateCmdResult result = new CreateCmdResult("", null);
        if (!data.getUri().startsWith("http")) {
            result.setResult("can't register an image which is not a http link");
            callback.complete(result);
            return;
        }
        
        if (data.getSize() == null && data.getType() == DataObjectType.TEMPLATE) {
            //the template size is unknown during registration, need to find out the size of template
            EndPoint ep = selector.select(data);
            if (ep == null) {
                result.setResult("can't find storage client for:" + data.getId() + "," + data.getType());
                callback.complete(result);
                return;
            }
            CreateObjectCommand createCmd = new CreateObjectCommand(data.getUri());
            CreateObjectAnswer answer = (CreateObjectAnswer)ep.sendMessage(createCmd);
            if (answer.getResult()) {
                //update imagestorevo
               
                result = new CreateCmdResult(answer.getPath(), answer.getSize());
            } else {
                result.setResult(answer.getDetails());
            }
            
        }
        
        callback.complete(result);
    }

    @Override
    public void deleteAsync(DataObject data,
            AsyncCompletionCallback<CommandResult> callback) {
        CommandResult result = new CommandResult();
        callback.complete(result);
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        // TODO Auto-generated method stub
        
    }
}
