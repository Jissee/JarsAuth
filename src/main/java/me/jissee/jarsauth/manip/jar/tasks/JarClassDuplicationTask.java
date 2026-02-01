package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class JarClassDuplicationTask implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    private final String classInternalName;
    private final String newClassInternalName;
    private final List<Integer> indices;

    public JarClassDuplicationTask(String classInternalName,
                                   String newClassInternalName,
                                   List<Integer> indices) {
        this.classInternalName = classInternalName;
        this.newClassInternalName = newClassInternalName;
        this.indices = indices;
    }

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> input) {
        List<JarEntryWrapper> result = new ArrayList<>();

        for (JarEntryWrapper entry : input) {
            result.add(entry); // 保留原始文件

            if (entry.getName().equals(classInternalName + ".class")) {
                for (int idx : indices) {
                    String actualNewName = newClassInternalName.replace("#", String.valueOf(idx));
                    byte[] newClassBytes = transformClass(entry.getBytes(), classInternalName, actualNewName, idx);
                    result.add(new JarEntryWrapper(actualNewName + ".class", newClassBytes));
                }
            }
        }
        return result;
    }

    private byte[] transformClass(byte[] originalBytes,
                                  String oldName, String newName, int index) {
        ClassReader cr = new ClassReader(originalBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        Remapper remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                if (internalName.equals(oldName)) {
                    return newName;
                }
                return internalName;
            }

            @Override
            public String mapDesc(String desc) {
                return super.mapDesc(desc.replace("L" + oldName + ";", "L" + newName + ";"));
            }

            @Override
            public Object mapValue(Object value) {
                if (value instanceof String str) {
                    if (str.equals(oldName)) {
                        return newName;
                    }
                }
                return super.mapValue(value);
            }
        };

        ClassVisitor cv = new ClassRemapper(cw, remapper) {
            @Override
            public void visitSource(String source, String debug) {
                String newSource = source;
                if (source != null) {
                    // 提取新类的简单类名
                    int lastSlash = newName.lastIndexOf('/');
                    String simpleName = lastSlash == -1 ? newName : newName.substring(lastSlash + 1);

                    // 如果原始文件名是 XXX.java，就替换成新类名
                    if (source.endsWith(".java")) {
                        newSource = simpleName + ".java";
                    } else {
                        newSource = simpleName + ".java";
                    }
                }
                super.visitSource(newSource, debug);
            }
        };

        cr.accept(cv, 0);

        return cw.toByteArray();
    }
}
