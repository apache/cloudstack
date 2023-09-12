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

/**
 * This class provides a facility to convert bytes through the scales (B, KiB, KB, MiB, MB...).
 *
 */
public class ByteScaleUtils {

    public static final long KiB = 1024;
    public static final long MiB = KiB * 1024;
    public static final long GiB = MiB * 1024;

    private ByteScaleUtils() {}

    /**
     * Converts mebibytes to bytes.
     *
     * @param mib The value to convert to bytes (eq: 1, 2, 3, ..., 42,...).
     * @return The parameter multiplied by 2²⁰ (1048576 | 1024 * 1024 | 1 mebibyte).
     */
    public static long mebibytesToBytes(long mib) {
        return mib * MiB;
    }

    /**
     * Converts bytes to kibibyte.
     *
     * @param bytes The value in bytes to convert to kibibyte.
     * @return The parameter divided by 2¹⁰ (1024 | 1 kibibyte).
     */
    public static long bytesToKibibytes(long bytes) {
        return bytes / KiB;
    }

    /**
     * Converts bytes to mebibytes.
     *
     * @param bytes The value in bytes to convert to mebibytes.
     * @return The parameter divided by 2²⁰ (1048576 | 1024 * 1024 | 1 mebibyte).
     */
    public static long bytesToMebibytes(long bytes) {
        return bytes / MiB;
    }

    /**
     * Converts bytes to gibibytes.
     *
     * @param b The value in bytes to convert to gibibytes.
     * @return The parameter divided by 1024 * 1024 * 1024 (1 GiB).
     */
    public static long bytesToGibibytes(long b) {
        return b / GiB;
    }
}
