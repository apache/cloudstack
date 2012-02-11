package com.cloud.agent.dhcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cloud.utils.concurrency.NamedThreadFactory;

public class DhcpProtocolParserServer extends Thread {
	private static final Logger s_logger = Logger
			.getLogger(DhcpProtocolParserServer.class);;
	protected ExecutorService _executor;
	private int dhcpServerPort = 67;
	private int bufferSize = 300;
	protected boolean _running = false;

	public DhcpProtocolParserServer(int workers) {
		_executor = new ThreadPoolExecutor(workers, 10 * workers, 1,
				TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(),
				new NamedThreadFactory("DhcpListener"));
		_running = true;
	}

	public void run() {
		while (_running) {
			try {
				DatagramSocket dhcpSocket = new DatagramSocket(dhcpServerPort,
						InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }));
				dhcpSocket.setBroadcast(true);

				while (true) {
					byte[] buf = new byte[bufferSize];
					DatagramPacket dgp = new DatagramPacket(buf, buf.length);
					dhcpSocket.receive(dgp);
					// _executor.execute(new DhcpPacketParser(buf));
				}
			} catch (IOException e) {
				s_logger.debug(e.getMessage());
			}
		}
	}
}
