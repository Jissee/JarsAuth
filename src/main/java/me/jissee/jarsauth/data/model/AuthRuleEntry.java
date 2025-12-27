package me.jissee.jarsauth.data.model;

import java.util.List;

public record AuthRuleEntry(
        String groupName,
        List<String> rules
) {

}
