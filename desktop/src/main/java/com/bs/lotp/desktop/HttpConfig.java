package com.bs.lotp.desktop;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Single shared HTTP setup: the JDK client follows the lotrointerface
 * redirects (http to https) that the default factory would reject.
 */
@Configuration
public class HttpConfig {

    @Bean
    ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }
}
