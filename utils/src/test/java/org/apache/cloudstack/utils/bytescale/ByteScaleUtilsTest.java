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
    public void validateMebibytesToBytes() {
        long mib = 3000L;
        long bytes = 1024L * 1024L * mib;
        Assert.assertEquals(bytes, ByteScaleUtils.mebibytesToBytes(mib));
    }

    @Test
    public void validateBytesToKibibytes() {
        long kib = 3000L;
        long bytes = 1024 * kib;
        Assert.assertEquals(kib, ByteScaleUtils.bytesToKibibytes(bytes));
    }

    @Test
    public void validateBytesToMebibytes() {
        long mib = 3000L;
        long bytes = 1024L * 1024L * mib;
        Assert.assertEquals(mib, ByteScaleUtils.bytesToMebibytes(bytes));
    }

    @Test
    public void validateMebibytesToBytesIfIntTimesIntThenMustExtrapolateIntMaxValue() {
        int mib = 3000;
        long bytes = 1024L * 1024L * mib;
        Assert.assertEquals(bytes, ByteScaleUtils.mebibytesToBytes(mib));
    }

    @Test
    public void validateBytesToKibibytesIfIntByIntThenMustExtrapolateIntMaxValue(){
        int bytes = Integer.MAX_VALUE;
        Assert.assertEquals(bytes, ByteScaleUtils.bytesToKibibytes(bytes * 1024L));
    }
}
