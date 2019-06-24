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
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;

public class SnapshotDescriptor {
    private static final Logger s_logger = Logger.getLogger(SnapshotDescriptor.class);

    private final Properties _properties = new Properties();

    public SnapshotDescriptor() {
    }

    public void parse(byte[] vmsdFileContent) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(vmsdFileContent),"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                // TODO, remember to remove this log, temporarily added for debugging purpose
                s_logger.info("Parse snapshot file content: " + line);

                String[] tokens = line.split("=");
                if (tokens.length == 2) {
                    String name = tokens[0].trim();
                    String value = tokens[1].trim();
                    if (value.charAt(0) == '\"')
                        value = value.substring(1, value.length() - 1);

                    _properties.put(name, value);
                }
            }
        } finally {
            if (in != null)
                in.close();
        }
    }

    public void removeDiskReferenceFromSnapshot(String diskFileName) {
        String numSnapshotsStr = _properties.getProperty("snapshot.numSnapshots");
        if (numSnapshotsStr != null) {
            int numSnaphosts = Integer.parseInt(numSnapshotsStr);
            for (int i = 0; i < numSnaphosts; i++) {
                String numDisksStr = _properties.getProperty(String.format("snapshot%d.numDisks", i));
                int numDisks = Integer.parseInt(numDisksStr);

                boolean diskFound = false;
                for (int j = 0; j < numDisks; j++) {
                    String keyName = String.format("snapshot%d.disk%d.fileName", i, j);
                    String fileName = _properties.getProperty(keyName);
                    if (!diskFound) {
                        if (fileName.equalsIgnoreCase(diskFileName)) {
                            diskFound = true;
                            _properties.remove(keyName);
                        }
                    } else {
                        _properties.setProperty(String.format("snapshot%d.disk%d.fileName", i, j - 1), fileName);
                    }
                }

                if (diskFound)
                    _properties.setProperty(String.format("snapshot%d.numDisks", i), String.valueOf(numDisks - 1));
            }
        }
    }

    public byte[] getVmsdContent() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(bos, "UTF-8"));) {

            out.write(".encoding = \"UTF-8\"");
            out.newLine();
            out.write(String.format("snapshot.lastUID = \"%s\"", _properties.getProperty("snapshot.lastUID")));
            out.newLine();
            String numSnapshotsStr = _properties.getProperty("snapshot.numSnapshots");
            if (numSnapshotsStr == null || numSnapshotsStr.isEmpty())
                numSnapshotsStr = "0";
            out.write(String.format("snapshot.numSnapshots = \"%s\"", numSnapshotsStr));
            out.newLine();

            String value = _properties.getProperty("snapshot.current");
            if (value != null) {
                out.write(String.format("snapshot.current = \"%s\"", value));
                out.newLine();
            }

            String key;
            for (int i = 0; i < Integer.parseInt(numSnapshotsStr); i++) {
                key = String.format("snapshot%d.uid", i);
                value = _properties.getProperty(key);
                out.write(String.format("%s = \"%s\"", key, value));
                out.newLine();

                key = String.format("snapshot%d.filename", i);
                value = _properties.getProperty(key);
                out.write(String.format("%s = \"%s\"", key, value));
                out.newLine();

                key = String.format("snapshot%d.displayName", i);
                value = _properties.getProperty(key);
                out.write(String.format("%s = \"%s\"", key, value));
                out.newLine();

                key = String.format("snapshot%d.description", i);
                value = _properties.getProperty(key);
                out.write(String.format("%s = \"%s\"", key, value));
                out.newLine();

                key = String.format("snapshot%d.createTimeHigh", i);
                value = _properties.getProperty(key);
                out.write(String.format("%s = \"%s\"", key, value));
                out.newLine();

                key = String.format("snapshot%d.createTimeLow", i);
                value = _properties.getProperty(key);
                out.write(String.format("%s = \"%s\"", key, value));
                out.newLine();

                key = String.format("snapshot%d.numDisks", i);
                value = _properties.getProperty(key);
                out.write(String.format("%s = \"%s\"", key, value));
                out.newLine();

                int numDisks = Integer.parseInt(value);
                for (int j = 0; j < numDisks; j++) {
                    key = String.format("snapshot%d.disk%d.fileName", i, j);
                    value = _properties.getProperty(key);
                    out.write(String.format("%s = \"%s\"", key, value));
                    out.newLine();

                    key = String.format("snapshot%d.disk%d.node", i, j);
                    value = _properties.getProperty(key);
                    out.write(String.format("%s = \"%s\"", key, value));
                    out.newLine();
                }
            }
        } catch (IOException e) {
            assert (false);
            s_logger.error("Unexpected exception ", e);
        }

        return bos.toByteArray();
    }

    private int getSnapshotId(String seqStr) {
        if (seqStr != null) {
            int seq = Integer.parseInt(seqStr);
            String numSnapshotStr = _properties.getProperty("snapshot.numSnapshots");
            assert (numSnapshotStr != null);
            for (int i = 0; i < Integer.parseInt(numSnapshotStr); i++) {
                String value = _properties.getProperty(String.format("snapshot%d.uid", i));
                if (value != null && Integer.parseInt(value) == seq)
                    return i;
            }
        }

        return 0;
    }

    public SnapshotInfo[] getCurrentDiskChain() {
        ArrayList<SnapshotInfo> l = new ArrayList<SnapshotInfo>();
        String current = _properties.getProperty("snapshot.current");
        int id;
        while (current != null) {
            id = getSnapshotId(current);
            String numDisksStr = _properties.getProperty(String.format("snapshot%d.numDisks", id));
            int numDisks = 0;
            if (numDisksStr != null && !numDisksStr.isEmpty()) {
                numDisks = Integer.parseInt(numDisksStr);
                DiskInfo[] disks = new DiskInfo[numDisks];
                for (int i = 0; i < numDisks; i++) {
                    disks[i] =
                        new DiskInfo(_properties.getProperty(String.format("snapshot%d.disk%d.fileName", id, i)), _properties.getProperty(String.format(
                            "snapshot%d.disk%d.node", id, i)));
                }

                SnapshotInfo info = new SnapshotInfo();
                info.setId(id);
                info.setNumOfDisks(numDisks);
                info.setDisks(disks);
                info.setDisplayName(_properties.getProperty(String.format("snapshot%d.displayName", id)));
                l.add(info);
            }

            current = _properties.getProperty(String.format("snapshot%d.parent", id));
        }

        return l.toArray(new SnapshotInfo[0]);
    }

    public static class SnapshotInfo {
        private int _id;
        private String _displayName;
        private int _numOfDisks;
        private DiskInfo[] _disks;

        public SnapshotInfo() {
        }

        public void setId(int id) {
            _id = id;
        }

        public int getId() {
            return _id;
        }

        public void setDisplayName(String name) {
            _displayName = name;
        }

        public String getDisplayName() {
            return _displayName;
        }

        public void setNumOfDisks(int numOfDisks) {
            _numOfDisks = numOfDisks;
        }

        public int getNumOfDisks() {
            return _numOfDisks;
        }

        public void setDisks(DiskInfo[] disks) {
            _disks = disks;
        }

        public DiskInfo[] getDisks() {
            return _disks;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("SnapshotInfo : { id: ");
            sb.append(_id);
            sb.append(", displayName: ").append(_displayName);
            sb.append(", numOfDisks: ").append(_numOfDisks);
            sb.append(", disks: [");
            if (_disks != null) {
                int i = 0;
                for (DiskInfo diskInfo : _disks) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(diskInfo.toString());
                    i++;
                }
            }
            sb.append("]}");

            return sb.toString();
        }
    }

    public static class DiskInfo {
        private final String _diskFileName;
        private final String _deviceName;

        public DiskInfo(String diskFileName, String deviceName) {
            _diskFileName = diskFileName;
            _deviceName = deviceName;
        }

        public String getDiskFileName() {
            return _diskFileName;
        }

        public String getDeviceName() {
            return _deviceName;
        }

        @Override
        public String toString() {
            return "DiskInfo: { device: " + _deviceName + ", file: " + _diskFileName + " }";
        }
    }
}
