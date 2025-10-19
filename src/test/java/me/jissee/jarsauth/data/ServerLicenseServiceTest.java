package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.model.LicenseType;
import me.jissee.jarsauth.data.model.ServerLicense;
import me.jissee.jarsauth.data.service.ServerLicenseService;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServerLicenseServiceTest {

    private ServerLicenseService service;

    @BeforeAll
    public void setup() {
        // 使用 DataManager 注册 ServerLicenseService，数据库使用内存临时库，保证测试隔离
        DataManager dataManager = new DataManager(false, true); // isTemp = true, isServer = true
        service = dataManager.getService(ServerLicenseService.class);
        assertNotNull(service);
    }

    @Test
    public void testAddAndGetLicense() {
        UUID uuid = UUID.randomUUID();
        ServerLicense license = new ServerLicense(
            uuid,
            "testuser",
            System.currentTimeMillis() / 1000 - 1000,
            System.currentTimeMillis() / 1000 + 10_000,
            LicenseType.TIME,
            5,
                5
        );
        service.saveLicense(license);

        ServerLicense fetched = service.getLicense(uuid);
        assertNotNull(fetched);
        assertEquals("testuser", fetched.userName());
        assertEquals(5, fetched.allowance());
    }

    @Test
    public void testUpdateAllowanceAllowance() {
        UUID uuid = UUID.randomUUID();
        ServerLicense license = new ServerLicense(
            uuid,
            "updateuser",
            System.currentTimeMillis() / 1000 - 1000,
            System.currentTimeMillis() / 1000 + 10_000,
            LicenseType.TIME,
            3,
                3
        );
        service.saveLicense(license);

        // 扣减额度
        service.updateAllowance(uuid, -1);

        ServerLicense updated = service.getLicense(uuid);
        assertEquals(2, updated.allowance());

        // 增加额度
        service.updateAllowance(uuid, 3);

        updated = service.getLicense(uuid);
        assertEquals(5, updated.allowance());
    }

    @Test
    public void testUpdateAllowanceAllowanceFailsWhenInvalid() {
        UUID uuid = UUID.randomUUID();
        ServerLicense license = new ServerLicense(
            uuid,
            "expireduser",
            System.currentTimeMillis() / 1000 - 10_000,
            System.currentTimeMillis() / 1000 - 5_000,  // 已过期
            LicenseType.TIME,
            3,
                3
        );
        service.saveLicense(license);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            service.updateAllowance(uuid, -1);
        });
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    public void testRemoveExpiredLicenses() throws InterruptedException {
        // 插入一个过期许可证
        UUID expiredUuid = UUID.randomUUID();
        ServerLicense expired = new ServerLicense(
            expiredUuid,
            "expireduser",
            System.currentTimeMillis() / 1000 - 10_000,
            System.currentTimeMillis() / 1000 - 5_000,
            LicenseType.TIME,
            5,
                5
        );
        service.saveLicense(expired);

        // 插入一个未过期许可证
        UUID validUuid = UUID.randomUUID();
        ServerLicense valid = new ServerLicense(
            validUuid,
            "validuser",
            System.currentTimeMillis() / 1000 - 1000,
            System.currentTimeMillis() / 1000 + 10_000,
            LicenseType.TIME,
            5,
                5
        );
        service.saveLicense(valid);

        // 删除所有过期
        service.removeExpiredLicenses();

        // 过期许可证应被删除
        assertNull(service.getLicense(expiredUuid));

        // 有效许可证依然存在
        assertNotNull(service.getLicense(validUuid));
    }

    @Test
    public void testRemoveLicense() {
        UUID uuid = UUID.randomUUID();
        ServerLicense license = new ServerLicense(
            uuid,
            "toremove",
            System.currentTimeMillis() / 1000 - 1000,
            System.currentTimeMillis() / 1000 + 10_000,
            LicenseType.TIME,
            5,
                5
        );
        service.saveLicense(license);

        assertNotNull(service.getLicense(uuid));

        service.removeLicense(uuid);

        assertNull(service.getLicense(uuid));
    }

    @Test
    public void testGetLicensesForUser() {
        String userName = "multiuser";
        // 先清理旧数据(假设测试环境是独立的，可忽略)

        // 插入两个不同用户的许可证
        ServerLicense license1 = new ServerLicense(
            UUID.randomUUID(),
            userName,
            System.currentTimeMillis() / 1000 - 1000,
            System.currentTimeMillis() / 1000 + 10_000,
            LicenseType.TIME,
            5,
                5
        );
        ServerLicense license2 = new ServerLicense(
            UUID.randomUUID(),
            "otheruser",
            System.currentTimeMillis() / 1000 - 1000,
            System.currentTimeMillis() / 1000 + 10_000,
            LicenseType.TIME,
            3,
                3
        );
        ServerLicense license3 = new ServerLicense(
            UUID.randomUUID(),
            userName,
            System.currentTimeMillis() / 1000 - 1000,
            System.currentTimeMillis() / 1000 + 20_000,
            LicenseType.TIME,
            7,
                7
        );
        service.saveLicense(license1);
        service.saveLicense(license2);
        service.saveLicense(license3);

        List<ServerLicense> userLicenses = service.getLicensesForUser(userName);
        assertEquals(2, userLicenses.size());
        for (ServerLicense lic : userLicenses) {
            assertEquals(userName, lic.userName());
        }
    }
}
