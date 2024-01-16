package com.capitalone.identity.platform.loading;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TestLoadTarget implements PolicyLoadTarget {

    Set<EntityInfo> entities = new HashSet<>();

    @Override
    public void load(Entity entity) {
        entities.add(entity.getInfo());
    }

    @Override
    public void unload(EntityInfo info) {
        entities.remove(info);
    }

    @Override
    public Collection<EntityInfo> getLoadedEntities() {
        return new HashSet<>(entities);
    }
}
