/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.maint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.maint.dao.AgentUpgradeDao;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;

/**
 *
 *  UpgradeManagerImpl implements the upgrade process.  It's functionalities
 *  include
 *    1. Identifying agents that require an upgrade before it can connect.
 *    2. Spread out a release update to agents so that the entire system
 *       does not come down at the same time.
 */
@Local(UpgradeManager.class)
public class UpgradeManagerImpl implements UpgradeManager {
	private final static Logger s_logger = Logger.getLogger(UpgradeManagerImpl.class);

    String _name;
    String _minimalVersion;
    String _recommendedVersion;
    String _upgradeUrl;
    String _agentPath;
    long _checkInterval;
    
    AgentUpgradeDao _upgradeDao;
    
    @Override
    public State registerForUpgrade(long hostId, String version) {
        State state = State.UpToDate;
        s_logger.debug("Minimal version is " + _minimalVersion + "; version is " + version + "; recommended version is " + _recommendedVersion);
        if (Version.compare(version, _minimalVersion) < 0) {
            state = State.RequiresUpdate;
        } else if (Version.compare(version, _recommendedVersion) < 0) {
            state = State.WaitingForUpdate;
        } else {
            state = State.UpToDate;
        }

        /*
        if (state != State.UpToDate) {
            AgentUpgradeVO vo = _upgradeDao.findById(hostId);
            if (vo == null) {
                vo = new AgentUpgradeVO(hostId, version, state);
                _upgradeDao.persist(vo);
            }
        }
        */
        
        return state;
    }
    
    public String deployNewAgent(String url) {
        s_logger.info("Updating agent with binary from " + url);

        final HttpClient client = new HttpClient();
        final GetMethod method = new GetMethod(url);
        int response;
        File file = null;
        try {
            response = client.executeMethod(method);
            if (response != HttpURLConnection.HTTP_OK) {
                s_logger.warn("Retrieving the agent gives response code: " + response);
                return "Retrieving the file from " + url + " got response code: " + response;
            }

            final InputStream is = method.getResponseBodyAsStream();
            file = File.createTempFile("agent-", "-" + Long.toString(new Date().getTime()));
            file.deleteOnExit();

            s_logger.debug("Retrieving new agent into " + file.getAbsolutePath());

            final FileOutputStream fos = new FileOutputStream(file);

            final ByteBuffer buffer = ByteBuffer.allocate(2048);
            final ReadableByteChannel in = Channels.newChannel(is);
            final WritableByteChannel out = fos.getChannel();

            while (in.read(buffer) != -1) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }

            in.close();
            out.close();

            s_logger.debug("New Agent zip file is now retrieved");
        } catch (final HttpException e) {
        	return "Unable to retrieve the file from " + url;
        } catch (final IOException e) {
        	return "Unable to retrieve the file from " + url;
        }
        
        file.delete();
        
        return "File will be deployed.";
    }
    
    @Override
    public String getAgentUrl() {
        return _upgradeUrl;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        final ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _upgradeDao = locator.getDao(AgentUpgradeDao.class);
        if (_upgradeDao == null) {
            throw new ConfigurationException("Unable to retrieve the storage layer.");
        }

        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }

        final Map<String, String> configs = configDao.getConfiguration("UpgradeManager", params);

        File agentUpgradeFile = PropertiesUtil.findConfigFile("agent-update.properties");
        Properties agentUpgradeProps = new Properties();
        try {
            if (agentUpgradeFile != null) {
                agentUpgradeProps.load(new FileInputStream(agentUpgradeFile));
            }

            _minimalVersion = agentUpgradeProps.getProperty("agent.minimal.version");
            _recommendedVersion = agentUpgradeProps.getProperty("agent.recommended.version");

            if (_minimalVersion == null) {
                _minimalVersion = "0.0.0.0";
            }

            if (_recommendedVersion == null) {
                _recommendedVersion = _minimalVersion;
            }

            _upgradeUrl = configs.get("upgrade.url");
            
			if (_upgradeUrl == null) {
				s_logger.debug("There is no upgrade url found in configuration table");
                // _upgradeUrl = "http://updates.vmops.com/releases/rss.xml";
            }

            return true;
        } catch (IOException ex) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Error reading agent-update.properties:  " + ex);
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
