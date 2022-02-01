//
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
//

package com.cloud.utils;

import java.net.InetAddress;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import org.apache.cloudstack.utils.imagestore.ImageStoreUtil;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import com.google.common.collect.ImmutableSet;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PowerMockIgnore({"javax.xml.*", "org.apache.xerces.*", "org.xml.*", "org.w3c.*"})
public class UriUtilsParametrizedTest {
    @FunctionalInterface
    public interface ThrowingBlock<E extends Exception> {
        void execute() throws E;
    }

    private static final Set<String> COMMPRESSION_FORMATS = ImmutableSet.of("",".zip", ".bz2", ".gz");
    private static final Set<String> ILLEGAL_COMMPRESSION_FORMATS = ImmutableSet.of(".7z", ".xz");
    private final static Set<String> FORMATS = ImmutableSet.of(
            "vhd",
            "vhdx",
            "qcow2",
            "ova",
            "tar",
            "raw",
            "img",
            "vmdk",
            "iso"
    );
    private final static Set<String> METALINK_FORMATS = ImmutableSet.of(
            "qcow2",
            "ova",
            "iso"
    );

    private final static Set<String> ILLEGAL_EXTENSIONS = ImmutableSet.of(
            "rar",
            "supernova",
            "straw",
            "miso",
            "tartar"
    );

    private String format;
    private String url;
    private boolean expectSuccess;
    private boolean isMetalink;
    private boolean isValidCompression;

    private <E extends Exception> void assertThrows(ThrowingBlock<E> consumer, Class<E> exceptionClass) {
        try {
            consumer.execute();
            Assert.fail("Expected " + exceptionClass.getName());
        } catch(Exception e) {
            Assert.assertThat(e, new IsInstanceOf(exceptionClass));
        }
    }

    public UriUtilsParametrizedTest(String format, String url, boolean expectSuccess, boolean isMetalink, boolean isValidCompression) {
        this.format = format;
        this.url = url;
        this.expectSuccess = expectSuccess;
        this.isMetalink = isMetalink;
        this.isValidCompression = isValidCompression;
    }

    @Parameterized.Parameters(name = "{index}: validateUrl(\"{0}\", \"{1}\") = {2}")
    public static Collection<Object[]> data() {
        String validBaseUri = "http://cloudstack.apache.org/images/image.";

        LinkedList<Object[]> data = new LinkedList<>();

        for (String format : FORMATS) {
            if (format.equals("img")) continue;

            final String realFormat = format;

            for (String extension : FORMATS) {
                boolean expectSuccess = format.equals(extension.replace("img", "raw"));
                if (format.equals("qcow2") && extension.equals("img")) {
                    expectSuccess = true;
                }

                for (String commpressionFormat : COMMPRESSION_FORMATS) {
                    final String url = validBaseUri + extension + commpressionFormat;
                    data.add(new Object[]{realFormat, url, expectSuccess, false, commpressionFormat.length() > 0});
                }

                for (String commpressionFormat : ILLEGAL_COMMPRESSION_FORMATS) {
                    final String url = validBaseUri + extension + commpressionFormat;
                    data.add(new Object[]{realFormat, url, false, false, false});
                }
            }

            for (String illegalExtension : ILLEGAL_EXTENSIONS) {
                data.add(new Object[]{format, validBaseUri + illegalExtension, false, false, false});

                for (String commpressionFormat : COMMPRESSION_FORMATS) {
                    final String url = validBaseUri + illegalExtension + commpressionFormat;
                    data.add(new Object[]{realFormat, url, false, false, commpressionFormat.length() > 0});
                }

                for (String commpressionFormat : ILLEGAL_COMMPRESSION_FORMATS) {
                    final String url = validBaseUri + illegalExtension + commpressionFormat;
                    data.add(new Object[]{realFormat, url, false, false, false});
                }
            }

            data.add(new Object[]{realFormat, validBaseUri + "metalink", METALINK_FORMATS.contains(realFormat), true, false});

        }

        return data;
    }

    @Test
    @PrepareForTest({UriUtils.class})

    public void validateUrl() throws Exception {

        InetAddress inetAddressMock = Mockito.mock(InetAddress.class);

        PowerMockito.mockStatic(InetAddress.class);
        PowerMockito.when(InetAddress.getByName(Mockito.anyString())).thenReturn(inetAddressMock);

        if (expectSuccess) {
            UriUtils.validateUrl(format, url);
        } else {
            assertThrows(() -> UriUtils.validateUrl(format, url), IllegalArgumentException.class);
        }

        PowerMockito.verifyStatic(InetAddress.class);
        InetAddress.getByName(Mockito.anyString());

        Mockito.verify(inetAddressMock).isAnyLocalAddress();
        Mockito.verify(inetAddressMock).isLinkLocalAddress();
        Mockito.verify(inetAddressMock).isLoopbackAddress();
        Mockito.verify(inetAddressMock).isMulticastAddress();

    }

    @Test
    public void isCorrectExtension() {
        Assert.assertThat(ImageStoreUtil.isCorrectExtension(url, format), Matchers.is(expectSuccess && !isMetalink));
    }

    @Test
    public void isCompressedExtension() {
        Assert.assertThat(ImageStoreUtil.isCompressedExtension(url), Matchers.is(isValidCompression));
    }
}
