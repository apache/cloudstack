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

package com.cloud.utils;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {
	
    public static String readFileAsString(String filePath) {
        File file = new File(filePath);
        if(!file.exists())
            return null;
        
        try {
            byte[] buffer = new byte[(int)file.length()];
            BufferedInputStream f = null;
            try {
                f = new BufferedInputStream(new FileInputStream(filePath));
                f.read(buffer);
            } finally {
                if (f != null) { 
                    try { 
                        f.close(); 
                    } catch (IOException ignored) {
                    }
                }
            }
            return new String(buffer);
        } catch(IOException e) {
            return null;
        }
    }
    
    public static void writeToFile(String content, String filePath) throws IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filePath));
            out.write(content);
        } finally {
            if(out != null)
                out.close();
        }
    }
    
    public static void copyfile(File f1, File f2) throws IOException {
        InputStream in = new FileInputStream(f1);
        OutputStream out = new FileOutputStream(f2);

        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            in.close();
            out.close();
        }
    }
}

