package com.cloud.utils.ssh;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Session;

public class SSHCmdHelper {
	private static final Logger s_logger = Logger.getLogger(SSHCmdHelper.class);
	
	public static com.trilead.ssh2.Connection acquireAuthorizedConnection(String ip, String username, String password) {
		return acquireAuthorizedConnection(ip, 22, username, password);
	}
	
	public static com.trilead.ssh2.Connection acquireAuthorizedConnection(String ip, int port, String username, String password) {
		com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(ip, port);
		try {
			sshConnection.connect(null, 60000, 60000);
			if (!sshConnection.authenticateWithPassword(username, password)) {
				String[] methods = sshConnection.getRemainingAuthMethods(username);
				StringBuffer mStr = new StringBuffer();
				for (int i=0; i<methods.length; i++) {
					mStr.append(methods[i]);
				}
				s_logger.warn("SSH authorizes failed, support authorized methods are " + mStr);
				return null;
			}
			return sshConnection;
		} catch (IOException e) {
			s_logger.warn("Get SSH connection failed", e);
			return null;
		}
	}
	
	public static void releaseSshConnection(com.trilead.ssh2.Connection sshConnection) {
		if (sshConnection != null) {
			sshConnection.close();
		}
	}
	
	public static boolean sshExecuteCmd(com.trilead.ssh2.Connection sshConnection, String cmd, int nTimes) {
		for (int i = 0; i < nTimes; i ++) {
			if (sshExecuteCmdOneShot(sshConnection, cmd))
				return true;
		}
		return false;
	}
	
	public static boolean sshExecuteCmd(com.trilead.ssh2.Connection sshConnection, String cmd) {
		return sshExecuteCmd(sshConnection, cmd, 3);
	}
	
	public static boolean sshExecuteCmdOneShot(com.trilead.ssh2.Connection sshConnection, String cmd) {
		s_logger.debug("Executing cmd: " + cmd);
		Session sshSession = null;
		try {
			sshSession = sshConnection.openSession();
			// There is a bug in Trilead library, wait a second before
			// starting a shell and executing commands, from http://spci.st.ewi.tudelft.nl/chiron/xref/nl/tudelft/swerl/util/SSHConnection.html
			Thread.sleep(1000);

			if (sshSession == null) {
				return false;
			}
			
			sshSession.execCommand(cmd);
			
			InputStream stdout = sshSession.getStdout();
			InputStream stderr = sshSession.getStderr();
			
	
			byte[] buffer = new byte[8192];
			while (true) {
				if (stdout == null || stderr == null) {
					return false;
				}
				
				if ((stdout.available() == 0) && (stderr.available() == 0)) {
					int conditions = sshSession.waitForCondition(
							ChannelCondition.STDOUT_DATA
							| ChannelCondition.STDERR_DATA
							| ChannelCondition.EOF, 120000);
					
					if ((conditions & ChannelCondition.TIMEOUT) != 0) {
						s_logger.info("Timeout while waiting for data from peer.");
						break;
					}

					if ((conditions & ChannelCondition.EOF) != 0) {
						if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {							
							break;
						}
					}
				}
							
				while (stdout.available() > 0) {
					stdout.read(buffer);
				}
			
				while (stderr.available() > 0) {
					stderr.read(buffer);
				}
			}
			
			s_logger.debug(cmd + " output:" + new String(buffer));
			
			Thread.sleep(1000);
			if (sshSession.getExitStatus() != 0) {
				return false;
			}
			
			return true;
		} catch (IOException e) {
			s_logger.debug("Executing cmd: " + cmd + " failed, due to: " + e.toString());
			return false;
		} catch (InterruptedException e) {
			return false;
		} catch (Exception e) {
			return false;
		}	finally {
			if (sshSession != null)
				sshSession.close();
		}
	}
}
