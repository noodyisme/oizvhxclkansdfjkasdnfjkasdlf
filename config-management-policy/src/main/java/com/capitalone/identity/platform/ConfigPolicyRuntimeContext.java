package com.capitalone.identity.platform;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.configmanagement.ConfigMatchingStrategy;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.LogicalVersion;
import com.capitalone.identity.platform.loading.PolicyLoadTarget;
import com.capitalone.identity.platform.runtime.PolicyError;
import com.capitalone.identity.platform.runtime.PolicyErrorInfo;
import com.capitalone.identity.platform.runtime.PolicyResult;
import com.capitalone.identity.platform.runtime.PolicyRuntimeContext;

import java.io.Serializable;
import java.util.*;

public class ConfigPolicyRuntimeContext implements
        PolicyRuntimeContext<ConfigurationPolicyRequest, ConfigurationPolicyResponse>,
        PolicyLoadTarget {
    private final ConfigMatchingStrategy configMatchingStrategy;
    private final String configEnvironment;
    private final Map<String, ConfigManagementModel> configItems = new HashMap<>();
    private final Map<String, EntityInfo> latestLoadedEntityVersionNumber = new HashMap<>();
    private final ConfigManagementService configManagementService = new ConfigManagementService();

    /**
     * @deprecated convert to constructor {@link #ConfigPolicyRuntimeContext(ConfigMatchingStrategy, String)}
     */
    @Deprecated
    public ConfigPolicyRuntimeContext(ConfigMatchingStrategy configMatchingStrategy) {
        this(configMatchingStrategy, null);
    }

    /**
     * @param configMatchingStrategy strategy that is used to match usecases against provided arguments
     * @param configEnvironment      if a policy is updated to use v2 of schema.json, then this value is used to
     *                               indicate which environment-specific features.json property file overrides to use
     *                               (e.g. 'qa' would apply properties from features-qa.json if that file existed).
     *                               Null is a valid argument for this property.
     */
    public ConfigPolicyRuntimeContext(ConfigMatchingStrategy configMatchingStrategy, String configEnvironment) {
        this.configMatchingStrategy = configMatchingStrategy;
        this.configEnvironment = configEnvironment;
    }

    @Override
    public void load(Entity entity) {
        if (entity instanceof Entity.Policy) {
            String patchId = getPatchIdentifier(entity.getInfo());
            ((Entity.Policy) entity).getConfigManagementModelForEnv(configEnvironment).ifPresent(model -> {
                configManagementService.setPolicyConfiguration(patchId, model);
                configItems.put(patchId, model);
                latestLoadedEntityVersionNumber.put(entity.getInfo().getId(), entity.getInfo());
            });
        } else {
            throw new UnsupportedOperationException(
                    String.format("Unsupported Entity type [entityInfo=%s]", entity.getInfo()));
        }
    }

    private static String getPatchIdentifier(LogicalVersion info) {
        return info.getName() + "/" + info.getPatchVersionString();
    }

    @Override
    public void unload(EntityInfo info) {
        String patchIdentifier = getPatchIdentifier(info);
        ConfigManagementModel remove = configItems.remove(patchIdentifier);
        if (remove != null) {
            latestLoadedEntityVersionNumber.remove(info.getId());
            configManagementService.deletePolicyConfiguration(patchIdentifier);
        }
    }

    @Override
    public Collection<EntityInfo> getLoadedEntities() {
        return new ArrayList<>(latestLoadedEntityVersionNumber.values());
    }

    @Override
    public PolicyResult<ConfigurationPolicyResponse> invoke(LogicalVersion policyVersion, ConfigurationPolicyRequest request) {
        String identifier = getPatchIdentifier(policyVersion);
        Map<String, Serializable> configMap = Optional.ofNullable(configManagementService.getPolicyConfiguration(identifier))
                .flatMap(configuration -> configuration.getConfiguration(
                        request.getBusinessEventName(),
                        configMatchingStrategy))
                .orElse(Collections.emptyMap());

        if (configMap.isEmpty()) {
            PolicyErrorInfo errorInfo = new PolicyErrorInfo(PolicyError.BAD_REQUEST_MISSING_CONFIG,
                    String.format("Could not find config with the use case name '%s' in requested policy.", request.getBusinessEventName()));
            return new PolicyResult.Failure<>(errorInfo);
        } else
            return new PolicyResult.Success<>(new ConfigurationPolicyResponse(configMap));
    }
}
