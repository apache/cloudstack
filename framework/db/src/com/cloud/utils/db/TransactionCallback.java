package com.cloud.utils.db;

public interface TransactionCallback<T> {

	public T doInTransaction(TransactionStatus status);

}
