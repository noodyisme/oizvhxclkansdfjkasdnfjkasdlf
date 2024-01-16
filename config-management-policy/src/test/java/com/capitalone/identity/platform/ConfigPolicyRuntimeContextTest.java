package com.capitalone.identity.platform;

import com.capitalone.identity.identitybuilder.configmanagement.MatchingStrategies;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.LogicalVersion;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.platform.loading.TestContent;
import com.capitalone.identity.platform.runtime.PolicyError;
import com.capitalone.identity.platform.runtime.PolicyErrorInfo;
import com.capitalone.identity.platform.runtime.PolicyResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigPolicyRuntimeContextTest {
    private Entity.Policy newConfigOnlyPolicy() {
        Set<ConfigStoreItem> items = Stream.of(
                new ConfigStoreItem("a/b/c/1.0/config/schema.json", TestContent.SIMPLE_CONFIG_SCHEMA),
                new ConfigStoreItem("a/b/c/1.0/config/defaults.json", TestContent.SIMPLE_CONFIG_DEFAULTS),
                new ConfigStoreItem("a/b/c/1.0/config/usecase_A.json", TestContent.SIMPLE_CONFIG_USECASE_A),
                new ConfigStoreItem("a/b/c/1.0/policy-metadata.json", TestContent.POLICY_METADATA_CONFIG_AVAILABLE)
        ).collect(Collectors.toSet());
        return new Entity.Policy(
                new EntityInfo.Policy(
                        new PolicyDefinition("a/b/c", "1.0"),
                        items.stream().map(item -> item.info).collect(Collectors.toSet())),
                items
        );
    }

    private Entity.Policy newConfigOnlyPolicyV2() {
        Set<ConfigStoreItem> items = Stream.of(
                new ConfigStoreItem("a/b/c/2.0/config/schema.json", TestContent.SIMPLE_CONFIG_V2_SCHEMA),
                new ConfigStoreItem("a/b/c/2.0/config/defaults.json", TestContent.SIMPLE_CONFIG_V2_DEFAULTS),
                new ConfigStoreItem("a/b/c/2.0/config/features.json", TestContent.SIMPLE_CONFIG_V2_FEATURES),
                new ConfigStoreItem("a/b/c/2.0/config/features-qa.json", TestContent.SIMPLE_CONFIG_V2_FEATURES_QA),
                new ConfigStoreItem("a/b/c/2.0/config/usecase_A.json", TestContent.SIMPLE_CONFIG_USECASE_A),
                new ConfigStoreItem("a/b/c/2.0/policy-metadata.json", TestContent.POLICY_METADATA_CONFIG_AVAILABLE)
        ).collect(Collectors.toSet());
        return new Entity.Policy(
                new EntityInfo.Policy(
                        new PolicyDefinition("a/b/c", "2.0"),
                        items.stream().map(item -> item.info).collect(Collectors.toSet())),
                items
        );
    }

    private Entity.Pip newPipEntity() {
        Set<ConfigStoreItem> items = Stream.of(
                new ConfigStoreItem("a/b/c/1.0/process/sample-1.0-get.xml", TestContent.SAMPLE_PIP)
        ).collect(Collectors.toSet());
        return new Entity.Pip(
                new EntityInfo.Pip("1.0", "a/b/c",
                        items.stream().map(item -> item.info).collect(Collectors.toSet())),
                items
        );
    }

    @Test
    void loaded_entities_lookup_test_success() {
        ConfigPolicyRuntimeContext context = new ConfigPolicyRuntimeContext(MatchingStrategies.MATCH_ALL_NON_NULL, null);
        context.load(newConfigOnlyPolicy());
        context.load(newConfigOnlyPolicyV2());

        assertDoesNotThrow(context::getLoadedEntities);
        ArrayList<EntityInfo> actualLoadedEntities = (ArrayList<EntityInfo>) context.getLoadedEntities();
        Assertions.assertEquals(2, actualLoadedEntities.size());
        assertEquals(newConfigOnlyPolicy().getInfo(), actualLoadedEntities.get(0));
        assertEquals(newConfigOnlyPolicyV2().getInfo(), actualLoadedEntities.get(1));
    }

    @Test
    void unload_entity_success() {
        ConfigPolicyRuntimeContext context = new ConfigPolicyRuntimeContext(MatchingStrategies.MATCH_ALL_NON_NULL, null);
        context.load(newConfigOnlyPolicy());
        context.load(newConfigOnlyPolicyV2());

        // Fetching all loaded entities before unload
        ArrayList<EntityInfo> beforeUnloadLoadedEntities = (ArrayList<EntityInfo>) context.getLoadedEntities();
        Assertions.assertEquals(2, beforeUnloadLoadedEntities.size());

        Entity.Policy expectedPolicy = newConfigOnlyPolicy();
        context.unload(expectedPolicy.getInfo());

        // Fetching all loaded entities after unload
        ArrayList<EntityInfo> afterUnloadLoadedEntities = (ArrayList<EntityInfo>) context.getLoadedEntities();
        Assertions.assertEquals(1, afterUnloadLoadedEntities.size());
    }

    @Test
    void loading_pip_entity() {
        ConfigPolicyRuntimeContext context = new ConfigPolicyRuntimeContext(MatchingStrategies.MATCH_ALL_NON_NULL, null);
        context.load(newConfigOnlyPolicy());
        context.load(newConfigOnlyPolicyV2());

        Assertions.assertEquals(2, context.getLoadedEntities().size());

        Entity.Pip pipEntity = newPipEntity();
        assertThrows(UnsupportedOperationException.class, () -> {
            context.load(pipEntity);
        }, String.format("Unsupported Entity type [entityInfo=%s]", pipEntity.getInfo()));

        Assertions.assertEquals(2, context.getLoadedEntities().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.0",
            "2.0",
    })
    void test_invoke_config_only_policy_default_match_all_non_null_strategy(String version) {
        ConfigPolicyRuntimeContext context = new ConfigPolicyRuntimeContext(MatchingStrategies.MATCH_ALL_NON_NULL, null);
        context.load(newConfigOnlyPolicy());
        context.load(newConfigOnlyPolicyV2());

        ConfigurationPolicyResponse expectedResponse = new ConfigurationPolicyResponse(new HashMap<>() {{
            put("param-A", "Z");
            put("param-B", "Y");
        }});

        MockLogicalVersion logicalVersion = new MockLogicalVersion("a/b/c", version);
        ConfigurationPolicyRequest request = new ConfigurationPolicyRequest("");
        PolicyResult<ConfigurationPolicyResponse> actualResponse = context.invoke(logicalVersion, request);
        assertEquals(expectedResponse, actualResponse.getResult());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.0",
            "2.0",
    })
    void test_invoke_config_only_policy_default_match_exact_strategy(String version) {
        ConfigPolicyRuntimeContext context = new ConfigPolicyRuntimeContext(MatchingStrategies.MATCH_EXACT_ONLY, null);
        context.load(newConfigOnlyPolicy());
        context.load(newConfigOnlyPolicyV2());

        Assertions.assertEquals(2, context.getLoadedEntities().size());

        LogicalVersion logicalVersion = new MockLogicalVersion("a/b/c", version);
        ConfigurationPolicyRequest request = new ConfigurationPolicyRequest("");

        PolicyErrorInfo expectedErrorInfo = new PolicyErrorInfo(PolicyError.BAD_REQUEST_MISSING_CONFIG,
                String.format("Could not find config with the use case name '%s' in requested policy.", request.getBusinessEventName()));

        PolicyResult<ConfigurationPolicyResponse> actualResponse = context.invoke(logicalVersion, request);
        assertEquals(expectedErrorInfo, actualResponse.getErrorInfo());
    }

    @ParameterizedTest
    @CsvSource({
            "qa,Y-QA",
            "prod,Y",
            "dev,Y",
            ",Y",
    })
    void test_invoke_config_only_policy_default_environmentSpecific_match_all_non_null_strategy(String env, String expectedProperty) {
        ConfigPolicyRuntimeContext context = new ConfigPolicyRuntimeContext(MatchingStrategies.MATCH_ALL_NON_NULL, env);
        context.load(newConfigOnlyPolicy());
        context.load(newConfigOnlyPolicyV2());
        ConfigurationPolicyResponse expectedResponse = new ConfigurationPolicyResponse(new HashMap<>() {{
            put("param-A", "Z");
            put("param-B", expectedProperty);
        }});
        LogicalVersion logicalVersion = new MockLogicalVersion("a/b/c", "2.0");
        ConfigurationPolicyRequest request = new ConfigurationPolicyRequest("");
        PolicyResult<ConfigurationPolicyResponse> actualResponse = context.invoke(logicalVersion, request);
        assertEquals(expectedResponse, actualResponse.getResult());
    }

    private static class MockLogicalVersion implements LogicalVersion {
        final int major;
        final int minor;

        final String name;

        private MockLogicalVersion(String name, String version) {
            this.major = Integer.parseInt(version.split("\\.")[0]);
            this.minor = Integer.parseInt(version.split("\\.")[1]);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getMajorVersion() {
            return major;
        }

        @Override
        public int getMinorVersion() {
            return minor;
        }

        @Override
        public int getPatchVersion() {
            return 0;
        }
    }
}
