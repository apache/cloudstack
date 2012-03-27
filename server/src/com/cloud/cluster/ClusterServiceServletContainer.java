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

package com.cloud.cluster;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.log4j.Logger;

import com.cloud.utils.concurrency.NamedThreadFactory;

public class ClusterServiceServletContainer {
	private static final Logger s_logger = Logger.getLogger(ClusterServiceServletContainer.class);
	
	private ListenerThread listenerThread;
	
	public ClusterServiceServletContainer() {
	}
	
	public boolean start(HttpRequestHandler requestHandler, int port) {
	
		listenerThread = new ListenerThread(requestHandler, port);
		listenerThread.start();
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public void stop() {
		if(listenerThread != null) {
			listenerThread.interrupt();
			listenerThread.stop();
		}
	}
	
    static class ListenerThread extends Thread {
        private HttpService _httpService = null;
        private ServerSocket _serverSocket = null;
        private HttpParams _params = null;
        private ExecutorService _executor;

        public ListenerThread(HttpRequestHandler requestHandler, int port) {
    		_executor = Executors.newCachedThreadPool(new NamedThreadFactory("Cluster-Listener"));
        	
            try {
                _serverSocket = new ServerSocket(port);
            } catch (IOException ioex) {
                s_logger.error("error initializing cluster service servlet container", ioex);
                return;
            }

            _params = new BasicHttpParams();
            _params
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

            // Set up the HTTP protocol processor
            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new ResponseDate());
            httpproc.addInterceptor(new ResponseServer());
            httpproc.addInterceptor(new ResponseContent());
            httpproc.addInterceptor(new ResponseConnControl());

            // Set up request handlers
            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("/clusterservice", requestHandler);

            // Set up the HTTP service
            _httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
            _httpService.setParams(_params);
            _httpService.setHandlerResolver(reqistry);
        }

        public void run() {
        	if(s_logger.isInfoEnabled())
        		s_logger.info("Cluster service servlet container listening on port " + _serverSocket.getLocalPort());
            
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = _serverSocket.accept();
                    final DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, _params);

                    _executor.execute(new Runnable() {
                    	public void run() {
                            HttpContext context = new BasicHttpContext(null);
                            try {
                            	while(!Thread.interrupted() && conn.isOpen()) {
	                            	if(s_logger.isTraceEnabled())
	                            		s_logger.trace("dispatching cluster request from " + conn.getRemoteAddress().toString());
	                            	
	                                _httpService.handleRequest(conn, context);
	                                
	                            	if(s_logger.isTraceEnabled())
	                            		s_logger.trace("Cluster request from " + conn.getRemoteAddress().toString() + " is processed");
                            	}
                            } catch (ConnectionClosedException ex) {
                                s_logger.error("Client closed connection", ex);
                            } catch (IOException ex) {
                                s_logger.error("I/O error", ex);
                            } catch (HttpException ex) {
                                s_logger.error("Unrecoverable HTTP protocol violation", ex);
                            } finally {
                                try {
                                    conn.shutdown();
                                } catch (IOException ignore) {
                                    s_logger.error("unexpected exception", ignore);
                                }
                            }
                    	}
                    });
                    
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    s_logger.error("Exception when initializing cluster service servlet container : ", e);
                    break;
                }
            }
            
            _executor.shutdown();
        	if(s_logger.isInfoEnabled())
        		s_logger.info("Cluster service servlet container shutdown");
        }
    }
}
