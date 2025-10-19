package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Function;

/**
 * 删除指定类的静态常量字段（支持通配符路径）
 */
public class JarClassStaticFinalFieldRemovalTask implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    /** 精确匹配规则: classInternalName -> 规则列表 */
    private final Map<String, List<String>> exactRules = new HashMap<>();

    /** 通配符匹配规则: packagePath/ -> 规则列表 */
    private final Map<String, List<String>> wildcardRules = new HashMap<>();

    /**
     * 添加删除规则
     * classInternalName 可以是具体类，如 "me/jissee/MyClass"
     * 也可以是目录通配符，如 "me/jissee/pkg/*"
     *
     * @param classInternalName 内部类名或目录通配符
     * @param fieldName         要删除的静态常量字段名
     */
    public JarClassStaticFinalFieldRemovalTask add(String classInternalName, String fieldName) {
        if (classInternalName.endsWith("/*")) {
            String prefix = classInternalName.substring(0, classInternalName.length() - 1);
            wildcardRules.computeIfAbsent(prefix, k -> new ArrayList<>())
                    .add(fieldName);
        } else {
            exactRules.computeIfAbsent(classInternalName, k -> new ArrayList<>())
                    .add(fieldName);
        }
        return this;
    }

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> input) {
        List<JarEntryWrapper> result = new ArrayList<>();

        for (JarEntryWrapper entry : input) {
            byte[] data = entry.getBytes();
            String name = entry.getName();

            if (name.endsWith(".class")) {
                String classInternalName = name.substring(0, name.length() - 6);

                List<String> fieldsToRemove = collectRulesForClass(classInternalName);

                if (!fieldsToRemove.isEmpty()) {
                    try {
                        ClassReader reader = new ClassReader(data);
                        ClassWriter writer = new ClassWriter(0);

                        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
                            @Override
                            public FieldVisitor visitField(int access, String fieldName,
                                                           String descriptor, String signature, Object value) {
                                // 匹配 static final 字段，并检查是否在删除列表中
                                if ((access & Opcodes.ACC_STATIC) != 0 &&
                                    (access & Opcodes.ACC_FINAL) != 0 &&
                                    fieldsToRemove.contains(fieldName)) {
                                    // 跳过此字段，相当于删除
                                    return null;
                                }
                                return super.visitField(access, fieldName, descriptor, signature, value);
                            }
                        };

                        reader.accept(visitor, 0);
                        data = writer.toByteArray();
                    } catch (Exception e) {
                        System.out.println("Failed to modify class: " + name + ", reason: " + e.getMessage());
                    }
                }
            }

            result.add(new JarEntryWrapper(name, data));
        }

        return result;
    }

    /**
     * 收集某个类应该应用的所有删除字段规则（精确 + 通配符）
     */
    private List<String> collectRulesForClass(String classInternalName) {
        List<String> result = new ArrayList<>();
        List<String> exact = exactRules.get(classInternalName);
        if (exact != null) result.addAll(exact);

        for (Map.Entry<String, List<String>> e : wildcardRules.entrySet()) {
            if (classInternalName.startsWith(e.getKey())) {
                result.addAll(e.getValue());
            }
        }
        return result;
    }
}
