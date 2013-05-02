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
package com.cloud.vm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

/**
 * ConsoleProxyVO domain object
 */

@Entity
@Table(name = "console_proxy")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue(value = "ConsoleProxy")
public class ConsoleProxyVO extends VMInstanceVO implements ConsoleProxy {
    public static final String keyContent =
            "-----BEGIN PRIVATE KEY-----\n" +
                    "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCDT9AtEfs+s/I8QXp6rrCw0iNJ\n" +
                    "0+GgsybNHheU+JpL39LMTZykCrZhZnyDvwdxCoOfE38Sa32baHKNds+y2SHnMNsOkw8OcNucHEBX\n" +
                    "1FIpOBGph9D6xC+umx9od6xMWETUv7j6h2u+WC3OhBM8fHCBqIiAol31/IkcqDxxsHlQ8S/oCfTl\n" +
                    "XJUY6Yn628OA1XijKdRnadV0hZ829cv/PZKljjwQUTyrd0KHQeksBH+YAYSo2JUl8ekNLsOi8/cP\n" +
                    "tfojnltzRI1GXi0ZONs8VnDzJ0a2gqZY+uxlz+CGbLnGnlN4j9cBpE+MfUE+35Dq121sTpsSgF85\n" +
                    "Mz+pVhn2S633AgMBAAECggEAH/Szd9RxbVADenCA6wxKSa3KErRyq1YN8ksJeCKMAj0FIt0caruE\n" +
                    "qO11DebWW8cwQu1Otl/cYI6pmg24/BBldMrp9IELX/tNJo+lhPpRyGAxxC0eSXinFfoASb8d+jJd\n" +
                    "Bd1mmemM6fSxqRlxSP4LrzIhjhR1g2CiyYuTsiM9UtoVKGyHwe7KfFwirUOJo3Mr18zUVNm7YqY4\n" +
                    "IVhOSq59zkH3ULBlYq4bG50jpxa5mNSCZ7IpafPY/kE/CbR+FWNt30+rk69T+qb5abg6+XGm+OAm\n" +
                    "bnQ18yZEqX6nJLk7Ch0cfA5orGgrTMOrM71wK7tBBDQ308kOxDGebx6j0qD36QKBgQDTRDr8kuhA\n" +
                    "9sUyKr9vk2DQCMpNvEeiwI3JRMqmmxpNAtg01aJ3Ya57vX5Fc+zcuV87kP6FM1xgpHQvnw5LWo2J\n" +
                    "s7ANwQcP8ricEW5zkZhSjI4ssMeAubmsHOloGxmLFYZqwx0JI7CWViGTLMcUlqKblmHcjeQDeDfP\n" +
                    "P1TaCItFmwKBgQCfHZwVvIcaDs5vxVpZ4ftvflIrW8qq0uOVK6QIf9A/YTGhCXl2qxxTg2A6+0rg\n" +
                    "ZqI7zKzUDxIbVv0KlgCbpHDC9d5+sdtDB3wW2pimuJ3p1z4/RHb4n/lDwXCACZl1S5l24yXX2pFZ\n" +
                    "wdPCXmy5PYkHMssFLNhI24pprUIQs66M1QKBgQDQwjAjWisD3pRXESSfZRsaFkWJcM28hdbVFhPF\n" +
                    "c6gWhwQLmTp0CuL2RPXcPUPFi6sN2iWWi3zxxi9Eyz+9uBn6AsOpo56N5MME/LiOnETO9TKb+Ib6\n" +
                    "rQtKhjshcv3XkIqFPo2XdVvOAgglPO7vajX91iiXXuH7h7RmJud6l0y/lwKBgE+bi90gLuPtpoEr\n" +
                    "VzIDKz40ED5bNYHT80NNy0rpT7J2GVN9nwStRYXPBBVeZq7xCpgqpgmO5LtDAWULeZBlbHlOdBwl\n" +
                    "NhNKKl5wzdEUKwW0yBL1WSS5PQgWPwgARYP25/ggW22sj+49WIo1neXsEKPGWObk8e050f1fTt92\n" +
                    "Vo1lAoGAb1gCoyBCzvi7sqFxm4V5oapnJeiQQJFjhoYWqGa26rQ+AvXXNuBcigIeDXNJPctSF0Uc\n" +
                    "p11KbbCgiruBbckvM1vGsk6Sx4leRk+IFHRpJktFUek4o0eUg0shOsyyvyet48Dfg0a8FvcxROs0\n" +
                    "gD+IYds5doiob/hcm1hnNB/3vk4=\n" +
                    "-----END PRIVATE KEY-----\n";

