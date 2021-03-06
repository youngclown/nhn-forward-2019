package com.nhn.forward2019.reactivekafka.core;

import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.*;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.function.Function;

public class Step5Subscriber extends BaseSubscriber<ReceiverRecord<String, String>> {
    private static final Logger logger = LoggerFactory.getLogger(Step5Subscriber.class);

    private Function<ReceiverRecord<String, String>, Mono<Boolean>> runner;
    private FluxSink<ReceiverRecord<String, String>> offsetSink;

    public Step5Subscriber(Function<ReceiverRecord<String, String>, Mono<Boolean>> runner) {
        this.runner = runner;

        Flux.<ReceiverRecord<String, String>>create(sink -> offsetSink = sink)
                .reduce(-1L, (last, r) -> last < r.offset()
                                        ? commit(r)
                                        : last)
                .subscribe();
    }

    private long commit(ReceiverRecord<String, String> record) {
        logger.info("[COMMIT] {}", record.value());
        record.receiverOffset().acknowledge();
        return record.offset();
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        request(5);
    }

    @Override
    protected void hookOnNext(ReceiverRecord<String, String> record) {
        Mono.just(record)
                .flatMap(runner)
                .subscribe(r -> {
                    logger.info("Consume END - {}", record.value());
                    offsetSink.next(record);
                    request(1);
                });
    }
}
