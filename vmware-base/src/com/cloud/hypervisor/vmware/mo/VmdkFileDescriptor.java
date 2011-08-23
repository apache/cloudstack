/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

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
	
	private Properties _properties = new Properties();
	private String _baseFileName;
	
	public VmdkFileDescriptor() {
	}
	
	public void parse(byte[] vmdkFileContent) throws IOException {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(vmdkFileContent)));
			String line;
			while((line = in.readLine()) != null) {
				// ignore empty and comment lines
				line = line.trim();
				if(line.isEmpty())
					continue;
				if(line.charAt(0) == '#')
					continue;
				
				String[] tokens = line.split("=");
				if(tokens.length == 2) {
					String name = tokens[0].trim();
					String value = tokens[1].trim();
					if(value.charAt(0) == '\"')
						value = value.substring(1, value.length() -1); 
					
					_properties.put(name, value);
				} else {
					if(line.startsWith("RW")) {
						int startPos = line.indexOf('\"');
						int endPos = line.lastIndexOf('\"');
						assert(startPos > 0);
						assert(endPos > 0);
						
						_baseFileName = line.substring(startPos + 1, endPos);
					} else {
						s_logger.warn("Unrecognized vmdk line content: " + line);
					}
				}
			}
		} finally {
			if(in != null)
				in.close();
		}
	}
	
	public String getBaseFileName() {
		return _baseFileName;
	}
	
	public String getParentFileName() {
		return _properties.getProperty("parentFileNameHint");
	}
	
	public static byte[] changeVmdkContentBaseInfo(byte[] vmdkContent, 
		String baseFileName, String parentFileName) throws IOException {
		
		assert(vmdkContent != null);
		
		BufferedReader in = null;
		BufferedWriter out = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try {
			in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(vmdkContent)));
			out = new BufferedWriter(new OutputStreamWriter(bos)); 
			String line;
			while((line = in.readLine()) != null) {
				// ignore empty and comment lines
				line = line.trim();
				if(line.isEmpty()) {
					out.newLine();
					continue;
				}
				if(line.charAt(0) == '#') {
					out.write(line);
					out.newLine();
					continue;
				}
			
				String[] tokens = line.split("=");
				if(tokens.length == 2) {
					String name = tokens[0].trim();
					String value = tokens[1].trim();
					if(value.charAt(0) == '\"')
						value = value.substring(1, value.length() - 1);
					
					if(parentFileName != null && name.equals("parentFileNameHint")) {
						out.write(name + "=\"" + parentFileName + "\"");
						out.newLine();
					} else {
						out.write(line);
						out.newLine();
					}
				} else {
					if(line.startsWith("RW")) {
						if(baseFileName != null) {
							int startPos = line.indexOf('\"');
							int endPos = line.lastIndexOf('\"');
							assert(startPos > 0);
							assert(endPos > 0);
							
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
			if(in != null)
				in.close();
			if(out != null)
				out.close();
		}
		
		return bos.toByteArray();
	}
}
