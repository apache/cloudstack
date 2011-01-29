/**
 * Copyright (c) 2008, 2009, VMOps Inc.
 *
 * This code is Copyrighted and must not be reused, modified, or redistributed without the explicit consent of VMOps.
 */
package com.cloud.agent.manager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.transport.Request;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.utils.nio.Link;


public class ClusteredAgentAttache extends ConnectedAgentAttache implements Routable {
	private final static Logger s_logger = Logger.getLogger(ClusteredAgentAttache.class);
	private static ClusteredAgentManagerImpl s_clusteredAgentMgr;
	protected ByteBuffer _buffer = ByteBuffer.allocate(2048);
	private boolean _forward = false;

	static public void initialize(ClusteredAgentManagerImpl agentMgr) {
		s_clusteredAgentMgr = agentMgr;
	}

	public ClusteredAgentAttache(long id) {
		super(id, null, false);
		_forward = true;
	}
	
	public ClusteredAgentAttache(long id, Link link, boolean maintenance) {
		super(id, link, maintenance);
		_forward = link == null;
	}
	
	@Override
	public boolean isClosed() {
		return _forward ? false : super.isClosed();
	}
	
	@Override
	public boolean forForward() {
		return _forward;
	}
	
	@Override
    public void cancel(long seq) {
	    if (forForward()) {
	        Listener listener = getListener(seq);
	        if (listener != null && listener instanceof SynchronousListener) {
	            SynchronousListener synchronous = (SynchronousListener)listener;
	            String peerName = synchronous.getPeer();
	            if (peerName != null) {
	                s_logger.debug(log(seq, "Forwarding to peer to cancel due to timeout"));
	                s_clusteredAgentMgr.cancel(peerName, _id, seq, "Timed Out");
	            }
	        }
	    }
	    
	    super.cancel(seq);
	}
	
	@Override
	public void routeToAgent(byte[] data) throws AgentUnavailableException {
		if (s_logger.isDebugEnabled()) {
			s_logger.debug(log(Request.getSequence(data), "Routing from " + Request.getManagementServerId(data)));
		}
			
		if (_link == null) {
			if (s_logger.isDebugEnabled()) {
			    s_logger.debug(log(Request.getSequence(data), "Link is closed"));
			}
		}
		
		try {
			_link.send(data);
		} catch (ClosedChannelException e) {
			if (s_logger.isDebugEnabled()) {
				s_logger.debug(log(Request.getSequence(data), "Channel is closed"));
			}
			
			throw new AgentUnavailableException("Channel to agent is closed", _id);
		} catch (NullPointerException e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(log(Request.getSequence(data), "Link is closed"));
            }
		    // Note: since this block is not in synchronized.  It is possible for _link to become null.
            throw new AgentUnavailableException("Channel to agent is closed", _id);
		}
	}
	
	@Override
	public void send(Request req, Listener listener) throws AgentUnavailableException {
		if (_link != null) {
			super.send(req, listener);
			return;
		}
		
		long seq = req.getSequence();
		
		if (listener != null) {
			registerListener(req.getSequence(), listener);
		}
		
		int i = 0;
		SocketChannel ch = null;
		boolean error = true;
		try {
    		while (i++ < 5) {
    		    String peerName = s_clusteredAgentMgr.findPeer(_id);
    		    if (peerName == null) {
    		        throw new AgentUnavailableException("Unable to find peer", _id);
    		    }
    		    
    			ch = s_clusteredAgentMgr.connectToPeer(peerName, ch);
    			if (ch == null) {
    				if (s_logger.isDebugEnabled()) {
    					s_logger.debug(log(seq, "Unable to forward " + req.toString()));
    				}
    				continue;
    			}
    			
    			try {
    				if (s_logger.isDebugEnabled()) {
    					s_logger.debug(log(seq, "Forwarding " + req.toString() + " to " + peerName));
    				}
    				if (req.executeInSequence() && listener != null && listener instanceof SynchronousListener) {
    				    SynchronousListener synchronous = (SynchronousListener)listener;
    				    synchronous.setPeer(peerName);
    				}
    				Link.write(ch, req.toBytes());
    				error = false;
    				return;
    			} catch (IOException e) {
    				if (s_logger.isDebugEnabled()) {
    					s_logger.debug(log(seq, "Error on connecting to management node: " + req.toString() + " try = " + i));
    				}
    
    				if(s_logger.isInfoEnabled())
    					s_logger.info("IOException " + e.getMessage() + " when sending data to peer " + peerName + ", close peer connection and let it re-open");
    			}
    		}
		} finally {
		    if (error) {
		        unregisterListener(seq);
		    }
		}
		throw new AgentUnavailableException("Unable to reach the peer that the agent is connected", _id);
	}
}
