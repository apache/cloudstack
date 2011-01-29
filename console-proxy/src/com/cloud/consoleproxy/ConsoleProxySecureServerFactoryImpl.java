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

package com.cloud.consoleproxy;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class ConsoleProxySecureServerFactoryImpl implements ConsoleProxyServerFactory  {
	private static final Logger s_logger = Logger.getLogger(ConsoleProxySecureServerFactoryImpl.class);
	
	private SSLContext sslContext = null;
	
	public ConsoleProxySecureServerFactoryImpl() {
		try {
		    s_logger.info("Start initializing SSL");
			
			char[] passphrase = "vmops.com".toCharArray();
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(ConsoleProxy.class.getResourceAsStream("/realhostip.keystore"), passphrase);
			//custom cert logic begins //
			try {
				//check if there is any custom cert added at /etc/cloud/consoleproxy/cert/
				String certPath = "/etc/cloud/consoleproxy/cert/customcert";				
				//now generate a cert
				FileInputStream fis = new FileInputStream(certPath);
				BufferedInputStream bis = new BufferedInputStream(fis);
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				
				while (bis.available() > 1) {
				   Certificate cert = cf.generateCertificate(bis);
				   if(s_logger.isDebugEnabled()){
					   s_logger.debug("The custom certificate generated is:"+cert.toString());
   				   }				
				//get the existing cert chain
				Certificate[] chain = ks.getCertificateChain("realhostip");
				Certificate[] newChain = new Certificate[chain.length+1];
				newChain[0] = cert;//make custom cert the default 
				System.arraycopy(chain, 0, newChain, 1, chain.length);
				Key key = ks.getKey("realhostip", passphrase);
				ks.setKeyEntry("realhostip", key, passphrase, newChain);
				if(s_logger.isDebugEnabled())
					s_logger.debug("Custom SSL cert added successfully to the keystore cert chain");
				}
			} catch (FileNotFoundException fnf) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to find the custom cert file at /etc/cloud/consoleproxy/cert/customcert",fnf);
			} catch (IOException ioe){
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to read the custom cert file at /etc/cloud/consoleproxy/cert/customcert",ioe);
			}catch (KeyStoreException kse){
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to add custom cert file at /etc/cloud/consoleproxy/cert/customcert to the keystore",kse);
			}catch (CertificateException ce){
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to generate certificate from the file /etc/cloud/consoleproxy/cert/customcert",ce);				
			}catch (Exception e){
				//catch other excpns
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to add custom cert file at /etc/cloud/consoleproxy/cert/customcert to the keystore",e);
			}
			//custom cert logic ends //	
			
		    s_logger.info("SSL certificate loaded");
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, passphrase);
		    s_logger.info("Key manager factory is initialized");

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);
		    s_logger.info("Trust manager factory is initialized");

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		    s_logger.info("SSL context is initialized");
		} catch (Exception ioe) {
			s_logger.error(ioe.toString(), ioe);
		}
	}
	
	public HttpServer createHttpServerInstance(int port) throws IOException {
		try {
			HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 5);
		    server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
		        @Override
                public void configure (HttpsParameters params) {

		        // get the remote address if needed
		        InetSocketAddress remote = params.getClientAddress();
		        SSLContext c = getSSLContext();

		        // get the default parameters
		        SSLParameters sslparams = c.getDefaultSSLParameters();

		        params.setSSLParameters(sslparams);
		        // statement above could throw IAE if any params invalid.
		        // eg. if app has a UI and parameters supplied by a user.
		        }
		    });
		    
		    s_logger.info("create HTTPS server instance on port: " + port);
		    return server;
		} catch (Exception ioe) {
			s_logger.error(ioe.toString(), ioe);
		}
		return null;
	}
	
	public SSLServerSocket createSSLServerSocket(int port) throws IOException {
		try {
			SSLServerSocket srvSock = null;
	        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
	        srvSock = (SSLServerSocket) ssf.createServerSocket(port);
	        
		    s_logger.info("create SSL server socket on port: " + port);
	        return srvSock;
		} catch (Exception ioe) {
			s_logger.error(ioe.toString(), ioe);
		}
		return null;
	}
}
