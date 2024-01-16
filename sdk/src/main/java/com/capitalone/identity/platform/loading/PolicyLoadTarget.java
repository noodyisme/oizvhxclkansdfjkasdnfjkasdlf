package com.capitalone.identity.platform.loading;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;

import java.util.Collection;

public interface PolicyLoadTarget {

    void load(Entity entity);

    void unload(EntityInfo info);

    Collection<EntityInfo> getLoadedEntities();

}
