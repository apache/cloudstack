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

package org.apache.cloudstack.storage.formatinspector;

import com.cloud.utils.NumbersUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class to inspect QCOW2 files/objects. In our context, a QCOW2 might be a threat to the environment if it meets one of the following criteria when coming from external sources
 * (like registering or uploading volumes and templates):
 * <ul>
 *   <li>has a backing file reference;</li>
 *   <li>has an external data file reference;</li>
 *   <li>has unknown incompatible features.</li>
 * </ul>
 *
 * The implementation was done based on the <a href="https://gitlab.com/qemu-project/qemu/-/blob/master/docs/interop/qcow2.txt"> QEMU's official interoperability documentation</a>
 * and on the <a href="https://review.opendev.org/c/openstack/cinder/+/923247/2/cinder/image/format_inspector.py">OpenStack's Cinder implementation for Python</a>.
 */
public class Qcow2Inspector {
    protected static Logger LOGGER = LogManager.getLogger(Qcow2Inspector.class);

    private static final byte[] QCOW_MAGIC_STRING = ArrayUtils.add("QFI".getBytes(), (byte) 0xfb);
    private static final int INCOMPATIBLE_FEATURES_MAX_KNOWN_BIT = 4;
    private static final int INCOMPATIBLE_FEATURES_MAX_KNOWN_BYTE = 0;
    private static final int EXTERNAL_DATA_FILE_BYTE_POSITION = 7;
    private static final int EXTERNAL_DATA_FILE_BIT = 2;
    private static final byte EXTERNAL_DATA_FILE_BITMASK = (byte) (1 << EXTERNAL_DATA_FILE_BIT);

    private static final Set<Qcow2HeaderField> SET_OF_HEADER_FIELDS_TO_READ = Set.of(Qcow2HeaderField.MAGIC,
            Qcow2HeaderField.VERSION,
            Qcow2HeaderField.SIZE,
            Qcow2HeaderField.BACKING_FILE_OFFSET,
            Qcow2HeaderField.INCOMPATIBLE_FEATURES);

    /**
     * Validates if the file is a valid and allowed QCOW2 (i.e.: does not contain external references).
     * @param filePath Path of the file to be validated.
     * @throws RuntimeException If the QCOW2 file meets one of the following criteria:
     * <ul>
     *   <li>has a backing file reference;</li>
     *   <li>has an external data file reference;</li>
     *   <li>has unknown incompatible features.</li>
     * </ul>
     */
    public static void validateQcow2File(String filePath) throws RuntimeException {
        LOGGER.info(String.format("Verifying if [%s] is a valid and allowed QCOW2 file .", filePath));

        Map<String, byte[]> headerFieldsAndValues;
        try (InputStream inputStream = new FileInputStream(filePath)) {
            headerFieldsAndValues = unravelQcow2Header(inputStream, filePath);
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Unable to validate file [%s] due to: ", filePath), ex);
        }

        validateQcow2HeaderFields(headerFieldsAndValues, filePath);

        LOGGER.info(String.format("[%s] is a valid and allowed QCOW2 file.", filePath));
    }

    /**
     * Unravels the QCOW2 header in a serial fashion, iterating through the {@link Qcow2HeaderField}, reading the fields specified in
     * {@link Qcow2Inspector#SET_OF_HEADER_FIELDS_TO_READ} and skipping the others.
     * @param qcow2InputStream InputStream of the QCOW2 being unraveled.
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @return A map of the header fields and their values according to the {@link Qcow2Inspector#SET_OF_HEADER_FIELDS_TO_READ}.
     * @throws IOException If the field cannot be read or skipped.
     */
    public static Map<String, byte[]> unravelQcow2Header(InputStream qcow2InputStream, String qcow2LogReference) throws IOException {
        Map<String, byte[]> result = new HashMap<>();

        LOGGER.debug(String.format("Unraveling QCOW2 [%s] headers.", qcow2LogReference));
        for (Qcow2HeaderField qcow2Header : Qcow2HeaderField.values()) {
            if (!SET_OF_HEADER_FIELDS_TO_READ.contains(qcow2Header)) {
                skipHeader(qcow2InputStream, qcow2Header, qcow2LogReference);
                continue;
            }

            byte[] headerValue = readHeader(qcow2InputStream, qcow2Header, qcow2LogReference);
            result.put(qcow2Header.name(), headerValue);
        }

        return result;
    }

