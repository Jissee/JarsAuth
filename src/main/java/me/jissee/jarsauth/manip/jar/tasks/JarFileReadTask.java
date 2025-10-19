package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 从 JAR 包里读取文件（支持字符串和二进制处理）
 */
public class JarFileReadTask implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    private final String fileName;
    private final BiConsumer<String, byte[]> fileHandler;

    /**
     * 构造函数，传入文件名和处理逻辑
     * fileHandler 会接收 (字符串内容, 原始二进制内容)
     *   - 如果文件不存在，则不会调用
     */
    public JarFileReadTask(String fileName, BiConsumer<String, byte[]> fileHandler) {
        this.fileName = fileName;
        this.fileHandler = fileHandler;
    }

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> input) {
        for (JarEntryWrapper entry : input) {
            if (entry.getName().equals(fileName)) {
                byte[] bytes = entry.getBytes();
                String text = new String(bytes, StandardCharsets.UTF_8);
                fileHandler.accept(text, bytes);
                break;
            }
        }
        return input; // 读取任务不修改输入，原样返回
    }
}
