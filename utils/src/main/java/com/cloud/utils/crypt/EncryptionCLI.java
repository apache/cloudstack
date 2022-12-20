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

package com.cloud.utils.crypt;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class EncryptionCLI {
    private static final String verboseOption = "verbose";
    private static final String decryptOption = "decrypt";
    private static final String inputOption = "input";
    private static final String passwordOption = "password";
    private static final String encryptorVersionOption = "encryptorversion";

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        Option verbose = Option.builder("v").longOpt(verboseOption).argName(verboseOption).required(false).desc("Verbose output").hasArg(false).build();
        Option decrypt = Option.builder("d").longOpt(decryptOption).argName(decryptOption).required(false).desc("Decrypt instead of encrypt").hasArg(false).build();
        Option input = Option.builder("i").longOpt(inputOption).argName(inputOption).required(true).hasArg().desc("The input string to process").build();
        Option password = Option.builder("p").longOpt(passwordOption).argName(passwordOption).required(true).hasArg().desc("The encryption password").build();
        Option encryptorVersion = Option.builder("e").longOpt(encryptorVersionOption).argName(encryptorVersionOption).required(false).hasArg().desc("The encryptor version").build();

        options.addOption(verbose);
        options.addOption(decrypt);
        options.addOption(input);
        options.addOption(password);
        options.addOption(encryptorVersion);

        CommandLine cmdLine = null;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter helper = new HelpFormatter();
        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(1);
        }

        CloudStackEncryptor encryptor = new CloudStackEncryptor(cmdLine.getOptionValue(passwordOption), cmdLine.getOptionValue(encryptorVersion), EncryptionCLI.class);

        String result;
        String inString = cmdLine.getOptionValue(inputOption);
        if (cmdLine.hasOption(decryptOption)) {
            result = encryptor.decrypt(inString);
        } else {
            result = encryptor.encrypt(inString);
        }

        if (cmdLine.hasOption(verboseOption)) {
            System.out.printf("Input: %s\n", inString);
            System.out.printf("Encrypted: %s\n", result);
        } else {
            System.out.printf("%s\n", result);
        }
    }
}
