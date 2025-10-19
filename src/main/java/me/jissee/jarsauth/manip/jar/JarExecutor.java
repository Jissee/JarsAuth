package me.jissee.jarsauth.manip.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarExecutor implements AutoCloseable {
    private final JarFile jarFile;
    private final List<JarEntryWrapper> entries = new ArrayList<>();
    /** 改成队列，任务执行后丢弃 */
    private final Queue<Function<List<JarEntryWrapper>, List<JarEntryWrapper>>> taskQueue = new ArrayDeque<>();
    private final Map<String, byte[]> binBuf = new HashMap<>();

    public JarExecutor(File input) throws IOException {
        jarFile = new JarFile(input);
        // 预读 jar 文件到内存
        Enumeration<JarEntry> enumeration = jarFile.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry entry = enumeration.nextElement();
            try (InputStream is = jarFile.getInputStream(entry)) {
                entries.add(new JarEntryWrapper(entry.getName(), is.readAllBytes()));
            }
        }
    }

    /** 定义任务：输入是 JarEntryWrapper 列表，返回修改后的列表 */
    public JarExecutor defineTask(Function<List<JarEntryWrapper>, List<JarEntryWrapper>> task) {
        taskQueue.add(task);
        return this;
    }

    /** 顺序执行任务链，执行后任务自动丢弃 */
    public void execute() {
        List<JarEntryWrapper> current = entries;
        Function<List<JarEntryWrapper>, List<JarEntryWrapper>> task;
        while ((task = taskQueue.poll()) != null) { // poll() 取出并删除队列头
            current = task.apply(current);
        }
        entries.clear();
        entries.addAll(current);
    }

    /** 导出到新文件 */
    public void export(File output) {
        System.out.println("Exporting to file: " + output);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(output.toPath()))) {
            for (JarEntryWrapper wrapper : entries) {
                JarEntry newEntry = new JarEntry(wrapper.getName());
                jos.putNextEntry(newEntry);
                jos.write(wrapper.getBytes());
                jos.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void bufBinary(String key, byte[] value) {
        binBuf.put(key, value);
    }

    public byte[] getBinary(String key) {
        return binBuf.get(key);
    }

    public void bufString(String key, String value) {
        if (value != null) {
            binBuf.put(key, value.getBytes(StandardCharsets.UTF_8));
        } else {
            binBuf.put(key, null);
        }
    }

    public String getString(String key) {
        byte[] data = binBuf.get(key);
        return data != null ? new String(data, StandardCharsets.UTF_8) : null;
    }

    @Override
    public void close() throws IOException {
        jarFile.close();
    }
}
