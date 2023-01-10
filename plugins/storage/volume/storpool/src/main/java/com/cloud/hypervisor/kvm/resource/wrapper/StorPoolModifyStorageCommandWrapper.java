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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.StorPoolModifyStoragePoolAnswer;
import com.cloud.agent.api.storage.StorPoolModifyStoragePoolCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.template.TemplateProp;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@ResourceWrapper(handles =  StorPoolModifyStoragePoolCommand.class)
public final class StorPoolModifyStorageCommandWrapper extends CommandWrapper<StorPoolModifyStoragePoolCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final StorPoolModifyStoragePoolCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String clusterId = getSpClusterId();
        if (clusterId == null) {
            logger.debug(String.format("Could not get StorPool cluster id for a command [%s]", command.getClass()));
            return new Answer(command, false, "spNotFound");
        }
        try {
            String result = attachOrDetachVolume("attach", "volume", command.getVolumeName());
            if (result != null) {
                return new Answer(command, false, result);
            }
            final KVMStoragePoolManager storagePoolMgr = libvirtComputingResource.getStoragePoolMgr();
            final KVMStoragePool storagepool =
                    storagePoolMgr.createStoragePool(command.getPool().getUuid(), command.getPool().getHost(), command.getPool().getPort(), command.getPool().getPath(), command.getPool()
                            .getUserInfo(), command.getPool().getType());
            if (storagepool == null) {
                logger.debug(String.format("Did not find a storage pool [%s]", command.getPool().getId()));
                return new Answer(command, false, String.format("Failed to create storage pool [%s]", command.getPool().getId()));
            }

            final Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();
            final StorPoolModifyStoragePoolAnswer answer = new StorPoolModifyStoragePoolAnswer(command, storagepool.getCapacity(), storagepool.getAvailable(), tInfo, clusterId);

            return answer;
        } catch (Exception e) {
            logger.debug(String.format("Could not modify storage due to %s", e.getMessage()));
            return new Answer(command, e);
        }
    }

    private String getSpClusterId() {
        Script sc = new Script("storpool_confget", 0, logger);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();

        String SP_CLUSTER_ID = null;
        final String err = sc.execute(parser);
        if (err != null) {
            StorPoolStorageAdaptor.SP_LOG("Could not execute storpool_confget. Error: %s", err);
            return SP_CLUSTER_ID;
        }

        for (String line: parser.getLines().split("\n")) {
            String[] toks = line.split("=");
            if( toks.length != 2 ) {
                continue;
            }
            if (toks[0].equals("SP_CLUSTER_ID")) {
                SP_CLUSTER_ID = toks[1];
                return SP_CLUSTER_ID;
            }
        }
        return SP_CLUSTER_ID;
    }

    public String attachOrDetachVolume(String command, String type, String volumeUuid) {
        final String name = StorPoolStorageAdaptor.getVolumeNameFromPath(volumeUuid, true);
        if (name == null) {
            return null;
        }

        String err = null;
        Script sc = new Script("storpool", 300000, logger);
        sc.add("-M");
        sc.add("-j");
        sc.add(command);
        sc.add(type, name);
        sc.add("here");
        sc.add("onRemoteAttached");
        sc.add("export");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();

        String res = sc.execute(parser);

        if (res != null) {
            if (!res.equals(Script.ERR_TIMEOUT)) {
                try {
                    Set<Entry<String, JsonElement>> obj2 = new JsonParser().parse(res).getAsJsonObject().entrySet();
                    for (Entry<String, JsonElement> entry : obj2) {
                        if (entry.getKey().equals("error")) {
                            res = entry.getValue().getAsJsonObject().get("name").getAsString();
                        }
                    }
                } catch (Exception e) {
                }
            }

            err = String.format("Unable to %s volume %s. Error: %s", command, name, res);
        }

        if (err != null) {
            logger.warn(err);
        }
        return res;
    }
}
