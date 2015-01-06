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
import java.util.List;

import com.cloud.utils.component.Manager;

/**
 * StorageLayer is an independence layer for
 *
 *   1. Proper synchronization between threads.
 *
 *
 */
public interface StorageLayer extends Manager {
    public final static String InstanceConfigKey = "storage.layer.instance";
    public final static String ClassConfigKey = "storage.layer.implementation";

    /**
     * @param path path to the file to get the size.
     * @return size of the file.
     */
    long getSize(String path);

    File createUniqDir();

    /**
     * Is this path a directory?
     * @param path path to check.
     * @return true if it is a directory; false otherwise.
     */
    boolean isDirectory(String path);

    /**
     * Is this path a file?
     * @param path path to check.
     * @return true if it is a file; false otherwise.
     */
    boolean isFile(String path);

    /**
     * creates the directory.  All parent directories have to already exists.
     * @param path path to make.
     * @return true if created; false if not.
     */
    boolean mkdir(String path);

    /**
     * Creates the entire path.
     * @param path path to create.
     * @return true if created; false if not.
     */
    boolean mkdirs(String path);

    /**
     * Does this path exists?
     * @param path directory or file to check if it exists.
     * @return true if exists; false if not.
     */
    boolean exists(String path);

    /**
     * list all the files in a certain path.
     * @param path directory that the file exists in.
     * @return list of files that exists under this path.
     */
    String[] listFiles(String path);

    /**
     * Get the total disk size in bytes.
     * @param path path
     * @return disk size if path given is a disk; -1 if not.
     */
    long getTotalSpace(String path);

    /**
     * Get the total available disk size in bytes.
     * @param path path to the disk.
     * @return disk size if path given is a disk; -1 if not.
     */
    long getUsedSpace(String path);

    /**
     * Get the total available disk size in bytes.
     * @param path path to the disk.
     * @return disk size if path given is a disk; -1 if not.
     */
    long getUsableSpace(String path);

    /**
     * delete the path
     * @param path to delete.
     * @return true if deleted; false if not.
     */
    boolean delete(String path);

    /**
     * creates a file on this path.
     * @param path directory to create the file in.
     * @param filename file to create.
     * @return true if created; false if not.
     * @throws IOException if create has problems.
     */
    boolean create(String path, String filename) throws IOException;

    /**
     * clean up the path.  This method will delete the parent paths if the parent
     * paths do not contain children.  If the original path cannot be deleted,
     * this method returns false.  If the parent cannot be deleted but does not
     * have any children, this method throws IOException.
     * @param path path to be cleaned up.
     * @param rootPath delete up to this path.
     * @return true if the path is deleted and false if it is not.
     * @throws IOException if the parent has no children but delete failed.
     */
    boolean cleanup(String path, String rootPath) throws IOException;

    /**
     * Retrieves the actual file object.
     * @param path path to the file.
     * @return File object representing the file.
     */
    File getFile(String path);

    /**
     * Sets permissions for a file to be world readable and writeable
     * @param file
     * @return true if the file was set to be both world readable and writeable
     */
    boolean setWorldReadableAndWriteable(File file);

    boolean deleteDir(String dir);

    List<String> listMountPointsByMsHost(String path, long msHostId);
}
