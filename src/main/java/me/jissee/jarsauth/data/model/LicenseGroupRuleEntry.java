package me.jissee.jarsauth.data.model;

import java.util.List;

public record LicenseGroupRuleEntry(
        String groupName,
        List<String> rules
) {
}
