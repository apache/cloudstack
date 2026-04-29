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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class ByteBufferTest {

    private static final Random random = new Random(System.currentTimeMillis());

    private final byte[] data;

    public ByteBufferTest(byte[] data) {
        this.data = data;
    }

    private static byte[] getRandomByteArray() {
        return new byte[] {(byte)random.nextInt(), (byte)random.nextInt(), (byte)random.nextInt(), (byte)random.nextInt(), (byte)random.nextInt(), (byte)random.nextInt(),
                (byte)random.nextInt()};
    }

    @Parameters
    public static Collection<Object[]> data() {
        int parameterCount = 50;
        List<Object[]> parameters = new ArrayList<Object[]>(parameterCount);

        for (int i = 0; i < parameterCount; i++) {
            parameters.add(new Object[] {getRandomByteArray()});
        }

        return parameters;
    }

    // This findbugs finding is meant to show that the shift by 32 does nothing
    // and was used to remove these cases from the production code.
    // Don't try to fix it
    @Test
    public void testShiftByteBy32BitsDoesNothing() throws Exception {
        for (byte b : data) {
            assertEquals(b, b << 32);
        }
    }

    @Test
    public void testReadSignedVarIntWhenLenIs5() throws Exception {
        int len = 5;
        ByteBuffer byteBuffer = new ByteBuffer(data);

        long expected = data[0] | ByteBuffer.calculateUnsignedInt(data[1], data[2], data[3], data[4]);
        long actual = byteBuffer.readSignedVarInt(len);

        assertEquals(expected, actual);
    }

    @Test
    public void testReadSignedVarIntWhenLenIs6() throws Exception {
        int len = 6;
        ByteBuffer byteBuffer = new ByteBuffer(data);

        long expected = ByteBuffer.calculateSignedShort(data[0], data[1]) | ByteBuffer.calculateUnsignedInt(data[2], data[3], data[4], data[5]);
        long actual = byteBuffer.readSignedVarInt(len);

        assertEquals(expected, actual);
    }

    @Test
    public void testReadSignedVarIntWhenLenIs7() throws Exception {
        int len = 7;
        ByteBuffer byteBuffer = new ByteBuffer(data);

        long expected = data[0] << 24 | ByteBuffer.calculateUnsignedShort(data[1], data[2]) | ByteBuffer.calculateUnsignedInt(data[3], data[4], data[5], data[6]);
        long actual = byteBuffer.readSignedVarInt(len);

        assertEquals(expected, actual);
    }

    @Test
    public void testReadUnsignedVarIntWhenLenIs5() throws Exception {
        int len = 5;
        ByteBuffer byteBuffer = new ByteBuffer(data);

        long expected = ByteBuffer.calculateUnsignedByte(data[0]) | ByteBuffer.calculateUnsignedInt(data[1], data[2], data[3], data[4]);
        long actual = byteBuffer.readUnsignedVarInt(len);

        assertEquals(expected, actual);
    }

    @Test
    public void testReadUnsignedVarIntWhenLenIs6() throws Exception {
        int len = 6;
        ByteBuffer byteBuffer = new ByteBuffer(data);

        long expected = ByteBuffer.calculateUnsignedShort(data[0], data[1]) | ByteBuffer.calculateUnsignedInt(data[2], data[3], data[4], data[5]);
        long actual = byteBuffer.readUnsignedVarInt(len);

        assertEquals(expected, actual);
    }

    @Test
    public void testReadUnsignedVarIntWhenLenIs7() throws Exception {
        int len = 7;
        ByteBuffer byteBuffer = new ByteBuffer(data);

        long expected = (ByteBuffer.calculateUnsignedByte(data[0]) << 16) | ByteBuffer.calculateUnsignedShort(data[1], data[2])
                | ByteBuffer.calculateUnsignedInt(data[3], data[4], data[5], data[6]);
        long actual = byteBuffer.readUnsignedVarInt(len);

        assertEquals(expected, actual);
    }

    @Test
    public void testAddBytesToBuilder() throws Exception {
        StringBuilder builder = new StringBuilder();
        byte[] data = new byte[] {(byte)1, (byte)2};
        ByteBuffer byteBuffer = new ByteBuffer(data);

        int expected = 2;
        int actual = byteBuffer.addBytesToBuilder(builder);

        assertNotNull(builder);
        assertFalse(builder.toString().isEmpty());
        assertEquals(expected, actual);
    }

}
