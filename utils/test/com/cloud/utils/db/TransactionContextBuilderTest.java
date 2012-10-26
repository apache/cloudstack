package com.cloud.utils.db;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/com/cloud/utils/db/transactioncontextBuilderTest.xml")
public class TransactionContextBuilderTest {

	@Inject
	DbAnnotatedBaseDerived _derived; 
	
	@Inject
	DbAnnotatedBase _base;
	
	@Test
	public void test() {
		_derived.DbAnnotatedMethod();
		_base.MethodWithClassDbAnnotated();
	}
}