    public static final String certContent =
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIIFZTCCBE2gAwIBAgIHKBCduBUoKDANBgkqhkiG9w0BAQUFADCByjELMAkGA1UE\n" +
                    "BhMCVVMxEDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxGjAY\n" +
                    "BgNVBAoTEUdvRGFkZHkuY29tLCBJbmMuMTMwMQYDVQQLEypodHRwOi8vY2VydGlm\n" +
                    "aWNhdGVzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkxMDAuBgNVBAMTJ0dvIERhZGR5\n" +
                    "IFNlY3VyZSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTERMA8GA1UEBRMIMDc5Njky\n" +
                    "ODcwHhcNMTIwMjAzMDMzMDQwWhcNMTcwMjA3MDUxMTIzWjBZMRkwFwYDVQQKDBAq\n" +
                    "LnJlYWxob3N0aXAuY29tMSEwHwYDVQQLDBhEb21haW4gQ29udHJvbCBWYWxpZGF0\n" +
                    "ZWQxGTAXBgNVBAMMECoucmVhbGhvc3RpcC5jb20wggEiMA0GCSqGSIb3DQEBAQUA\n" +
                    "A4IBDwAwggEKAoIBAQCDT9AtEfs+s/I8QXp6rrCw0iNJ0+GgsybNHheU+JpL39LM\n" +
                    "TZykCrZhZnyDvwdxCoOfE38Sa32baHKNds+y2SHnMNsOkw8OcNucHEBX1FIpOBGp\n" +
                    "h9D6xC+umx9od6xMWETUv7j6h2u+WC3OhBM8fHCBqIiAol31/IkcqDxxsHlQ8S/o\n" +
                    "CfTlXJUY6Yn628OA1XijKdRnadV0hZ829cv/PZKljjwQUTyrd0KHQeksBH+YAYSo\n" +
                    "2JUl8ekNLsOi8/cPtfojnltzRI1GXi0ZONs8VnDzJ0a2gqZY+uxlz+CGbLnGnlN4\n" +
                    "j9cBpE+MfUE+35Dq121sTpsSgF85Mz+pVhn2S633AgMBAAGjggG+MIIBujAPBgNV\n" +
                    "HRMBAf8EBTADAQEAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAOBgNV\n" +
                    "HQ8BAf8EBAMCBaAwMwYDVR0fBCwwKjAooCagJIYiaHR0cDovL2NybC5nb2RhZGR5\n" +
                    "LmNvbS9nZHMxLTY0LmNybDBTBgNVHSAETDBKMEgGC2CGSAGG/W0BBxcBMDkwNwYI\n" +
                    "KwYBBQUHAgEWK2h0dHA6Ly9jZXJ0aWZpY2F0ZXMuZ29kYWRkeS5jb20vcmVwb3Np\n" +
                    "dG9yeS8wgYAGCCsGAQUFBwEBBHQwcjAkBggrBgEFBQcwAYYYaHR0cDovL29jc3Au\n" +
                    "Z29kYWRkeS5jb20vMEoGCCsGAQUFBzAChj5odHRwOi8vY2VydGlmaWNhdGVzLmdv\n" +
                    "ZGFkZHkuY29tL3JlcG9zaXRvcnkvZ2RfaW50ZXJtZWRpYXRlLmNydDAfBgNVHSME\n" +
                    "GDAWgBT9rGEyk2xF1uLuhV+auud2mWjM5zArBgNVHREEJDAighAqLnJlYWxob3N0\n" +
                    "aXAuY29tgg5yZWFsaG9zdGlwLmNvbTAdBgNVHQ4EFgQUZyJz9/QLy5TWIIscTXID\n" +
                    "E8Xk47YwDQYJKoZIhvcNAQEFBQADggEBAKiUV3KK16mP0NpS92fmQkCLqm+qUWyN\n" +
                    "BfBVgf9/M5pcT8EiTZlS5nAtzAE/eRpBeR3ubLlaAogj4rdH7YYVJcDDLLoB2qM3\n" +
                    "qeCHu8LFoblkb93UuFDWqRaVPmMlJRnhsRkL1oa2gM2hwQTkBDkP7w5FG1BELCgl\n" +
                    "gZI2ij2yxjge6pOEwSyZCzzbCcg9pN+dNrYyGEtB4k+BBnPA3N4r14CWbk+uxjrQ\n" +
                    "6j2Ip+b7wOc5IuMEMl8xwTyjuX3lsLbAZyFI9RCyofwA9NqIZ1GeB6Zd196rubQp\n" +
                    "93cmBqGGjZUs3wMrGlm7xdjlX6GQ9UvmvkMub9+lL99A5W50QgCmFeI=\n" +
                    "-----END CERTIFICATE-----\n";

