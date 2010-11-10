package com.cloud.agent.resource.computing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.libvirt.Connect;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;


public class KVMHAChecker extends KVMHABase implements Callable<Boolean> {
	private static final Logger s_logger = Logger.getLogger(KVMHAChecker.class);
	private List<NfsStoragePool> _pools;
	private String _hostIP;
	private long _heartBeatCheckerTimeout = 300000; /*5 minutes*/
	public KVMHAChecker(List<NfsStoragePool> pools,  Connect conn, String host) {
		this._pools = pools;
		this._libvirtConnection = conn;
		this._hostIP = host;
	}
	
	/*True means heartbeaing is on going, or we can't get it's status. False means heartbeating is stopped definitely */
	private Boolean checkingHB() {
		List<Boolean> results = new ArrayList<Boolean>();
		for (NfsStoragePool pool : _pools) {
			
			Script cmd = new Script(_heartBeatPath, _heartBeatCheckerTimeout, s_logger);
			cmd.add("-i", pool._poolIp);
			cmd.add("-p", pool._poolMountSourcePath);
			cmd.add("-m", pool._mountDestPath);
			cmd.add("-h", _hostIP);
			cmd.add("-r");
			cmd.add("-t", String.valueOf(_heartBeatUpdateFreq/1000 * 2));
			OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
			String result = cmd.execute(parser);
			s_logger.debug("pool: " + pool._poolIp);
			s_logger.debug("reture: " + result);
			s_logger.debug("parser: " + parser.getLine());
			if (result == null && parser.getLine().contains("> DEAD <")) {
				s_logger.debug("read heartbeat failed: " + result);
				results.add(false);
			} else {
				results.add(true);
			}
		}
			
		
		for (Boolean r : results) {
			if (r) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public Boolean call() throws Exception {
		//s_logger.addAppender(new org.apache.log4j.ConsoleAppender(new org.apache.log4j.PatternLayout(), "System.out"));
		return checkingHB();	
	}
}
