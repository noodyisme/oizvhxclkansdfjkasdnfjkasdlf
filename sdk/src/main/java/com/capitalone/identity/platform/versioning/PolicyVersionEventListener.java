package com.capitalone.identity.platform.versioning;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.platform.loading.EntityLoadEvents_ApplicationEventPublisher;
import com.capitalone.identity.platform.loading.Loaded;
import com.capitalone.identity.platform.loading.Unloaded;

public class PolicyVersionEventListener implements EntityLoadEvents_ApplicationEventPublisher {

    private final PolicyVersionService policyVersionService;

    public PolicyVersionEventListener(PolicyVersionService policyVersionService) {
        this.policyVersionService = policyVersionService;
    }

    @Override
    public void publishEvent(Loaded event) {
        Entity entity = event.getEntity();
        policyVersionService.set(entity.getInfo(), ((Entity.Policy) entity).getEntityActivationStatus());
    }

    @Override
    public void publishEvent(Unloaded event) {
        policyVersionService.remove(event.getInfo());
    }
}
