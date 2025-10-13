package org.apache.cloudstack.storage.feign;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Retryer;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.Client;
import feign.httpclient.ApacheHttpClient;
import javax.net.ssl.SSLContext;
import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfiguration {
    private static Logger logger = LogManager.getLogger(FeignConfiguration.class);

    private int retryMaxAttempt = 3;

    private int retryMaxInterval = 5;

    private String ontapFeignMaxConnection = "80";

    private String ontapFeignMaxConnectionPerRoute = "20";

    @Bean
    public Client client(ApacheHttpClientFactory httpClientFactory) {

        int maxConn;
        int maxConnPerRoute;
        try {
            maxConn = Integer.parseInt(this.ontapFeignMaxConnection);
        } catch (Exception e) {
            logger.error("ontapFeignClient: encounter exception while parse the max connection from env. setting default value");
            maxConn = 20;
        }
        try {
            maxConnPerRoute = Integer.parseInt(this.ontapFeignMaxConnectionPerRoute);
        } catch (Exception e) {
            logger.error("ontapFeignClient: encounter exception while parse the max connection per route from env. setting default value");
            maxConnPerRoute = 2;
        }
        // Disable Keep Alive for Http Connection
        logger.debug("ontapFeignClient: Setting the feign client config values as max connection: {}, max connections per route: {}", maxConn, maxConnPerRoute);
        ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> 0;
        CloseableHttpClient httpClient = httpClientFactory.createBuilder()
                .setMaxConnTotal(maxConn)
                .setMaxConnPerRoute(maxConnPerRoute)
                .setKeepAliveStrategy(keepAliveStrategy)
                .setSSLSocketFactory(getSSLSocketFactory())
                .setConnectionTimeToLive(60, TimeUnit.SECONDS)
                .build();
        return new ApacheHttpClient(httpClient);
    }

    private SSLConnectionSocketFactory getSSLSocketFactory() {
        try {
            // The TrustAllStrategy is a strategy used in SSL context configuration that accepts any certificate
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustAllStrategy()).build();
            return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                logger.info("Feign Request URL: {}", template.url());
                logger.info("HTTP Method: {}", template.method());
                logger.info("Headers: {}", template.headers());
                logger.info("Body: {}", template.requestBody().asString());
            }
        };
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(1000L, retryMaxInterval * 1000L, retryMaxAttempt);
    }
}