    /**
     * Skips the field's length in the InputStream.
     * @param qcow2InputStream InputStream of the QCOW2 being unraveled.
     * @param field Field being skipped (name and length).
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @throws IOException If the bytes skipped do not match the field length.
     */
    protected static void skipHeader(InputStream qcow2InputStream, Qcow2HeaderField field, String qcow2LogReference) throws IOException {
        LOGGER.trace(String.format("Skipping field [%s] of QCOW2 [%s].", field, qcow2LogReference));

        if (qcow2InputStream.skip(field.getLength()) != field.getLength()) {
            throw new IOException(String.format("Unable to skip field [%s] of QCOW2 [%s].", field, qcow2LogReference));
        }
    }

    /**
     * Reads the field's length in the InputStream.
     * @param qcow2InputStream InputStream of the QCOW2 being unraveled.
     * @param field Field being read (name and length).
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @throws IOException If the bytes read do not match the field length.
     */
    protected static byte[] readHeader(InputStream qcow2InputStream, Qcow2HeaderField field, String qcow2LogReference) throws IOException {
        byte[] readBytes = new byte[field.getLength()];

        LOGGER.trace(String.format("Reading field [%s] of QCOW2 [%s].", field, qcow2LogReference));
        if (qcow2InputStream.read(readBytes) != field.getLength()) {
            throw new IOException(String.format("Unable to read field [%s] of QCOW2 [%s].", field, qcow2LogReference));
        }

        LOGGER.trace(String.format("Read %s as field [%s] of QCOW2 [%s].", ArrayUtils.toString(readBytes), field, qcow2LogReference));
        return readBytes;
    }

    /**
     * Validates the values of the header fields {@link Qcow2HeaderField#MAGIC}, {@link Qcow2HeaderField#BACKING_FILE_OFFSET}, and {@link Qcow2HeaderField#INCOMPATIBLE_FEATURES}.
     * @param headerFieldsAndValues A map of the header fields and their values.
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @throws SecurityException If the QCOW2 does not contain the QCOW magic string or contains a backing file reference or incompatible features.
     */
    public static void validateQcow2HeaderFields(Map<String, byte[]> headerFieldsAndValues, String qcow2LogReference) throws SecurityException{
        byte[] fieldValue = headerFieldsAndValues.get(Qcow2HeaderField.MAGIC.name());
        validateQcowMagicString(fieldValue, qcow2LogReference);

        fieldValue = headerFieldsAndValues.get(Qcow2HeaderField.BACKING_FILE_OFFSET.name());
        validateAbsenceOfBackingFileReference(NumbersUtil.bytesToLong(fieldValue), qcow2LogReference);

        fieldValue = headerFieldsAndValues.get(Qcow2HeaderField.INCOMPATIBLE_FEATURES.name());
        validateAbsenceOfIncompatibleFeatures(fieldValue, qcow2LogReference);
    }

    /**
     * Verifies if the first 4 bytes of the header are the QCOW magic string. Throws an exception if not.
     * @param headerMagicString The first 4 bytes of the header.
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @throws SecurityException If the header's magic string is not the QCOW magic string.
     */
    private static void validateQcowMagicString(byte[] headerMagicString, String qcow2LogReference) throws SecurityException {
        LOGGER.debug(String.format("Verifying if [%s] has a valid QCOW magic string.", qcow2LogReference));

        if (!Arrays.equals(QCOW_MAGIC_STRING, headerMagicString)) {
            throw new SecurityException(String.format("[%s] is not a valid QCOW2 because its first 4 bytes are not the QCOW magic string.", qcow2LogReference));
        }

        LOGGER.debug(String.format("[%s] has a valid QCOW magic string.", qcow2LogReference));
    }

    /**
     * Verifies if the QCOW2 has a backing file and throws an exception if so.
     * @param backingFileOffset The backing file offset value of the QCOW2 header.
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @throws SecurityException If the QCOW2 has a backing file reference.
     */
    private static void validateAbsenceOfBackingFileReference(long backingFileOffset, String qcow2LogReference) throws SecurityException {
        LOGGER.debug(String.format("Verifying if [%s] has a backing file reference.", qcow2LogReference));

        if (backingFileOffset != 0) {
            throw new SecurityException(String.format("[%s] has a backing file reference. This can be an attack to the infrastructure; therefore, we will not accept" +
                    " this QCOW2.", qcow2LogReference));
        }

        LOGGER.debug(String.format("[%s] does not have a backing file reference.", qcow2LogReference));
    }

