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
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.cloud.utils.StringUtils;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  ModifySshKeysCommand.class)
public final class LibvirtModifySshKeysCommandWrapper extends CommandWrapper<ModifySshKeysCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtModifySshKeysCommandWrapper.class);

    @Override
    public Answer execute(final ModifySshKeysCommand command, final LibvirtComputingResource libvirtComputingResource) {

        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

        final String sshkeyspath = libvirtUtilitiesHelper.retrieveSshKeysPath();
        final String sshpubkeypath = libvirtUtilitiesHelper.retrieveSshPubKeyPath();
        final String sshprvkeypath = libvirtUtilitiesHelper.retrieveSshPrvKeyPath();

        final File sshKeysDir = new File(sshkeyspath);
        String result = null;
        if (!sshKeysDir.exists()) {
            // Change permissions for the 700
            final Script script = new Script("mkdir", libvirtComputingResource.getTimeout(), s_logger);
            script.add("-m", "700");
            script.add(sshkeyspath);
            script.execute();

            if (!sshKeysDir.exists()) {
                s_logger.debug("failed to create directory " + sshkeyspath);
            }
        }

        final File pubKeyFile = new File(sshpubkeypath);
        if (!pubKeyFile.exists()) {
            try {
                pubKeyFile.createNewFile();
            } catch (final IOException e) {
                result = "Failed to create file: " + e.toString();
                s_logger.debug(result);
            }
        }

        if (pubKeyFile.exists()) {
            try (FileOutputStream pubkStream = new FileOutputStream(pubKeyFile)) {
                pubkStream.write(command.getPubKey().getBytes(StringUtils.getPreferredCharset()));
            } catch (final FileNotFoundException e) {
                result = "File" + sshpubkeypath + "is not found:"
                        + e.toString();
                s_logger.debug(result);
            } catch (final IOException e) {
                result = "Write file " + sshpubkeypath + ":" + e.toString();
                s_logger.debug(result);
            }
        }

        final File prvKeyFile = new File(sshprvkeypath);
        if (!prvKeyFile.exists()) {
            try {
                prvKeyFile.createNewFile();
            } catch (final IOException e) {
                result = "Failed to create file: " + e.toString();
                s_logger.debug(result);
            }
        }

        if (prvKeyFile.exists()) {
            final String prvKey = command.getPrvKey();
            try (FileOutputStream prvKStream = new FileOutputStream(prvKeyFile);){
                if ( prvKStream != null) {
                    prvKStream.write(prvKey.getBytes(StringUtils.getPreferredCharset()));
                }
            } catch (final FileNotFoundException e) {
                result = "File" + sshprvkeypath + "is not found:" + e.toString();
                s_logger.debug(result);
            } catch (final IOException e) {
                result = "Write file " + sshprvkeypath + ":" + e.toString();
                s_logger.debug(result);
            }
            final Script script = new Script("chmod", libvirtComputingResource.getTimeout(), s_logger);
            script.add("600", sshprvkeypath);
            script.execute();
        }

        if (result != null) {
            return new Answer(command, false, result);
        } else {
            return new Answer(command, true, null);
        }
    }
}
