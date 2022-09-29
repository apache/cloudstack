// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.qemu;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.LibvirtException;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.storage.Storage;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class QemuImg {
    private Logger logger = Logger.getLogger(this.getClass());

    public static final String BACKING_FILE = "backing_file";
    public static final String BACKING_FILE_FORMAT = "backing_file_format";
    public static final String CLUSTER_SIZE = "cluster_size";
    public static final String FILE_FORMAT = "file_format";
    public static final String IMAGE = "image";
    public static final String VIRTUAL_SIZE = "virtual_size";
    public static final String ENCRYPT_FORMAT = "encrypt.format";
    public static final String ENCRYPT_KEY_SECRET = "encrypt.key-secret";
    public static final String TARGET_ZERO_FLAG = "--target-is-zero";
    public static final long QEMU_2_10 = 2010000;

    /* The qemu-img binary. We expect this to be in $PATH */
    public String _qemuImgPath = "qemu-img";
    private String cloudQemuImgPath = "cloud-qemu-img";
    private int timeout;
    private boolean skipZero = false;
    private boolean noCache = false;
    private long version;

    private String getQemuImgPathScript = String.format("which %s >& /dev/null; " +
                    "if [ $? -gt 0 ]; then echo \"%s\"; else echo \"%s\"; fi",
            cloudQemuImgPath, _qemuImgPath, cloudQemuImgPath);

    /* Shouldn't we have KVMPhysicalDisk and LibvirtVMDef read this? */
    public static enum PhysicalDiskFormat {
        RAW("raw"), QCOW2("qcow2"), VMDK("vmdk"), FILE("file"), RBD("rbd"), SHEEPDOG("sheepdog"), HTTP("http"), HTTPS("https"), TAR("tar"), DIR("dir"), LUKS("luks");
        String format;

        private PhysicalDiskFormat(final String format) {
            this.format = format;
        }

        @Override
        public String toString() {
            return format;
        }
    }

    public static enum PreallocationType {
        Off("off"),
        Metadata("metadata"),
        Full("full");

        private final String preallocationType;

        private PreallocationType(final String preallocationType){
            this.preallocationType = preallocationType;
        }

        @Override
        public String toString(){
            return preallocationType;
        }

        public static PreallocationType getPreallocationType(final Storage.ProvisioningType provisioningType){
            switch (provisioningType){
            case THIN:
                return PreallocationType.Off;
            case SPARSE:
                return PreallocationType.Metadata;
            case FAT:
                return PreallocationType.Full;
            default:
                throw new NotImplementedException(String.format("type %s not defined as member-value of PreallocationType", provisioningType));
            }
        }
    }

    /**
     * Create a QemuImg object that supports skipping target zeroes
     * We detect this support via qemu-img help since support can
     * be backported rather than found in a specific version.
     *
     * @param timeout script timeout, default 0
     * @param skipZeroIfSupported Don't write zeroes to target device during convert, if supported by qemu-img
     * @param noCache Ensure we flush writes to target disk (useful for block device targets)
     */
    public QemuImg(final int timeout, final boolean skipZeroIfSupported, final boolean noCache) throws LibvirtException {
        if (skipZeroIfSupported) {
            final Script s = new Script(_qemuImgPath, timeout);
            s.add("--help");

            final OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            final String result = s.execute(parser);

            // Older Qemu returns output in result due to --help reporting error status
            if (result != null) {
                if (result.contains(TARGET_ZERO_FLAG)) {
                    this.skipZero = true;
                }
            } else {
                if (parser.getLines().contains(TARGET_ZERO_FLAG)) {
                    this.skipZero = true;
                }
            }
        }
        this.timeout = timeout;
        this.noCache = noCache;
        this.version = LibvirtConnection.getConnection().getVersion();
    }

    public QemuImg(final int timeout) throws LibvirtException, QemuImgException {
        this(timeout, false, false);
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    /**
     * Create a QemuImg object
     *
     *
     * @param qemuImgPath
     *            A alternative path to the qemu-img binary
     * @return void
     */
    public QemuImg(final String qemuImgPath) throws LibvirtException {
        this(0, false, false);
        _qemuImgPath = qemuImgPath;
    }

    /* These are all methods supported by the qemu-img tool */

    /* Perform a consistency check on the disk image */
    public void check(final QemuImgFile file) {

    }

    /**
     * Create a new image
     *
     * This method calls 'qemu-img create'
     *
     * @param file
     *            The file to create
     * @param backingFile
     *            A backing file if used (for example with qcow2)
     * @param options
     *            Options for the create. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @return void
     */
    public void create(final QemuImgFile file, final QemuImgFile backingFile, final Map<String, String> options) throws QemuImgException {
        create(file, backingFile, options, null);
    }

    /**
     * Create a new image
     *
     * This method calls 'qemu-img create'
     *
     * @param file
     *            The file to create
     * @param backingFile
     *            A backing file if used (for example with qcow2)
     * @param options
     *            Options for the create. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @param qemuObjects
     *            Pass list of qemu Object to create - see objects in qemu man page
     * @return void
     */
    public void create(final QemuImgFile file, final QemuImgFile backingFile, final Map<String, String> options, final List<QemuObject> qemuObjects) throws QemuImgException {
        final Script s = new Script(_qemuImgPath, timeout);
        s.add("create");

        if (this.version >= QEMU_2_10 && qemuObjects != null) {
            for (QemuObject o : qemuObjects) {
                s.add(o.toCommandFlag());
            }
        }

        if (options != null && !options.isEmpty()) {
            s.add("-o");
            final StringBuilder optionsStr = new StringBuilder();
            final Iterator<Map.Entry<String, String>> optionsIter = options.entrySet().iterator();
            while(optionsIter.hasNext()){
                final Map.Entry option = optionsIter.next();
                optionsStr.append(option.getKey()).append('=').append(option.getValue());
                if(optionsIter.hasNext()){
                    //Add "," only if there are more options
                    optionsStr.append(',');
                }
            }
            s.add(optionsStr.toString());
        }

        /*
            -b for a backing file does not show up in the docs, but it works.
            Shouldn't this be -o backing_file=filename instead?
         */
        s.add("-f");
        if (backingFile != null) {
            s.add(backingFile.getFormat().toString());
            s.add("-F");
            s.add(backingFile.getFormat().toString());
            s.add("-b");
            s.add(backingFile.getFileName());
        } else {
            s.add(file.getFormat().toString());
        }

        s.add(file.getFileName());
        if (file.getSize() != 0L) {
            s.add(Long.toString(file.getSize()));
        } else if (backingFile == null) {
            throw new QemuImgException("No size was passed, and no backing file was passed");
        }

        final String result = s.execute();
        if (result != null) {
            throw new QemuImgException(result);
        }
    }

    /**
     * Create a new image
     *
     * This method calls 'qemu-img create'
     *
     * @param file
     *            The file to create
     * @return void
     */
    public void create(final QemuImgFile file) throws QemuImgException {
        this.create(file, null, null);
    }

    /**
     * Create a new image
     *
     * This method calls 'qemu-img create'
     *
     * @param file
     *            The file to create
     * @param backingFile
     *            A backing file if used (for example with qcow2)
     * @return void
     */
    public void create(final QemuImgFile file, final QemuImgFile backingFile) throws QemuImgException {
        this.create(file, backingFile, null);
    }

    /**
     * Create a new image
     *
     * This method calls 'qemu-img create'
     *
     * @param file
     *            The file to create
     * @param options
     *            Options for the create. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @return void
     */
    public void create(final QemuImgFile file, final Map<String, String> options) throws QemuImgException {
        this.create(file, null, options);
    }

    /**
     * Convert a image from source to destination
     *
     * This method calls 'qemu-img convert' and takes five objects
     * as an argument.
     *
     *
     * @param srcFile
     *            The source file
     * @param destFile
     *            The destination file
     * @param options
     *            Options for the convert. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @param snapshotName
     *            If it is provided, convertion uses it as parameter
     * @param forceSourceFormat
     *            If true, specifies the source format in the conversion cmd
     * @return void
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile,
                        final Map<String, String> options, final String snapshotName, final boolean forceSourceFormat) throws QemuImgException, LibvirtException {
        convert(srcFile, destFile, options, null, snapshotName, forceSourceFormat);
    }

    /**
     * Convert a image from source to destination
     *
     * This method calls 'qemu-img convert' and takes five objects
     * as an argument.
     *
     *
     * @param srcFile
     *            The source file
     * @param destFile
     *            The destination file
     * @param options
     *            Options for the convert. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @param qemuObjects
     *            Pass qemu Objects to create - see objects in qemu man page
     * @param snapshotName
     *            If it is provided, convertion uses it as parameter
     * @param forceSourceFormat
     *            If true, specifies the source format in the conversion cmd
     * @return void
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile,
                        final Map<String, String> options, final List<QemuObject> qemuObjects, final String snapshotName, final boolean forceSourceFormat) throws QemuImgException {
        QemuImageOptions imageOpts = new QemuImageOptions(srcFile.getFormat(), srcFile.getFileName(), null);
        convert(srcFile, destFile, options, qemuObjects, imageOpts, snapshotName, forceSourceFormat);
    }

    /**
     * Convert a image from source to destination
     *
     * This method calls 'qemu-img convert' and takes five objects
     * as an argument.
     *
     *
     * @param srcFile
     *            The source file
     * @param destFile
     *            The destination file
     * @param options
     *            Options for the convert. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @param qemuObjects
     *            Pass qemu Objects to convert - see objects in qemu man page
     * @param srcImageOpts
     *            pass qemu --image-opts to convert
     * @param snapshotName
     *            If it is provided, convertion uses it as parameter
     * @param forceSourceFormat
     *            If true, specifies the source format in the conversion cmd
     * @return void
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile,
                        final Map<String, String> options, final List<QemuObject> qemuObjects, final QemuImageOptions srcImageOpts, final String snapshotName, final boolean forceSourceFormat) throws QemuImgException {
        Script script = new Script(_qemuImgPath, timeout);
        if (StringUtils.isNotBlank(snapshotName)) {
            String qemuPath = Script.runSimpleBashScript(getQemuImgPathScript);
            script = new Script(qemuPath, timeout);
        }

        script.add("convert");

        if (skipZero && Files.exists(Paths.get(destFile.getFileName()))) {
            script.add("-n");
            script.add(TARGET_ZERO_FLAG);
            script.add("-W");
            // with target-is-zero we skip zeros in 1M chunks for compatibility
            script.add("-S");
            script.add("1M");
        }

        script.add("-O");
        script.add(destFile.getFormat().toString());

        addScriptOptionsFromMap(options, script);
        addSnapshotToConvertCommand(srcFile.getFormat().toString(), snapshotName, forceSourceFormat, script, version);

        if (noCache) {
            script.add("-t");
            script.add("none");
        }

        if (this.version >= QEMU_2_10) {
            script.add("-U");

            if (forceSourceFormat) {
                srcImageOpts.setFormat(srcFile.getFormat());
            }
            script.add(srcImageOpts.toCommandFlag());

            if (qemuObjects != null) {
                for (QemuObject o : qemuObjects) {
                    script.add(o.toCommandFlag());
                }
            }
        } else {
            if (forceSourceFormat) {
                script.add("-f");
                script.add(srcFile.getFormat().toString());
            }
            script.add(srcFile.getFileName());
        }

        script.add(destFile.getFileName());

        final String result = script.execute();
        if (result != null) {
            throw new QemuImgException(result);
        }

        if (srcFile.getSize() < destFile.getSize()) {
            this.resize(destFile, destFile.getSize());
        }
    }

    /**
     * Qemu version 2.0.0 added (via commit <a href="https://github.com/qemu/qemu/commit/ef80654d0dc1edf2dd2a51feff8cc3e1102a6583">ef80654d0dc1edf2dd2a51feff8cc3e1102a6583</a>) the
     * flag "-l" to inform the snapshot name or ID
     */
    private static final int QEMU_VERSION_THAT_ADDS_FLAG_L_TO_CONVERT_SNAPSHOT = 2000000;

    /**
     * Adds a flag to inform snapshot name or ID on conversion. If the QEMU version is less than {@link QemuImg#QEMU_VERSION_THAT_ADDS_FLAG_L_TO_CONVERT_SNAPSHOT), adds the
     * flag "-s", otherwise, adds the flag "-l".
     */
    protected void addSnapshotToConvertCommand(String srcFormat, String snapshotName, boolean forceSourceFormat, Script script, Long qemuVersion) {
        if (StringUtils.isBlank(snapshotName)) {
            return;
        }

        if (qemuVersion >= QEMU_VERSION_THAT_ADDS_FLAG_L_TO_CONVERT_SNAPSHOT) {
            script.add("-l");
            script.add(String.format("snapshot.name=%s", snapshotName));
            return;
        }

        logger.debug(String.format("Current QEMU version [%s] does not support flag \"-l\" (added on version >= 2.0.0) to inform the snapshot name or ID on conversion."
                + " Adding the old flag \"-s\" instead.", qemuVersion));

        if (!forceSourceFormat) {
            script.add("-f");
            script.add(srcFormat);
        }

        script.add("-s");
        script.add(snapshotName);
    }

    /**
     * Convert a image from source to destination
     *
     * This method calls 'qemu-img convert' and takes four objects
     * as an argument.
     *
     *
     * @param srcFile
     *            The source file
     * @param destFile
     *            The destination file
     * @param options
     *            Options for the convert. Takes a Map<String, String> with key value
     *            pairs which are passed on to qemu-img without validation.
     * @param snapshotName
     *            If it is provided, convertion uses it as parameter
     * @return void
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile,
                        final Map<String, String> options, final String snapshotName) throws QemuImgException, LibvirtException {
        this.convert(srcFile, destFile, options, snapshotName, false);
    }

        /**
         * Convert a image from source to destination
         *
         * This method calls 'qemu-img convert' and takes two objects
         * as an argument.
         *
         *
         * @param srcFile
         *            The source file
         * @param destFile
         *            The destination file
         * @return void
         */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile) throws QemuImgException, LibvirtException {
        this.convert(srcFile, destFile, null, null);
    }

    /**
     * Convert a image from source to destination
     *
     * This method calls 'qemu-img convert' and takes three objects
     * as an argument.
     *
     *
     * @param srcFile
     *            The source file
     * @param destFile
     *            The destination file
     * @param forceSourceFormat
     *            If true, specifies the source format in the conversion cmd
     * @return void
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile, final boolean forceSourceFormat) throws QemuImgException, LibvirtException {
        this.convert(srcFile, destFile, null, null, forceSourceFormat);
    }

    /**
     * Convert a image from source to destination
     *
     * This method calls 'qemu-img convert' and takes three objects
     * as an argument.
     *
     *
     * @param srcFile
     *            The source file
     * @param destFile
     *            The destination file
     * @param snapshotName
     *            The snapshot name
     * @return void
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile, String snapshotName) throws QemuImgException, LibvirtException {
        this.convert(srcFile, destFile, null, snapshotName);
    }

    /**
     * Commit the changes recorded in the file in its base image.
     *
     * This method calls 'qemu-img commit' and takes one object as
     * an argument
     *
     * @param file
     *            The file of which changes have to be committed
     * @return void
     */
    public void commit(final QemuImgFile file) throws QemuImgException {

    }

    /**
     * Execute qemu-img info for the given file
     *
     * Qemu-img returns human readable output, but this method does it's best
     * to turn that into machine readeable data.
     *
     * Spaces in keys are replaced by underscores (_).
     * Sizes (virtual_size and disk_size) are returned in bytes
     * Paths (image and backing_file) are the absolute path to the file
     *
     * @param file
     *            A QemuImgFile object containing the file to get the information from
     * @return A HashMap with String key-value information as returned by 'qemu-img info'
     */
    public Map<String, String> info(final QemuImgFile file) throws QemuImgException {
        final Script s = new Script(_qemuImgPath);
        s.add("info");
        if (this.version >= QEMU_2_10) {
            s.add("-U");
        }
        s.add(file.getFileName());

        final OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        final String result = s.execute(parser);
        if (result != null) {
            throw new QemuImgException(result);
        }

        final HashMap<String, String> info = new HashMap<String, String>();
        final String[] outputBuffer = parser.getLines().trim().split("\n");
        for (int i = 0; i < outputBuffer.length; i++) {
            final String[] lineBuffer = outputBuffer[i].split(":", 2);
            if (lineBuffer.length == 2) {
                final String key = lineBuffer[0].trim().replace(" ", "_");
                String value = null;

                if (key.equals("virtual_size")) {
                    value = lineBuffer[1].trim().replaceAll("^.*\\(([0-9]+).*$", "$1");
                } else {
                    value = lineBuffer[1].trim();
                }

                info.put(key, value);
            }
        }

        // set some missing attributes in passed file, if found
        if (info.containsKey(VIRTUAL_SIZE) && file.getSize() == 0L) {
            file.setSize(Long.parseLong(info.get(VIRTUAL_SIZE)));
        }

        if (info.containsKey(FILE_FORMAT) && file.getFormat() == null) {
            file.setFormat(PhysicalDiskFormat.valueOf(info.get(FILE_FORMAT).toUpperCase()));
        }

        return info;
    }

    /* create snapshots in image */
    public void snapshot(final QemuImageOptions srcImageOpts, final String snapshotName, final List<QemuObject> qemuObjects) throws QemuImgException {
        final Script s = new Script(_qemuImgPath, timeout);
        s.add("snapshot");
        s.add("-c");
        s.add(snapshotName);

        for (QemuObject o : qemuObjects) {
            s.add(o.toCommandFlag());
        }

        s.add(srcImageOpts.toCommandFlag());

        final String result = s.execute();
        if (result != null) {
            throw new QemuImgException(result);
        }
    }

    /* delete snapshots in image */
    public void deleteSnapshot(final QemuImageOptions srcImageOpts, final String snapshotName, final List<QemuObject> qemuObjects) throws QemuImgException {
        final Script s = new Script(_qemuImgPath, timeout);
        s.add("snapshot");
        s.add("-d");
        s.add(snapshotName);

        for (QemuObject o : qemuObjects) {
            s.add(o.toCommandFlag());
        }

        s.add(srcImageOpts.toCommandFlag());

        final String result = s.execute();
        if (result != null) {
            // support idempotent delete calls, if no snapshot exists we are good.
            if (result.contains("snapshot not found") || result.contains("Can't find the snapshot")) {
                return;
            }
            throw new QemuImgException(result);
        }
    }

    private void addScriptOptionsFromMap(Map<String, String> options, Script s) {
        if (options != null && !options.isEmpty()) {
            s.add("-o");
            final StringBuffer optionsBuffer = new StringBuffer();
            for (final Map.Entry<String, String> option : options.entrySet()) {
                optionsBuffer.append(option.getKey()).append('=').append(option.getValue()).append(',');
            }
            String optionsStr = optionsBuffer.toString();
            optionsStr = optionsStr.replaceAll(",$", "");
            s.add(optionsStr);
        }
    }

    /* Changes the backing file of an image */
    public void rebase(final QemuImgFile file, final QemuImgFile backingFile, final String backingFileFormat, final boolean secure) throws QemuImgException {
        if (backingFile == null) {
            throw new QemuImgException("No backing file was passed");
        }
        final Script s = new Script(_qemuImgPath, timeout);
        s.add("rebase");
        if (! secure) {
            s.add("-u");
        }
        s.add("-F");
        if (backingFileFormat != null) {
            s.add(backingFileFormat);
        } else {
            s.add(backingFile.getFormat().toString());
        }
        s.add("-b");
        s.add(backingFile.getFileName());

        s.add(file.getFileName());
        final String result = s.execute();
        if (result != null) {
            throw new QemuImgException(result);
        }
    }

    /**
     * Resize an image
     *
     * This method simple calls 'qemu-img resize'.
     * A negative size value will get prefixed with - and a positive with +
     *
     * Sizes are in bytes and will be passed on that way
     *
     * @param file
     *            The file to resize
     * @param size
     *            The new size
     * @param delta
     *            Flag if the new size is a delta
     */
    public void resize(final QemuImgFile file, final long size, final boolean delta) throws QemuImgException {
        String newSize = null;

        if (size == 0) {
            throw new QemuImgException("size should never be exactly zero");
        }

        if (delta) {
            if (size > 0) {
                newSize = "+" + Long.toString(size);
            } else {
                newSize = Long.toString(size);
            }
        } else {
            if (size <= 0) {
                throw new QemuImgException("size should not be negative if 'delta' is false!");
            }
            newSize = Long.toString(size);
        }

        final Script s = new Script(_qemuImgPath);
        s.add("resize");
        s.add(file.getFileName());
        s.add(newSize);
        s.execute();
    }

    /**
     * Resize an image, new style flags/options
     *
     * @param imageOptions
     *         Qemu style image options for the image to resize
     * @param qemuObjects
     *         Qemu style options (e.g. for passing secrets)
     * @param size
     *      The absolute final size of the image
     */
    public void resize(final QemuImageOptions imageOptions, final List<QemuObject> qemuObjects, final long size) throws QemuImgException {
        final Script s = new Script(_qemuImgPath);
        s.add("resize");

        for (QemuObject o : qemuObjects) {
            s.add(o.toCommandFlag());
        }

        s.add(imageOptions.toCommandFlag());
        s.add(Long.toString(size));

        final String result = s.execute();
        if (result != null) {
            throw new QemuImgException(result);
        }
    }

    /**
     * Resize an image
     *
     * This method simple calls 'qemu-img resize'.
     * A negative size value will get prefixed with - and a positive with +
     *
     * Sizes are in bytes and will be passed on that way
     *
     * @param file
     *            The file to resize
     * @param size
     *            The new size
     */
    public void resize(final QemuImgFile file, final long size) throws QemuImgException {
        this.resize(file, size, false);
    }

    /**
     * Does qemu-img support --target-is-zero
     * @return boolean
     */
    public boolean supportsSkipZeros() {
        return this.skipZero;
    }

    public void setSkipZero(boolean skipZero) {
        this.skipZero = skipZero;
    }

    public boolean supportsImageFormat(QemuImg.PhysicalDiskFormat format) {
        final Script s = new Script(_qemuImgPath, timeout);
        s.add("--help");

        final OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = s.execute(parser);
        String output = parser.getLines();

        // Older Qemu returns output in result due to --help reporting error status
        if (result != null) {
           output = result;
        }

        return helpSupportsImageFormat(output, format);
    }

    protected static boolean helpSupportsImageFormat(String text, QemuImg.PhysicalDiskFormat format) {
        Pattern pattern = Pattern.compile("Supported\\sformats:[a-zA-Z0-9-_\\s]*?\\b" + format + "\\b", CASE_INSENSITIVE);
        return pattern.matcher(text).find();
    }
}
