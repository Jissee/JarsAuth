package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 检查指定类中是否存在某个静态变量/常量，并返回其值（如果能获取）
 * @param <T> 变量类型（如 Integer、String、Long 等）
 */
public class JarClassVarCheckTask<T> implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    private final Class<T> type;
    private final String classInternalName;
    private final String fieldName;
    private final BiConsumer<Boolean, T> onAccept;

    private boolean found = false;

    public JarClassVarCheckTask(Class<T> type,
                                String classInternalName,
                                String fieldName,
                                BiConsumer<Boolean, T> onAccept) {
        this.type = type;
        this.classInternalName = classInternalName;
        this.fieldName = fieldName;
        this.onAccept = onAccept;
    }

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> input) {
        List<JarEntryWrapper> result = new ArrayList<>();

        for (JarEntryWrapper entry : input) {
            byte[] data = entry.getBytes();
            String entryName = entry.getName();
            if(entryName.contains("verification")){
                int pausehere = 0;
            }

            // 只检查指定类
            if (entryName.equals(classInternalName + ".class")) {
                try {
                    ClassReader reader = new ClassReader(data);
                    reader.accept(new ClassVisitor(Opcodes.ASM9) {

                        @Override
                        public FieldVisitor visitField(int access, String name, String descriptor,
                                                       String signature, Object value) {
                            if (!found && name.equals(fieldName)) {
                                if (value != null && type.isInstance(value)) {
                                    @SuppressWarnings("unchecked")
                                    T castValue = (T) value;
                                    onAccept.accept(true, castValue);
                                } else {
                                    // 变量存在，但不是编译期常量
                                    onAccept.accept(true, null);
                                }
                                found = true;
                            }
                            return super.visitField(access, name, descriptor, signature, value);
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                         String signature, String[] exceptions) {
                            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                            // 只检查 <clinit>，即类静态初始化块
                            if ("<clinit>".equals(name)) {
                                return new MethodVisitor(Opcodes.ASM9, mv) {
                                    private Object lastValue = null;

                                    @Override
                                    public void visitLdcInsn(Object value) {
                                        lastValue = value;
                                        super.visitLdcInsn(value);
                                    }

                                    @Override
                                    public void visitInsn(int opcode) {
                                        switch (opcode) {
                                            case Opcodes.ICONST_M1: lastValue = -1; break;
                                            case Opcodes.ICONST_0: lastValue = 0; break;
                                            case Opcodes.ICONST_1: lastValue = 1; break;
                                            case Opcodes.ICONST_2: lastValue = 2; break;
                                            case Opcodes.ICONST_3: lastValue = 3; break;
                                            case Opcodes.ICONST_4: lastValue = 4; break;
                                            case Opcodes.ICONST_5: lastValue = 5; break;
                                        }
                                        super.visitInsn(opcode);
                                    }

                                    @Override
                                    public void visitIntInsn(int opcode, int operand) {
                                        if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                                            lastValue = operand;
                                        }
                                        super.visitIntInsn(opcode, operand);
                                    }

                                    @Override
                                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                        if (!found && opcode == Opcodes.PUTSTATIC && name.equals(fieldName)) {
                                            if (lastValue != null && type.isInstance(lastValue)) {
                                                @SuppressWarnings("unchecked")
                                                T castValue = (T) lastValue;
                                                onAccept.accept(true, castValue);
                                            } else {
                                                onAccept.accept(true, null);
                                            }
                                            found = true;
                                        }
                                        super.visitFieldInsn(opcode, owner, name, descriptor);
                                    }
                                };
                            }
                            return mv;
                        }
                    }, 0);
                } catch (Exception e) {
                    System.out.println("Failed to read class: " + entryName + ", reason: " + e.getMessage());
                }
            }

            result.add(entry);
        }

        if (!found) {
            onAccept.accept(false, null);
        }

        return result;
    }
}
