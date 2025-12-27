package me.jissee.jarsauth.data.service;

import com.sun.jna.platform.unix.solaris.LibKstat;
import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.dao.ServerLicenseDAO;
import me.jissee.jarsauth.data.dao.ServerLicenseInstanceDAO;
import me.jissee.jarsauth.data.model.LicenseGroupRuleEntry;
import me.jissee.jarsauth.data.model.ServerLicense;
import me.jissee.jarsauth.data.model.ServerLicenseInstance;
import oshi.util.tuples.Pair;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static me.jissee.jarsauth.data.TimeUtil.isDateInRange;
import static me.jissee.jarsauth.data.TimeUtil.isWeekdayMatched;

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
    public static List<Pair<String, String>> combineLicenseIdsAndGroupChain(List<String> licenses, List<String> groupChains) {
        int x = licenses.size();
        int y = groupChains.size();
        if (x != y) {
            throw new IllegalArgumentException("count mismatch");
        }
        List<Pair<String, String>> result = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            result.add(new Pair<>(licenses.get(i), groupChains.get(i)));
        }
        return result;
    }

    public void makeNewInstances(String groupName, List<Pair<String, String>> licensesWithGroupChains, List<String> playerNames) {
        List<ServerLicenseInstance> newInstances = new ArrayList<>();
        for (Pair<String, String> licenseWithGroupChain : licensesWithGroupChains) {
            String id = licenseWithGroupChain.getA();
            String groupChain = licenseWithGroupChain.getB();
            for (String playerName : playerNames) {
                ServerLicenseInstance sli = ServerLicenseInstance.createNew(id, playerName, groupName, groupChain);
                newInstances.add(sli);
            }
        }
        licenseInstanceDao.saveInstances(newInstances, false);
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

    public boolean updateAndVerifyForPlayer(String playerName) {
        Set<String> group = groupService.getAllGroupNames();
        List<ServerLicenseInstance> instances = licenseInstanceDao.getLicenseInstancesForPlayer(playerName);

        boolean result = false;
        for (String groupName : group) {
            List<ServerLicenseInstance> instancesOfGroup = instances.stream()
                    .filter(instance -> instance.groupName().equals(groupName))
                    .toList();
            result |= updateAndVerifyForPlayerInGroup(groupName, playerName, instancesOfGroup);
        }
        return result;
    }

    public boolean updateAndVerifyForPlayerInGroup(String groupName, String playerName, List<ServerLicenseInstance> instances) {
        LicenseGroupRuleEntry entry = groupRuleService.getFlattenRuleEntry(groupName);
        if(!entry.rules().contains(playerName) && Objects.equals(groupName, "default")) {
            return false;
        }
        Set<String> licenseIds = entry.rules().stream()
                .filter(s -> s.startsWith(":"))
                .map(s -> s.substring(1))
                .collect(Collectors.toSet());

        final LocalDateTime now = LocalDateTime.now().withNano(0);

        List<ServerLicense> sortedLicenses = licenseIds.stream()
                .map(licenseDao::getLicense)
                .filter(Objects::nonNull)
                .sorted(ServerLicense::compareTo)
                .filter(license -> license.isValid(now))
                .toList();

        List<String> sortedLicenseIds = sortedLicenses.stream().map(ServerLicense::id).toList();

        TimeCacheService tcs = DataManager.getServerInstance().getService(TimeCacheService.class);

        Optional<LocalDateTime> lastUpdatedOptional = tcs.get(playerName, "sl/" + groupName);
        // -1: left behind
        //  0: up to date
        //  1: after now
        int upToDateStatus = -1;
        if(lastUpdatedOptional.isPresent()){
            upToDateStatus = 0;
        }
        if(upToDateStatus == 0){
            LocalDateTime lastUpdated = lastUpdatedOptional.get();
            LocalDate lastUpdatedDate = lastUpdated.toLocalDate();
            if(now.toLocalDate().isAfter(lastUpdatedDate)){
                upToDateStatus = -1;
            }else if(now.toLocalDate().isBefore(lastUpdatedDate)){
                upToDateStatus = 1;
            }
        }
        // left behind
        if(upToDateStatus == -1){

        }
        // up to date
        else if(upToDateStatus == 0){

        }
        // after now
        else {

        }
        Duration duration = Duration.between(now, lastUpdated);

        sortedLicenses.forEach(sortedLicense -> {

        });

        List<ServerLicenseInstance> sortedInstances = instances.stream()
                .filter(instance -> instance.licenseId());

    }

    private void updateGroup(String groupName, List<String> currentPlayers, boolean onlyCurrentPlayers) {
        LicenseGroupRuleEntry entry = groupRuleService.getFlattenRuleEntry(groupName);
        Set<String> licenseIds = entry.rules().stream().
                filter(s -> s.startsWith(":"))
                .map(s -> s.substring(1))
                .collect(Collectors.toSet());

        final LocalDateTime now = LocalDateTime.now().withNano(0);

        List<ServerLicense> sortedLicenses = licenseIds.stream()
                .map(licenseDao::getLicense)
                .filter(Objects::nonNull)
                .sorted(ServerLicense::compareTo)
                .filter(license -> license.isValid(now))
                .toList();

        Map<String, ServerLicense> licensesById = new HashMap<>();
        sortedLicenses.forEach(license -> licensesById.put(license.id(), license));

        List<ServerLicenseInstance> instances = licenseInstanceDao.getLicenseInstancesForGroup(groupName);

        Map<ServerLicense, List<ServerLicenseInstance>> groupedInstances = new HashMap<>();
        for(ServerLicense license : sortedLicenses) {
            groupedInstances.computeIfAbsent(license, k -> new ArrayList<>());
        }

        for(ServerLicenseInstance instance : instances){
            String id = instance.licenseId();
            ServerLicense license = licensesById.get(id);
            if(license == null) continue;
            groupedInstances.get(license).add(instance);
        }

        for(ServerLicense license : sortedLicenses) {
            List<ServerLicenseInstance> instancesOfLicense = groupedInstances.get(license);

        }

        System.out.println(groupName + licenseIds);
        if(1==1){
            return;
        }
        //Set<String> licenseIds = new HashSet<>();


        instances.forEach(instance -> {
            licenseIds.add(instance.licenseId());
        });

        List<ServerLicenseInstance> newInstances = instances.stream().map(instance -> updateSingle(instance, now, sortedLicenses)).toList();
        licenseInstanceDao.saveInstances(newInstances, true);
    }
/*
    private ServerLicenseInstance updateSingle(ServerLicenseInstance instance, LocalDateTime now, List<ServerLicense> licenseMap) {
        if(1==1){
            return instance;
        }
        String        licenseId  = instance.licenseId();
        String        player     = instance.player();
        String        groupName  = instance.groupName();
        String        groupChain = instance.groupChain();
        long          remaining  = instance.remaining();


        Duration duration = Duration.between(lastUpdate, now);

        LocalDateTime newUpdate = lastUpdate.plus(duration);
        long durationSeconds = duration.toSeconds();
        long newSeconds = remaining - durationSeconds;
        if (newSeconds < 0) {
            newSeconds = 0;
        }

        //ServerLicense license = licenseMap.get(licenseId);

        // 完善更新逻辑，规则如下
        // 1.

        ServerLicenseInstance newInst = new ServerLicenseInstance(
                licenseId,
                player,
                groupName,
                groupChain,
                newSeconds,
                newUpdate
        );


        return newInst;
    }

*/







    @Override
    public void initTable() {
        licenseDao.initTable();
        licenseInstanceDao.initTable();
    }
}
