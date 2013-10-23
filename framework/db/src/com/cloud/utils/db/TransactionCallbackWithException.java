package com.cloud.utils.db;

public interface TransactionCallbackWithException<T,E extends Throwable> {

	public T doInTransaction(TransactionStatus status) throws E;
	
}
