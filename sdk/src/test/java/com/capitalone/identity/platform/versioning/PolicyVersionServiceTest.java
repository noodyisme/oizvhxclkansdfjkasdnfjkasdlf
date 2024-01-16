package com.capitalone.identity.platform.versioning;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.LogicalVersion;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.paukov.combinatorics3.Generator;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PolicyVersionServiceTest {


    private static PolicyDataObject getMockVersion(String name) {
        String[] split = name.split("/");
        String fullName = Strings.join(Arrays.asList(Arrays.copyOf(split, 3)), '/');
        String version = split[3];
        String[] semanticVersions = version.split("\\.");
        int major = Integer.parseInt(semanticVersions[0]);
        int minor = Integer.parseInt(semanticVersions[1]);
        int patch = semanticVersions.length < 3 ? 0 : Integer.parseInt(semanticVersions[2]);
        return new PolicyDataObject(minor, major, patch, fullName);
    }

    @Test
    void getHighestMinorVersion() {
        PolicyVersionService service = new PolicyVersionService();
        service.set(getMockVersion("a/b/c/1.0"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/1.1"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/1.2"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/1.3"), EntityActivationStatus.DISABLED);
        service.set(getMockVersion("a/b/c/2.0"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/2.2"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/2.13"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/2.14"), EntityActivationStatus.AVAILABLE);

        assertEquals(getMockVersion("a/b/c/1.2"), service.getPolicyVersion("a/b/c", "1"));
        assertEquals(getMockVersion("a/b/c/2.13"), service.getPolicyVersion("a/b/c", "2"));
    }

    @Test
    void getHighestMinorVersion_loadedOutOfOrder() {
        PolicyVersionService service = new PolicyVersionService();
        service.set(getMockVersion("a/b/c/1.2"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/1.1"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/1.0"), EntityActivationStatus.ACTIVE);
        assertEquals(getMockVersion("a/b/c/1.2"), service.getPolicyVersion("a/b/c", "1"));
    }

    @Test
    void getHighestMinorVersion_notLoaded() {
        PolicyVersionService service = new PolicyVersionService();
        assertNull(service.getPolicyVersion("c", "3"));
    }

    @Test
    void getHighestMinorVersion_specified() {
        PolicyVersionService service = new PolicyVersionService();
        service.set(getMockVersion("a/b/c/1.1"), EntityActivationStatus.DISABLED);
        service.set(getMockVersion("a/b/c/1.2"), EntityActivationStatus.ACTIVE);
        service.set(getMockVersion("a/b/c/1.3"), EntityActivationStatus.AVAILABLE);

        assertNull(service.getPolicyVersion("c", "1.1"));
        assertEquals(getMockVersion("a/b/c/1.2"), service.getPolicyVersion("a/b/c", "1.2"));
        assertEquals(getMockVersion("a/b/c/1.3"), service.getPolicyVersion("a/b/c", "1.3"));
    }

    @Test
    void getHighestMinorVersion_putDynamic() {
        PolicyVersionService service = new PolicyVersionService();
        service.set(getMockVersion("a/b/c/1.0"), EntityActivationStatus.ACTIVE);
        assertEquals(getMockVersion("a/b/c/1.0"), service.getPolicyVersion("a/b/c", "1"));
        service.set(getMockVersion("a/b/c/1.1"), EntityActivationStatus.ACTIVE);
        assertEquals(getMockVersion("a/b/c/1.1"), service.getPolicyVersion("a/b/c", "1"));
        service.remove(getMockVersion("a/b/c/1.1"));
        assertEquals(getMockVersion("a/b/c/1.0"), service.getPolicyVersion("a/b/c", "1"));
    }

    @Test
    void removeOk() {
        PolicyVersionService service = new PolicyVersionService();
        LogicalVersion policy = getMockVersion("a/b/c/1.1");
        assertDoesNotThrow(() -> service.remove(policy));
        service.set(policy, EntityActivationStatus.ACTIVE);
        assertEquals(getMockVersion("a/b/c/1.1"), service.getPolicyVersion("a/b/c", "1"));
        assertDoesNotThrow(() -> service.remove(policy));
        assertNotEquals(getMockVersion("a/b/c/1.1"), service.getPolicyVersion("a/b/c", "1"));


        service.set(policy, EntityActivationStatus.ACTIVE);
        LogicalVersion noExistPolicy = getMockVersion("k/l/c/1.1");
        assertDoesNotThrow(() -> service.remove(noExistPolicy));
        assertEquals(getMockVersion("a/b/c/1.1"), service.getPolicyVersion("a/b/c", "1"));
    }

    @Test
    void removeOk_ignoreConflictRemoval() {
        PolicyVersionService service = new PolicyVersionService();
        LogicalVersion policy = getMockVersion("a/b/c/1.1");
        service.set(policy, EntityActivationStatus.ACTIVE);
        LogicalVersion conflictPolicy = getMockVersion("k/l/c/1.1");
        assertDoesNotThrow(() -> service.remove(conflictPolicy));
        assertEquals(getMockVersion("a/b/c/1.1"), service.getPolicyVersion("a/b/c", "1"));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1,2,0,1",
            "1,10,0,1",
            "9,10,0,1",
            "1,2,0,10",
            "1,10,0,10",
            "9,10,0,10",

    })
    void checkMajorVersionIsolation(int majorVersionLow, int majorVersionHigh,
                                    int minorVersionLow, int minorVersionHigh) {

        List<Integer> majorVersions = Arrays.asList(majorVersionLow, majorVersionHigh);
        List<Integer> minorVersions = Arrays.asList(minorVersionLow, minorVersionHigh);
        List<Integer> patchVersions = Arrays.asList(0, 1, 10, 11, 100);

        // check to ensure test has reasonable count of permutations
        // total permutations = patchVersionsCount^minorVersionsCount * minorVersionCount*(minorVersionCount-1)
        int minorVersionsCount = majorVersions.size() * minorVersions.size();
        double expected = Math.pow(patchVersions.size(), minorVersionsCount) * minorVersionsCount * (minorVersionsCount - 1);
        assertFalse(expected > 10000, String.format("Illegal dataset Larger than 10,000 (actual=%s)", expected));

        // list of possible patch versions for each major version
        List<List<String>> majorVersionList = majorVersions.stream().map(majorVersion ->
                        minorVersions.stream().flatMap(minorVersion ->
                                        patchVersions.stream().map(patch -> Strings.join(Arrays.asList(majorVersion, minorVersion, patch), '.')))
                                .collect(Collectors.toList()))
                .collect(Collectors.toList());

        // cartesian product - list all possible combinations, across major versions, of different patch versions
        List<List<String>> testPairs = Generator.cartesianProduct(majorVersionList).stream()
                // permutations - list all possible pairwise permutations for the combinations (ensures all load orders are checked)
                .flatMap(combination -> Generator.permutation(combination).k(2).stream())
                .collect(Collectors.toList());

        // isolation check of each combination
        for (List<String> pair : testPairs) {
            PolicyVersionService service = new PolicyVersionService();
            for (String version : pair) {
                final String majorVersion = version.substring(0, version.indexOf('.'));
                LogicalVersion entity = getMockVersion("a/b/c/" + version);
                service.set(entity, EntityActivationStatus.ACTIVE);

                LogicalVersion majorResult = service.getPolicyVersion("a/b/c", majorVersion);
                assertEquals(PolicyDataObject.create(entity), majorResult);
            }
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
            "1,2,0,1",
            "1,2,0,10",
            "1,2,0,11",
            "1,2,0,100",
            "1,2,1,10",
            "1,2,1,11",
            "1,2,1,100",
            "1,10,0,1",
            "1,10,0,10",
            "1,10,0,11",
            "1,10,0,100",
            "1,10,1,10",
            "1,10,1,11",
            "1,10,1,100",
    })
    void checkMinorVersionIsolation(int majorVersionA, int majorVersionB,
                                    int minorVersionA, int minorVersionB) {

        List<Integer> majorVersions = Arrays.asList(majorVersionA, majorVersionB);
        List<Integer> minorVersions = Arrays.asList(minorVersionA, minorVersionB);
        List<Integer> patchVersions = Arrays.asList(0, 1, 10, 11, 100);

        // check to ensure test has reasonable count of permutations
        // total permutations = patchVersionsCount^minorVersionsCount * minorVersionCount*(minorVersionCount-1)
        int minorVersionsCount = majorVersions.size() * minorVersions.size();
        double expected = Math.pow(patchVersions.size(), minorVersionsCount) * minorVersionsCount * (minorVersionsCount - 1);
        assertFalse(expected > 10000, String.format("Illegal dataset Larger than 10,000 (actual=%s)", expected));

        // list of possible patch versions for each minor version
        List<List<String>> minorVersionList = majorVersions.stream().flatMap(majorVersion ->
                        minorVersions.stream().map(minorVersion ->
                                patchVersions.stream().map(patch -> Strings.join(Arrays.asList(majorVersion, minorVersion, patch), '.'))
                                        .collect(Collectors.toList())))
                .collect(Collectors.toList());

        // cartesian product - list all possible combinations across minor versions (of different patch versions)
        List<List<String>> testPairs = Generator.cartesianProduct(minorVersionList).stream()
                // permutations - list all possible pairwise permutations for the combinations (ensures all load orders are checked)
                .flatMap(combination -> Generator.permutation(combination).k(2).stream())
                .collect(Collectors.toList());

        // isolation check of each combination
        for (List<String> pair : testPairs) {
            PolicyVersionService service = new PolicyVersionService();

            for (String version : pair) {
                final String minorVersion = version.substring(0, version.lastIndexOf('.'));
                LogicalVersion entity = getMockVersion("a/b/c/" + version);
                service.set(entity, EntityActivationStatus.ACTIVE);

                LogicalVersion minorResult = service.getPolicyVersion("a/b/c", minorVersion);
                assertEquals(PolicyDataObject.create(entity), minorResult);
            }

        }
    }

    @Test
    void getHighestPatchVersionActivationStatus() {
        PolicyVersionService service = new PolicyVersionService();
        LogicalVersion patch1 = getMockVersion("a/b/c/1.0.1");

        service.set(patch1, EntityActivationStatus.AVAILABLE);
        assertNull(service.getPolicyVersion("a/b/c", "1"));
        assertEquals(patch1, PolicyDataObject.create(service.getPolicyVersion("a/b/c", "1.0")));

        LogicalVersion patch2 = getMockVersion("a/b/c/1.0.2");
        service.set(patch2, EntityActivationStatus.ACTIVE);
        assertEquals(patch2, PolicyDataObject.create(service.getPolicyVersion("a/b/c", "1")));
        assertEquals(patch2, PolicyDataObject.create(service.getPolicyVersion("a/b/c", "1.0")));

        LogicalVersion patch10 = getMockVersion("a/b/c/1.0.10");
        service.set(patch10, EntityActivationStatus.DISABLED);
        assertNull(service.getPolicyVersion("a/b/c", "1"));
        assertNull(service.getPolicyVersion("a/b/c", "1.0"));

        LogicalVersion patch3 = getMockVersion("a/b/c/1.0.3");
        service.set(patch3, EntityActivationStatus.ACTIVE);
        assertNull(service.getPolicyVersion("a/b/c", "1"));
        assertNull(service.getPolicyVersion("a/b/c", "1.0"));
    }
}
