// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package streamer;

import java.nio.charset.Charset;

/**
 * Assert that writes to this buffer are matching expected data.
 */
public class AssertingByteBuffer extends ByteBuffer {

    public AssertingByteBuffer(byte[] expectedData) {
        super(expectedData);
    }

    private void assertEquals(int expected, int actual) {
        if (expected != actual)
            throw new RuntimeException("Expected value does not match actual value. Expected value: " + expected + ", actual value: " + actual + ", buf: " + this + ".");
    }

    @Override
    public void writeByte(int b) {
        if (b < 0)
            throw new RuntimeException();
        //*DEBUG*/System.out.println("WriteByte: "+b+", cursor:"+cursor+".");
        assertEquals(readUnsignedByte(), b & 0xff);
    }

    @Override
    public void writeShort(int x) {
        //*DEBUG*/System.out.println("WriteShort: "+x+", cursor:"+cursor+".");
        assertEquals(readUnsignedShort(), x & 0xFFff);
    }

    @Override
    public void writeShortLE(int x) {
        //*DEBUG*/System.out.println("WriteShortLE: "+x+", cursor:"+cursor+".");
        assertEquals(readUnsignedShortLE(), x & 0xFFff);
    }

    @Override
    public void writeInt(int i) {
        //*DEBUG*/System.out.println("WriteInt: "+i+", cursor:"+cursor+".");
        assertEquals(readSignedInt(), i);
    }

    @Override
    public void writeIntLE(int i) {
        //*DEBUG*/System.out.println("WriteIntLE: "+i+", cursor:"+cursor+".");
        assertEquals(readSignedIntLE(), i);
    }

    @Override
    public void writeVariableIntLE(int i) {
        //*DEBUG*/System.out.println("WriteVariableIntLE: "+i+", cursor:"+cursor+".");
        assertEquals(readVariableSignedIntLE(), i);
    }

    @Override
    public void writeString(String actual, Charset charset) {
        //*DEBUG*/System.out.println("WriteString: "+actual+", cursor:"+cursor+".");
        String expected = readString(actual.length(), charset);
        if (!actual.equals(expected))
            throw new RuntimeException("Expected value does not match actual value. Expected value: " + expected + ", actual value: " + actual + ".");
    }

    @Override
    public void writeBytes(ByteBuffer actual) {
        //*DEBUG*/System.out.println("WriteString: "+actual+", cursor:"+cursor+".");
        ByteBuffer expected = readBytes(actual.length);
        if (!actual.equals(expected))
            throw new RuntimeException("Expected value does not match actual value. Expected value: " + expected + ", actual value: " + actual + ".");
    }

    @Override
    public void writeBytes(byte[] actualData) {
        ByteBuffer actual = new ByteBuffer(actualData);
        //*DEBUG*/System.out.println("WriteString: "+actual+", cursor:"+cursor+".");
        ByteBuffer expected = readBytes(actual.length);
        if (!actual.equals(expected))
            throw new RuntimeException("Expected value does not match actual value. Expected value: " + expected + ", actual value: " + actual + ".");
    }

    @Override
    public void writeBytes(byte[] actualData, int offset, int length) {
        ByteBuffer actual = new ByteBuffer(actualData, offset, length);
        //*DEBUG*/System.out.println("WriteString: "+actual+", cursor:"+cursor+".");
        ByteBuffer expected = readBytes(actual.length);
        if (!actual.equals(expected))
            throw new RuntimeException("Expected value does not match actual value. Expected value: " + expected + ", actual value: " + actual + ".");
    }

}
