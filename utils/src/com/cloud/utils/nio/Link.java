/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils.nio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

/**
 * Link is the link between the socket listener and the handler threads.
 */
public class Link {
    private static final Logger s_logger = Logger.getLogger(Link.class);
   
    private final InetSocketAddress _addr;
    private final NioConnection _connection;
    private SelectionKey _key;
    private final ConcurrentLinkedQueue<ByteBuffer[]> _writeQueue;
    private ByteBuffer _readBuffer;
    private Object _attach;
    private boolean _readSize;
    
    public Link(InetSocketAddress addr, NioConnection connection) {
        _addr = addr;
        _connection = connection;
        _readBuffer = ByteBuffer.allocate(2048);
        _attach = null;
        _key = null;
        _writeQueue = new ConcurrentLinkedQueue<ByteBuffer[]>();
        _readSize = true;
    }
    
    public Link (Link link) {
        this(link._addr, link._connection);
    }
    
    public Object attachment() {
        return _attach;
    }
    
    public void attach(Object attach) {
        _attach = attach;
    }
    
    public void setKey(SelectionKey key) {
        _key = key;
    }
    
    /**
     * Static methods for reading from a channel in case
     * you need to add a client that doesn't require nio.
     * @param ch channel to read from.
     * @param bytebuffer to use.
     * @return bytes read
     * @throws IOException if not read to completion.
     */
    public static byte[] read(SocketChannel ch, ByteBuffer buff) throws IOException {
    	synchronized(buff) {
	    	buff.clear();
	    	buff.limit(4);
	    	
	    	while (buff.hasRemaining()) {
		    	if (ch.read(buff) == -1) {
		    		throw new IOException("Connection closed with -1 on reading size.");
		    	}
	    	}
	    	
	    	buff.flip();
	    	
	    	int length = buff.getInt();
	    	ByteArrayOutputStream output = new ByteArrayOutputStream(length);
	    	WritableByteChannel outCh = Channels.newChannel(output);
	    	
	    	int count = 0;
	    	while (count < length) {
	        	buff.clear();
	    		int read = ch.read(buff);
	    		if (read < 0) {
	    			throw new IOException("Connection closed with -1 on reading data.");
	    		}
	    		count += read;
	    		buff.flip();
	    		outCh.write(buff);
	    	}
	    	
	        return output.toByteArray();
    	}
    }
    	
    
    /**
     * write method to write to a socket.  This method writes to completion so
     * it doesn't follow the nio standard.  We use this to make sure we write
     * our own protocol.
     * 
     * @param ch channel to write to.
     * @param buffers buffers to write.
     * @throws IOException if unable to write to completion.
     */
    public static void write(SocketChannel ch, ByteBuffer[] buffers) throws IOException {
        synchronized(ch) {
    		int length = 0;
    		ByteBuffer[] buff = new ByteBuffer[buffers.length + 1];
    		for (int i = 0; i < buffers.length; i++) {
    			length += buffers[i].remaining();
    			buff[i + 1] = buffers[i];
    		}
    		buff[0] = ByteBuffer.allocate(4);
    		buff[0].putInt(length);
    		buff[0].flip();
    		long count = 0;
    		while (count < length + 4) {
    			long written = ch.write(buff);
    			if (written < 0) {
    				throw new IOException("Unable to write after " + count);
    			}
    			count += written;
    		}
        }
    }
    
    public byte[] read(SocketChannel ch) throws IOException {
        if (_readSize) {   // Start of a packet
            if (_readBuffer.position() == 0) {
                _readBuffer.limit(4);
            }
            
            if (ch.read(_readBuffer) == -1) {
                throw new IOException("Connection closed with -1 on reading size.");
            }
            
            if (_readBuffer.hasRemaining()) {
                s_logger.trace("Need to read the rest of the packet length");
                return null;
            }
            _readBuffer.flip();
            int readSize = _readBuffer.getInt();
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Packet length is " + readSize);
            }
            _readBuffer.clear();
            _readSize = false;
            
            if (_readBuffer.capacity() < readSize) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Resizing the byte buffer from " + _readBuffer.capacity());
                }
                _readBuffer = ByteBuffer.allocate(readSize);
            }
            _readBuffer.limit(readSize);
        }
        
        if (ch.read(_readBuffer) == -1) {
            throw new IOException("Connection closed with -1 on read.");
        }
        
        if (_readBuffer.hasRemaining()) {   // We're not done yet.
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Still has " + _readBuffer.remaining());
            }
            return null;
        }
        
        _readBuffer.flip();
        byte[] result = new byte[_readBuffer.limit()];
        _readBuffer.get(result);
        _readBuffer.clear();
        _readSize = true;
        
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Done with packet: " + result.length);
        }
        
        return result;
    }
    
    public void send(byte[] data) throws ClosedChannelException {
        send(data, false);
    }
    
    public void send(byte[] data, boolean close) throws ClosedChannelException {
        send(new ByteBuffer[] { ByteBuffer.wrap(data) }, close);
    }
    
    public void send(ByteBuffer[] data, boolean close) throws ClosedChannelException {
        ByteBuffer[] item = new ByteBuffer[data.length + 1];
        int remaining = 0;
        for (int i = 0; i < data.length; i++) {
            remaining += data[i].remaining();
            item[i + 1] = data[i];
        }
        
        item[0] = ByteBuffer.allocate(4);
        item[0].putInt(remaining);
        item[0].flip();
        
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Sending packet of length " + remaining);
        }
        
        _writeQueue.add(item);
        if  (close) {
            _writeQueue.add(new ByteBuffer[0]);
        }
        synchronized (this) {
            if (_key == null) {
                throw new ClosedChannelException();
            }
            _connection.change(SelectionKey.OP_WRITE, _key, null);
        }
    }
    
    public void send(ByteBuffer[] data) throws ClosedChannelException {
        send(data, false);
    }
    
    public synchronized void close() {
        if (_key != null) {
            _connection.close(_key);
        }
    }
    
    public boolean write(SocketChannel ch) throws IOException {
        ByteBuffer[] data = null;
        while ((data = _writeQueue.poll()) != null) {
            if (data.length == 0) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Closing connection requested");
                }
                return true;
            }

            data[0].mark();
            int remaining = data[0].getInt() + 4;
            data[0].reset();
            
            while (remaining > 0) {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Writing " + remaining);
                }
                long count = ch.write(data);
                remaining -= count;
            }
        }
        return false;
    }
    
    public synchronized void connect(SocketChannel ch) {
        _connection.register(SelectionKey.OP_CONNECT, ch, this);
    }
    
    public InetSocketAddress getSocketAddress() {
        return _addr;
    }
    
    public String getIpAddress() {
        return _addr.getAddress().toString();
    }
    
    public synchronized void terminated() {
        _key = null;
    }
    
    public synchronized void schedule(Task task) throws ClosedChannelException {
        if (_key == null) {
            throw new ClosedChannelException();
        }
        _connection.scheduleTask(task);
    }
}
