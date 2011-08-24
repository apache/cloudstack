/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.test.longrun;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.cloud.test.stress.TestClientWithAPI;


import java.io.IOException;


public class VirtualMachine {
	public static final Logger s_logger= Logger.getLogger(VirtualMachine.class.getClass());
	
	private String privateIp;
	private String userId;
	
	
	public VirtualMachine(String userId){
		this.userId=userId;
	}
	
	
	public String getPrivateIp() {
		return privateIp;
	}


	public void setPrivateIp(String privateIp) {
		this.privateIp = privateIp;
	}


	public String getUserId() {
		return userId;
	}


	public void setUserId(String userId) {
		this.userId = userId;
	}


	public void deployVM(long zoneId, long serviceOfferingId, long templateId, String server, String apiKey, String secretKey) throws IOException{

		String encodedZoneId = URLEncoder.encode("" + zoneId, "UTF-8");
		String encodedServiceOfferingId = URLEncoder.encode(""
				+ serviceOfferingId, "UTF-8");
		String encodedTemplateId = URLEncoder.encode("" + templateId,
		"UTF-8");
		String encodedApiKey=URLEncoder.encode(apiKey, "UTF-8");
		String requestToSign = "apiKey=" + encodedApiKey
		+ "&command=deployVirtualMachine&serviceOfferingId="
		+ encodedServiceOfferingId + "&templateId="
		+ encodedTemplateId + "&zoneId=" + encodedZoneId;
		
		requestToSign = requestToSign.toLowerCase();
		String signature = TestClientWithAPI.signRequest(requestToSign, secretKey);
		String encodedSignature = URLEncoder.encode(signature, "UTF-8");
		String url = server + "?command=deployVirtualMachine"
		+ "&zoneId=" + encodedZoneId + "&serviceOfferingId="
		+ encodedServiceOfferingId + "&templateId="
		+ encodedTemplateId + "&apiKey=" + encodedApiKey
		+ "&signature=" + encodedSignature;
		
		s_logger.info("Sending this request to deploy a VM: "+url);
		HttpClient client = new HttpClient();
		HttpMethod method = new GetMethod(url);
		int responseCode = client.executeMethod(method);
		s_logger.info("deploy linux vm response code: " + responseCode);
		if (responseCode == 200) {
			InputStream is = method.getResponseBodyAsStream();
			Map<String, String> values = TestClientWithAPI.getSingleValueFromXML(is,
					new String[] { "id", "ipaddress" });
			long linuxVMId = Long.parseLong(values.get("id"));
			s_logger.info("got linux virtual machine id: " + linuxVMId);
			this.setPrivateIp(values.get("ipaddress"));

		} else if (responseCode == 500) {
			InputStream is = method.getResponseBodyAsStream();
			Map<String, String> errorInfo = TestClientWithAPI.getSingleValueFromXML(is,
					new String[] { "errorcode", "description" });
			s_logger.error("deploy linux vm test failed with errorCode: "
					+ errorInfo.get("errorCode") + " and description: "
					+ errorInfo.get("description"));
		} else {
			s_logger.error("internal error processing request: "
					+ method.getStatusText());
		}
	}
	
		

}
