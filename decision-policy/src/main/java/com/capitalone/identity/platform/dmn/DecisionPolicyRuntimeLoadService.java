package com.capitalone.identity.platform.dmn;

import com.capitalone.identity.identitybuilder.decisionengine.adapter.api.DecisionEngineRuntimeLoadResult;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEngineService;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateRequest;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateResponse;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionRuntimeLoadRequest;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionPolicyInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DecisionPolicyRuntimeLoadService {
    private static final Logger LOGGER = LogManager.getLogger(DecisionPolicyRuntimeLoadService.class);

    private final DecisionEngineService decisionEngineService;
    private final Map<String, String> runtimeIdToEntityIdMap = new HashMap<>();

    public DecisionPolicyRuntimeLoadService(DecisionEngineService decisionEngineService) {
        this.decisionEngineService = decisionEngineService;
    }

    /**
     * Load the specified rules as a decision runtime into decision engine core library
     * @param ruleDefinitionModelGroup the rules
     */
    public void loadRulesIntoDecisionRuntime(final RuleDefinitionModelGroup ruleDefinitionModelGroup) {
        final String runtimeId = ruleDefinitionModelGroup.getRuntimeId();
        final String identifier = ruleDefinitionModelGroup.getIdentifier();
        throwIfRuntimeIdConflictDetected(runtimeId, identifier);
        final Map<String, String> dmnContentMap = new HashMap<>();
        final List<RuleDefinitionModel> rules = ruleDefinitionModelGroup.getRuleDefinitionModelList();
        rules.forEach(newRule -> dmnContentMap.put(newRule.getRuleShortName(), newRule.getContent()));
        final DecisionEngineRuntimeLoadResult loadResult;
        try {
            loadResult = decisionEngineService.load(
                    DecisionRuntimeLoadRequest.builder()
                            .runtimeId(runtimeId)
                            .dmnContentMap(dmnContentMap)
                            .decisionPolicyInfoOptional(createDecisionPolicyInfo(ruleDefinitionModelGroup))
                            .supplementalAttributes(createSupplementalAttributes(ruleDefinitionModelGroup))
                            .build());
        } catch (final RuntimeException e) {
            throw new LoadRuleException(runtimeId, rules, e);
        }
        if (loadResult.getStatus() != DecisionEngineStatus.SUCCESS) {
            throw new LoadRuleException(runtimeId, rules);
        }
    }

    public void removeDecisionRuntime(final RuleDefinitionModelGroup ruleDefinitionModelGroup) {
        decisionEngineService.removeDecisionRuntime(ruleDefinitionModelGroup.getRuntimeId());
    }

    public DecisionEvaluateResponse evaluate(DecisionEvaluateRequest request) {
        return decisionEngineService.evaluate(request);
    }

    /**
     * Validation to confirm that the runtimeId is globally unique even between different entity identifiers.
     * Raise exception to caller if a conflicting runtime id is detected from another entity identifier.
     * @param runtimeId the runtimeId
     * @param identifier the identifier
     */
    private void throwIfRuntimeIdConflictDetected(final String runtimeId, final String identifier) {
        final String existingIdentifier = runtimeIdToEntityIdMap.get(runtimeId);
        if (existingIdentifier != null && !existingIdentifier.equals(identifier)) {
            throw new IllegalStateException(String.format("Conflicting decision engine runtime id, %s, detected "
                    + "between identifier %s and %s. The policy name and version must be unique among all policies. "
                    + "Please assign unique names to resolve.", runtimeId, existingIdentifier, identifier));
        }
        runtimeIdToEntityIdMap.put(runtimeId, identifier);
    }

    private Map<String, String> createSupplementalAttributes(final RuleDefinitionModelGroup ruleDefinitionModelGroup) {
        final String identifier = ruleDefinitionModelGroup.getIdentifier();
        final Map<String, String> retVal = new HashMap<>();

        // Add entity identifier
        retVal.put("identifier", identifier);

        return retVal;
    }

    private Optional<DecisionPolicyInfo> createDecisionPolicyInfo(final RuleDefinitionModelGroup ruleDefinitionModelGroup) {
        final String identifier = ruleDefinitionModelGroup.getIdentifier();
        final String runtimeId = ruleDefinitionModelGroup.getRuntimeId();
        final String[] runtimeIdSplit = StringUtils.split(runtimeId, "/");
        if (runtimeIdSplit.length != 2) {
            LOGGER.error("Unexpected decision runtimeId format detected. Entity Identifier: {}, Runtime id: {}",
                    identifier, runtimeId);
            return Optional.empty();
        }
        return Optional.of(DecisionPolicyInfo.builder()
                .policyName(runtimeIdSplit[0])
                .policyVersion(runtimeIdSplit[1])
                .build());
    }
}
