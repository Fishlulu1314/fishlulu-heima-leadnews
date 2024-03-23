package com.heima.common.baiduyun.config;

import com.baidu.aip.contentcensor.AipContentCensor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "baiduyun")
@Getter
@Setter
public class AipContentCencorConfig {
    private String appId;
    private String apiKey;
    private String secretKey;

    /**
     * 注入百度云内容审核客户端,单例使用
     * @return
     */
    @Bean
    public AipContentCensor buildAipContentCensor() {
        AipContentCensor client = new AipContentCensor(appId, apiKey, secretKey);
//        // 可选：设置网络连接参数
//        client.setConnectionTimeoutInMillis(2000);
//        client.setSocketTimeoutInMillis(60000);
//
//        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
//        // 设置http代理
//        client.setHttpProxy("proxy_host", proxy_port);
//        // 设置socket代理
//        client.setSocketProxy("proxy_host", proxy_port);
        return client;
    }
    public void testKey() {
        System.out.println(appId);
        System.out.println(apiKey);
        System.out.println(secretKey);
    }
}
