/*
 * Copyright (c) 2008 Citrix Systems, Inc.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.xensource.xenapi.*;

/**
 * Tests using an https (SSL) connection to a XenServer.
 * 
 * Before running, perform these steps:
 * 
 * 1. Copy to your client machine /etc/xensource/xapi-ssl.pem from the XenServer you want to connect to.
 * 2. Run 'openssl x509 -inform PEM -outform DER -in xapi-ssl.pem -out xapi-ssl.jks'
 *    This converts the certificate into a form that Java's keytool can understand.
 * 3. Run keytool (found in Java's bin directory) as follows:
 *    'keytool -importcert -file xapi-ssl.jks -alias <hostname>'
 *    You can optionally pass the -keystore argument if you want to specify the location of your keystore.
 * 4. To tell the JVM the location and password of your keystore, run it with the additional parameters
 *    (Sun's keytool seems to insist on using private key and keystore passwords):
 *    -Djavax.net.ssl.trustStore="<path to keystore>" -Djavax.net.ssl.trustStorePassword=<password>
 *    For extra debug info, try:
 *    -Djavax.net.debug=ssl
 */
public class Https extends TestBase
{
    protected static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;

        try
        {
            // Create our own HostnameVerifier
            HostnameVerifier hv = new HostnameVerifier()
            {
                public boolean verify(String hostname, SSLSession session)
                {
                    return session.getPeerHost().equals(hostname);
                }
            };

            // Sets the default HostnameVerifier used on all Https connections created after this point
            HttpsURLConnection.setDefaultHostnameVerifier(hv);

            URL url = new URL("https://" + server.Hostname);
            connection = new Connection(url);

            // Log in
            Session.loginWithPassword(connection, server.Username, server.Password, "1.3");

            // Print a record
            for (Host host : Host.getAllRecords(connection).keySet())
            {
                logln(host.toString());
                break;
            }
        } finally
        {
            if (connection != null)
            {
                Session.logout(connection);
            }
        }
    }
}
