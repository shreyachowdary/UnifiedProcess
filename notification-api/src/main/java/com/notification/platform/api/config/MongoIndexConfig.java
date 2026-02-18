package com.notification.platform.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoIndexConfig {

    @Bean
    public IndexResolver indexResolver(MongoMappingContext mappingContext) {
        return new MongoPersistentEntityIndexResolver(mappingContext);
    }
}
