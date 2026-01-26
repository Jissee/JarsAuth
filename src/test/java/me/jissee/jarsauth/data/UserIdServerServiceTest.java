package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.service.UserIdServerService;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserIdServerServiceTest {

    private static DataManager dataManager;
    private static UserIdServerService userIdServerService;

    @BeforeEach
    void setup() {
        dataManager = new DataManager("memory:", true); // 使用内存数据库
        userIdServerService = dataManager.getService(UserIdServerService.class);
    }

    @Test
    void testInitTable() {
        assertDoesNotThrow(() -> userIdServerService.initTable());
    }

    @Test
    void testGetOrCreateUserId() {
        String userName = "alice";
        UUID id1 = userIdServerService.getOrCreateUserId(userName).get();
        UUID id2 = userIdServerService.getOrCreateUserId(userName).get();

        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals(id1, id2); // 相同用户名返回同一个 UUID
    }

    @Test
    void testHasUserId() {
        String userName = "bob";
        assertFalse(userIdServerService.hasUserId(userName)); // 初始不存在

        UUID id = userIdServerService.getOrCreateUserId(userName).get(); // 自动创建
        assertTrue(userIdServerService.hasUserId(userName)); // 创建后存在
    }

    @Test
    void testMultipleUsers() {
        String user1 = "charlie";
        String user2 = "dave";

        UUID id1 = userIdServerService.getOrCreateUserId(user1).get();
        UUID id2 = userIdServerService.getOrCreateUserId(user2).get();

        assertNotEquals(id1, id2); // 不同用户应该有不同 UUID
    }
}
