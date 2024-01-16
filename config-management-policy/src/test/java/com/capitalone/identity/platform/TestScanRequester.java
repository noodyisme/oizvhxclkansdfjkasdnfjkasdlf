package com.capitalone.identity.platform;

import com.capitalone.identity.identitybuilder.model.ScanRequest;
import com.capitalone.identity.identitybuilder.polling.ScanRequester;
import reactor.core.publisher.Flux;
import reactor.test.publisher.TestPublisher;

public class TestScanRequester implements ScanRequester {
    private final TestPublisher<ScanRequest> scanRequestTestPublisher = TestPublisher.create();

    public void triggerTestScanRequest(ScanRequest request) {
        scanRequestTestPublisher.next(request);
    }

    @Override
    public Flux<ScanRequest> getScanRequests() {
        return scanRequestTestPublisher.flux();
    }
}
