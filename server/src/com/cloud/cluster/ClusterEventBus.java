package com.cloud.cluster;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.cloud.utils.events.EventBusBase;
import com.cloud.utils.events.PublishScope;

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
