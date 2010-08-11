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

// repackage it to VMOps common packaging structure
package com.cloud.console;

import java.io.*;

public class SplitInputStream extends FilterInputStream {
	ByteArrayOutputStream bo;
	public SplitInputStream(InputStream in) {
		super(in);
	}
	public int read() throws IOException {
		int b = super.read();
		if (b >= 0 && bo != null) {
			bo.write(b);
		}
		return b;
	}
	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}
	public int read(byte b[], int off, int len) throws IOException {
		int res = super.read(b, off, len);
		if (res > 0 && bo != null) {
			bo.write(b, off, res);
		}
		return res;
	}
	public long skip(long n) throws IOException {
		long res = super.skip(n);
		if (res > 0 && bo != null) {
			byte[] b = new byte[(int)res];
			bo.write(b, 0, (int)res);
		}
		return res;
	}
	public int available() throws IOException {
		return super.available();
	}
	public void close() throws IOException {
		super.close();
	}
	public void mark(int readlimit) {
		super.mark(readlimit);
	}
	public void reset() throws IOException {
		super.reset();
	}
	public boolean markSupported() {
		return false;
	}
	public void setSplit() {
		bo = new ByteArrayOutputStream();
	}
	public byte[] getSplit() {
		if (bo == null) {
			return null;
		}
		byte[] res = bo.toByteArray();
		bo = null;
		return res;
	}
	public byte[] getSplitData() {
		if(bo == null)
			return null;
		
		return bo.toByteArray();
	}
}
