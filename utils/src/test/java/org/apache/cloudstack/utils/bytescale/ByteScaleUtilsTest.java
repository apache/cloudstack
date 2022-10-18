/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cloudstack.utils.bytescale;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ByteScaleUtilsTest extends TestCase {

    @Test
    public void validateMibToBytes() {
        long mib = 3000L;
        long b = 1024L * 1024L * mib;
        assertEquals(b, ByteScaleUtils.mibToBytes(mib));
    }

    @Test
    public void validateBytesToKib() {
        long kib = 3000L;
        long b = 1024 * kib;
        assertEquals(kib, ByteScaleUtils.bytesToKib(b));
    }

    @Test
    public void validateBytesToMib() {
        long mib = 3000L;
        long b = 1024L * 1024L * mib;
        assertEquals(mib, ByteScaleUtils.bytesToMib(b));
    }

    @Test
    public void validateMibToBytesIfIntTimesIntThenMustExtrapolateIntMaxValue() {
        int mib = 3000;
        long b = 1024L * 1024L * mib;
        assertEquals(b, ByteScaleUtils.mibToBytes(mib));
    }

    @Test
    public void validateBytesToKibIfIntByIntThenMustExtrapolateIntMaxValue(){
        int b = Integer.MAX_VALUE;
        assertEquals(b, ByteScaleUtils.bytesToKib(b * 1024L));
    }
}
