package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 往 JAR 包里添加或覆盖文件（支持字符串和二进制）
 */
public class JarFileAdditionTask implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    private final String fileName;
    private final Supplier<byte[]> contentProvider;

    public static Supplier<byte[]> supplierWrap(Supplier<String> contentProvider){
        return ()-> contentProvider.get().getBytes(StandardCharsets.UTF_8);
    }


    /** 构造函数，直接传二进制数据 */
    public JarFileAdditionTask(String fileName, Supplier<byte[]> contentProvider) {
        this.fileName = fileName;
        this.contentProvider = contentProvider;
    }

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> input) {
        List<JarEntryWrapper> result = new ArrayList<>();

        boolean replaced = false;
        for (JarEntryWrapper entry : input) {
            if (entry.getName().equals(fileName)) {
                // 覆盖旧条目
                result.add(new JarEntryWrapper(fileName, contentProvider.get()));
                replaced = true;
            } else {
                result.add(entry);
            }
        }

        // 如果原来没有这个文件，添加新条目
        if (!replaced) {
            result.add(new JarEntryWrapper(fileName, contentProvider.get()));
        }

        return result;
    }
}
