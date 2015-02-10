package com.cloud.agent.manager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.resource.ServerResource;

@RunWith(MockitoJUnitRunner.class)
public class DirectAgentAttacheTest {
	@Mock
	private AgentManagerImpl _agentMgr;

	@Mock
	private ServerResource _resource;

	long _id = 0L;

	@Before
	public void setup() {
		directAgentAttache = new DirectAgentAttache(_agentMgr, _id, "myDirectAgentAttache", _resource, false);
		
		MockitoAnnotations.initMocks(directAgentAttache);
	}
	private DirectAgentAttache directAgentAttache;

	@Test
	public void testPingTask() throws Exception {
		DirectAgentAttache.PingTask pt = directAgentAttache.new PingTask();
		Mockito.doReturn(2).when(_agentMgr).getDirectAgentThreadCap();
		pt.runInContext();
		Mockito.verify(_resource, Mockito.times(1)).getCurrentStatus(_id);
	}
}
