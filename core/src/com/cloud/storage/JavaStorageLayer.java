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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;


@Local(value=StorageLayer.class)
public class JavaStorageLayer implements StorageLayer {
    
    String _name;
    
    @Override
    public boolean cleanup(String path, String rootPath) throws IOException {
        assert path.startsWith(rootPath) : path + " does not start with " + rootPath;
        
        synchronized(path) {
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
        synchronized(path.intern()) {
            File file = new File(path);
            return file.delete();
        }
    }

    @Override
    public boolean exists(String path) {
        synchronized(path.intern()) {
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
    public boolean mkdir(String path) {
        synchronized(path.intern()) {
            File file = new File(path);
            
            if (file.exists()) {
                return file.isDirectory();
            }
            
            return (file.mkdirs() && setWorldReadableAndWriteable(file));
        }
    }
    
    @Override
    public long getSize(String path) {
        File file = new File(path);
        return file.length();
    }

    @Override
    public boolean mkdirs(String path) {
        synchronized(path.intern()) {
            File dir = new File(path);
            
            if (dir.exists()) {
                return dir.isDirectory();
            }
            
            boolean success = true;
            List<String> dirPaths = listDirPaths(path);
            for (String dirPath : dirPaths) {
            	dir = new File(dirPath);
            	if (!dir.exists()) {
            		success = dir.mkdir() && setWorldReadableAndWriteable(dir);
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
    
    

}
