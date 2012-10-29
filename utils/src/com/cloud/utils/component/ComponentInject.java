package com.cloud.utils.component;

import javax.inject.Inject;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class ComponentInject {
	
	private static AutowireCapableBeanFactory beanFactory;
	@Inject
	private void setbeanFactory(AutowireCapableBeanFactory bf) {
		ComponentInject.beanFactory = bf;
	}
	
	public static <T> T inject(Class<T> clazz) {
		return beanFactory.createBean(clazz);
	}
}
