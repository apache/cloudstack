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

package com.cloud.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

@Local(value = StorageLayer.class)
public class JavaStorageLayer implements StorageLayer {

    String _name;
    boolean _makeWorldWriteable = true;

    public JavaStorageLayer() {
        super();
    }

    public JavaStorageLayer(boolean makeWorldWriteable) {
        this();
        _makeWorldWriteable = makeWorldWriteable;
    }

    @Override
    public boolean cleanup(String path, String rootPath) throws IOException {
        assert path.startsWith(rootPath) : path + " does not start with " + rootPath;

        synchronized (path) {
            File file = new File(path);
            if (!file.delete()) {
                return false;
            }
            int index = -1;
            long rootLength = rootPath.length();
            while ((index = path.lastIndexOf(File.separator)) != -1 && path.length() > rootLength) {
                file = new File(path.substring(0, index));
                String[] children = file.list();
                if (children != null && children.length > 0) {
                    break;
                }
                if (!file.delete()) {
                    throw new IOException("Unable to delete " + file.getAbsolutePath());
                }
            }
            return true;
        }
    }

    @Override
    public boolean create(String path, String filename) throws IOException {
        synchronized (path.intern()) {
            String newFile = path + File.separator + filename;
            File file = new File(newFile);
            if (file.exists()) {
                return true;
            }

            return file.createNewFile();
        }
    }

    @Override
    public boolean delete(String path) {
        synchronized (path.intern()) {
            File file = new File(path);
            return file.delete();
        }
    }

    @Override
    public boolean deleteDir(String dir) {
        File Dir = new File(dir);
        if (!Dir.isDirectory()) {
            return false;
        }

        synchronized (dir.intern()) {
            File[] files = Dir.listFiles();
            for (File file : files) {
                if (!file.delete()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean exists(String path) {
        synchronized (path.intern()) {
            File file = new File(path);
            return file.exists();
        }
    }

    @Override
    public long getTotalSpace(String path) {
        File file = new File(path);
        return file.getTotalSpace();
    }

    @Override
    public long getUsableSpace(String path) {
        File file = new File(path);
        return file.getUsableSpace();
    }

    @Override
    public String[] listFiles(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return new String[0];
        }
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            paths[i] = files[i].getAbsolutePath();
        }
        return paths;
    }

    @Override
    public List<String> listMountPointsByMsHost(String path, long msHostId) {
        List<String> mountPaths = new ArrayList<String>();
        File[] files = new File(path).listFiles();
        if (files == null) {
            return mountPaths;
        }
        for (File file : files) {
            if (file.getName().startsWith(String.valueOf(msHostId) + "."))
                mountPaths.add(file.getAbsolutePath());
        }
        return mountPaths;
    }

    @Override
    public boolean mkdir(String path) {
        synchronized (path.intern()) {
            File file = new File(path);

            if (file.exists()) {
                return file.isDirectory();
            }
            if (_makeWorldWriteable) {
                return (file.mkdirs() && setWorldReadableAndWriteable(file));
            } else {
                return file.mkdirs();
            }
        }
    }

    @Override
    public long getSize(String path) {
        File file = new File(path);
        return file.length();
    }

    @Override
    public File createUniqDir() {
        String dirName = System.getProperty("java.io.tmpdir");
        if (dirName != null) {
            File dir = new File(dirName);
            if (dir.exists()) {
                String uniqDirName = dir.getAbsolutePath() + File.separator + UUID.randomUUID().toString();
                if (mkdir(uniqDirName)) {
                    return new File(uniqDirName);
                }
            }
        }
        return null;
    }

    @Override
    public boolean mkdirs(String path) {
        synchronized (path.intern()) {
            File dir = new File(path);

            if (dir.exists()) {
                return dir.isDirectory();
            }

            boolean success = true;
            List<String> dirPaths = listDirPaths(path);
            for (String dirPath : dirPaths) {
                dir = new File(dirPath);
                if (!dir.exists()) {
                    success = dir.mkdir();
                    if (_makeWorldWriteable)
                        success = success && setWorldReadableAndWriteable(dir);
                }
            }

            return success;
        }
    }

    private List<String> listDirPaths(String path) {
        String[] dirNames = path.split("/");
        List<String> dirPaths = new ArrayList<String>();

        String currentPath = "";
        for (int i = 0; i < dirNames.length; i++) {
            String currentName = dirNames[i].trim();
            if (!currentName.isEmpty()) {
                currentPath += "/" + currentName;
                dirPaths.add(currentPath);
            }
        }

        return dirPaths;
    }

    @Override
    public boolean setWorldReadableAndWriteable(File file) {
        return (file.setReadable(true, false) && file.setWritable(true, false));
    }

    @Override
    public boolean isDirectory(String path) {
        File file = new File(path);
        return file.isDirectory();
    }

    @Override
    public boolean isFile(String path) {
        File file = new File(path);
        return file.isFile();
    }

    @Override
    public File getFile(String path) {
        return new File(path);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
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

    @Override
    public long getUsedSpace(String path) {
        File file = new File(path);
        return file.getTotalSpace() - file.getFreeSpace();
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
