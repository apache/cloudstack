package org.apache.cloudstack.storage.datastore.client;

import java.net.ConnectException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.naming.ServiceUnavailableException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.apache.cloudstack.storage.datastore.response.ListCapabilitiesResponse;
import org.apache.http.auth.InvalidCredentialsException;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class ElastiCenterClient {

    public static boolean debug = false;

    private boolean initialized = false;

    private String apiKey = null;
    private String elastiCenterAddress = null;
    private String responseType = "json";
    private boolean ignoreSSLCertificate = false;

    private String restprotocol = "https://";
    private String restpath = "/client/api";
    private String restdefaultcommand = "listCapabilities";

    private String queryparamcommand = "command";
    private String queryparamapikey = "apikey";
    private String queryparamresponse = "response";

    public ElastiCenterClient() {

    }

    public ElastiCenterClient(String address, String key)
            throws InvalidCredentialsException, InvalidParameterException,
            SSLHandshakeException, ServiceUnavailableException {
        this.elastiCenterAddress = address;
        this.apiKey = key;
        this.initialize();
    }

    public void initialize() throws InvalidParameterException,
            SSLHandshakeException, InvalidCredentialsException,
            ServiceUnavailableException {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new InvalidParameterException(
                    "Unable to initialize. Please specify a valid API Key.");
        }

        if (elastiCenterAddress == null || elastiCenterAddress.trim().isEmpty()) {
            // TODO : Validate the format, like valid IP address or hostname.
            throw new InvalidParameterException(
                    "Unable to initialize. Please specify a valid ElastiCenter IP Address or Hostname.");
        }

        if (ignoreSSLCertificate) {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs,
                        String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs,
                        String authType) {
                }
            } };

            HostnameVerifier hv = new HostnameVerifier() {
                @Override
                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting trust manager
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc
                        .getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
            } catch (Exception e) {
                ;
            }
        }

        ListCapabilitiesResponse listCapabilitiesResponse = null;
        try {
            initialized = true;
            listCapabilitiesResponse = (ListCapabilitiesResponse) executeCommand(
                    restdefaultcommand, null, new ListCapabilitiesResponse());

        } catch (Throwable t) {
            initialized = false;
            if (t instanceof InvalidCredentialsException) {
                throw (InvalidCredentialsException) t;
            } else if (t instanceof ServiceUnavailableException) {
                throw (ServiceUnavailableException) t;
            } else if (t.getCause() instanceof SSLHandshakeException) {
                throw new SSLHandshakeException(
                        "Unable to initialize. An untrusted SSL Certificate was received from "
                                + elastiCenterAddress
                                + ". Please verify your truststore or configure ElastiCenterClient to skip the SSL Validation. ");
            } else if (t.getCause() instanceof ConnectException) {
                throw new ServiceUnavailableException(
                        "Unable to initialize. Failed to connect to "
                                + elastiCenterAddress
                                + ". Please verify the IP Address, Network Connectivity and ensure that Services are running on the ElastiCenter Server. ");
            }
            throw new ServiceUnavailableException(
                    "Unable to initialize. Please contact your ElastiCenter Administrator. Exception "
                            + t.getMessage());
        }

        if (null == listCapabilitiesResponse
                || null == listCapabilitiesResponse.getCapabilities()
                || null == listCapabilitiesResponse.getCapabilities()
                        .getVersion()) {
            initialized = false;
            throw new ServiceUnavailableException(
                    "Unable to execute command on the server");
        }

    }

    public Object executeCommand(ElastiCenterCommand cmd) throws Throwable {
        return executeCommand(cmd.getCommandName(), cmd.getCommandParameters(),
                cmd.getResponseObject());
    }

    public Object executeCommand(String command,
            MultivaluedMap<String, String> params, Object responeObj)
            throws Throwable {

        if (!initialized) {
            throw new IllegalStateException(
                    "Error : ElastiCenterClient is not initialized.");
        }

        if (command == null || command.trim().isEmpty()) {
            throw new InvalidParameterException("No command to execute.");
        }

        try {
            ClientConfig config = new DefaultClientConfig();
            Client client = Client.create(config);
            WebResource webResource = client.resource(UriBuilder.fromUri(
                    restprotocol + elastiCenterAddress + restpath).build());

            MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
            queryParams.add(queryparamapikey, apiKey);
            queryParams.add(queryparamcommand, responseType);

            queryParams.add(queryparamcommand, command);

            if (null != params) {
                for (String key : params.keySet()) {
                    // TODO : Iterate over multi-value and add it multiple times
                    queryParams.add(key, params.getFirst(key));
                }
            }
            if (debug) {
                System.out.println("Command Sent " + command + " : "
                        + queryParams);
            }
            ClientResponse response = webResource.queryParams(queryParams)
                    .accept(MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);

            if (response.getStatus() >= 300) {
                if (debug)
                    System.out.println("ElastiCenter returned error code : "
                            + response.getStatus());
                if (401 == response.getStatus()) {
                    throw new InvalidCredentialsException(
                            "Please specify a valid API Key.");
                } else if (431 == response.getStatus()) {
                    throw new InvalidParameterException(response.getHeaders()
                            .getFirst("X-Description"));
                } else if (432 == response.getStatus()) {
                    throw new InvalidParameterException(
                            command
                                    + " does not exist on the ElastiCenter server.  Please specify a valid command or contact your ElastiCenter Administrator.");
                } else {
                    throw new ServiceUnavailableException(
                            "Internal Error. Please contact your ElastiCenter Administrator.");
                }
            } else if (null != responeObj) {
                String jsonResponse = response.getEntity(String.class);
                if (debug) {
                    System.out.println("Command Response : " + jsonResponse);
                }
                Gson gson = new Gson();
                return gson.fromJson(jsonResponse, responeObj.getClass());
            } else {
                return "Success";
            }
        } catch (Throwable t) {
            throw t;
            // t.printStackTrace();
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getElastiCenterAddress() {
        return elastiCenterAddress;
    }

    public void setElastiCenterAddress(String elastiCenterAddress) {
        this.elastiCenterAddress = elastiCenterAddress;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public boolean isIgnoreElastiCenterCertificate() {
        return ignoreSSLCertificate;
    }

    public void ignoreSSLCertificate(boolean ignoreSSLCertificate) {
        this.ignoreSSLCertificate = ignoreSSLCertificate;
    }

}
