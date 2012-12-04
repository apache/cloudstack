package org.apache.cloudstack.framework.messaging;

public class ClientOnlyEventDrivenStyle {
	RpcProvider _rpcProvider;
	
	public void AsyncCallRpcService() {
		TestCommand cmd = new TestCommand();
		_rpcProvider.newCall("host-2").setCommand("TestCommand").setCommandArg(cmd).setTimeout(10000)
			.setCallbackDispatcherTarget(this)
			.setContextParam("origCmd", cmd)		// save context object for callback handler
			.apply();
	}
	
	@RpcCallbackHandler(command="TestCommand")
	public void OnAsyncCallRpcServiceCallback(RpcClientCall call) {
		try {
			TestCommand origCmd = call.getContextParam("origCmd");	// restore calling context at callback handler	

			TestCommandAnswer answer = call.get();
			
		} catch(RpcTimeoutException e) {
			
		} catch(RpcIOException e) {
			
		} catch(RpcException e) {
		}
	}
}
