package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.ServerLicenseDAO;
import me.jissee.jarsauth.data.dao.ServerLicenseInstanceDAO;
import me.jissee.jarsauth.data.model.LicenseGroupRuleEntry;
import me.jissee.jarsauth.data.model.ServerLicense;
import me.jissee.jarsauth.data.model.ServerLicenseInstance;
import oshi.util.tuples.Pair;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ServerLicenseService implements Service{
    private final ServerLicenseDAO licenseDao;
    private final ServerLicenseInstanceDAO licenseInstanceDao;
    private LicenseGroupService groupService;
    private LicenseGroupRuleService groupRuleService;
    private TimeCacheService timeCacheService;

    @Override
    public void inject(ServiceResolver resolver) {
        groupService = resolver.getService(LicenseGroupService.class);
        groupRuleService = resolver.getService(LicenseGroupRuleService.class);
        timeCacheService = resolver.getService(TimeCacheService.class);
    }

    public ServerLicenseService(ConnectionProvider provider) {
        this.licenseDao = new ServerLicenseDAO(provider);
        this.licenseInstanceDao = new ServerLicenseInstanceDAO(provider);
    }


    public String getNextAvailableId(){
        return licenseDao.getNextAvailableId();
    }

    public boolean isIdExist(String id){
        return licenseDao.exists(id);
    }

    public List<ServerLicense> getAllLicenses() {
        return licenseDao.getAllLicenses();
    }

    public Set<String> getAllLicensesIds() {
        return new HashSet<>(licenseDao.getAllLicenses().stream().map(ServerLicense::id).toList());
    }

    public ServerLicense getLicense(String id) {
        return licenseDao.getLicense(id);
    }

    public ServerLicenseInstance getLicenseInstance(String id, String playerName, String groupName, String groupChain){
        return licenseInstanceDao.getLicenseInstance(id, playerName, groupName, groupChain);
    }

    public void saveLicense(ServerLicense license) {
        if(isIdExist(license.id())){
            updateLicense(license.id(),  license);
        }else {
            licenseDao.saveLicense(license);
        }
    }

    public void removeLicense(String id) {
        licenseDao.removeLicense(id);
    }

    public void updateLicense(String id, ServerLicense license) {
        licenseDao.updateLicense(id, license);
    }



    //Instances
    public void saveInstances(List<ServerLicenseInstance> licenseInstances, boolean replaceExist) {
        licenseInstanceDao.saveInstances(licenseInstances, replaceExist);
    }

    public List<Pair<String, String>> combineLicenseIdsAndGroupChain(
            List<String> licenses,
            List<String> groupChains,
            boolean sort
    ) {

        if (licenses.size() != groupChains.size()) {
            throw new IllegalArgumentException("count mismatch");
        }

        List<Pair<String, String>> result = new ArrayList<>();
        for (int i = 0; i < licenses.size(); i++) {
            result.add(new Pair<>(licenses.get(i), groupChains.get(i)));
        }
        if (sort) {
            // 一次性加载所有 License
            Map<String, ServerLicense> licenseMap = result.stream()
                    .map(Pair::getA)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(id -> new AbstractMap.SimpleEntry<>(id, getLicense(id)))
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));

            // 使用内存数据排序
            result.sort(Comparator.comparing(p -> licenseMap.get(p.getA())));
        }
        return result;
    }

    //playerName:{"group (chain)": remaining}
    public Map<String, Map<String, Long>> getRemainingMatrixForGroup(String groupName) {
        List<ServerLicenseInstance> instances = licenseInstanceDao.getLicenseInstancesForGroup(groupName);
        Map<String, Map<String, Long>> result = new HashMap<>();
        for (ServerLicenseInstance instance : instances) {
            String playerName = instance.player();
            Map<String, Long> instanceMap = result.computeIfAbsent(playerName, k -> new HashMap<>());
            instanceMap.put(instance.getTaggedId(), instance.remaining());
        }
        return result;
    }




    private static class InstancesHandler {
        private final Map<String, List<ServerLicenseInstance>> groupMap;

        public InstancesHandler(List<ServerLicenseInstance> unsorted) {
            List<ServerLicenseInstance> instances = new ArrayList<>(unsorted);

            // 按 groupName 分组
            this.groupMap = instances.stream()
                    .collect(Collectors.groupingBy(ServerLicenseInstance::groupName));
        }

        // 统计输入的所有对象中有那些不同的 groupName
        public Set<String> groupNames() {
            if (!groupMap.containsKey("default")) {
                groupMap.put("default", new ArrayList<>());
            }
            return groupMap.keySet();
        }

        // 对所有输入对象进行索引，通过 groupName 获取所有组内根据 licenseId 分组的对象
        // licenseId 可能重复，但 groupChain 不同，确保不同 groupChain 的实例在同一个列表中
        public Map<String, List<ServerLicenseInstance>> instancesById(String groupName) {
            List<ServerLicenseInstance> groupInstances = groupMap.getOrDefault(groupName, Collections.emptyList());

            // 按 licenseId 分组
            Map<String, List<ServerLicenseInstance>> byLicenseId = new HashMap<>();
            for (ServerLicenseInstance instance : groupInstances) {
                byLicenseId.computeIfAbsent(instance.licenseId(), k -> new ArrayList<>()).add(instance);
            }

            return byLicenseId;
        }
    }

    public boolean updateAndVerifyForPlayer(String playerName) {
        Set<String> groupNames = groupService.getAllGroupNames();
        List<ServerLicenseInstance> updatedInstances = new LinkedList<>();
        Map<String, Set<String>> taggedFlattenRulesByGroup = new HashMap<>();

        for(String groupName : groupNames) {
            LicenseGroupRuleEntry entry = groupRuleService.getTaggedFlattenRuleEntry(groupName);
            taggedFlattenRulesByGroup.put(groupName, entry.rules());
        }


        List<ServerLicenseInstance> existInstances = licenseInstanceDao.getLicenseInstancesForPlayer(playerName);
        InstancesHandler existInstancesHandler = new InstancesHandler(existInstances);

        LocalDateTime lastUpdate = timeCacheService.getPermanent(playerName);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        for(String groupName : groupNames) {
            Map<String, List<ServerLicenseInstance>> existInstancesById = existInstancesHandler.instancesById(groupName);
            updateForPlayerInGroup(
                    groupName,
                    playerName,
                    existInstancesById,
                    taggedFlattenRulesByGroup.get(groupName),
                    lastUpdate,
                    now,
                    updatedInstances
            );
        }
        licenseInstanceDao.saveInstances(updatedInstances, true);
        updatedInstances.clear();
        timeCacheService.setPermanent(playerName, now);
        // update ended

        existInstances = licenseInstanceDao.getLicenseInstancesForPlayer(playerName);
        existInstancesHandler = new InstancesHandler(existInstances);
        LocalDateTime lastVerify = timeCacheService.getVolatile(playerName);
        if(lastVerify == null) {
            lastVerify = now;
        }

        boolean result = false;
        for(String groupName : existInstancesHandler.groupNames()) {
            Map<String, List<ServerLicenseInstance>> existInstancesById = existInstancesHandler.instancesById(groupName);
            result |= verifyForPlayerInGroup(
                    groupName,
                    playerName,
                    existInstancesById,
                    taggedFlattenRulesByGroup.get(groupName),
                    lastVerify,
                    now,
                    updatedInstances
            );
        }
        licenseInstanceDao.saveInstances(updatedInstances, true);
        timeCacheService.setVolatile(playerName, now);
        return result;
    }

    private void parseRules(Set<String> rules, Map<String, List<String>> licenseIds, Set<String> playersOfGroup) {
        rules.forEach(s -> {
                    if(s.startsWith(":")){
                        String rule = s.substring(1);
                        int idx = rule.lastIndexOf('(');
                        int endIdx = rule.lastIndexOf(')');
                        if (idx > 0 && endIdx > idx) {
                            String licenseId = rule.substring(0, idx - 1);
                            String groupChain = rule.substring(idx + 1, endIdx);
                            licenseIds
                                    .computeIfAbsent(licenseId, k -> new ArrayList<>())
                                    .add(groupChain);
                        }
                    }else{
                        int idx = s.lastIndexOf('(');
                        if (idx > 0) {
                            String playerName = s.substring(0, idx - 1);
                            playersOfGroup.add(playerName);
                        }else{
                            playersOfGroup.add(s);
                        }
                    }
                });
    }

    private void updateForPlayerInGroup(
            String groupName,
            String playerName,
            Map<String, List<ServerLicenseInstance>> existInstancesById,
            Set<String> taggedFlattenRules,
            LocalDateTime lastUpdate,
            LocalDateTime now,
            List<ServerLicenseInstance> updatedInstances
    ){
        if (lastUpdate == null) {
            lastUpdate = LocalDateTime.now()
                    .withNano(0)
                    .minusDays(1);
        }

        if(taggedFlattenRules == null){
            return;
        }
        // id -> groupChain
        Map<String, List<String>> licenseIds = new HashMap<>();
        Set<String> playersOfGroup = new HashSet<>();

        parseRules(taggedFlattenRules, licenseIds, playersOfGroup);

        if(!playersOfGroup.contains(playerName) && !Objects.equals(groupName, "default")) {
            return;
        }


        List<ServerLicense> sortedValidLicenses = licenseIds.keySet().stream()
                .map(licenseDao::getLicense)
                .filter(Objects::nonNull)
                .filter(license -> license.isValid(now))
                .sorted(ServerLicense::compareTo)
                .toList();


        for(ServerLicense license : sortedValidLicenses) {
            String id = license.id();
            List<ServerLicenseInstance> instances = existInstancesById.get(id);

            if(instances == null){
                instances = new ArrayList<>();
            }

            // calculate time for update
            LocalTime clearTime = license.clearTime();
            LocalTime resetTime = license.resetTime();
            boolean doClear = crossedTimePoint(lastUpdate, now, clearTime);
            boolean doReset = crossedTimePoint(lastUpdate, now, resetTime);

            // calculate diff and make new instance
            if(instances.size() < licenseIds.get(id).size()){
                List<String> chains = licenseIds.get(id);

                Set<Pair<String, String>> exist = instances.stream()
                        .map(inst -> new Pair<>(inst.licenseId(), inst.groupChain()))
                        .collect(Collectors.toSet());

                for (String chain : chains) {
                    Pair<String, String> key = new Pair<>(id, chain);
                    if (!exist.contains(key)) {
                        ServerLicenseInstance newInst =
                                ServerLicenseInstance.createNewEmpty(id, playerName, groupName, chain);
                        assert license.isValid(now);
                        // 单次型
                        if(license.type() == 0){
                            newInst = newInst.withRemaining(license.allowance());
                        }
                        // 周期型
                        else{
                            if(doClear) {
                                newInst = newInst.withRemaining(0);
                            }
                            if(doReset) {
                                newInst = newInst.withRemaining(license.allowance());
                            }
                            if(doClear || doReset) {
                                updatedInstances.add(newInst);
                            }
                        }

                        instances.add(newInst);
                        updatedInstances.add(newInst);
                    }
                }
            }

            // update exist instance
            for(ServerLicenseInstance instance : instances) {
                ServerLicenseInstance newInst = instance;
                if(license.type() != 0){
                    if(doClear) {
                        newInst = newInst.withRemaining(0);
                    }
                    if(doReset) {
                        newInst = newInst.withRemaining(license.allowance());
                    }
                    if(doClear || doReset) {
                        updatedInstances.add(newInst);
                    }else if(newInst.remaining() > license.allowance()) {
                        updatedInstances.add(newInst.withRemaining(license.allowance()));
                    }
                }
            }
        }
    }

    private boolean verifyForPlayerInGroup(
            String groupName,
            String playerName,
            Map<String, List<ServerLicenseInstance>> existInstancesById,
            Set<String> taggedFlattenRules,
            LocalDateTime lastUpdate,
            LocalDateTime now,
            List<ServerLicenseInstance> updatedInstances
    ) {
        if(taggedFlattenRules == null){
            return false;
        }
        // id -> groupChain
        Map<String, List<String>> licenseIds = new HashMap<>();
        Set<String> playersOfGroup = new HashSet<>();

        parseRules(taggedFlattenRules, licenseIds, playersOfGroup);

        if(!playersOfGroup.contains(playerName) && !Objects.equals(groupName, "default")) {
            return false;
        }


        List<ServerLicense> sortedValidLicenses = licenseIds.keySet().stream()
                .map(licenseDao::getLicense)
                .filter(Objects::nonNull)
                .filter(license -> license.isValid(now))
                .sorted(ServerLicense::compareTo)
                .toList();

        Duration timeElapsed = Duration.between(lastUpdate, now);
        long sec = timeElapsed.get(ChronoUnit.SECONDS);

        for(ServerLicense license : sortedValidLicenses) {
            String id = license.id();
            List<ServerLicenseInstance> instances = existInstancesById.get(id);
            if(instances != null) {
                for(ServerLicenseInstance instance : instances) {
                    long instanceRemaining = instance.remaining();
                    if(instanceRemaining == 0){
                        continue;
                    }
                    if(instanceRemaining < sec){
                        sec -= instanceRemaining;
                        ServerLicenseInstance newInst = instance.withRemaining(0);
                        updatedInstances.add(newInst);
                        continue;
                    }
                    if(instanceRemaining >= sec && sec >= 0) {
                        instanceRemaining -= sec;
                        ServerLicenseInstance newInst = instance.withRemaining(instanceRemaining);
                        updatedInstances.add(newInst);
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private static boolean crossedTimePoint(
            LocalDateTime lastUpdate,
            LocalDateTime now,
            LocalTime targetTime
    ) {
        if (!lastUpdate.isBefore(now)) {
            return false;
        }

        // 以 now 的日期作为基准，构造目标时间点
        LocalDateTime targetDateTime = LocalDateTime.of(now.toLocalDate(), targetTime);

        /*
         * 如果目标时间在 now 之后，说明真正要判断的目标时间
         * 是“昨天的 targetTime”
         */
        if (targetDateTime.isAfter(now)) {
            targetDateTime = targetDateTime.minusDays(1);
        }

        return !lastUpdate.isAfter(targetDateTime) && !now.isBefore(targetDateTime);
    }



    @Override
    public void initTable() {
        licenseDao.initTable();
        licenseInstanceDao.initTable();
    }
}
