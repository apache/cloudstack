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
package com.cloud.hypervisor.vmware.mo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

import org.apache.log4j.Logger;

public class VmdkFileDescriptor {
    private static final Logger s_logger = Logger.getLogger(VmdkFileDescriptor.class);
    private static final String VMDK_PROPERTY_CREATE_TYPE = "createType";
    private static final String VMDK_CREATE_TYPE_VMFSSPARSE = "vmfsSparse";
    private static final String VMDK_PROPERTY_ADAPTER_TYPE = "ddb.adapterType";

    private Properties _properties = new Properties();
    private String _baseFileName;

    public VmdkFileDescriptor() {
    }

    public void parse(byte[] vmdkFileContent) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(vmdkFileContent),"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                // ignore empty and comment lines
                line = line.trim();
                if (line.isEmpty())
                    continue;
                if (line.charAt(0) == '#')
                    continue;

                String[] tokens = line.split("=");
                if (tokens.length == 2) {
                    String name = tokens[0].trim();
                    String value = tokens[1].trim();
                    if (value.charAt(0) == '\"')
                        value = value.substring(1, value.length() - 1);

                    _properties.put(name, value);
                } else {
                    if (line.startsWith("RW")) {
                        int startPos = line.indexOf('\"');
                        int endPos = line.lastIndexOf('\"');
                        assert (startPos > 0);
                        assert (endPos > 0);

                        _baseFileName = line.substring(startPos + 1, endPos);
                    } else {
                        s_logger.warn("Unrecognized vmdk line content: " + line);
                    }
                }
            }
        } finally {
            if (in != null)
                in.close();
        }
    }

    public String getBaseFileName() {
        return _baseFileName;
    }

    public String getParentFileName() {
        return _properties.getProperty("parentFileNameHint");
    }

    public boolean isVmfsSparseFile() {
        String vmdkCreateType = _properties.getProperty(VMDK_PROPERTY_CREATE_TYPE);
        if (vmdkCreateType.equalsIgnoreCase(VMDK_CREATE_TYPE_VMFSSPARSE)) {
            return true;
        }
        return false;
    }

    public String getAdapterType() {
        return _properties.getProperty(VMDK_PROPERTY_ADAPTER_TYPE);
    }


    public static byte[] changeVmdkAdapterType(byte[] vmdkContent, String newAdapterType) throws IOException {
        assert (vmdkContent != null);

        BufferedReader in = null;
        BufferedWriter out = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(vmdkContent)));
            out = new BufferedWriter(new OutputStreamWriter(bos));
            String line;
            while ((line = in.readLine()) != null) {
                // ignore empty and comment lines
                line = line.trim();
                if (line.isEmpty()) {
                    out.newLine();
                    continue;
                }
                if (line.charAt(0) == '#') {
                    out.write(line);
                    out.newLine();
                    continue;
                }

                String[] tokens = line.split("=");
                if (tokens.length == 2) {
                    String name = tokens[0].trim();
                    String value = tokens[1].trim();
                    if (value.charAt(0) == '\"')
                        value = value.substring(1, value.length() - 1);

                    if (newAdapterType != null && name.equals(VMDK_PROPERTY_ADAPTER_TYPE)) {
                        out.write(name + "=\"" + newAdapterType + "\"");
                        out.newLine();
                    } else {
                        out.write(line);
                        out.newLine();
                    }
                } else {
                    out.write(line);
                    out.newLine();
                }
            }
        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }

        return bos.toByteArray();

    }

    public static byte[] changeVmdkContentBaseInfo(byte[] vmdkContent, String baseFileName, String parentFileName) throws IOException {

        assert (vmdkContent != null);

        BufferedReader in = null;
        BufferedWriter out = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(vmdkContent),"UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(bos,"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                // ignore empty and comment lines
                line = line.trim();
                if (line.isEmpty()) {
                    out.newLine();
                    continue;
                }
                if (line.charAt(0) == '#') {
                    out.write(line);
                    out.newLine();
                    continue;
                }

                String[] tokens = line.split("=");
                if (tokens.length == 2) {
                    String name = tokens[0].trim();
                    String value = tokens[1].trim();
                    if (value.charAt(0) == '\"')
                        value = value.substring(1, value.length() - 1);

                    if (parentFileName != null && name.equals("parentFileNameHint")) {
                        out.write(name + "=\"" + parentFileName + "\"");
                        out.newLine();
                    } else {
                        out.write(line);
                        out.newLine();
                    }
                } else {
                    if (line.startsWith("RW")) {
                        if (baseFileName != null) {
                            int startPos = line.indexOf('\"');
                            int endPos = line.lastIndexOf('\"');
                            assert (startPos > 0);
                            assert (endPos > 0);

                            // replace it with base file name
                            out.write(line.substring(0, startPos + 1));
                            out.write(baseFileName);
                            out.write(line.substring(endPos));
                            out.newLine();
                        } else {
                            out.write(line);
                            out.newLine();
                        }
                    } else {
                        s_logger.warn("Unrecognized vmdk line content: " + line);
                    }
                }
            }
        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }

        return bos.toByteArray();
    }
}
