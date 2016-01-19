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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.cloud.storage.Storage;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class QemuImg {

    /* The qemu-img binary. We expect this to be in $PATH */
    public String _qemuImgPath = "qemu-img";
    private int timeout;

    /* Shouldn't we have KVMPhysicalDisk and LibvirtVMDef read this? */
    public static enum PhysicalDiskFormat {
        RAW("raw"), QCOW2("qcow2"), VMDK("vmdk"), FILE("file"), RBD("rbd"), SHEEPDOG("sheepdog"), HTTP("http"), HTTPS("https"), TAR("tar"), DIR("dir");
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
                throw new NotImplementedException();
            }
        }
    }

    public QemuImg(final int timeout) {
        this.timeout = timeout;
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
    public QemuImg(final String qemuImgPath) {
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
        final Script s = new Script(_qemuImgPath, timeout);
        s.add("create");

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
     * This method calls 'qemu-img convert' and takes two objects
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
     * @return void
     */
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile, final Map<String, String> options) throws QemuImgException {
        final Script script = new Script(_qemuImgPath, timeout);
        script.add("convert");
        // autodetect source format. Sometime int he future we may teach KVMPhysicalDisk about more formats, then we can explicitly pass them if necessary
        //s.add("-f");
        //s.add(srcFile.getFormat().toString());
        script.add("-O");
        script.add(destFile.getFormat().toString());

        if (options != null && !options.isEmpty()) {
            script.add("-o");
            final StringBuffer optionsBuffer = new StringBuffer();
            for (final Map.Entry<String, String> option : options.entrySet()) {
                optionsBuffer.append(option.getKey()).append('=').append(option.getValue()).append(',');
            }
            String optionsStr = optionsBuffer.toString();
            optionsStr = optionsStr.replaceAll(",$", "");
            script.add(optionsStr);
        }

        script.add(srcFile.getFileName());
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
    public void convert(final QemuImgFile srcFile, final QemuImgFile destFile) throws QemuImgException {
        this.convert(srcFile, destFile, null);
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
        return info;
    }

    /* List, apply, create or delete snapshots in image */
    public void snapshot() throws QemuImgException {

    }

    /* Changes the backing file of an image */
    public void rebase() throws QemuImgException {

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
}
