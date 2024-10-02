package com.funtikov.sch_parser.configuration;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfiguration {

    @Value("${http.client.connection-timeout}")
    private String connectionTimeOut;
    @Value("${http.client.socket-timeout}")
    private String socketTimeOut;
    @Value("${http.client.request-timeout}")
    private String requestTimeout;
    @Value("${http.client.proxy.enabled}")
    private boolean proxyEnabled;
    @Value("${http.client.proxy.host}")
    private String proxyHost;
    @Value("${http.client.proxy}")
    private String proxyPort;

    @Bean
    public CloseableHttpClient closeableHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Integer.parseInt(connectionTimeOut))
                .setSocketTimeout(Integer.parseInt(socketTimeOut))
                .build();

        if (proxyEnabled) {
            HttpHost httpProxyHost = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
            return HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setProxy(httpProxyHost)
                    .build();
        } else {
            return HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        }

    }
}
