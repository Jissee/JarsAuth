package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.UserIdService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserIdServiceTest {

    private static DataManager dataManager;
    private static UserIdService userIdService;

    @BeforeEach
    void setup() {
        dataManager = new DataManager(false, true); // 使用内存数据库
        userIdService = dataManager.getService(UserIdService.class);
    }

    @Test
    void testInitTable() {
        assertDoesNotThrow(() -> userIdService.initTable());
    }

    @Test
    void testGetOrCreateUserId() {
        String userName = "alice";
        UUID id1 = userIdService.getOrCreateUserId(userName);
        UUID id2 = userIdService.getOrCreateUserId(userName);

        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals(id1, id2); // 相同用户名返回同一个 UUID
    }

    @Test
    void testHasUserId() {
        String userName = "bob";
        assertFalse(userIdService.hasUserId(userName)); // 初始不存在

        UUID id = userIdService.getOrCreateUserId(userName); // 自动创建
        assertTrue(userIdService.hasUserId(userName)); // 创建后存在
    }

    @Test
    void testMultipleUsers() {
        String user1 = "charlie";
        String user2 = "dave";

        UUID id1 = userIdService.getOrCreateUserId(user1);
        UUID id2 = userIdService.getOrCreateUserId(user2);

        assertNotEquals(id1, id2); // 不同用户应该有不同 UUID
    }
}
