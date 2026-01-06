package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Function;

public class JarClassNumModificationTask
        implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    /* =========================
       Target configuration
       ========================= */

    private final Set<String> exactTargets = new HashSet<>();
    private final Set<String> wildcardTargets = new HashSet<>();
    private final List<ReplaceRule> replaceRules = new ArrayList<>();

    public JarClassNumModificationTask addTarget(String classInternalName) {
        if (classInternalName.endsWith("/*")) {
            wildcardTargets.add(
                    classInternalName.substring(0, classInternalName.length() - 1)
            );
        } else {
            exactTargets.add(classInternalName);
        }
        return this;
    }

    public JarClassNumModificationTask addReplace(int oldNum, int newNum) {
        replaceRules.add(new ReplaceRule(oldNum, newNum));
        return this;
    }

    /* =========================
       Task entry
       ========================= */

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> input) {
        List<JarEntryWrapper> result = new ArrayList<>();

        for (JarEntryWrapper entry : input) {
            byte[] data = entry.getBytes();
            String name = entry.getName();

            if (name.endsWith(".class")) {
                String classInternalName = name.substring(0, name.length() - 6);

                if (shouldProcess(classInternalName) && !replaceRules.isEmpty()) {
                    try {
                        data = transformClass(data, classInternalName);
                    } catch (Throwable t) {
                        System.out.println(
                                "Failed to modify class: " + name + ", reason: " + t.getMessage()
                        );
                    }
                }
            }

            result.add(new JarEntryWrapper(name, data));
        }

        return result;
    }

    private boolean shouldProcess(String classInternalName) {
        if (exactTargets.contains(classInternalName)) {
            return true;
        }
        for (String prefix : wildcardTargets) {
            if (classInternalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /* =========================
       ASM transform
       ========================= */

    private byte[] transformClass(byte[] original, String classInternalName) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(0);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {

            /** static final int 字段在当前类中的新值（字段名 -> 新值） */
            final Map<String, Integer> staticFinalFieldValues = new HashMap<>();

            @Override
            public FieldVisitor visitField(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    Object value) {

                if ((access & Opcodes.ACC_STATIC) != 0
                        && (access & Opcodes.ACC_FINAL) != 0
                        && "I".equals(descriptor)
                        && value instanceof Integer) {

                    Integer replaced = tryReplace((Integer) value);
                    if (replaced != null) {
                        staticFinalFieldValues.put(name, replaced);
                        return super.visitField(
                                access, name, descriptor, signature, replaced
                        );
                    }
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {

                MethodVisitor mv = super.visitMethod(
                        access, name, descriptor, signature, exceptions
                );

                return new MethodVisitor(Opcodes.ASM9, mv) {

                    /* ---------- 普通常量 ---------- */

                    @Override
                    public void visitInsn(int opcode) {
                        Integer v = iconstValue(opcode);
                        if (v != null) {
                            Integer r = tryReplace(v);
                            if (r != null) {
                                pushInt(mv, r);
                                return;
                            }
                        }
                        super.visitInsn(opcode);
                    }

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        Integer r = tryReplace(operand);
                        if (r != null) {
                            pushInt(mv, r);
                            return;
                        }
                        super.visitIntInsn(opcode, operand);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Integer) {
                            Integer r = tryReplace((Integer) value);
                            if (r != null) {
                                pushInt(mv, r);
                                return;
                            }
                        }
                        super.visitLdcInsn(value);
                    }

                    /* ---------- static final 内联 ---------- */

                    @Override
                    public void visitFieldInsn(
                            int opcode,
                            String owner,
                            String fieldName,
                            String descriptor) {

                        if (opcode == Opcodes.GETSTATIC
                                && owner.equals(classInternalName)
                                && "I".equals(descriptor)) {

                            Integer newVal = staticFinalFieldValues.get(fieldName);
                            if (newVal != null) {
                                pushInt(mv, newVal);
                                return;
                            }
                        }
                        super.visitFieldInsn(opcode, owner, fieldName, descriptor);
                    }

                    /* ---------- switch 支持 ---------- */

                    @Override
                    public void visitLookupSwitchInsn(
                            Label dflt,
                            int[] keys,
                            Label[] labels) {

                        int[] newKeys = new int[keys.length];
                        for (int i = 0; i < keys.length; i++) {
                            Integer r = tryReplace(keys[i]);
                            newKeys[i] = r != null ? r : keys[i];
                        }

                        super.visitLookupSwitchInsn(dflt, newKeys, labels);
                    }

                    @Override
                    public void visitTableSwitchInsn(
                            int min,
                            int max,
                            Label dflt,
                            Label... labels) {

                        int count = max - min + 1;
                        int[] keys = new int[count];

                        for (int i = 0; i < count; i++) {
                            int v = min + i;
                            Integer r = tryReplace(v);
                            keys[i] = r != null ? r : v;
                        }

                        // 强制转为 lookupswitch，避免连续性被破坏
                        super.visitLookupSwitchInsn(dflt, keys, labels);
                    }
                };
            }
        };

        reader.accept(cv, 0);
        return writer.toByteArray();
    }

    /* =========================
       Utilities
       ========================= */

    private Integer tryReplace(int oldVal) {
        for (ReplaceRule rule : replaceRules) {
            if (rule.oldNum == oldVal) {
                return rule.newNum;
            }
        }
        return null;
    }

    private static Integer iconstValue(int opcode) {
        return switch (opcode) {
            case Opcodes.ICONST_M1 -> -1;
            case Opcodes.ICONST_0 -> 0;
            case Opcodes.ICONST_1 -> 1;
            case Opcodes.ICONST_2 -> 2;
            case Opcodes.ICONST_3 -> 3;
            case Opcodes.ICONST_4 -> 4;
            case Opcodes.ICONST_5 -> 5;
            default -> null;
        };
    }

    private static void pushInt(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    private record ReplaceRule(int oldNum, int newNum) {}
}
