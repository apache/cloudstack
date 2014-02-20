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

package com.cloud.utils.crypto;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.cloud.utils.crypt.RSAHelper;

public class RSAHelperTest {
    @Test
    public void testEncryptWithRSA() {
        String rsaKey =
            "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC2D2Cs0XAEqm+ajJpumIPrMpKp0CWtIW+8ZY2/MJCW"
                + "hge1eY18u9I3PPnkMVJsTOaN0wQojjw4AkKgKjNZXA9wyUq56UyN/stmipu8zifWPgxQGDRkuzzZ6buk"
                + "ef8q2Awjpo8hv5/0SRPJxQLEafESnUP+Uu/LUwk5VVC7PHzywJRUGFuzDl/uT72+6hqpL2YpC6aTl4/P"
                + "2eDvUQhCdL9dBmUSFX8ftT53W1jhsaQl7mPElVgSCtWz3IyRkogobMPrpJW/IPKEiojKIuvNoNv4CDR6" + "ybeVjHOJMb9wi62rXo+CzUsW0Y4jPOX/OykAm5vrNOhQhw0aaBcv5XVv8BRX";
        String encryptedString = RSAHelper.encryptWithSSHPublicKey(rsaKey, "content");
        assertNotNull(encryptedString);
    }

    @Test
    public void testEncryptWithDSA() {
        String dssKey =
            "ssh-dss AAAAB3NzaC1kc3MAAACBALbaewDnzZ5AcGbZno7VW1m7Si3Q+yEANXZioVupfSwOP0q9aP2iV"
                + "tyqq575JnUVZXMDR2Gr254F/qCJ0TKAvucN0gcd2XslX4jBcu1Z7s7YZf6d7fC58k0NE6/keokJNKhQO"
                + "i56iirRzSA/YFrD64mzfq6rEmai0q7GjGGP0RT1AAAAFQDO5++6JonyqnoRkV9Yl1OaEOPjVwAAAIAYA"
                + "tqtKtU/INlTIuL3wt3nyKzwPUnz3fqxP5Ger3OlRZsOahalTFt2OF5jGGmCunyBTRteOetZObr0QhUIF"
                + "4bSDr6UiYYYbH1ES0ws/t1mDIeTh3UUHV1QYACN6c07FKyKLMtB9AthiG2FMLKCEedG3NeXItuNzsuQD"
                + "+n/K1rzMAAAAIBi5SM4pFPiB7BvTZvARV56vrG5QNgWVazSwbwgl/EACiWYbRauHDUQA9f+Rq+ayWcsR"
                + "os1CD+Q81y9SmlQaZVKkSPZLxXfu5bi3s4o431xjilhZdt4vKbj2pK364IjghJPNBBfmRXzlj9awKxr/" + "UebZcBgNRyeky7VZSbbF2jQSQ==";
        String encryptedString = RSAHelper.encryptWithSSHPublicKey(dssKey, "content");
        assertNull(encryptedString);
    }
}
