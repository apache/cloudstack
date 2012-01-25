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
	private long _heartBeatCheckerTimeout = 360000; /*6 minutes*/
	public KVMHAChecker(List<NfsStoragePool> pools, String host) {
		this._pools = pools;
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
			cmd.add("-t", String.valueOf((_heartBeatUpdateFreq + _heartBeatUpdateTimeout)/1000 * 2));
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
