package me.sonam.authzmanager;


import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import reactor.core.publisher.Mono;


public class SpringWebFluxConstructTest {

    private static final Logger LOG = LoggerFactory.getLogger(SpringWebFluxConstructTest.class);

    @Test
    public void falseEmptyTest() {
        Mono<Boolean> falseMono = Mono.just(false);


    }
}
