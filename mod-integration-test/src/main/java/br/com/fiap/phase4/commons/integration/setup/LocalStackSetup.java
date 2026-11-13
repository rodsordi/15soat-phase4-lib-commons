package br.com.fiap.phase4.commons.integration.setup;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;
import static org.testcontainers.utility.DockerImageName.parse;

public interface LocalStackSetup {

    LocalStackContainer LOCAL_STACK = new LocalStackContainer(parse("localstack/localstack:4.2.0"))
            .withServices(SNS, SQS)
            .withEnv("SKIP_SSL_CERT_DOWNLOAD", "true");

    @BeforeAll
    static void beforeAllLocalStack() {
        LOCAL_STACK.start();
    }

    @DynamicPropertySource
    static void configurePropertiesLocalstack(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", LOCAL_STACK::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", LOCAL_STACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCAL_STACK::getSecretKey);
        registry.add("spring.cloud.aws.sqs.endpoint", () -> LOCAL_STACK.getEndpointOverride(SQS).toString());
        registry.add("spring.cloud.aws.sns.endpoint", () -> LOCAL_STACK.getEndpointOverride(SNS).toString());
    }
}
