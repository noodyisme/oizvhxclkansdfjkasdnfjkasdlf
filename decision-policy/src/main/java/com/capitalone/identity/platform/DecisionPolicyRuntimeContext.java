package com.capitalone.identity.platform;

import com.capitalone.identity.identitybuilder.audit.models.HostContext;
import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.configmanagement.ConfigMatchingStrategy;
import com.capitalone.identity.identitybuilder.configmanagement.MatchingStrategies;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEngineService;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateRequest;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateResponse;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.LogicalVersion;
import com.capitalone.identity.platform.dmn.DecisionPolicyRuntimeLoadService;
import com.capitalone.identity.platform.dmn.RuleDefinitionModelGroup;
import com.capitalone.identity.platform.dmn.RuleDefinitionModelGroupFactory;
import com.capitalone.identity.platform.loading.PolicyLoadTarget;
import com.capitalone.identity.platform.runtime.PolicyError;
import com.capitalone.identity.platform.runtime.PolicyErrorInfo;
import com.capitalone.identity.platform.runtime.PolicyResult;
import com.capitalone.identity.platform.runtime.PolicyRuntimeContext;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class DecisionPolicyRuntimeContext implements
        PolicyRuntimeContext<DecisionPolicyRequest, DecisionPolicyResponse>,
        PolicyLoadTarget {

    private static final Logger logger = LoggerFactory.getLogger(DecisionPolicyRuntimeContext.class);

    private static DecisionPolicyRuntimeLoadService newDefaultService() {
        return new DecisionPolicyRuntimeLoadService(
                DecisionEngineService.createDefaultConfiguration(
                        HostContext.builder()
                                .businessApplication("ba_test")
                                .applicationComponent("appComponent_test")
                                .clientIdentifier("clientIdentifier_test")
                                .hostName("hostName_test")
                                .containerId("containerId_localTest")
                                .build()
                )
        );
    }

    final DecisionPolicyRuntimeLoadService decisionPolicyRuntimeLoadService;

    private final RuleDefinitionModelGroupFactory ruleDefinitionModelGroupFactory =
            new RuleDefinitionModelGroupFactory();

    private final Map<String, RuleDefinitionModelGroup> ruleItems = new HashMap<>();

    private final ConfigManagementService configManagementService = new ConfigManagementService();

    private final Map<String, EntityInfo> latestLoadedEntityVersionNumber = new HashMap<>();

    private final ConfigMatchingStrategy configurationMatchingStrategy;

    private final String configEnvironment;

    public DecisionPolicyRuntimeContext() {
        this(newDefaultService());
    }

    public DecisionPolicyRuntimeContext(ConfigMatchingStrategy matchingStrategy) {
        this(newDefaultService(), matchingStrategy, null);
    }

    public DecisionPolicyRuntimeContext(DecisionPolicyRuntimeLoadService decisionPolicyRuntimeLoadService) {
        this(decisionPolicyRuntimeLoadService, MatchingStrategies.MATCH_ALL_NON_NULL, null);
    }

    /**
     * @deprecated use config matching environment constructor {@link #DecisionPolicyRuntimeContext(DecisionPolicyRuntimeLoadService, ConfigMatchingStrategy, String)}
     */
    @Deprecated
    public DecisionPolicyRuntimeContext(DecisionPolicyRuntimeLoadService decisionPolicyRuntimeLoadService,
                                        ConfigMatchingStrategy matchingStrategy) {
        this(decisionPolicyRuntimeLoadService, matchingStrategy, null);
    }

    /**
     * @param decisionPolicyRuntimeLoadService service where dms are loaded
     * @param configMatchingStrategy           strategy that is used to match usecases against provided arguments
     * @param configEnvironment                if a policy is updated to use v2 of schema.json, then this value is used to
     *                                         indicate which environment-specific features.json property file overrides to use
     *                                         (e.g. 'qa' would apply properties from features-qa.json if that file existed).
     *                                         Null is a valid argument for this property.
     */
    public DecisionPolicyRuntimeContext(DecisionPolicyRuntimeLoadService decisionPolicyRuntimeLoadService,
                                        ConfigMatchingStrategy configMatchingStrategy,
                                        String configEnvironment) {
        this.decisionPolicyRuntimeLoadService = Objects.requireNonNull(decisionPolicyRuntimeLoadService);
        this.configurationMatchingStrategy = Objects.requireNonNull(configMatchingStrategy);
        this.configEnvironment = configEnvironment;
    }

    private static String getPatchIdentifier(LogicalVersion info) {
        return info.getName() + "/" + info.getPatchVersionString();
    }

    @Override
    public Collection<EntityInfo> getLoadedEntities() {
        return new ArrayList<>(latestLoadedEntityVersionNumber.values());
    }

    @Override
    public void load(Entity entity) {
        if (entity instanceof Entity.Policy) {
            final String patchId = getPatchIdentifier(entity.getInfo());
            final EntityInfo prevEntity = latestLoadedEntityVersionNumber.get(entity.getId());
            // item already loaded
            if (entity.getInfo().equals(prevEntity)) return;

            ((Entity.Policy) entity).getConfigManagementModelForEnv(configEnvironment).ifPresent(model -> {
                configManagementService.setPolicyConfiguration(patchId, model);
            });

            RuleDefinitionModelGroup ruleModelGroup = ruleDefinitionModelGroupFactory.create(
                    patchId,
                    RuleDefinitionModelGroupFactory.convertItemsToMap(((Entity.Policy) entity).getRuleItems())
            );

            decisionPolicyRuntimeLoadService.loadRulesIntoDecisionRuntime(ruleModelGroup);
            ruleItems.put(patchId, ruleModelGroup);
            latestLoadedEntityVersionNumber.put(entity.getId(), entity.getInfo());

            // clean up the previous patch version
            String prevPatchId = prevEntity == null ? null : getPatchIdentifier(prevEntity);
            if (!patchId.equals(prevPatchId)) {
                // clean up the prior patch
                try {
                    unload(prevEntity);
                } catch (Exception e) {
                    logger.error(String.format("Error cleaning up this entity patch:=%s", prevPatchId), e);
                }
            }

        } else {
            throw new UnsupportedOperationException(
                    String.format("Unsupported Entity type [entityInfo=%s]", entity.getInfo()));
        }
    }

    @Override
    public void unload(EntityInfo info) {
        String patchIdentifier = getPatchIdentifier(info);
        RuleDefinitionModelGroup remove = ruleItems.remove(patchIdentifier);
        if (remove != null) {
            decisionPolicyRuntimeLoadService.removeDecisionRuntime(remove);
        }
        configManagementService.deletePolicyConfiguration(patchIdentifier);
        latestLoadedEntityVersionNumber.remove(info.getId(), info);

    }

    @Override
    public PolicyResult<DecisionPolicyResponse> invoke(LogicalVersion policyVersion, DecisionPolicyRequest request) {
        // validate
        String reservedConfigPrefix = "config.";
        Set<String> keys = request.getBody().keySet();
        Optional<String> invalidKey = keys.stream().filter(key -> key.startsWith(reservedConfigPrefix)).findFirst();
        if (invalidKey.isPresent()) {
            PolicyErrorInfo errorInfo = new PolicyErrorInfo(PolicyError.BAD_REQUEST_PARAMETER_RESERVED_PREFIX,
                    Strings.join(keys, ','));
            return new PolicyResult.Failure<>(errorInfo);
        }

        // prepare arguments
        String identifier = getPatchIdentifier(policyVersion);
        final Map<String, Object> argument = new HashMap<>(request.getBody());

        // config-management-based arguments
        ConfigManagementModel policyConfiguration = configManagementService.getPolicyConfiguration(identifier);
        Map<String, Serializable> configMap = Collections.emptyMap();
        if (policyConfiguration != null) {
            // business event must match a configuration if defined in the policy
            configMap = policyConfiguration.getConfiguration(
                    request.getBusinessEventName(),
                    configurationMatchingStrategy).orElse(null);
        }
        if (configMap == null) {
            PolicyErrorInfo errorInfo = new PolicyErrorInfo(PolicyError.BAD_REQUEST_MISSING_CONFIG,
                    String.format("Could not find a configuration that matches supplied business event '%s'",
                            request.getBusinessEventName()));
            return new PolicyResult.Failure<>(errorInfo);
        }
        configMap.forEach((key, value) -> {
            argument.put(reservedConfigPrefix + key, value);
        });
        DecisionEvaluateRequest decisionEvaluateRequest = DecisionEvaluateRequest.builder()
                .runtimeId(identifier)
                .dmnName(request.getDmnName())
                .input(argument)
                .build();

        // evaluate
        RuleDefinitionModelGroup ruleDefinitionModelGroup = ruleItems.get(identifier);
        if (ruleDefinitionModelGroup == null) {
            throw new IllegalArgumentException("Requested Policy Not Found. identifier:=" + identifier);
        } else if (ruleDefinitionModelGroup.getRuleDefinitionModelList().stream().noneMatch(model -> model.getRuleShortName().equals(request.getDmnName()))) {
            PolicyErrorInfo errorInfo = new PolicyErrorInfo(PolicyError.BAD_REQUEST_MISSING_DMN_FILE,
                    String.format("Could not find dmn with name '%s' in requested policy.", request.getDmnName()));
            return new PolicyResult.Failure<>(errorInfo);
        }

        try {
            DecisionEvaluateResponse response = decisionPolicyRuntimeLoadService.evaluate(decisionEvaluateRequest);
            if (response.getStatus() == DecisionEngineStatus.SUCCESS) {
                Map<String, Serializable> resultMap = new HashMap<>();
                response.getResult().forEach((key, value) -> resultMap.put(key, (Serializable) value));
                return new PolicyResult.Success<>(new DecisionPolicyResponse(resultMap));
            } else {
                PolicyErrorInfo errorInfo = new PolicyErrorInfo(PolicyError.POLICY_EXECUTION_ERROR,
                        "Decision execution error.");
                return new PolicyResult.Failure<>(errorInfo);
            }
        } catch (Exception e) {
            PolicyErrorInfo errorInfo = new PolicyErrorInfo(PolicyError.POLICY_EXECUTION_ERROR,
                    "Decision invocation error.");
            return new PolicyResult.Failure<>(errorInfo);
        }

    }

}
