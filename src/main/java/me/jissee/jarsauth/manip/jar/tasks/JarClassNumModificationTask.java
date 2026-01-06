package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Function;

/**
 * 对指定类（或包通配符）批量替换整数常量
 * 目标类集合与替换规则集合彼此独立
 */
public class JarClassNumModificationTask
        implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    /** 精确匹配的目标类（internal name，如 me/jissee/MyClass） */
    private final Set<String> exactTargets = new HashSet<>();

    /** 通配符目标（前缀，如 me/jissee/pkg/） */
    private final Set<String> wildcardTargets = new HashSet<>();

    /** 所有替换规则 */
    private final List<ReplaceRule> replaceRules = new ArrayList<>();

    /* =========================
       API
       ========================= */

    /**
     * 添加目标类或包通配符
     * @param classInternalName 例如:
     *  - me/jissee/MyClass
     *  - me/jissee/pkg/*
     */
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

    /**
     * 添加整数替换规则
     */
    public JarClassNumModificationTask addReplace(int oldNum, int newNum) {
        replaceRules.add(new ReplaceRule(oldNum, newNum));
        return this;
    }

    /* =========================
       Task 执行
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
                    } catch (Exception e) {
                        System.out.println(
                                "Failed to modify class: " + name + ", reason: " + e.getMessage()
                        );
                    }
                }
            }

            result.add(new JarEntryWrapper(name, data));
        }

        return result;
    }

    /* =========================
       内部实现
       ========================= */

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

    private byte[] transformClass(byte[] original, String classInternalName) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(0);

        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {

            /** 记录当前类中被替换过的 static final int 字段 */
            final Map<String, Integer> fieldNewValues = new HashMap<>();

            @Override
            public FieldVisitor visitField(int access,
                                           String fieldName,
                                           String descriptor,
                                           String signature,
                                           Object value) {

                if ((access & Opcodes.ACC_STATIC) != 0
                        && (access & Opcodes.ACC_FINAL) != 0
                        && "I".equals(descriptor)
                        && value instanceof Integer) {

                    Integer replaced = tryReplace((Integer) value);
                    if (replaced != null) {
                        fieldNewValues.put(fieldName, replaced);
                        return super.visitField(
                                access, fieldName, descriptor, signature, replaced
                        );
                    }
                }
                return super.visitField(access, fieldName, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access,
                                             String name,
                                             String descriptor,
                                             String signature,
                                             String[] exceptions) {

                MethodVisitor mv = super.visitMethod(
                        access, name, descriptor, signature, exceptions
                );

                return new MethodVisitor(Opcodes.ASM9, mv) {

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Integer) {
                            Integer replaced = tryReplace((Integer) value);
                            if (replaced != null) {
                                pushInteger(mv, replaced);
                                return;
                            }
                        }
                        super.visitLdcInsn(value);
                    }

                    @Override
                    public void visitIntInsn(int opcode, int operand) {
                        Integer replaced = tryReplace(operand);
                        if (replaced != null) {
                            pushInteger(mv, replaced);
                            return;
                        }
                        super.visitIntInsn(opcode, operand);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        Integer val = iconstValue(opcode);
                        if (val != null) {
                            Integer replaced = tryReplace(val);
                            if (replaced != null) {
                                pushInteger(mv, replaced);
                                return;
                            }
                        }
                        super.visitInsn(opcode);
                    }

                    @Override
                    public void visitFieldInsn(int opcode,
                                               String owner,
                                               String fieldName,
                                               String descriptor) {

                        if (opcode == Opcodes.GETSTATIC
                                && owner.equals(classInternalName)
                                && "I".equals(descriptor)) {

                            Integer newVal = fieldNewValues.get(fieldName);
                            if (newVal != null) {
                                pushInteger(mv, newVal);
                                return;
                            }
                        }
                        super.visitFieldInsn(opcode, owner, fieldName, descriptor);
                    }
                };
            }
        };

        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

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

    private static void pushInteger(MethodVisitor mv, int value) {
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
