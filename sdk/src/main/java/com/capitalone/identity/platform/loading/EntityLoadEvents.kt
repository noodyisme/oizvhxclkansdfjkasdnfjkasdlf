package com.capitalone.identity.platform.loading

import com.capitalone.identity.identitybuilder.events.*
import com.capitalone.identity.identitybuilder.model.Entity
import com.capitalone.identity.identitybuilder.model.EntityInfo
import com.capitalone.identity.identitybuilder.model.EntityState

abstract class EntityLoadOperationResult(metadata: Metadata) : ApplicationEvent(metadata) {
    constructor() : this(Metadata())

    abstract val changeType: EntityState.Delta.ChangeType
    abstract val isBootstrap: Boolean
    abstract val info: EntityInfo

}

@PolicyCoreEventPublisher(
    value = [
        Failure::class, Loaded::class, Unloaded::class, NonLoadingError::class,
    ]
)
interface EntityLoadEvents

@PolicyCoreEvent
data class Failure(
    override val changeType: EntityState.Delta.ChangeType,
    val error: Throwable,
    override val isBootstrap: Boolean,
    override val info: EntityInfo,
    override val metadata: Metadata,
) : EntityLoadOperationResult()

@PolicyCoreEvent
data class Loaded(
    val entity: Entity,
    override val changeType: EntityState.Delta.ChangeType,
    override val isBootstrap: Boolean,
    override val metadata: Metadata,
) : EntityLoadOperationResult() {
    override val info: EntityInfo = entity.info
}

@PolicyCoreEvent
data class Unloaded @JvmOverloads constructor(
    override val info: EntityInfo,
    override val changeType: EntityState.Delta.ChangeType = EntityState.Delta.ChangeType.DELETE,
    override val isBootstrap: Boolean = false,
    override val metadata: Metadata,
) : EntityLoadOperationResult()

@PolicyCoreEvent
data class NonLoadingError(val error: Throwable?, override val metadata: Metadata) : ApplicationEvent()
