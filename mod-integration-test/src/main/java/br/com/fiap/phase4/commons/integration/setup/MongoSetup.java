package br.com.fiap.phase4.commons.integration.setup;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public interface MongoSetup {

    MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withAccessToHost(true);

    @BeforeAll
    static void beforeAllMongo() {
        MONGO.start();
    }

    @DynamicPropertySource
    static void configurePropertiesMongo(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
    }
}
