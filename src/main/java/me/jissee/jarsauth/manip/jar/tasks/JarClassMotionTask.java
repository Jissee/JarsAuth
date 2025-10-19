package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.util.*;
import java.util.function.Function;

public class JarClassMotionTask implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    private final List<Motion> motions = new ArrayList<>();

    private static class Motion {
        String oldName;
        String newName;

        Motion(String oldName, String newName) {
            this.oldName = oldName;
            this.newName = newName;
        }
    }

    public JarClassMotionTask add(String classInternalName, String newInternalName) {
        motions.add(new Motion(classInternalName, newInternalName));
        return this;
    }

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> input) {
        // 构建 map，key: entry name，value: JarEntryWrapper
        Map<String, JarEntryWrapper> entryMap = new HashMap<>();
        for (JarEntryWrapper entry : input) {
            entryMap.put(entry.getName(), entry);
        }

        // 构建批量重命名表
        Map<String, String> renameMap = new HashMap<>();
        for (Motion motion : motions) {
            String oldClassName = motion.oldName + ".class";
            if (motion.newName == null || motion.newName.isEmpty()) {
                // 删除类
                entryMap.remove(oldClassName);
            } else {
                renameMap.put(oldClassName, motion.newName + ".class");
            }
        }

        // 批量 remap 类
        for (Map.Entry<String, String> renameEntry : renameMap.entrySet()) {
            String oldEntryName = renameEntry.getKey();
            String newEntryName = renameEntry.getValue();
            JarEntryWrapper oldEntry = entryMap.remove(oldEntryName);
            if (oldEntry != null) {
                byte[] newBytes = remapClass(
                        oldEntry.getBytes(),
                        oldEntryName.replace(".class", ""),
                        newEntryName.replace(".class", "")
                );
                entryMap.put(newEntryName, new JarEntryWrapper(newEntryName, newBytes));
            }
        }

        return new ArrayList<>(entryMap.values());
    }

    private byte[] remapClass(byte[] original, String oldName, String newName) {
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(0);

        Map<String, String> singleMap = Collections.singletonMap(oldName, newName);

        Remapper remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                return singleMap.getOrDefault(internalName, internalName);
            }
        };

        ClassVisitor cv = new ClassRemapper(cw, remapper) {
            @Override
            public void visitSource(String source, String debug) {
                String newSource = source;
                if (source != null) {
                    // 取新类名的简单类名作为 SourceFile
                    int lastSlash = newName.lastIndexOf('/');
                    String simpleName = lastSlash == -1 ? newName : newName.substring(lastSlash + 1);
                    newSource = simpleName + ".java";
                }
                super.visitSource(newSource, debug);
            }
        };

        cr.accept(cv, 0);
        return cw.toByteArray();
    }
}
