package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Command;

public class IpmISetBootDevCommand extends Command {	
	public enum BootDev {
		pxe(),
		disk(),
		cdrom(),
	}
	
	BootDev bootDev;
		
	public BootDev getBootDev() {
		return bootDev;
	}
	
	public IpmISetBootDevCommand(BootDev dev) {
		bootDev = dev;
	}
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

}
