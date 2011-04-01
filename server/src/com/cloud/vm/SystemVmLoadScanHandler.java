package com.cloud.vm;

import com.cloud.utils.Pair;
import com.cloud.vm.SystemVmLoadScanner.AfterScanAction;

public interface SystemVmLoadScanHandler<T> {
	boolean canScan();

	void onScanStart();
	
	T[] getScannablePools();
	boolean isPoolReadyForScan(T pool);
	Pair<AfterScanAction, Object> scanPool(T pool);
	void expandPool(T pool, Object actionArgs);
	void shrinkPool(T pool, Object actionArgs);
	
	void onScanEnd();
}

