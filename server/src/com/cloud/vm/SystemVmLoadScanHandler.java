package com.cloud.vm;

import com.cloud.vm.SystemVmLoadScanner.AfterScanAction;

public interface SystemVmLoadScanHandler<T> {
	String getScanHandlerName();
	boolean canScan();

	void onScanStart();
	
	T[] getScannablePools();
	boolean isPoolReadyForScan(T pool);
	AfterScanAction scanPool(T pool);
	void expandPool(T pool);
	void shrinkPool(T pool);
	
	void onScanEnd();
}

