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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.cloudstack.agent.directdownload.SetupDirectDownloadCertificateCommand;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.FileUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  SetupDirectDownloadCertificateCommand.class)
public class LibvirtSetupDirectDownloadCertificateCommandWrapper extends CommandWrapper<SetupDirectDownloadCertificateCommand, Answer, LibvirtComputingResource> {

    private static final String temporaryCertFilePrefix = "CSCERTIFICATE";


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
                logger.error("Could not get 'keystore.passphrase' property value due to: " + e.getMessage());
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

    /**
     * Import certificate from temporary file into keystore
     */
    private void importCertificate(String tempCerFilePath, String keyStoreFile, String certificateName, String privatePassword) {
        logger.debug("Importing certificate from temporary file to keystore");
        String keyToolPath = Script.getExecutableAbsolutePath("keytool");
        int result = Script.executeCommandForExitValue(keyToolPath, "-importcert", "file", tempCerFilePath,
                "-keystore", keyStoreFile, "-alias", sanitizeBashCommandArgument(certificateName), "-storepass",
                privatePassword, "-noprompt");
        if (result != 0) {
            logger.debug("Certificate " + certificateName + " not imported as it already exist on keystore");
        }
    }

    /**
     * Create temporary file and return its path
     */
    private String createTemporaryFile(File agentFile, String certificateName, String certificate) {
        String tempCerFilePath = String.format("%s/%s-%s",
                agentFile.getParent(), temporaryCertFilePrefix, certificateName);
        logger.debug("Creating temporary certificate file into: " + tempCerFilePath);
        if (!FileUtil.writeToFile(tempCerFilePath, certificate)) {
            throw new CloudRuntimeException("Could not create the certificate file on path: " + tempCerFilePath);
        }
        return tempCerFilePath;
    }

    /**
     * Remove temporary file
     */

    protected void cleanupTemporaryFile(String temporaryFile) {
        logger.debug("Cleaning up temporary certificate file");
        if (StringUtils.isBlank(temporaryFile)) {
            logger.debug("Provided temporary certificate file path is empty");
            return;
        }
        try {
            Path filePath = Paths.get(temporaryFile);
            if (!Files.exists(filePath)) {
                logger.debug("Temporary certificate file does not exist: " + temporaryFile);
                return;
            }
            Files.delete(filePath);
        } catch (IOException e) {
            logger.warn(String.format("Error while cleaning up temporary file: %s", temporaryFile));
            logger.debug(String.format("Error while cleaning up temporary file: %s", temporaryFile), e);
        }
    }

    @Override
    public Answer execute(SetupDirectDownloadCertificateCommand cmd, LibvirtComputingResource serverResource) {
        String certificate = cmd.getCertificate();
        String certificateName = cmd.getCertificateName();

        try {
            File agentFile = getAgentPropertiesFile();
            String privatePassword = getKeystorePassword(agentFile);
            if (StringUtils.isBlank(privatePassword)) {
                return new Answer(cmd, false, "No password found for keystore: " + KeyStoreUtils.KS_FILENAME);
            }

            final String keyStoreFile = getKeyStoreFilePath(agentFile);
            String temporaryFile = createTemporaryFile(agentFile, certificateName, certificate);
            importCertificate(temporaryFile, keyStoreFile, certificateName, privatePassword);
            cleanupTemporaryFile(temporaryFile);
        } catch (FileNotFoundException | CloudRuntimeException e) {
            logger.error("Error while setting up certificate " + certificateName, e);
            return new Answer(cmd, false, e.getMessage());
        }

        return new Answer(cmd, true, "Certificate " + certificateName + " imported");
    }
}
