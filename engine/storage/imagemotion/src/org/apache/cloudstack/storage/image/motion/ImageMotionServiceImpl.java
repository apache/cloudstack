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
package org.apache.cloudstack.storage.image.motion;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;


public class ImageMotionServiceImpl implements ImageMotionService {
    @Inject
    List<ImageMotionStrategy> motionStrategies;
    @Inject
    VolumeService volumeService;
    @Inject
    ImageService imageService;

    @Override
    public boolean copyIso(String isoUri, String destIsoUri) {
        // TODO Auto-generated method stub
        return false;
    }

  

    @Override
    public void copyTemplateAsync(TemplateInfo destTemplate, TemplateInfo srcTemplate, AsyncCompletionCallback<CommandResult> callback) {
     /*   ImageMotionStrategy ims = null;
        for (ImageMotionStrategy strategy : motionStrategies) {
            if (strategy.canHandle(srcTemplate)) {
                ims = strategy;
                break;
            }
        }

        if (ims == null) {
            throw new CloudRuntimeException("Can't find proper image motion strategy");
        }

        EndPoint ep = ims.getEndPoint(destTemplate, srcTemplate);
        String srcUri = srcTemplate.getDataStore().grantAccess(srcTemplate, ep);
        String destUri = destTemplate.getDataStore().grantAccess(destTemplate, ep);
        
        ims.copyTemplateAsync(destUri, srcUri, ep, callback);*/
    }


}
