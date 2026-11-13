package br.com.fiap.phase4.commons.integration.setup;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;

public interface WiremockSetup {

    WireMockServer WIRE_MOCK = new WireMockServer(9090);

    @BeforeAll
    static void beforeAllWiremock() {
        WIRE_MOCK.start();
        configureFor("localhost", 9090);
    }

    @AfterEach
    default void wiremockAfterEach() {
        WIRE_MOCK.resetAll();
    }
}
