package com.cloud.agent.api;

import com.cloud.agent.api.to.VirtualMachineTO;

public class ScaleVmCommand extends Command {

    VirtualMachineTO vm;
    String vmName;
    int cpus;
    Integer speed;
    long minRam;
    long maxRam;
    
    public VirtualMachineTO getVm() {
		return vm;
	}

	public void setVm(VirtualMachineTO vm) {
		this.vm = vm;
	}

	public int getCpus() {
		return cpus;
	}

	public ScaleVmCommand(String vmName, int cpus,
			Integer speed, long minRam, long maxRam) {
		super();
		this.vmName = vmName;
		this.cpus = cpus;
		//this.speed = speed;
		this.minRam = minRam;
		this.maxRam = maxRam;
		this.vm = new VirtualMachineTO(1L, vmName, null, cpus, null, minRam, maxRam, null, null, false, false, null);
		/*vm.setName(vmName);
		vm.setCpus(cpus);
		vm.setRam(minRam, maxRam);*/
	}

	public void setCpus(int cpus) {
		this.cpus = cpus;
	}

	public Integer getSpeed() {
		return speed;
	}

	public void setSpeed(Integer speed) {
		this.speed = speed;
	}

	public long getMinRam() {
		return minRam;
	}

	public void setMinRam(long minRam) {
		this.minRam = minRam;
	}

	public long getMaxRam() {
		return maxRam;
	}

	public void setMaxRam(long maxRam) {
		this.maxRam = maxRam;
	}

	public String getVmName() {
		return vmName;
	}

	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public VirtualMachineTO getVirtualMachine() {
        return vm;
    }
    
    @Override
    public boolean executeInSequence() {
        return true;
    }
    
    protected ScaleVmCommand() {
    }
    
    public ScaleVmCommand(VirtualMachineTO vm) {
        this.vm = vm;
    }

	public boolean getLimitCpuUse() {
		// TODO Auto-generated method stub
		return false;
	}

}
