package org.apache.cloudstack.framework.messaging;

public interface ComponentContainer {
	ComponentEndpoint wireComponent(ComponentEndpoint endpoint, String predefinedAddress);
}
