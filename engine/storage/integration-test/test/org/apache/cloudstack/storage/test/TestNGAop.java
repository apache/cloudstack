package org.apache.cloudstack.storage.test;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.internal.ConstructorOrMethod;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;

public class TestNGAop implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods,
            ITestContext context) {
        for (IMethodInstance methodIns : methods) {
            ITestNGMethod method = methodIns.getMethod();
            ConstructorOrMethod meth = method.getConstructorOrMethod();
            Method m = meth.getMethod();
            if (m != null) {
                DB db = m.getAnnotation(DB.class);
                if (db != null) {
                    Transaction txn = Transaction.open(m.getName());
                }
            }
        }
        
        
        // TODO Auto-generated method stub
        return methods;
    }

}
