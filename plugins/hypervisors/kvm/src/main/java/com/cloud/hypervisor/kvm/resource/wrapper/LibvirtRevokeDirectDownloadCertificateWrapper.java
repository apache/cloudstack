//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.agent.directdownload.RevokeDirectDownloadCertificateCommand;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@ResourceWrapper(handles =  RevokeDirectDownloadCertificateCommand.class)
public class LibvirtRevokeDirectDownloadCertificateWrapper extends CommandWrapper<RevokeDirectDownloadCertificateCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtRevokeDirectDownloadCertificateWrapper.class);

    /**
     * Retrieve agent.properties file
     */
    private File getAgentPropertiesFile() throws FileNotFoundException {
        final File agentFile = PropertiesUtil.findConfigFile("agent.properties");
        if (agentFile == null) {
            throw new FileNotFoundException("Failed to find agent.properties file");
        }
        return agentFile;
    }

    /**
     * Get the property 'keystore.passphrase' value from agent.properties file
     */
    private String getKeystorePassword(File agentFile) {
        String pass = null;
        if (agentFile != null) {
            try {
                pass = PropertiesUtil.loadFromFile(agentFile).getProperty(KeyStoreUtils.KS_PASSPHRASE_PROPERTY);
            } catch (IOException e) {
                s_logger.error("Could not get 'keystore.passphrase' property value due to: " + e.getMessage());
            }
        }
        return pass;
    }

    /**
     * Get keystore path
     */
    private String getKeyStoreFilePath(File agentFile) {
        return agentFile.getParent() + "/" + KeyStoreUtils.KS_FILENAME;
    }

    @Override
    public Answer execute(RevokeDirectDownloadCertificateCommand command, LibvirtComputingResource serverResource) {
        String certificateAlias = command.getCertificateAlias();
        try {
            File agentFile = getAgentPropertiesFile();
            String privatePassword = getKeystorePassword(agentFile);
            if (StringUtils.isBlank(privatePassword)) {
                return new Answer(command, false, "No password found for keystore: " + KeyStoreUtils.KS_FILENAME);
            }

            final String keyStoreFile = getKeyStoreFilePath(agentFile);

            String checkCmd = String.format("keytool -list -alias %s -keystore %s -storepass %s",
                    certificateAlias, keyStoreFile, privatePassword);
            int existsCmdResult = Script.runSimpleBashScriptForExitValue(checkCmd);
            if (existsCmdResult == 1) {
                s_logger.error("Certificate alias " + certificateAlias + " does not exist, no need to revoke it");
            } else {
                String revokeCmd = String.format("keytool -delete -alias %s -keystore %s -storepass %s",
                        certificateAlias, keyStoreFile, privatePassword);
                s_logger.debug("Revoking certificate alias " + certificateAlias + " from keystore " + keyStoreFile);
                Script.runSimpleBashScriptForExitValue(revokeCmd);
            }
        } catch (FileNotFoundException | CloudRuntimeException e) {
            s_logger.error("Error while setting up certificate " + certificateAlias, e);
            return new Answer(command, false, e.getMessage());
        }
        return new Answer(command);
    }
}
