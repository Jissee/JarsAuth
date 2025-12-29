package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.LicenseGroupRuleDAO;
import me.jissee.jarsauth.data.model.LicenseGroupRuleEntry;

import java.util.*;
import java.util.stream.Collectors;

public class LicenseGroupRuleService implements Service {
    private final LicenseGroupRuleDAO dao;

    public LicenseGroupRuleService(ConnectionProvider provider) {
        this.dao = new LicenseGroupRuleDAO(provider);
    }

    /* ==================== 查询 ==================== */

    /** 获取原始规则（不展开子组） */
    public LicenseGroupRuleEntry getUnflattenedRuleEntry(String groupName) {
        List<String> rules = dao.findRulesByGroup(groupName);
        return new LicenseGroupRuleEntry(groupName, new LinkedHashSet<>(rules.stream().sorted().collect(Collectors.toList())));
    }

    /** 获取递归展开后的规则（无 tag） */
    public LicenseGroupRuleEntry getFlattenRuleEntry(String groupName) {
        Set<String> visited = new HashSet<>();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        collectRules(groupName, result, visited);
        return new LicenseGroupRuleEntry(groupName, result);
    }

    private void collectRules(String groupName, Set<String> output, Set<String> visited) {
        if (!visited.add(groupName)) return;

        LicenseGroupRuleEntry entry = getUnflattenedRuleEntry(groupName);
        for (String rule : entry.rules()) {
            if (rule.startsWith("&")) {
                collectRules(rule.substring(1), output, visited);
            } else {
                output.add(rule);
            }
        }
    }

    /** 获取递归展开后的规则（带 tag，枚举全部来源，root 优先） */
    public LicenseGroupRuleEntry getTaggedFlattenRuleEntry(String groupName) {
        Map<String, LinkedHashSet<String>> ruleToTags = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();

        collectTaggedRules(
                groupName,
                new LinkedList<>(),
                groupName,
                ruleToTags,
                visited
        );

        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : ruleToTags.entrySet()) {
            if (entry.getValue().isEmpty()) {
                result.add(entry.getKey());
            } else {
                for (String rule : entry.getValue()) {
                    result.add(entry.getKey() + " (" + rule + ")");
                }
            }
        }

        return new LicenseGroupRuleEntry(groupName, result.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new)));
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
        LicenseGroupRuleEntry entry = getUnflattenedRuleEntry(currentGroup);

        for (String rule : entry.rules()) {
            if (rule.startsWith("&")) {
                collectTaggedRules(rule.substring(1), path, rootGroup, ruleToTags, visited);
            }else if(path.size() == 1 || rule.startsWith(":")){
                ruleToTags.computeIfAbsent(rule, k -> new LinkedHashSet<>());
                LinkedHashSet<String> tags = ruleToTags.get(rule);

                if (currentGroup.equals(rootGroup)) {
                    // root 组规则：确保 rootGroup 在 tag 最前
                    if (!tags.contains(rootGroup)) {
                        LinkedHashSet<String> newTags = new LinkedHashSet<>();
                        newTags.add(rootGroup);
                        newTags.addAll(tags);
                        ruleToTags.put(rule, newTags);
                    }
                } else if (path.size() > 1) {
                    // 子组来源：记录完整路径（不含 root）
                    Iterator<String> it = path.iterator();
                    it.next(); // skip root
                    List<String> chain = new ArrayList<>();
                    while (it.hasNext()) {
                        chain.add(it.next());
                    }
                    tags.add(String.join("/", chain));
                }
            }
        }

        path.removeLast();
    }

    /* ==================== CRUD ==================== */

    public void saveRuleEntry(LicenseGroupRuleEntry entry) {
        if(entry.groupName().equals("default")) {
            dao.insertRules("default", entry.rules().stream().filter(s -> s.startsWith(":")).collect(Collectors.toSet()));
        }else{
            dao.insertRules(entry.groupName(), entry.rules());
        }
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
