package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.dao.AuthProfileDAO;
import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.model.AuthProfile;

import java.sql.Connection;
import java.util.*;

public class AuthProfileService implements Service {
    private final AuthProfileDAO dao;

    public AuthProfileService(Connection connection) {
        this.dao = new AuthProfileDAO(connection);
    }

    public List<String> getAllGroups(){
        return dao.getAllGroups();
    }



    /** 获取原始 profile（不展开子集） */
    public AuthProfile getUnflattenedProfile(String groupName) {
        List<String> rules = dao.findRulesByGroup(groupName);
        return new AuthProfile(groupName, rules);
    }

    public AuthProfile getUnflattenedProfile(AcceptedDetail detail) {
        return getUnflattenedProfile(detail.groupName());
    }

    public AuthProfile getFlattenProfile(AcceptedDetail detail) {
        return getFlattenProfile(detail.groupName());
    }

    // 获取递归展开后的 AuthProfile 对象
    public AuthProfile getFlattenProfile(String groupName) {
        Set<String> visited = new HashSet<>();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        collectRules(groupName, result, visited);
        return new AuthProfile(groupName, new ArrayList<>(result));
    }

    private void collectRules(String groupName, Set<String> output, Set<String> visited) {
        if (!visited.add(groupName)) return;

        AuthProfile profile = getUnflattenedProfile(groupName);
        for (String rule : profile.rules()) {
            if (rule.startsWith("&")) {
                String subset = rule.substring(1);
                collectRules(subset, output, visited);
            } else {
                output.add(rule);
            }
        }
    }



    public AuthProfile getTaggedFlattenProfile(String groupName) {
        Map<String, LinkedHashSet<String>> ruleToTags = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        collectTaggedRules(groupName, new LinkedList<>(), groupName, ruleToTags, visited);

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : ruleToTags.entrySet()) {
            String rule = entry.getKey();
            LinkedHashSet<String> tags = entry.getValue();
            if (tags.isEmpty()) {
                result.add(rule); // 本组定义的规则，无标签
            } else {
                result.add(rule + " (" + String.join(",", tags) + ")");
            }
        }
        return new AuthProfile(groupName, result);
    }

    private void collectTaggedRules(
            String currentGroup,
            Deque<String> path,
            String rootGroup,
            Map<String, LinkedHashSet<String>> ruleToTags,
            Set<String> visited
    ) {
        if (!visited.add(currentGroup)) return;

        path.addLast(currentGroup);
        AuthProfile profile = getUnflattenedProfile(currentGroup);

        for (String rule : profile.rules()) {
            if (rule.startsWith("&")) {
                String subset = rule.substring(1);
                collectTaggedRules(subset, path, rootGroup, ruleToTags, visited);
            } else {
                // 构造路径标签
                String tag = null;
                if (path.size() > 1) {
                    Iterator<String> it = path.iterator();
                    it.next(); // skip root
                    List<String> tagChain = new ArrayList<>();
                    while (it.hasNext()) tagChain.add(it.next());
                    tag = String.join("/", tagChain);
                }

                ruleToTags.computeIfAbsent(rule, k -> new LinkedHashSet<>());

                if (currentGroup.equals(rootGroup)) {
                    // 本组定义，优先加入特殊标签 "<groupName>" 到前面
                    LinkedHashSet<String> tags = ruleToTags.get(rule);
                    if (!tags.contains(rootGroup)) {
                        LinkedHashSet<String> newTags = new LinkedHashSet<>();
                        newTags.add(rootGroup);
                        newTags.addAll(tags);
                        ruleToTags.put(rule, newTags);
                    }
                } else if (tag != null) {
                    ruleToTags.get(rule).add(tag);
                }
            }
        }

        path.removeLast();
    }




    public void saveProfile(AuthProfile profile) {
        dao.insertRules(profile.groupName(), profile.rules());
    }

    public void changeRule(String groupName, String oldRule, String newRule) {
        dao.changeRule(groupName, oldRule, newRule);
    }
    public void removeRule(String groupName, String rule) {
        dao.removeRule(groupName, rule);
    }
    public void removeGroup(String groupName) {
        dao.removeGroup(groupName);
    }



    @Override
    public void initTable() {
        dao.initTable();
    }
}
