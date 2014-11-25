package com.cloud.hypervisor.ovm3.object;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.parser.XmlRpcResponseParser;
import org.apache.xmlrpc.util.SAXParsers;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/* This is a stub for XML parsing into result sets, it also contains test for Connection */
public class ConnectionTest extends Connection {
    XmlTestResultTest results = new XmlTestResultTest();
    String result;

    @Override
    public Object callTimeoutInSec(String method, List<?> params, int timeout, boolean debug) throws XmlRpcException {
        XmlRpcStreamConfig config = new XmlRpcHttpRequestConfigImpl();
        XmlRpcClient client = new XmlRpcClient();
        // XmlRpcRequestParser parser = new XmlRpcRequestParser(config, client.getTypeFactory());
        client.setTypeFactory(new RpcTypeFactory(client));
        XmlRpcResponseParser parser = new XmlRpcResponseParser((XmlRpcStreamRequestConfig) config, client.getTypeFactory());
        XMLReader xr = SAXParsers.newXMLReader();
        xr.setContentHandler(parser);
        try {
            xr.parse(new InputSource(new StringReader(getResult())));
        } catch (Exception e) {
            throw new XmlRpcException("Exception: " + e.getMessage(), e);
        }
        if (parser.getErrorCode() != 0) {
            throw new XmlRpcException("Fault received["
                    + parser.getErrorCode()
                    + "]: " + parser.getErrorMessage());
        }
        return parser.getResult();
    }

    public void setResult(String res) {
        result = res;
    }
    public String getResult() {
        return  result;
    }

    @Test
    public void testConnection() {
        String host = "ovm-1";
        String user = "admin";
        String pass = "password";
        Integer port = 8899;
        List<?> emptyParams = new ArrayList<Object>();
        Connection con = new Connection(host, port, user, pass);
        results.basicStringTest(con.getIp(), host);
        results.basicStringTest(con.getUserName(), user);
        results.basicStringTest(con.getPassword(), pass);
        results.basicIntTest(con.getPort(), port);
        try {
            con.callTimeoutInSec("ping", emptyParams, 1);
            //            con.call("ping", emptyParams, 1, false);
        } catch (XmlRpcException e) {
            // TODO Auto-generated catch block
            System.out.println();
        }
        new Connection(host, user, pass);
    }
}

