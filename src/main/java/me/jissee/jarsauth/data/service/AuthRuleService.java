package me.jissee.jarsauth.data.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.AuthRuleDAO;
import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.model.AuthRuleEntry;

import java.util.*;

public class AuthRuleService implements Service {
    private final AuthRuleDAO dao;
    private final Gson gson = new Gson();

    public AuthRuleService(ConnectionProvider provider) {
        this.dao = new AuthRuleDAO(provider);
    }

    public List<String> getAllGroups(){
        return dao.getAllGroups();
    }



    /** 获取原始 rule（不展开子集） */
    public AuthRuleEntry getUnflattenedRuleEntry(String groupName) {
        List<String> rules = dao.findRulesByGroup(groupName);
        return new AuthRuleEntry(groupName, rules);
    }

    public AuthRuleEntry getUnflattenedRuleEntry(AcceptedDetail detail) {
        return getUnflattenedRuleEntry(detail.groupName());
    }

    public AuthRuleEntry getFlattenRuleEntry(AcceptedDetail detail) {
        return getFlattenRuleEntry(detail.groupName());
    }

    // 获取递归展开后的 AuthProfile 对象
    public AuthRuleEntry getFlattenRuleEntry(String groupName) {
        Set<String> visited = new HashSet<>();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        collectRules(groupName, result, visited);
        return new AuthRuleEntry(groupName, new ArrayList<>(result));
    }

    private void collectRules(String groupName, Set<String> output, Set<String> visited) {
        if (!visited.add(groupName)) return;

        AuthRuleEntry ruleEntry = getUnflattenedRuleEntry(groupName);
        for (String rule : ruleEntry.rules()) {
            if (rule.startsWith("&")) {
                String subset = rule.substring(1);
                collectRules(subset, output, visited);
            } else {
                output.add(rule);
            }
        }
    }



    public AuthRuleEntry getTaggedFlattenRuleEntry(String groupName) {
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
        return new AuthRuleEntry(groupName, result);
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
        AuthRuleEntry ruleEntry = getUnflattenedRuleEntry(currentGroup);

        for (String rule : ruleEntry.rules()) {
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

    public String getAllRulesAsJson(){
        List<String> groups = getAllGroups();
        JsonObject root = new JsonObject();
        for (String group : groups) {
            AuthRuleEntry entry = getFlattenRuleEntry(group);
            JsonArray json = gson.toJsonTree(entry.rules()).getAsJsonArray();
            root.add(group, json);
        }
        return root.toString();
    }




    public void saveRuleEntry(AuthRuleEntry ruleEntry) {
        dao.insertRules(ruleEntry.groupName(), ruleEntry.rules());
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
