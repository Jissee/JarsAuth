package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.model.AuthRuleEntry;
import me.jissee.jarsauth.data.service.AccProfileService;
import me.jissee.jarsauth.data.service.AuthRuleService;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthRuleEntryServiceTest {

    private DataManager dataManager;
    private AccProfileService accProfileService;
    private AuthRuleService authRuleService;

    @BeforeEach
    void setup() {
        // 使用内存数据库，测试用临时环境
        dataManager = new DataManager("memory:", true);
        accProfileService = dataManager.getService(AccProfileService.class);
        authRuleService = dataManager.getService(AuthRuleService.class);
    }

    @Test
    void testGetAllGroups() {
        // 保存几个 AcceptedDetail 和对应 AuthProfile
        accProfileService.saveAcceptedDetail(new AcceptedDetail("groupA"));
        authRuleService.saveRuleEntry(new AuthRuleEntry("groupA", List.of("ruleA")));

        accProfileService.saveAcceptedDetail(new AcceptedDetail("groupB"));
        authRuleService.saveRuleEntry(new AuthRuleEntry("groupB", List.of("ruleB1", "ruleB2")));

        accProfileService.saveAcceptedDetail(new AcceptedDetail("groupC"));
        authRuleService.saveRuleEntry(new AuthRuleEntry("groupC", List.of("&groupA", "ruleC")));

        List<String> allGroups = authRuleService.getAllGroups();

        // 断言包含全部已保存的组名，顺序不重要
        assertTrue(allGroups.containsAll(List.of("groupA", "groupB", "groupC")));
        assertEquals(3, allGroups.size());
    }

    @Test
    void testSaveAndRetrieveProfile() {
        String group = "acc0";
        AcceptedDetail detail = new AcceptedDetail(group);
        accProfileService.saveAcceptedDetail(detail);

        AuthRuleEntry profile = new AuthRuleEntry(group, List.of("rule1", "rule2"));
        authRuleService.saveRuleEntry(profile);

        AuthRuleEntry loaded = authRuleService.getUnflattenedRuleEntry(group);
        assertEquals(group, loaded.groupName());
        assertEquals(profile.rules(), loaded.rules());
    }

    @Test
    void testGetProfileFromAcceptedDetail() {
        String group = "accX";
        accProfileService.saveAcceptedDetail(new AcceptedDetail(group));

        authRuleService.saveRuleEntry(new AuthRuleEntry(group, List.of("allow:/admin", "deny:/etc")));

        AcceptedDetail detail = new AcceptedDetail(group);
        AuthRuleEntry retrieved = authRuleService.getFlattenRuleEntry(detail);

        assertEquals(group, retrieved.groupName());
        assertEquals(List.of("allow:/admin", "deny:/etc"), retrieved.rules());
    }

    @Test
    void testGetFlattenProfile() {
        accProfileService.saveAcceptedDetail(new AcceptedDetail("base"));
        authRuleService.saveRuleEntry(new AuthRuleEntry("base", List.of("r1", "r2")));

        accProfileService.saveAcceptedDetail(new AcceptedDetail("admin"));
        authRuleService.saveRuleEntry(new AuthRuleEntry("admin", List.of("&base", "r3")));

        AuthRuleEntry flat = authRuleService.getFlattenRuleEntry("admin");

        assertEquals("admin", flat.groupName());
        assertEquals(List.of("r1", "r2", "r3"), flat.rules());
    }

    @Test
    void testFlattenedRules() {
        accProfileService.saveAcceptedDetail(new AcceptedDetail("base"));
        authRuleService.saveRuleEntry(new AuthRuleEntry("base", List.of("read", "write")));

        accProfileService.saveAcceptedDetail(new AcceptedDetail("admin"));
        authRuleService.saveRuleEntry(new AuthRuleEntry("admin", List.of("&base", "delete")));

        AuthRuleEntry expanded = authRuleService.getFlattenRuleEntry("admin");

        assertEquals(List.of("read", "write", "delete"), expanded.rules());
    }

    @Test
    void testMissingGroupThrows() {
        assertDoesNotThrow(() -> {
            authRuleService.getUnflattenedRuleEntry("nonexistent");
        });
    }
}
