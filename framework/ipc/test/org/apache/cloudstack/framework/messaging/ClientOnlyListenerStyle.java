package org.apache.cloudstack.framework.messaging;

public class ClientOnlyListenerStyle {
	
	RpcProvider _rpcProvider;
	
	public void AsyncCallRpcService() {
		TestCommand cmd = new TestCommand();
		_rpcProvider.newCall("host-2").setCommand("TestCommand").setCommandArg(cmd).setTimeout(10000)
			.addCallbackListener(new RpcCallbackListener<TestCommandAnswer>() {
				@Override
				public void onSuccess(TestCommandAnswer result) {
				}

				@Override
				public void onFailure(RpcException e) {
				}
			}).apply();
	}
	
	public void SyncCallRpcService() {
		TestCommand cmd = new TestCommand();
		RpcClientCall call = _rpcProvider.newCall("host-2").setCommand("TestCommand").setCommandArg(cmd).setTimeout(10000).apply();
		
		try {
			TestCommandAnswer answer = call.get();
		} catch (RpcTimeoutException e) {
			
		} catch (RpcIOException e) {
			
		} catch (RpcException e) {
		}
	}
}
