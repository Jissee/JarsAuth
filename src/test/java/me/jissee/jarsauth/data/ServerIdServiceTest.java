package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ServerIdService;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServerIdServiceTest {

    private DataManager dataManager;
    private ServerIdService service;

    @BeforeEach
    void setUp() {
        // 使用内存数据库初始化 DataManager
        dataManager = new DataManager(false, true);
        service = dataManager.getService(ServerIdService.class);
    }

    @Test
    void testGeneratedServerIdIsNotNull() {
        UUID id = service.getOrCreateServerId();
        assertNotNull(id, "Server ID should not be null after generation");
    }

    @Test
    void testServerIdIsPersistent() {
        UUID id1 = service.getOrCreateServerId();
        UUID id2 = service.getOrCreateServerId();
        assertEquals(id1, id2, "Server ID should remain the same across multiple calls");
    }

    @Test
    void testServerIdFromPreexistingDatabaseEntry() throws Exception {
        UUID preexisting = UUID.randomUUID();
        dataManager.getConnection().createStatement()
            .execute("INSERT INTO server_id (value) VALUES ('" + preexisting.toString() + "');");

        // 重新获取服务以避免缓存
        dataManager = new DataManager(true, true);
        dataManager.getConnection().createStatement()
            .execute("INSERT INTO server_id (value) VALUES ('" + preexisting.toString() + "');");
        ServerIdService newService = dataManager.getService(ServerIdService.class);

        UUID loaded = newService.getOrCreateServerId();
        assertEquals(preexisting, loaded, "Should return pre-inserted server_id from database");
    }
}
