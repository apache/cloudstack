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

