package com.cloud.utils.fsm;

public interface StateObject<S> {
	 /**
     * @return finite state.
     */
    S getState();
}
