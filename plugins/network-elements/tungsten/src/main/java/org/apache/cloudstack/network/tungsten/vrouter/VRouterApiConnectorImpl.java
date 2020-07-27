package org.apache.cloudstack.network.tungsten.vrouter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class VRouterApiConnectorImpl implements VRouterApiConnector {
  private static final Logger s_logger = Logger.getLogger(VRouterApiConnector.class);
  private String _url;

  public VRouterApiConnectorImpl(String host, String port) {
    _url = "http://" + host + ":" + port + "/";
  }

  @Override
  public boolean addPort(final Port port) throws IOException {
    final StringBuffer url = new StringBuffer();
    url.append(_url).append("port");
    Gson gson = new Gson();
    final String jsonData = gson.toJson(port);
    HttpPost httpPost = new HttpPost(url.toString());
    httpPost.setEntity(new StringEntity(jsonData));
    try (CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
      return getResponse(httpResponse);
    } catch (IOException ex) {
      s_logger.error("Failed to add vrouter port : " + ex.getMessage());
      return false;
    }
  }

  @Override
  public boolean deletePort(final String portId) {
    final StringBuffer url = new StringBuffer();
    url.append(_url).append("port/").append(portId);
    HttpDelete httpDelete = new HttpDelete(url.toString());
    try (CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse httpResponse = httpClient.execute(httpDelete)) {
      return getResponse(httpResponse);
    } catch (IOException ex) {
      s_logger.error("Failed to delete vrouter port : " + ex.getMessage());
      return false;
    }
  }

  @Override
  public boolean enablePort(final String portId) {
    final StringBuffer url = new StringBuffer();
    url.append(_url).append("enable-port/").append(portId);
    HttpPut httpPut = new HttpPut(url.toString());
    try (CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse httpResponse = httpClient.execute(httpPut)) {
      return getResponse(httpResponse);
    } catch (IOException ex) {
      s_logger.error("Failed to enable vrouter port : " + ex.getMessage());
      return false;
    }
  }

  @Override
  public boolean disablePort(final String portId) {
    final StringBuffer url = new StringBuffer();
    url.append(_url).append("disable-port/").append(portId);
    HttpPut httpPut = new HttpPut(url.toString());
    try (CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse httpResponse = httpClient.execute(httpPut)) {
      return getResponse(httpResponse);
    } catch (IOException ex) {
      s_logger.error("Failed to disable vrouter port : " + ex.getMessage());
      return false;
    }
  }

  private boolean getResponse(final CloseableHttpResponse httpResponse) throws IOException {
    JsonParser parser = new JsonParser();
    String result = EntityUtils.toString(httpResponse.getEntity());
    JsonObject jsonObject = parser.parse(result).getAsJsonObject();
    if (jsonObject.entrySet().size() == 0) {
      return true;
    } else {
      String error = jsonObject.get("error").getAsString();
      s_logger.error(error);
      return false;
    }
  }
}