    public static final String rootCa =
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIIE3jCCA8agAwIBAgICAwEwDQYJKoZIhvcNAQEFBQAwYzELMAkGA1UEBhMCVVMx\n" +
                    "ITAfBgNVBAoTGFRoZSBHbyBEYWRkeSBHcm91cCwgSW5jLjExMC8GA1UECxMoR28g\n" +
                    "RGFkZHkgQ2xhc3MgMiBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTAeFw0wNjExMTYw\n" +
                    "MTU0MzdaFw0yNjExMTYwMTU0MzdaMIHKMQswCQYDVQQGEwJVUzEQMA4GA1UECBMH\n" +
                    "QXJpem9uYTETMBEGA1UEBxMKU2NvdHRzZGFsZTEaMBgGA1UEChMRR29EYWRkeS5j\n" +
                    "b20sIEluYy4xMzAxBgNVBAsTKmh0dHA6Ly9jZXJ0aWZpY2F0ZXMuZ29kYWRkeS5j\n" +
                    "b20vcmVwb3NpdG9yeTEwMC4GA1UEAxMnR28gRGFkZHkgU2VjdXJlIENlcnRpZmlj\n" +
                    "YXRpb24gQXV0aG9yaXR5MREwDwYDVQQFEwgwNzk2OTI4NzCCASIwDQYJKoZIhvcN\n" +
                    "AQEBBQADggEPADCCAQoCggEBAMQt1RWMnCZM7DI161+4WQFapmGBWTtwY6vj3D3H\n" +
                    "KrjJM9N55DrtPDAjhI6zMBS2sofDPZVUBJ7fmd0LJR4h3mUpfjWoqVTr9vcyOdQm\n" +
                    "VZWt7/v+WIbXnvQAjYwqDL1CBM6nPwT27oDyqu9SoWlm2r4arV3aLGbqGmu75RpR\n" +
                    "SgAvSMeYddi5Kcju+GZtCpyz8/x4fKL4o/K1w/O5epHBp+YlLpyo7RJlbmr2EkRT\n" +
                    "cDCVw5wrWCs9CHRK8r5RsL+H0EwnWGu1NcWdrxcx+AuP7q2BNgWJCJjPOq8lh8BJ\n" +
                    "6qf9Z/dFjpfMFDniNoW1fho3/Rb2cRGadDAW/hOUoz+EDU8CAwEAAaOCATIwggEu\n" +
                    "MB0GA1UdDgQWBBT9rGEyk2xF1uLuhV+auud2mWjM5zAfBgNVHSMEGDAWgBTSxLDS\n" +
                    "kdRMEXGzYcs9of7dqGrU4zASBgNVHRMBAf8ECDAGAQH/AgEAMDMGCCsGAQUFBwEB\n" +
                    "BCcwJTAjBggrBgEFBQcwAYYXaHR0cDovL29jc3AuZ29kYWRkeS5jb20wRgYDVR0f\n" +
                    "BD8wPTA7oDmgN4Y1aHR0cDovL2NlcnRpZmljYXRlcy5nb2RhZGR5LmNvbS9yZXBv\n" +
                    "c2l0b3J5L2dkcm9vdC5jcmwwSwYDVR0gBEQwQjBABgRVHSAAMDgwNgYIKwYBBQUH\n" +
                    "AgEWKmh0dHA6Ly9jZXJ0aWZpY2F0ZXMuZ29kYWRkeS5jb20vcmVwb3NpdG9yeTAO\n" +
                    "BgNVHQ8BAf8EBAMCAQYwDQYJKoZIhvcNAQEFBQADggEBANKGwOy9+aG2Z+5mC6IG\n" +
                    "OgRQjhVyrEp0lVPLN8tESe8HkGsz2ZbwlFalEzAFPIUyIXvJxwqoJKSQ3kbTJSMU\n" +
                    "A2fCENZvD117esyfxVgqwcSeIaha86ykRvOe5GPLL5CkKSkB2XIsKd83ASe8T+5o\n" +
                    "0yGPwLPk9Qnt0hCqU7S+8MxZC9Y7lhyVJEnfzuz9p0iRFEUOOjZv2kWzRaJBydTX\n" +
                    "RE4+uXR21aITVSzGh6O1mawGhId/dQb8vxRMDsxuxN89txJx9OjxUUAiKEngHUuH\n" +
                    "qDTMBqLdElrRhjZkAzVvb3du6/KFUJheqwNTrZEjYx8WnM25sgVjOuH0aBsXBTWV\n" +
                    "U+4=\n" +
                    "-----END CERTIFICATE-----\n" +
                    "-----BEGIN CERTIFICATE-----\n" +
                    "MIIE+zCCBGSgAwIBAgICAQ0wDQYJKoZIhvcNAQEFBQAwgbsxJDAiBgNVBAcTG1Zh\n" +
                    "bGlDZXJ0IFZhbGlkYXRpb24gTmV0d29yazEXMBUGA1UEChMOVmFsaUNlcnQsIElu\n" +
                    "Yy4xNTAzBgNVBAsTLFZhbGlDZXJ0IENsYXNzIDIgUG9saWN5IFZhbGlkYXRpb24g\n" +
                    "QXV0aG9yaXR5MSEwHwYDVQQDExhodHRwOi8vd3d3LnZhbGljZXJ0LmNvbS8xIDAe\n" +
                    "BgkqhkiG9w0BCQEWEWluZm9AdmFsaWNlcnQuY29tMB4XDTA0MDYyOTE3MDYyMFoX\n" +
                    "DTI0MDYyOTE3MDYyMFowYzELMAkGA1UEBhMCVVMxITAfBgNVBAoTGFRoZSBHbyBE\n" +
                    "YWRkeSBHcm91cCwgSW5jLjExMC8GA1UECxMoR28gRGFkZHkgQ2xhc3MgMiBDZXJ0\n" +
                    "aWZpY2F0aW9uIEF1dGhvcml0eTCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgC\n" +
                    "ggEBAN6d1+pXGEmhW+vXX0iG6r7d/+TvZxz0ZWizV3GgXne77ZtJ6XCAPVYYYwhv\n" +
                    "2vLM0D9/AlQiVBDYsoHUwHU9S3/Hd8M+eKsaA7Ugay9qK7HFiH7Eux6wwdhFJ2+q\n" +
                    "N1j3hybX2C32qRe3H3I2TqYXP2WYktsqbl2i/ojgC95/5Y0V4evLOtXiEqITLdiO\n" +
                    "r18SPaAIBQi2XKVlOARFmR6jYGB0xUGlcmIbYsUfb18aQr4CUWWoriMYavx4A6lN\n" +
                    "f4DD+qta/KFApMoZFv6yyO9ecw3ud72a9nmYvLEHZ6IVDd2gWMZEewo+YihfukEH\n" +
                    "U1jPEX44dMX4/7VpkI+EdOqXG68CAQOjggHhMIIB3TAdBgNVHQ4EFgQU0sSw0pHU\n" +
                    "TBFxs2HLPaH+3ahq1OMwgdIGA1UdIwSByjCBx6GBwaSBvjCBuzEkMCIGA1UEBxMb\n" +
                    "VmFsaUNlcnQgVmFsaWRhdGlvbiBOZXR3b3JrMRcwFQYDVQQKEw5WYWxpQ2VydCwg\n" +
                    "SW5jLjE1MDMGA1UECxMsVmFsaUNlcnQgQ2xhc3MgMiBQb2xpY3kgVmFsaWRhdGlv\n" +
                    "biBBdXRob3JpdHkxITAfBgNVBAMTGGh0dHA6Ly93d3cudmFsaWNlcnQuY29tLzEg\n" +
                    "MB4GCSqGSIb3DQEJARYRaW5mb0B2YWxpY2VydC5jb22CAQEwDwYDVR0TAQH/BAUw\n" +
                    "AwEB/zAzBggrBgEFBQcBAQQnMCUwIwYIKwYBBQUHMAGGF2h0dHA6Ly9vY3NwLmdv\n" +
                    "ZGFkZHkuY29tMEQGA1UdHwQ9MDswOaA3oDWGM2h0dHA6Ly9jZXJ0aWZpY2F0ZXMu\n" +
                    "Z29kYWRkeS5jb20vcmVwb3NpdG9yeS9yb290LmNybDBLBgNVHSAERDBCMEAGBFUd\n" +
                    "IAAwODA2BggrBgEFBQcCARYqaHR0cDovL2NlcnRpZmljYXRlcy5nb2RhZGR5LmNv\n" +
                    "bS9yZXBvc2l0b3J5MA4GA1UdDwEB/wQEAwIBBjANBgkqhkiG9w0BAQUFAAOBgQC1\n" +
                    "QPmnHfbq/qQaQlpE9xXUhUaJwL6e4+PrxeNYiY+Sn1eocSxI0YGyeR+sBjUZsE4O\n" +
                    "WBsUs5iB0QQeyAfJg594RAoYC5jcdnplDQ1tgMQLARzLrUc+cb53S8wGd9D0Vmsf\n" +
                    "SxOaFIqII6hR8INMqzW/Rn453HWkrugp++85j09VZw==\n" +
                    "-----END CERTIFICATE-----\n" +
                    "-----BEGIN CERTIFICATE-----\n" +
                    "MIIC5zCCAlACAQEwDQYJKoZIhvcNAQEFBQAwgbsxJDAiBgNVBAcTG1ZhbGlDZXJ0\n" +
                    "IFZhbGlkYXRpb24gTmV0d29yazEXMBUGA1UEChMOVmFsaUNlcnQsIEluYy4xNTAz\n" +
                    "BgNVBAsTLFZhbGlDZXJ0IENsYXNzIDIgUG9saWN5IFZhbGlkYXRpb24gQXV0aG9y\n" +
                    "aXR5MSEwHwYDVQQDExhodHRwOi8vd3d3LnZhbGljZXJ0LmNvbS8xIDAeBgkqhkiG\n" +
                    "9w0BCQEWEWluZm9AdmFsaWNlcnQuY29tMB4XDTk5MDYyNjAwMTk1NFoXDTE5MDYy\n" +
                    "NjAwMTk1NFowgbsxJDAiBgNVBAcTG1ZhbGlDZXJ0IFZhbGlkYXRpb24gTmV0d29y\n" +
                    "azEXMBUGA1UEChMOVmFsaUNlcnQsIEluYy4xNTAzBgNVBAsTLFZhbGlDZXJ0IENs\n" +
                    "YXNzIDIgUG9saWN5IFZhbGlkYXRpb24gQXV0aG9yaXR5MSEwHwYDVQQDExhodHRw\n" +
                    "Oi8vd3d3LnZhbGljZXJ0LmNvbS8xIDAeBgkqhkiG9w0BCQEWEWluZm9AdmFsaWNl\n" +
                    "cnQuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDOOnHK5avIWZJV16vY\n" +
                    "dA757tn2VUdZZUcOBVXc65g2PFxTXdMwzzjsvUGJ7SVCCSRrCl6zfN1SLUzm1NZ9\n" +
                    "WlmpZdRJEy0kTRxQb7XBhVQ7/nHk01xC+YDgkRoKWzk2Z/M/VXwbP7RfZHM047QS\n" +
                    "v4dk+NoS/zcnwbNDu+97bi5p9wIDAQABMA0GCSqGSIb3DQEBBQUAA4GBADt/UG9v\n" +
                    "UJSZSWI4OB9L+KXIPqeCgfYrx+jFzug6EILLGACOTb2oWH+heQC1u+mNr0HZDzTu\n" +
                    "IYEZoDJJKPTEjlbVUjP9UNV+mWwD5MlM/Mtsq2azSiGM5bUMMj4QssxsodyamEwC\n" +
                    "W/POuZ6lcg5Ktz885hZo+L7tdEy8W9ViH0Pd\n" +
                    "-----END CERTIFICATE-----\n";    

