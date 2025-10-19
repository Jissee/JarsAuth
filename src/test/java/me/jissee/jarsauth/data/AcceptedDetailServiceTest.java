package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.model.AcceptedDetail;
import me.jissee.jarsauth.data.service.AcceptedDetailService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class AcceptedDetailServiceTest {

    private DataManager dataManager;
    private AcceptedDetailService service;

    @BeforeEach
    public void setup() {
        dataManager = new DataManager(false, true); // 使用内存数据库
        service = dataManager.getService(AcceptedDetailService.class);
    }

    @Test
    public void testCreateNewDetail() {
        AcceptedDetail detail = service.createNewDetail();
        assertNotNull(detail, "New detail should not be null");
        assertTrue(detail.groupName().startsWith("acc"),
                "Group name should start with 'acc', but was: " + detail.groupName());
    }

    @Test
    public void testSaveAcceptedDetailAndGetGroup() {
        AcceptedDetail detail = new AcceptedDetail("testGroup");
        detail.addFile("file1.txt", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        detail.addFolder("folder1/");

        service.saveAcceptedDetail(detail);

        AcceptedDetail loaded = service.getGroup("testGroup");
        assertEquals("testGroup", loaded.groupName(), "Group name mismatch");
        assertEquals(1, loaded.files().size(), "File count mismatch");
        assertEquals(1, loaded.folders().size(), "Folder count mismatch");
        assertTrue(loaded.files().containsKey("file1.txt"), "Expected file1.txt in files: " + loaded.files().keySet());
        assertTrue(loaded.folders().contains("folder1"), "Expected folder1/ in folders: " + loaded.folders());
    }

    @Test
    public void testGetAllFromPath() {
        AcceptedDetail detail = new AcceptedDetail("group1");
        detail.addFolder("folderA");
        detail.addFile("folderA/file.txt", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");

        service.saveAcceptedDetail(detail);

        AcceptedDetail result = service.getAllFromPath("group1", "folderA");
        assertTrue(result.folders().contains("folderA"), "Expected folderA/ in: " + result.folders());
        assertTrue(result.files().containsKey("folderA/file.txt"), "Expected folderA/file.txt in: " + result.files());
    }

    @Test
    public void testExactPathReturnsFile() {
        AcceptedDetail detail = new AcceptedDetail("fileGroup");
        String filePath = "dir/file.txt";
        String sha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        detail.addFile(filePath, sha256);

        service.saveAcceptedDetail(detail);

        AcceptedDetail result = service.getExactFromPath("fileGroup", filePath);
        assertEquals(1, result.files().size(), "Should contain one file");
        assertTrue(result.files().containsKey(filePath), "Expected file: " + filePath);
        assertEquals(sha256, result.files().get(filePath), "File hash mismatch");
        assertTrue(result.folders().isEmpty(), "Should not contain any folder");
    }

    @Test
    public void testExactPathReturnsFolderChildrenOnly() {
        AcceptedDetail detail = new AcceptedDetail("group3");
        detail.addFolder("dir");
        detail.addFile("dir/file1.txt", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        detail.addFolder("dir/subdir");
        detail.addFile("dir/subdir/file2.txt", "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");

        service.saveAcceptedDetail(detail);

        AcceptedDetail result = service.getExactFromPath("group3", "dir");

        assertFalse(result.folders().contains("dir"), "Result should not include the folder itself");
        assertTrue(result.files().containsKey("dir/file1.txt"), "Should include direct child file: dir/file1.txt");
        assertTrue(result.folders().contains("dir/subdir"), "Should include direct child folder: dir/subdir");
        assertFalse(result.files().containsKey("dir/subdir/file2.txt"), "Should not include nested file: dir/subdir/file2.txt");

        assertEquals(1, result.files().size(), "Only one direct file should be present");
        assertEquals(1, result.folders().size(), "Only one direct folder should be present");
    }

    @Test
    public void testExactPathNotFound() {
        AcceptedDetail detail = new AcceptedDetail("missingGroup");
        detail.addFile("real.txt", "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        service.saveAcceptedDetail(detail);

        AcceptedDetail result = service.getExactFromPath("missingGroup", "not_exist.txt");

        assertTrue(result.files().isEmpty(), "Should contain no files");
        assertTrue(result.folders().isEmpty(), "Should contain no folders");
    }

    @Test
    public void testRemoveGroup() {
        AcceptedDetail detail = new AcceptedDetail("removeMe");
        detail.addFile("key", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        service.saveAcceptedDetail(detail);

        AcceptedDetail before = service.getGroup("removeMe");
        assertNotNull(before, "Group should exist before removal");

        service.removeGroup("removeMe");

        AcceptedDetail after = service.getGroup("removeMe");
        assertTrue(after.files().isEmpty() && after.folders().isEmpty(), "Group should be empty after removal");
    }
}
