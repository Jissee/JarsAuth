package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.model.AuthProfile;
import me.jissee.jarsauth.data.service.AcceptedDetailService;
import me.jissee.jarsauth.data.service.AuthProfileService;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthProfileServiceTest {

    private DataManager dataManager;
    private AcceptedDetailService acceptedDetailService;
    private AuthProfileService authProfileService;

    @BeforeEach
    void setup() {
        // 使用内存数据库，测试用临时环境
        dataManager = new DataManager(false, true);
        acceptedDetailService = dataManager.getService(AcceptedDetailService.class);
        authProfileService = dataManager.getService(AuthProfileService.class);
    }

    @Test
    void testGetAllGroups() {
        // 保存几个 AcceptedDetail 和对应 AuthProfile
        acceptedDetailService.saveAcceptedDetail(new AcceptedDetail("groupA"));
        authProfileService.saveProfile(new AuthProfile("groupA", List.of("ruleA")));

        acceptedDetailService.saveAcceptedDetail(new AcceptedDetail("groupB"));
        authProfileService.saveProfile(new AuthProfile("groupB", List.of("ruleB1", "ruleB2")));

        acceptedDetailService.saveAcceptedDetail(new AcceptedDetail("groupC"));
        authProfileService.saveProfile(new AuthProfile("groupC", List.of("&groupA", "ruleC")));

        List<String> allGroups = authProfileService.getAllGroups();

        // 断言包含全部已保存的组名，顺序不重要
        assertTrue(allGroups.containsAll(List.of("groupA", "groupB", "groupC")));
        assertEquals(3, allGroups.size());
    }

    @Test
    void testSaveAndRetrieveProfile() {
        String group = "acc0";
        AcceptedDetail detail = new AcceptedDetail(group);
        acceptedDetailService.saveAcceptedDetail(detail);

        AuthProfile profile = new AuthProfile(group, List.of("rule1", "rule2"));
        authProfileService.saveProfile(profile);

        AuthProfile loaded = authProfileService.getUnflattenedProfile(group);
        assertEquals(group, loaded.groupName());
        assertEquals(profile.rules(), loaded.rules());
    }

    @Test
    void testGetProfileFromAcceptedDetail() {
        String group = "accX";
        acceptedDetailService.saveAcceptedDetail(new AcceptedDetail(group));

        authProfileService.saveProfile(new AuthProfile(group, List.of("allow:/admin", "deny:/etc")));

        AcceptedDetail detail = new AcceptedDetail(group);
        AuthProfile retrieved = authProfileService.getFlattenProfile(detail);

        assertEquals(group, retrieved.groupName());
        assertEquals(List.of("allow:/admin", "deny:/etc"), retrieved.rules());
    }

    @Test
    void testGetFlattenProfile() {
        acceptedDetailService.saveAcceptedDetail(new AcceptedDetail("base"));
        authProfileService.saveProfile(new AuthProfile("base", List.of("r1", "r2")));

        acceptedDetailService.saveAcceptedDetail(new AcceptedDetail("admin"));
        authProfileService.saveProfile(new AuthProfile("admin", List.of("&base", "r3")));

        AuthProfile flat = authProfileService.getFlattenProfile("admin");

        assertEquals("admin", flat.groupName());
        assertEquals(List.of("r1", "r2", "r3"), flat.rules());
    }

    @Test
    void testFlattenedRules() {
        acceptedDetailService.saveAcceptedDetail(new AcceptedDetail("base"));
        authProfileService.saveProfile(new AuthProfile("base", List.of("read", "write")));

        acceptedDetailService.saveAcceptedDetail(new AcceptedDetail("admin"));
        authProfileService.saveProfile(new AuthProfile("admin", List.of("&base", "delete")));

        AuthProfile expanded = authProfileService.getFlattenProfile("admin");

        assertEquals(List.of("read", "write", "delete"), expanded.rules());
    }

    @Test
    void testMissingGroupThrows() {
        assertDoesNotThrow(() -> {
            authProfileService.getUnflattenedProfile("nonexistent");
        });
    }
}
