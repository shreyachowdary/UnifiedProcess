package com.notification.platform.worker.config;

import com.notification.platform.sms.MockSmppSimulator;
import com.notification.platform.sms.SmsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerConfig {

    @Bean
    @ConditionalOnMissingBean(SmsProvider.class)
    public SmsProvider smsProvider() {
        return new MockSmppSimulator();
    }
}
