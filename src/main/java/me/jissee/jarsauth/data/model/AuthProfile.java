package me.jissee.jarsauth.data.model;

import java.util.List;

public record AuthProfile(
        String groupName,
        List<String> rules
) {

}
