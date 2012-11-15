package com.cloud.cluster;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.apache.cloudstack.framework.messaging.EventBusBase;
import org.apache.cloudstack.framework.messaging.PublishScope;
import org.springframework.stereotype.Component;


@Component
public class ClusterEventBus extends EventBusBase {
	
	@PostConstruct
	public void init() {
	}
	
	@Override
	public void publish(String subject, PublishScope scope, Object sender,
		Serializable args) {
		super.publish(subject, scope, sender, args);
	}
}
