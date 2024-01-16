package com.capitalone.identity.platform.loading

import com.capitalone.identity.identitybuilder.model.Entity
import com.capitalone.identity.identitybuilder.model.EntityInfo

sealed class ChangeNotification(open val entityInfo: EntityInfo) {

    data class Add(val entity: Entity) : ChangeNotification(entity.info)
    data class Update(val entity: Entity) : ChangeNotification(entity.info)
    data class Delete(override val entityInfo: EntityInfo) : ChangeNotification(entityInfo)

}
