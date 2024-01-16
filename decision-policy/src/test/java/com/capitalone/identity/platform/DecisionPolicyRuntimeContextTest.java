package com.capitalone.identity.platform;

import com.capitalone.identity.identitybuilder.configmanagement.MatchingStrategies;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.platform.dmn.DecisionPolicyRuntimeLoadService;
import com.capitalone.identity.platform.loading.TestContent;
import com.capitalone.identity.platform.runtime.PolicyError;
import com.capitalone.identity.platform.runtime.PolicyErrorInfo;
import com.capitalone.identity.platform.runtime.PolicyResult;
import com.capitalone.identity.platform.runtime.PolicyResultStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DecisionPolicyRuntimeContextTest {

    Entity.Policy newBlankDecisionPolicy(String name, int major, int minor, int patch) {
        //String location, String policyFullName, String policyShortName, int majorVersion, int minorVersion, int patchVersion)
        String location = String.format("%s/%s.%s/%s", name, major, minor, patch);
        Set<ConfigStoreItem> items = Stream.of(
                new ConfigStoreItem(location + "/rules/simple.dmn", TestContent.DMN_BLANK),
                new ConfigStoreItem(location + "/config/defaults.json", TestContent.SIMPLE_CONFIG_DEFAULTS),
                new ConfigStoreItem(location + "/config/schema.json", TestContent.SIMPLE_CONFIG_SCHEMA),
                new ConfigStoreItem(location + "/config/A.A.A.A.json", TestContent.SIMPLE_CONFIG_USECASE_A),
                new ConfigStoreItem(location + "/policy-metadata.json", TestContent.POLICY_METADATA_DECISION_AVAILABLE)
        ).collect(Collectors.toSet());
        return new Entity.Policy(
                new EntityInfo.Policy(
                        new PolicyDefinition(location, name, name, major, minor, patch),
                        items.stream().map(item -> item.info).collect(Collectors.toSet())),
                items
        );
    }

    Entity.Policy newSimpleDecisionPolicy(String name, int major, int minor, int patch) {
        //String location, String policyFullName, String policyShortName, int majorVersion, int minorVersion, int patchVersion)
        String location = String.format("%s/%s.%s/%s", name, major, minor, patch);
        Set<ConfigStoreItem> items = Stream.of(
                new ConfigStoreItem(location + "/rules/simple.dmn", TestContent.SIMPLE_DMN),
                new ConfigStoreItem(location + "/config/defaults.json", TestContent.SIMPLE_CONFIG_DEFAULTS),
                new ConfigStoreItem(location + "/config/schema.json", TestContent.SIMPLE_CONFIG_SCHEMA),
                new ConfigStoreItem(location + "/config/A.A.A.A.json", TestContent.SIMPLE_CONFIG_USECASE_A),
                new ConfigStoreItem(location + "/policy-metadata.json", TestContent.POLICY_METADATA_DECISION_AVAILABLE)
        ).collect(Collectors.toSet());
        return new Entity.Policy(
                new EntityInfo.Policy(
                        new PolicyDefinition(location, name, name, major, minor, patch),
                        items.stream().map(item -> item.info).collect(Collectors.toSet())),
                items
        );
    }

    @Test
    void load() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();

        List<Entity.Policy> policies = Arrays.asList(
                newBlankDecisionPolicy("a/b/a", 1, 0, 0),
                newBlankDecisionPolicy("a/b/a", 1, 1, 1),
                newBlankDecisionPolicy("a/b/b", 1, 0, 0),
                newBlankDecisionPolicy("a/b/b", 1, 10, 5),
                newBlankDecisionPolicy("a/b/a", 2, 0, 0),
                newBlankDecisionPolicy("a/b/a", 2, 1, 0),
                newBlankDecisionPolicy("a/b/b", 2, 1, 0),
                newBlankDecisionPolicy("a/b/b", 2, 20, 0)
        );

        policies.forEach(context::load);

        Set<EntityInfo.Policy> expected = policies.stream().map(Entity.Policy::getInfo).collect(Collectors.toSet());
        Set<EntityInfo> loadedEntities = new HashSet<>(context.getLoadedEntities());
        assertEquals(policies.size(), loadedEntities.size());
        assertEquals(expected, loadedEntities);

    }

    @Test
    void loadAndInvoke_patch_change() {
        DecisionPolicyRuntimeContext context = Mockito.spy(new DecisionPolicyRuntimeContext());

        Entity.Policy policy1 = newSimpleDecisionPolicy("a/b/c", 1, 0, 1);
        Entity.Policy policy2 = newBlankDecisionPolicy("a/b/c", 1, 0, 2);
        context.load(policy1);
        context.load(policy2);

        List<EntityInfo> loadedEntities = new ArrayList<>(context.getLoadedEntities());
        assertEquals(Collections.singletonList(policy2.getInfo()), loadedEntities);

        // check prior policy patch version was unloaded
        verify(context, times(1)).unload(policy1.getInfo());

        DecisionPolicyRequest request = new DecisionPolicyRequest(new HashMap<>(), "XYZ", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policy2.getInfo(), request);
        assertDecisionSuccess(result);
    }

    @Test
    void loadAndInvoke_patch_change_with_unloadError() {
        DecisionPolicyRuntimeContext context = Mockito.spy(new DecisionPolicyRuntimeContext());
        doThrow(new RuntimeException("test")).when(context).unload(any());

        Entity.Policy policy1 = newSimpleDecisionPolicy("a/b/c", 1, 0, 1);
        Entity.Policy policy2 = newBlankDecisionPolicy("a/b/c", 1, 0, 2);
        context.load(policy1);
        assertDoesNotThrow(() -> context.load(policy2));

        List<EntityInfo> loadedEntities = new ArrayList<>(context.getLoadedEntities());
        assertEquals(Collections.singletonList(policy2.getInfo()), loadedEntities);

        DecisionPolicyRequest request = new DecisionPolicyRequest(new HashMap<>(), "XYZ", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policy2.getInfo(), request);
        assertDecisionSuccess(result);

    }

    @Test
    void loadAndInvoke_idempotent() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();
        Entity.Policy policy = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policy);
        context.load(policy);

        assertEquals(Collections.singletonList(policy.getInfo()), context.getLoadedEntities());

        DecisionPolicyRequest request = new DecisionPolicyRequest(new HashMap<>(), "XYZ", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policy.getInfo(), request);
        assertDecisionSuccess(result);

    }

    @Test
    void loadAndInvoke_unsafe_patch_content_update() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();

        // policy content at the same patch version is replaced
        Entity.Policy policyA = newSimpleDecisionPolicy("a/b/c", 1, 0, 0);
        Entity.Policy policyB = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policyA);
        context.load(policyB);

        assertEquals(Collections.singletonList(policyB.getInfo()), context.getLoadedEntities());

        DecisionPolicyRequest request = new DecisionPolicyRequest(new HashMap<>(), "XYZ", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policyB.getInfo(), request);
        assertDecisionSuccess(result);

    }

    @Test
    void unload() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();

        List<Entity.Policy> policies = Arrays.asList(
                newBlankDecisionPolicy("a/b/a", 1, 0, 0),
                newBlankDecisionPolicy("a/b/a", 1, 1, 1),
                newBlankDecisionPolicy("a/b/b", 1, 0, 0),
                newBlankDecisionPolicy("a/b/b", 1, 10, 5),
                newBlankDecisionPolicy("a/b/a", 2, 0, 0),
                newBlankDecisionPolicy("a/b/a", 2, 1, 0),
                newBlankDecisionPolicy("a/b/b", 2, 1, 0),
                newBlankDecisionPolicy("a/b/b", 2, 20, 0)
        );

        policies.forEach(context::load);
        policies.stream().map(Entity.Policy::getInfo).forEach(context::unload);

        assertTrue(context.getLoadedEntities().isEmpty());

    }

    @Test
    void unload_differentPatchHasNoEffect() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();

        Entity.Policy policyPatch1 = newBlankDecisionPolicy("a/b/a", 1, 0, 0);
        Entity.Policy policyPatch2 = newBlankDecisionPolicy("a/b/a", 1, 0, 1);

        context.load(policyPatch1);
        context.unload(policyPatch2.getInfo());
        assertEquals(Collections.singletonList(policyPatch1.getInfo()), context.getLoadedEntities());

    }

    @Test
    void invoke_decision_with_no_config_policy_success() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();
        Entity.Policy policy = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policy);

        DecisionPolicyRequest request = new DecisionPolicyRequest(new HashMap<>(), "XYZ", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policy.getInfo(), request);
        assertDecisionSuccess(result);

    }

    @ParameterizedTest
    @CsvSource({
            "A.A.A.A,A",
            "UNKNOWN-Business-Event,Z",
    })
    void invoke_decision_with_config_policy_success(String businessEvent, String expectedConfigOutput) {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext(MatchingStrategies.MATCH_ALL_NON_NULL);
        Entity.Policy policy = newSimpleDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policy);

        HashMap<String, Serializable> requestBody = new HashMap<String, Serializable>() {{
            put("param-A", "stringValue");
        }};
        DecisionPolicyRequest request = new DecisionPolicyRequest(requestBody, businessEvent, "simple.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policy.getInfo(), request);

        DecisionPolicyResponse expectResponse = new DecisionPolicyResponse(new HashMap<String, Serializable>() {{
            put("Decision-1", String.format("Decision-1 Output (config.param-A=%s, param-A=stringValue)", expectedConfigOutput));
            put("Decision-2", "Decision-2 Output (param-A=stringValue)");
        }});
        assertDecisionSuccess(expectResponse, result);

    }

    @Test
    void invoke_error_missingDmn() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();
        Entity.Policy policy = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policy);

        DecisionPolicyRequest request = new DecisionPolicyRequest(new HashMap<>(), "A.B.C.D", "unknownDMN.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policy.getInfo(), request);

        PolicyErrorInfo expectedError = new PolicyErrorInfo(PolicyError.BAD_REQUEST_MISSING_DMN_FILE,
                "Could not find dmn with name 'unknownDMN.dmn' in requested policy.");
        assertDecisionFailure(expectedError, result);

    }

    @Test
    void invoke_error_requestUsesReservedPrefix() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();
        Entity.Policy policy = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policy);

        String reservedParameterKey = "config.test";
        HashMap<String, Serializable> body = new HashMap<String, Serializable>() {{
            put(reservedParameterKey, "stringValue");
        }};

        DecisionPolicyRequest request = new DecisionPolicyRequest(body, "A.B.C.D", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policy.getInfo(), request);

        PolicyErrorInfo expectedError = new PolicyErrorInfo(PolicyError.BAD_REQUEST_PARAMETER_RESERVED_PREFIX,
                reservedParameterKey);
        assertDecisionFailure(expectedError, result);

    }

    @Test
    void invoke_error_executionError() {
        DecisionPolicyRuntimeLoadService mock = Mockito.mock(DecisionPolicyRuntimeLoadService.class);
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext(mock);
        RuntimeException testException = new RuntimeException("test");
        when(mock.evaluate(any())).thenThrow(testException);
        Entity.Policy policy = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policy);

        DecisionPolicyRequest request = new DecisionPolicyRequest(new HashMap<>(), "A.B.C.D", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> result = context.invoke(policy.getInfo(), request);

        PolicyErrorInfo expectedError = new PolicyErrorInfo(PolicyError.POLICY_EXECUTION_ERROR,
                "Decision invocation error.");
        assertDecisionFailure(expectedError, result);

    }

    @Test
    void invoke_error_missingPolicy() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();

        Entity.Policy policy = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        EntityInfo.Policy info = policy.getInfo();
        DecisionPolicyRequest request = new DecisionPolicyRequest(
                new HashMap<>(), "A.B.C.D", "unknownDMN.dmn");

        String msg = assertThrows(IllegalArgumentException.class, () -> context.invoke(info, request)).getMessage();
        assertTrue(msg.contains("Requested Policy Not Found"));
        assertTrue(msg.contains("a/b/c/1.0.0"));

    }

    @Test
    void invoke_matchingStrategyDefault() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext();
        Entity.Policy policy = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policy);

        for (String businessEvent : Arrays.asList("A.A.A.A", "W.X.Y.Z")) {
            DecisionPolicyRequest successRequest = new DecisionPolicyRequest(new HashMap<>(), businessEvent, "simple.dmn");
            PolicyResult<DecisionPolicyResponse> successResult = context.invoke(policy.getInfo(), successRequest);
            DecisionPolicyResponse expectSuccessResponse = new DecisionPolicyResponse(new HashMap<>());
            assertDecisionSuccess(expectSuccessResponse, successResult);
        }

    }

    @Test
    void invoke_matchingStrategyExact() {
        DecisionPolicyRuntimeContext context = new DecisionPolicyRuntimeContext(MatchingStrategies.MATCH_EXACT_ONLY);
        Entity.Policy policy = newBlankDecisionPolicy("a/b/c", 1, 0, 0);
        context.load(policy);

        DecisionPolicyRequest successRequest = new DecisionPolicyRequest(new HashMap<>(), "A.A.A.A", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> successResult = context.invoke(policy.getInfo(), successRequest);
        DecisionPolicyResponse expectSuccessResponse = new DecisionPolicyResponse(new HashMap<>());
        assertDecisionSuccess(expectSuccessResponse, successResult);

        DecisionPolicyRequest failRequest = new DecisionPolicyRequest(new HashMap<>(), "W.X.Y.Z", "simple.dmn");
        PolicyResult<DecisionPolicyResponse> failResult = context.invoke(policy.getInfo(), failRequest);
        PolicyErrorInfo errorInfo = new PolicyErrorInfo(PolicyError.BAD_REQUEST_MISSING_CONFIG,
                "Could not find a configuration that matches supplied business event 'W.X.Y.Z'");
        assertDecisionFailure(errorInfo, failResult);

    }

    private void assertDecisionFailure(PolicyErrorInfo expectedError,
                                       PolicyResult<DecisionPolicyResponse> actualResult) {
        assertEquals(PolicyResultStatus.FAILURE, actualResult.getStatus());
        assertNull(actualResult.getResult());
        assertEquals(expectedError, actualResult.getErrorInfo());
    }

    private void assertDecisionSuccess(PolicyResult<DecisionPolicyResponse> actualResult) {
        assertDecisionSuccess(new DecisionPolicyResponse(new HashMap<>()), actualResult);
    }

    private void assertDecisionSuccess(DecisionPolicyResponse expectedResponse,
                                       PolicyResult<DecisionPolicyResponse> actualResult) {
        assertEquals(PolicyResultStatus.SUCCESS, actualResult.getStatus(),
                () -> Objects.requireNonNull(actualResult.getErrorInfo()).toString());
        assertEquals(expectedResponse, actualResult.getResult());
        assertNull(actualResult.getErrorInfo());
    }

}
