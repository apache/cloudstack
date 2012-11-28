package org.apache.cloudstack.storage.test;

import org.aspectj.lang.ProceedingJoinPoint;

import com.cloud.utils.db.Transaction;

public class AopTestAdvice {
	public Object AopTestMethod(ProceedingJoinPoint call) throws Throwable {
		Transaction txn = Transaction.open(call.getSignature().getName());
		Object ret = null;
		try {
			 ret = call.proceed();
		} finally {
			txn.close();
		}
		return ret;
	}
}
