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
package com.cloud.storage;

import java.io.File;
import java.io.IOException;

import com.cloud.utils.component.Manager;


/**
 * StorageLayer is an independence layer for
 * interfacing with the file system storage.
 * 
 * All implementations must guarantee the following things:
 *   1. Proper synchronization between threads.
 * 
 *
 */
public interface StorageLayer extends Manager {
    public final static String InstanceConfigKey = "storage.layer.instance";
    public final static String ClassConfigKey = "storage.layer.implementation";
    
    /**
     * Returns the size of the file.
     * @param path path to the file to get the size.
     * @return size of the file.
     */
    long getSize(String path);

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
}
