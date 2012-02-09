/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.upgrade.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.consoleproxy.ConsoleProxyManagerImpl;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade2213to2214 implements DbUpgrade {
	final static Logger s_logger = Logger.getLogger(Upgrade2213to2214.class);


	@Override
	public String[] getUpgradableVersionRange() {
		return new String[] { "2.2.13", "2.2.14"};
	}

    private final String priviateKey = 
    		"-----BEGIN CERTIFICATE-----\n" +
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
    		"-----END PRIVATE KEY----- |\n" ;
    
    private final String pubcertificate = 
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
    
    private final String rootCa =
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
   

	@Override
	public String getUpgradedVersion() {
		return "2.2.14";
	}

	@Override
	public boolean supportsRollingUpgrade() {
		return true;
	}


	@Override
	public File[] getPrepareScripts() {
		String script = Script.findScript("", "db/schema-2213to2214.sql");
		if (script == null) {
			throw new CloudRuntimeException("Unable to find db/schema-2213to2214.sql");
		}

		return new File[] { new File(script) };
	}

    private void upgradeCerts(Connection conn) {
    	PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement("select md5(`cloud`.`keystore`.key) from `cloud`.`keystore` where name = 'CPVMCertificate'");
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String privateKeyMd5 = rs.getString(1);
				if (privateKeyMd5.equalsIgnoreCase("b2d3089241ed0ea0336efeb125dbb610")) {
					s_logger.debug("Need to upgrade cloudstack provided certificate");
					pstmt = conn.prepareStatement("update `cloud`.`keystore` set `cloud`.`keystore`.key = ?, certificate = ? where name = 'CPVMCertificate'");
					pstmt.setString(1, ConsoleProxyManagerImpl.keyContent);
					pstmt.setString(2, ConsoleProxyManagerImpl.certContent);
					pstmt.executeUpdate();
					
					pstmt = conn.prepareStatement("insert into `cloud`.`keystore` (name, certificate, seq, domain_suffix) VALUES (?,?,?,?)");
					pstmt.setString(1, "root");
					pstmt.setString(2, ConsoleProxyManagerImpl.rootCa);
					pstmt.setInt(3, 0);
					pstmt.setString(4, "realhostip.com");
					pstmt.executeUpdate();
				}
			}
			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			s_logger.debug("Failed to upgrade keystore: " + e.toString());
		}
	   
    }

	@Override
	public void performDataMigration(Connection conn) {
		fixIndexes(conn);
	}

	@Override
	public File[] getCleanupScripts() {
		return null;
	}


	private void fixIndexes(Connection conn) {
		//Drop i_usage_event__created key (if exists) and re-add it again
		List<String> keys = new ArrayList<String>();
		keys.add("i_usage_event__created");
		DbUpgradeUtils.dropKeysIfExist(conn, "usage_event", keys, false);
		try {
			PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`usage_event` ADD INDEX `i_usage_event__created`(`created`)");
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			throw new CloudRuntimeException("Unable to execute usage_event table update", e);
		}

		//In cloud_usage DB, drop i_usage_event__created key (if exists) and re-add it again
		keys = new ArrayList<String>();
		keys.add("i_usage_event__created");
		DbUpgradeUtils.dropKeysIfExist(conn, "cloud_usage.usage_event", keys, false);
		try {
			PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud_usage`.`usage_event` ADD INDEX `i_usage_event__created`(`created`)");
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			throw new CloudRuntimeException("Unable to execute cloud_usage usage_event table update", e);
		}

		//Drop netapp_volume primary key and add it again
		DbUpgradeUtils.dropPrimaryKeyIfExists(conn, "cloud.netapp_volume");
		try {
			PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`netapp_volume` add PRIMARY KEY (`id`)");
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			throw new CloudRuntimeException("Unable to update primary key for netapp_volume", e);
		}

		//Drop i_snapshots__removed key (if exists) and re-add it again
		keys = new ArrayList<String>();
		keys.add("i_snapshots__removed");
		DbUpgradeUtils.dropKeysIfExist(conn, "cloud.snapshots", keys, false);
		try {
			PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`snapshots` ADD INDEX `i_snapshots__removed`(`removed`)");
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			throw new CloudRuntimeException("Unable to insert index for removed column in snapshots", e);
		}
		//Drop i_op_vm_ruleset_log__instance_id, u_op_vm_ruleset_log__instance_id key (if exists) and re-add u_op_vm_ruleset_log__instance_id again
		keys = new ArrayList<String>();
		keys.add("i_op_vm_ruleset_log__instance_id");
		DbUpgradeUtils.dropKeysIfExist(conn, "cloud.op_vm_ruleset_log", keys, false);

		keys = new ArrayList<String>();
		keys.add("u_op_vm_ruleset_log__instance_id");
		DbUpgradeUtils.dropKeysIfExist(conn, "cloud.op_vm_ruleset_log", keys, false);
		try {
			PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`op_vm_ruleset_log` ADD CONSTRAINT `u_op_vm_ruleset_log__instance_id` UNIQUE (`instance_id`)");
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			throw new CloudRuntimeException("Unable to execute changes for op_vm_ruleset_log", e);
		}  


		//Drop i_async__removed, i_async_job__removed  (if exists) and add i_async_job__removed 
		keys = new ArrayList<String>();
		keys.add("i_async__removed");
		keys.add("i_async_job__removed");
		DbUpgradeUtils.dropKeysIfExist(conn, "cloud.async_job", keys, false);
		try {
			PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`async_job` ADD INDEX `i_async_job__removed`(`removed`)");
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException e) {
			throw new CloudRuntimeException("Unable to insert index for removed column in async_job", e);
		}
		
    	keys = new ArrayList<String>();
    	keys.add("fk_ssh_keypair__account_id");
    	keys.add("fk_ssh_keypair__domain_id");
    	keys.add("fk_ssh_keypairs__account_id");
    	keys.add("fk_ssh_keypairs__domain_id");
    	DbUpgradeUtils.dropKeysIfExist(conn, "ssh_keypairs", keys, true);
    	
    	keys = new ArrayList<String>();
    	keys.add("fk_ssh_keypair__account_id");
    	keys.add("fk_ssh_keypair__domain_id");
    	keys.add("fk_ssh_keypairs__account_id");
    	keys.add("fk_ssh_keypairs__domain_id");
    	DbUpgradeUtils.dropKeysIfExist(conn, "ssh_keypairs", keys, false);
    	
    	try {
    		PreparedStatement pstmt; pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT `fk_ssh_keypairs__account_id` FOREIGN KEY `fk_ssh_keypairs__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE");
    		pstmt.executeUpdate();
    		pstmt.close();
    	} catch (SQLException e) {
    		throw new CloudRuntimeException("Unable to execute ssh_keypairs table update for adding account_id foreign key", e);
    	}
			
    	try {
    		PreparedStatement pstmt; pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT `fk_ssh_keypairs__domain_id` FOREIGN KEY `fk_ssh_keypairs__domain_id` (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE");
    		pstmt.executeUpdate();
    		pstmt.close();
    	} catch (SQLException e) {
    		throw new CloudRuntimeException("Unable to execute ssh_keypairs table update for adding domain_id foreign key", e);
    	}

    	//Drop i_async__removed, i_async_job__removed  (if exists) and add i_async_job__removed 
    	keys = new ArrayList<String>();
    	keys.add("i_async__removed");
    	keys.add("i_async_job__removed");
    	DbUpgradeUtils.dropKeysIfExist(conn, "cloud.async_job", keys, false);
    	try {
    	    PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`async_job` ADD INDEX `i_async_job__removed`(`removed`)");
    	    pstmt.executeUpdate();
    	    pstmt.close();
    	} catch (SQLException e) {
    	    throw new CloudRuntimeException("Unable to insert index for removed column in async_job", e);
    	}
    	
    	//Drop storage pool details keys (if exists) and insert one with correct name
    	keys = new ArrayList<String>();
        keys.add("fk_storage_pool__pool_id");
        keys.add("fk_storage_pool_details__pool_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.storage_pool_details", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.storage_pool_details", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`storage_pool_details` ADD CONSTRAINT `fk_storage_pool_details__pool_id` FOREIGN KEY `fk_storage_pool_details__pool_id`(`pool_id`) REFERENCES `storage_pool`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in storage_pool_details ", e);
        }
        
        //Drop securityGroup keys (if exists) and insert one with correct name
        keys = new ArrayList<String>();
        keys.add("fk_security_group___account_id");
        keys.add("fk_security_group__account_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.security_group", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.security_group", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`security_group` ADD CONSTRAINT `fk_security_group__account_id` FOREIGN KEY `fk_security_group__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in security_group table ", e);
        }
        
        //Drop vmInstance keys (if exists) and insert one with correct name
        keys = new ArrayList<String>();
        keys.add("i_vm_instance__host_id");
        keys.add("fk_vm_instance__host_id");
        
        keys.add("fk_vm_instance__last_host_id");
        keys.add("i_vm_instance__last_host_id");
        
        keys.add("fk_vm_instance__service_offering_id");
        keys.add("i_vm_instance__service_offering_id");
        
        keys.add("fk_vm_instance__account_id");
        keys.add("i_vm_instance__account_id");
        
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_instance", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_instance", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`)");
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__last_host_id` FOREIGN KEY (`last_host_id`) REFERENCES `host` (`id`)");
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)");
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__service_offering_id` FOREIGN KEY (`service_offering_id`) REFERENCES `service_offering` (`id`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in vm_instance table ", e);
        }
        
        //Drop user_ip_address keys (if exists) and insert one with correct name
        keys = new ArrayList<String>();
        keys.add("fk_user_ip_address__account_id");
        keys.add("i_user_ip_address__account_id");
        
        keys.add("fk_user_ip_address__vlan_db_id");
        keys.add("i_user_ip_address__vlan_db_id");
        
        keys.add("fk_user_ip_address__data_center_id");
        keys.add("i_user_ip_address__data_center_id");
        
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.user_ip_address", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.user_ip_address", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`)");
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__vlan_db_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in vm_instance table ", e);
        }

    }
}
