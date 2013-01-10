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
package org.apache.cloudstack.acl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;

/*
 * This is the default API access checker that grab's the user's account
 * based on the account type, access is granted referring to commands in all *.properties files.
 */

@Local(value=APIAccessChecker.class)
public class StaticRoleBasedAPIAccessChecker extends AdapterBase implements APIAccessChecker {

    protected static final Logger s_logger = Logger.getLogger(StaticRoleBasedAPIAccessChecker.class);
    public static final short ADMIN_COMMAND = 1;
    public static final short DOMAIN_ADMIN_COMMAND = 4;
    public static final short RESOURCE_DOMAIN_ADMIN_COMMAND = 2;
    public static final short USER_COMMAND = 8;
    private static List<String> s_userCommands = null;
    private static List<String> s_resellerCommands = null; // AKA domain-admin
    private static List<String> s_adminCommands = null;
    private static List<String> s_resourceDomainAdminCommands = null;
    private static List<String> s_allCommands = null;

    @Inject AccountManager _accountMgr;
    @Inject List<PluggableService> _services;

    protected StaticRoleBasedAPIAccessChecker() {
        super();
        s_allCommands = new ArrayList<String>();
        s_userCommands = new ArrayList<String>();
        s_resellerCommands = new ArrayList<String>();
        s_adminCommands = new ArrayList<String>();
        s_resourceDomainAdminCommands = new ArrayList<String>();
    }

    @Override
    public boolean canAccessAPI(User user, String apiCommandName)
            throws PermissionDeniedException{

        boolean commandExists = s_allCommands.contains(apiCommandName);

        if(commandExists && user != null){
            Long accountId = user.getAccountId();
            Account userAccount = _accountMgr.getAccount(accountId);
            short accountType = userAccount.getType();
            return isCommandAvailableForAccount(accountType, apiCommandName);
        }

        return commandExists;
    }

    private static boolean isCommandAvailableForAccount(short accountType, String commandName) {
        boolean isCommandAvailable = false;
        switch (accountType) {
        case Account.ACCOUNT_TYPE_ADMIN:
            isCommandAvailable = s_adminCommands.contains(commandName);
            break;
        case Account.ACCOUNT_TYPE_DOMAIN_ADMIN:
            isCommandAvailable = s_resellerCommands.contains(commandName);
            break;
        case Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN:
            isCommandAvailable = s_resourceDomainAdminCommands.contains(commandName);
            break;
        case Account.ACCOUNT_TYPE_NORMAL:
            isCommandAvailable = s_userCommands.contains(commandName);
            break;
        }
        return isCommandAvailable;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        List<String> configFiles = new ArrayList<String>();
        for (PluggableService service : _services) {
            configFiles.addAll(Arrays.asList(service.getPropertiesFiles()));
        }

        processConfigFiles(configFiles);
        return true;
    }

    private void processConfigFiles(List<String> configFiles) {
        Properties preProcessedCommands = new Properties();

        for (String configFile : configFiles) {
            File commandsFile = PropertiesUtil.findConfigFile(configFile);
            if (commandsFile != null) {
                try {
                    preProcessedCommands.load(new FileInputStream(commandsFile));
                } catch (FileNotFoundException fnfex) {
                    // in case of a file within a jar in classpath, try to open stream using url
                    InputStream stream = PropertiesUtil.openStreamFromURL(configFile);
                    if (stream != null) {
                        try {
                            preProcessedCommands.load(stream);
                        } catch (IOException e) {
                            s_logger.error("IO Exception, unable to find properties file:", fnfex);
                        }
                    } else {
                        s_logger.error("Unable to find properites file", fnfex);
                    }
                } catch (IOException ioe) {
                    s_logger.error("IO Exception loading properties file", ioe);
                }
            }
        }

        for (Object key : preProcessedCommands.keySet()) {
            String preProcessedCommand = preProcessedCommands.getProperty((String) key);
            int splitIndex = preProcessedCommand.lastIndexOf(";");
            // Backward compatible to old style, apiname=pkg;mask
            String mask = preProcessedCommand.substring(splitIndex+1);

            try {
                short cmdPermissions = Short.parseShort(mask);
                if ((cmdPermissions & ADMIN_COMMAND) != 0) {
                    s_adminCommands.add((String) key);
                }
                if ((cmdPermissions & RESOURCE_DOMAIN_ADMIN_COMMAND) != 0) {
                    s_resourceDomainAdminCommands.add((String) key);
                }
                if ((cmdPermissions & DOMAIN_ADMIN_COMMAND) != 0) {
                    s_resellerCommands.add((String) key);
                }
                if ((cmdPermissions & USER_COMMAND) != 0) {
                    s_userCommands.add((String) key);
                }
                s_allCommands.addAll(s_adminCommands);
                s_allCommands.addAll(s_resourceDomainAdminCommands);
                s_allCommands.addAll(s_userCommands);
                s_allCommands.addAll(s_resellerCommands);
            } catch (NumberFormatException nfe) {
                s_logger.info("Malformed command.properties permissions value, key = " + key + ", value = " + preProcessedCommand);
            }
        }
    }

}
