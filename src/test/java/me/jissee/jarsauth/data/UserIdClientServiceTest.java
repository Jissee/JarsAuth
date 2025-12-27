package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.UserIdClientService;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserIdClientServiceTest {

    private static DataManager dataManager;
    private static UserIdClientService clientService;

    @BeforeEach
    void setup() {
        dataManager = new DataManager("memory:", false); // 使用内存数据库 + 客户端模式
        clientService = dataManager.getService(UserIdClientService.class);
    }

    @Test
    void testInitTable() {
        assertDoesNotThrow(() -> clientService.initTable());
    }

    @Test
    void testSaveAndRetrieve() {
        String userName = "eve";
        UUID serverId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        clientService.saveUserId(userName, userId, serverId);

        Optional<UUID> retrieved = clientService.getUserId(userName, serverId);
        assertTrue(retrieved.isPresent());
        assertEquals(userId, retrieved.get());
    }

    @Test
    void testGetUserId_NotExists() {
        Optional<UUID> result = clientService.getUserId("ghost", UUID.randomUUID());
        assertFalse(result.isPresent());
    }

    @Test
    void testSaveMultipleDifferentServerIds() {
        String userName = "bob";
        UUID server1 = UUID.randomUUID();
        UUID server2 = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        clientService.saveUserId(userName, id1, server1);
        clientService.saveUserId(userName + "_s2", id2, server2); // 避免主键冲突

        Optional<UUID> got1 = clientService.getUserId(userName, server1);
        Optional<UUID> got2 = clientService.getUserId(userName + "_s2", server2);

        assertEquals(id1, got1.orElseThrow());
        assertEquals(id2, got2.orElseThrow());
    }

    @Test
    void testPrimaryKeyConstraintViolation() {
        String userName = "alice";
        UUID serverId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        clientService.saveUserId(userName, id1, serverId);

        Exception ex = assertThrows(RuntimeException.class, () ->
            clientService.saveUserId(userName, id2, serverId) // same user_name -> primary key violation
        );

        assertTrue(ex.getCause().getMessage().contains("UNIQUE") || ex.getCause().getMessage().contains("PRIMARY"));
    }
}
