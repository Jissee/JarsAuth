package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Function;

/**
 * 批量修改多个类/目录的整数常量（支持通配符路径）
 * 仅对当前被处理的类生效（不会跨类全局扫描）。
 */
public class JarClassNumModificationTask implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    /** 精确匹配规则: classInternalName -> 规则列表 */
    private final Map<String, List<ReplaceRule>> exactRules = new HashMap<>();

    /** 通配符匹配规则: packagePath/ -> 规则列表（形如 me/jissee/pkg/*） */
    private final Map<String, List<ReplaceRule>> wildcardRules = new HashMap<>();

    /**
     * 添加一条修改规则
     * classInternalName 可以是具体类，如 "me/jissee/MyClass"
     * 也可以是目录通配符，如 "me/jissee/pkg/*"
     */
    public JarClassNumModificationTask add(String classInternalName, int oldNum, int newNum) {
        if (classInternalName.endsWith("/*")) {
            String prefix = classInternalName.substring(0, classInternalName.length() - 1);
            wildcardRules.computeIfAbsent(prefix, k -> new ArrayList<>())
                    .add(new ReplaceRule(oldNum, newNum));
        } else {
            exactRules.computeIfAbsent(classInternalName, k -> new ArrayList<>())
                    .add(new ReplaceRule(oldNum, newNum));
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
                List<ReplaceRule> rules = collectRulesForClass(classInternalName);

                if (!rules.isEmpty()) {
                    try {
                        ClassReader reader = new ClassReader(data);
                        ClassWriter writer = new ClassWriter(0);

                        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {

                            // 记录当前类中被替换成功过的 static final int 字段的新值（按字段名）
                            final Map<String, Integer> fieldNewValuesByName = new HashMap<>();

                            @Override
                            public FieldVisitor visitField(int access, String fieldName, String descriptor,
                                                           String signature, Object value) {
                                // 仅处理 static final 且为 int 的常量字段，并且有编译期常量（ConstantValue）
                                if ((access & Opcodes.ACC_STATIC) != 0
                                        && (access & Opcodes.ACC_FINAL) != 0
                                        && "I".equals(descriptor)
                                        && value instanceof Integer) {
                                    Integer oldVal = (Integer) value;
                                    Integer replaced = tryReplace(rules, oldVal);
                                    if (replaced != null) {
                                        // 记下“该字段名 -> 新值”，供 GETSTATIC 时按字段名内联
                                        fieldNewValuesByName.put(fieldName, replaced);
                                        return super.visitField(access, fieldName, descriptor, signature, replaced);
                                    }
                                }
                                return super.visitField(access, fieldName, descriptor, signature, value);
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                             String signature, String[] exceptions) {
                                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                return new MethodVisitor(Opcodes.ASM9, mv) {

                                    @Override
                                    public void visitLdcInsn(Object value) {
                                        if (value instanceof Integer) {
                                            Integer oldVal = (Integer) value;
                                            Integer replaced = tryReplace(rules, oldVal);
                                            if (replaced != null) {
                                                // 统一走 pushInteger，确保选择合适指令
                                                pushInteger(mv, replaced);
                                                return;
                                            }
                                        }
                                        super.visitLdcInsn(value);
                                    }

                                    @Override
                                    public void visitIntInsn(int opcode, int operand) {
                                        // BIPUSH/SIPUSH 的新值可能超出原范围，必须换指令
                                        Integer replaced = tryReplace(rules, operand);
                                        if (replaced != null) {
                                            pushInteger(mv, replaced);
                                            return;
                                        }
                                        super.visitIntInsn(opcode, operand);
                                    }

                                    @Override
                                    public void visitInsn(int opcode) {
                                        // 仅 ICONST_* 才有意义
                                        int val = -2; // sentinel
                                        switch (opcode) {
                                            case Opcodes.ICONST_M1: val = -1; break;
                                            case Opcodes.ICONST_0:  val = 0;  break;
                                            case Opcodes.ICONST_1:  val = 1;  break;
                                            case Opcodes.ICONST_2:  val = 2;  break;
                                            case Opcodes.ICONST_3:  val = 3;  break;
                                            case Opcodes.ICONST_4:  val = 4;  break;
                                            case Opcodes.ICONST_5:  val = 5;  break;
                                        }
                                        if (val != -2) {
                                            Integer replaced = tryReplace(rules, val);
                                            if (replaced != null) {
                                                pushInteger(mv, replaced);
                                                return;
                                            }
                                        }
                                        super.visitInsn(opcode);
                                    }

                                    @Override
                                    public void visitFieldInsn(int opcode, String owner, String fieldName, String descriptor) {
                                        // 仅替换“当前类内”的 GETSTATIC int 字段为“新值入栈”，保证只对当前类生效
                                        if (opcode == Opcodes.GETSTATIC
                                                && "I".equals(descriptor)
                                                && owner.equals(classInternalName)) {
                                            Integer newVal = fieldNewValuesByName.get(fieldName);
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

    /** 收集某个类应该应用的所有规则（精确 + 通配符） */
    private List<ReplaceRule> collectRulesForClass(String classInternalName) {
        List<ReplaceRule> result = new ArrayList<>();
        List<ReplaceRule> exact = exactRules.get(classInternalName);
        if (exact != null) result.addAll(exact);
        for (Map.Entry<String, List<ReplaceRule>> e : wildcardRules.entrySet()) {
            if (classInternalName.startsWith(e.getKey())) {
                result.addAll(e.getValue());
            }
        }
        return result;
    }

    private static Integer tryReplace(List<ReplaceRule> rules, int oldVal) {
        for (ReplaceRule rule : rules) {
            if (rule.oldNum == oldVal) {
                return rule.newNum;
            }
        }
        return null;
    }

    private static class ReplaceRule {
        final int oldNum;
        final int newNum;
        ReplaceRule(int oldNum, int newNum) {
            this.oldNum = oldNum;
            this.newNum = newNum;
        }
    }

    /** 根据值范围自动选择 ICONST/BIPUSH/SIPUSH/LDC 指令 */
    private static void pushInteger(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            switch (value) {
                case -1: mv.visitInsn(Opcodes.ICONST_M1); break;
                case 0:  mv.visitInsn(Opcodes.ICONST_0);  break;
                case 1:  mv.visitInsn(Opcodes.ICONST_1);  break;
                case 2:  mv.visitInsn(Opcodes.ICONST_2);  break;
                case 3:  mv.visitInsn(Opcodes.ICONST_3);  break;
                case 4:  mv.visitInsn(Opcodes.ICONST_4);  break;
                case 5:  mv.visitInsn(Opcodes.ICONST_5);  break;
            }
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }
}
