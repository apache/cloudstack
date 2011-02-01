package com.cloud.utils;

public interface CleanupDelegate<T, M> {
	boolean cleanup(T itemContext, M managerContext);
}