    /**
     * Verifies if the QCOW2 has incompatible features and throw an exception if it has an external data file reference or unknown incompatible features.
     * @param incompatibleFeatures The incompatible features bytes of the QCOW2 header.
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @throws SecurityException If the QCOW2 has an external data file reference or unknown incompatible features.
     */
    private static void validateAbsenceOfIncompatibleFeatures(byte[] incompatibleFeatures, String qcow2LogReference) throws SecurityException {
        LOGGER.debug(String.format("Verifying if [%s] has incompatible features.", qcow2LogReference));

        if (NumbersUtil.bytesToLong(incompatibleFeatures) == 0) {
            LOGGER.debug(String.format("[%s] does not have incompatible features.", qcow2LogReference));
            return;
        }

        LOGGER.debug(String.format("[%s] has incompatible features.", qcow2LogReference));

        validateAbsenceOfExternalDataFileReference(incompatibleFeatures, qcow2LogReference);
        validateAbsenceOfUnknownIncompatibleFeatures(incompatibleFeatures, qcow2LogReference);
    }

    /**
     * Verifies if the QCOW2 has an external data file reference and throw an exception if so.
     * @param incompatibleFeatures The incompatible features bytes of the QCOW2 header.
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @throws SecurityException If the QCOW2 has an external data file reference.
     */
    private static void validateAbsenceOfExternalDataFileReference(byte[] incompatibleFeatures, String qcow2LogReference) throws SecurityException {
        LOGGER.debug(String.format("Verifying if [%s] has an external data file reference.", qcow2LogReference));

        if ((incompatibleFeatures[EXTERNAL_DATA_FILE_BYTE_POSITION] & EXTERNAL_DATA_FILE_BITMASK) != 0) {
            throw new SecurityException(String.format("[%s] has an external data file reference. This can be an attack to the infrastructure; therefore, we will discard" +
                    " this file.", qcow2LogReference));
        }

        LOGGER.info(String.format("[%s] does not have an external data file reference.", qcow2LogReference));
    }

    /**
     * Verifies if the QCOW2 has unknown incompatible features and throw an exception if so.
     * <br/><br/>
     * Unknown incompatible features are those with bit greater than
     * {@link Qcow2Inspector#INCOMPATIBLE_FEATURES_MAX_KNOWN_BIT}, which will be the represented by bytes in positions greater than
     * {@link Qcow2Inspector#INCOMPATIBLE_FEATURES_MAX_KNOWN_BYTE} (in Big Endian order). Therefore, we expect that those bytes are always zero. If not, an exception is thrown.
     * @param incompatibleFeatures The incompatible features bytes of the QCOW2 header.
     * @param qcow2LogReference A reference (like the filename) of the QCOW2 being unraveled to print in the logs and exceptions.
     * @throws SecurityException If the QCOW2 has unknown incompatible features.
     */
    private static void validateAbsenceOfUnknownIncompatibleFeatures(byte[] incompatibleFeatures, String qcow2LogReference) throws SecurityException {
        LOGGER.debug(String.format("Verifying if [%s] has unknown incompatible features [%s].", qcow2LogReference, ArrayUtils.toString(incompatibleFeatures)));

        for (int byteNum = incompatibleFeatures.length - 1; byteNum >= 0; byteNum--) {
            int bytePosition = incompatibleFeatures.length - 1 - byteNum;
            LOGGER.trace(String.format("Looking for unknown incompatible feature bit in position [%s].", bytePosition));

            byte bitmask = 0;
            if (byteNum == INCOMPATIBLE_FEATURES_MAX_KNOWN_BYTE) {
                bitmask = ((1 << INCOMPATIBLE_FEATURES_MAX_KNOWN_BIT) - 1);
            }

            LOGGER.trace(String.format("Bitmask for byte in position [%s] is [%s].", bytePosition, Integer.toBinaryString(bitmask)));

            int featureBit = incompatibleFeatures[bytePosition] & ~bitmask;
            if (featureBit != 0) {
                throw new SecurityException(String.format("Found unknown incompatible feature bit [%s] in byte [%s] of [%s]. This can be an attack to the infrastructure; " +
                        "therefore, we will discard this QCOW2.", featureBit, bytePosition + Qcow2HeaderField.INCOMPATIBLE_FEATURES.getOffset(), qcow2LogReference));
            }

            LOGGER.trace(String.format("Did not find unknown incompatible feature in position [%s].", bytePosition));
        }

        LOGGER.info(String.format("[%s] does not have unknown incompatible features.", qcow2LogReference));
    }

}
