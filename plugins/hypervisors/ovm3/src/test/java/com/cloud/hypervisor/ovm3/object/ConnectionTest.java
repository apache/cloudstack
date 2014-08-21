package com.cloud.hypervisor.ovm3.object;

import java.io.StringReader;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.parser.XmlRpcResponseParser;
import org.apache.xmlrpc.util.SAXParsers;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class ConnectionTest extends Connection {
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
        this.result = res;
    }
    public String getResult() {
        return  this.result;
    }
}

