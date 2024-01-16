package com.capitalone.identity.platform.versioning;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.LogicalVersion;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PolicyVersionService {

    private final Map<PolicyDataObject, EntityActivationStatus> policyStatusMap = new HashMap<>();

    private final MultiValuedMap<String, PolicyDataObject> policyVersionMap = new HashSetValuedHashMap<>();

    public LogicalVersion getPolicyVersion(String policyFullName, String version) {
        if (version.contains(".")) {
            return policyVersionMap.get(policyFullName + version).stream()
                    .max(Comparator.comparingInt(PolicyDataObject::getPatchVersion))
                    .filter(policy -> {
                        EntityActivationStatus status = policyStatusMap.get(policy);
                        return status == EntityActivationStatus.ACTIVE || status == EntityActivationStatus.AVAILABLE;
                    })
                    .orElse(null);
        } else {
            final int majorVersion = Integer.parseInt(version);
            return policyVersionMap.get(policyFullName + majorVersion).stream()
                    .collect(Collectors.groupingBy(PolicyDataObject::getMinorVersion))
                    .values().stream()
                    .map(patches -> patches.stream().max(Comparator.comparingInt(PolicyDataObject::getPatchVersion)))
                    .flatMap(patch -> patch.map(Stream::of).orElseGet(Stream::empty))
                    .filter(entry -> policyStatusMap.get(entry) == EntityActivationStatus.ACTIVE)
                    .max(Comparator.comparingInt(PolicyDataObject::getMinorVersion))
                    .orElse(null);
        }
    }

    public void set(LogicalVersion policy, EntityActivationStatus policyActivationStatus) {
        PolicyDataObject policyDataObject = PolicyDataObject.create(policy);
        policyStatusMap.put(policyDataObject, policyActivationStatus);

        policyVersionMap.get(policy.getName() + policy.getMinorVersionString()).add(policyDataObject);
        policyVersionMap.get(policy.getName() + policy.getMajorVersion()).add(policyDataObject);
        policyVersionMap.get(policy.getName() + policy.getPatchVersionString()).add(policyDataObject);

    }

    public void remove(LogicalVersion policy) {
        PolicyDataObject policyDataObject = PolicyDataObject.create(policy);
        policyStatusMap.remove(policyDataObject);
        policyVersionMap.get(policy.getName() + policy.getMinorVersionString()).remove(policyDataObject);
        policyVersionMap.get(policy.getName() + policy.getMajorVersion()).remove(policyDataObject);
        policyVersionMap.get(policy.getName() + policy.getPatchVersionString()).remove(policyDataObject);

    }

}

