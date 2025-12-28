package me.jissee.jarsauth.data.model;

import java.util.Set;

public record LicenseGroupRuleEntry(
        String groupName,
        Set<String> rules
) {
}
