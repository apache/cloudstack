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
package org.apache.cloudstack.storage.resource;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.template.DownloadManager;
import org.apache.cloudstack.storage.template.DownloadManagerImpl;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingStorageCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.storage.ListTemplateAnswer;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResourceBase;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.template.TemplateProp;
import com.cloud.utils.component.ComponentContext;

public class LocalSecondaryStorageResource extends ServerResourceBase implements SecondaryStorageResource {
    private static final Logger s_logger = Logger.getLogger(LocalSecondaryStorageResource.class);
    int _timeout;

    String _instance;
    String _parent;

    String _dc;
    String _pod;
    String _guid;

    StorageLayer _storage;

    DownloadManager _dlMgr;

    @Override
    public void disconnected() {
    }

    @Override
    public String getRootDir(String url, Integer nfsVersion) {
        return getRootDir();

    }

    public String getRootDir() {
        return _parent;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadProgressCommand) {
            return _dlMgr.handleDownloadCommand(this, (DownloadProgressCommand)cmd);
        } else if (cmd instanceof DownloadCommand) {
            return _dlMgr.handleDownloadCommand(this, (DownloadCommand)cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return new CheckHealthAnswer((CheckHealthCommand)cmd, true);
        } else if (cmd instanceof SecStorageSetupCommand) {
            return new Answer(cmd, true, "success");
        } else if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand)cmd);
        } else if (cmd instanceof ListTemplateCommand) {
            return execute((ListTemplateCommand)cmd);
        } else if (cmd instanceof ComputeChecksumCommand) {
            return execute((ComputeChecksumCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private Answer execute(ComputeChecksumCommand cmd) {
        return new Answer(cmd, false, null);
    }

    private Answer execute(ListTemplateCommand cmd) {
        String root = getRootDir();
        Map<String, TemplateProp> templateInfos = _dlMgr.gatherTemplateInfo(root);
        return new ListTemplateAnswer(((NfsTO)cmd.getDataStore()).getUrl(), templateInfos);
    }

    @Override
    public Type getType() {
        return Host.Type.LocalSecondaryStorage;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingStorageCommand(Host.Type.Storage, id, new HashMap<String, Boolean>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        _dc = (String)params.get("zone");
        if (_dc == null) {
            throw new ConfigurationException("Unable to find the zone");
        }
        _pod = (String)params.get("pod");

        _instance = (String)params.get("instance");

        _parent = (String)params.get("mount.path");
        if (_parent == null) {
            throw new ConfigurationException("No directory specified.");
        }

        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            String value = (String)params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                value = "com.cloud.storage.JavaStorageLayer";
            }

            try {
                Class<StorageLayer> clazz = (Class<StorageLayer>)Class.forName(value);
                _storage = ComponentContext.inject(clazz);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to find class " + value);
            }
        }

        if (!_storage.mkdirs(_parent)) {
            s_logger.warn("Unable to create the directory " + _parent);
            throw new ConfigurationException("Unable to create the directory " + _parent);
        }

        s_logger.info("Mount point established at " + _parent);

        params.put("template.parent", _parent);
        params.put(StorageLayer.InstanceConfigKey, _storage);

        _dlMgr = new DownloadManagerImpl();
        _dlMgr.configure("DownloadManager", params);

        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public StartupCommand[] initialize() {

        final StartupStorageCommand cmd =
            new StartupStorageCommand(_parent, StoragePoolType.Filesystem, 1024l * 1024l * 1024l * 1024l, _dlMgr.gatherTemplateInfo(_parent));
        cmd.setResourceType(Storage.StorageResourceType.LOCAL_SECONDARY_STORAGE);
        cmd.setIqn("local://");
        fillNetworkInformation(cmd);
        cmd.setDataCenter(_dc);
        cmd.setPod(_pod);
        cmd.setGuid(_guid);
        cmd.setName(_guid);
        cmd.setVersion(LocalSecondaryStorageResource.class.getPackage().getImplementationVersion());

        return new StartupCommand[] {cmd};
    }

    @Override
    protected String getDefaultScriptsDir() {
        return "scripts/storage/secondary";
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub

    }
}
