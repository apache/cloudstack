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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ByteScaleUtilsTest {

    @Test
    public void validateMibToBytes() {
        long mib = 3000L;
        long bytes = 1024L * 1024L * mib;
        Assert.assertEquals(bytes, ByteScaleUtils.mibToBytes(mib));
    }

    @Test
    public void validateBytesToKiB() {
        long kib = 3000L;
        long bytes = 1024 * kib;
        Assert.assertEquals(kib, ByteScaleUtils.bytesToKiB(bytes));
    }

    @Test
    public void validateBytesToMiB() {
        long mib = 3000L;
        long bytes = 1024L * 1024L * mib;
        Assert.assertEquals(mib, ByteScaleUtils.bytesToMiB(bytes));
    }

    @Test
    public void validateMibToBytesIfIntTimesIntThenMustExtrapolateIntMaxValue() {
        int mib = 3000;
        long bytes = 1024L * 1024L * mib;
        Assert.assertEquals(bytes, ByteScaleUtils.mibToBytes(mib));
    }

    @Test
    public void validateBytesToKiBIfIntByIntThenMustExtrapolateIntMaxValue(){
        int bytes = Integer.MAX_VALUE;
        Assert.assertEquals(bytes, ByteScaleUtils.bytesToKiB(bytes * 1024L));
    }
}
