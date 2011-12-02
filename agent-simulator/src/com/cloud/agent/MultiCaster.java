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

package com.cloud.agent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class MultiCaster implements Runnable {
    private static final Logger s_logger = Logger.getLogger(MultiCaster.class);
	
	public final int MAX_PACKET_SIZE = 8096;
	
	private List<MultiCasterListener> listeners;
	private DatagramSocket socket;
	private byte[] recvBuffer;
	
	private Thread driver;
	private volatile boolean stopRequested = false;
	
	public MultiCaster() {
		listeners = new ArrayList<MultiCasterListener>();
		recvBuffer = new byte[MAX_PACKET_SIZE];
	}
	
	public void addListener(MultiCasterListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}
	
	public void removeListener(MultiCasterListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	
	public void cast(byte[] buf, int off, int len, 
		InetAddress toAddress, int nToPort) throws IOException {
		
		if(socket == null)
			throw new IOException("multi caster is not started");
		
		if(len >= MAX_PACKET_SIZE)
			throw new IOException("packet size exceeds limit of " + MAX_PACKET_SIZE);
			
		DatagramPacket packet = new DatagramPacket(buf, off, 
			len, toAddress, nToPort);

		socket.send(packet);
	}
	
	public void start(String strOutboundAddress, 
		String strClusterAddress, int nPort) throws SocketException {
		assert(socket == null);

		InetAddress addr = null;
		try {
			addr = InetAddress.getByName(strClusterAddress);
		} catch(IOException e) {
			s_logger.error("Unexpected exception " , e);
		}
		
		if(addr != null && addr.isMulticastAddress()) {
			try {
				socket = new MulticastSocket(nPort);
				socket.setReuseAddress(true);
				
				if(s_logger.isInfoEnabled())
					s_logger.info("Join multicast group : " + addr);
				
				((MulticastSocket)socket).joinGroup(addr);
				((MulticastSocket)socket).setTimeToLive(1);
				
				if(strOutboundAddress != null) {
					if(s_logger.isInfoEnabled())
						s_logger.info("set outgoing interface to : " + strOutboundAddress);
					
					InetAddress ia = InetAddress.getByName(strOutboundAddress);
					NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
					((MulticastSocket)socket).setNetworkInterface(ni);
				}
			} catch(IOException e) {
				s_logger.error("Unexpected exception " , e);
			}
		} else {
			socket = new DatagramSocket(nPort);
			socket.setReuseAddress(true);
		}
		
		driver = new Thread(this, "Multi-caster");
		driver.setDaemon(true);
		driver.start();
	}
	
	public void stop() {
		if(socket != null) {
			stopRequested = true;
			
			socket.close();
			if(driver != null) {
				try {
					driver.join();
				} catch(InterruptedException e) {
				}
				driver = null;
			}
		}
		
		socket = null;
		stopRequested = false;
	}
	
	public void run() {
		while(!stopRequested) {
			try {
				DatagramPacket packet = new DatagramPacket(recvBuffer, recvBuffer.length);
				socket.receive(packet);
				
				for(Object listener : listeners.toArray()) {
					((MultiCasterListener)listener).onMultiCasting(packet.getData(),
						packet.getOffset(), packet.getLength(), packet.getAddress());
				}
			} catch(IOException e) {
			} catch(Throwable e) {
				s_logger.error("Unhandled exception : ", e);
			}
		}
	}
}
