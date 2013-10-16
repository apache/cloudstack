package com.cloud.utils.db;

public interface TransactionCallbackWithException<T> {

	public T doInTransaction(TransactionStatus status) throws Exception;
	
}