    @Column(name = "public_ip_address", nullable = false)
    private String publicIpAddress;

    @Column(name = "public_mac_address", nullable = false)
    private String publicMacAddress;

    @Column(name = "public_netmask", nullable = false)
    private String publicNetmask;

    @Column(name = "active_session", updatable = true, nullable = false)
    private int activeSession;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", updatable = true, nullable = true)
    private Date lastUpdateTime;

    @Column(name = "session_details", updatable = true, nullable = true)
    private byte[] sessionDetails;

    @Transient
    private boolean sslEnabled = false;

    @Transient
    private int port;

    /**
     * Correct constructor to use.
     * 
     */
    public ConsoleProxyVO(long id, long serviceOfferingId, String name, long templateId, HypervisorType hypervisorType, long guestOSId, long dataCenterId, long domainId, long accountId,
            int activeSession, boolean haEnabled) {
        super(id, serviceOfferingId, name, name, Type.ConsoleProxy, templateId, hypervisorType, guestOSId, domainId, accountId, haEnabled);
        this.activeSession = activeSession;
    }

    protected ConsoleProxyVO() {
        super();
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    public void setActiveSession(int activeSession) {
        this.activeSession = activeSession;
    }

    public void setLastUpdateTime(Date time) {
        this.lastUpdateTime = time;
    }

    public void setSessionDetails(byte[] details) {
        this.sessionDetails = details;
    }

    @Override
    public String getPublicIpAddress() {
        return this.publicIpAddress;
    }

    @Override
    public String getPublicNetmask() {
        return this.publicNetmask;
    }

    @Override
    public String getPublicMacAddress() {
        return this.publicMacAddress;
    }

    @Override
    public int getActiveSession() {
        return this.activeSession;
    }

    @Override
    public Date getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    @Override
    public byte[] getSessionDetails() {
        return this.sessionDetails;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

}
