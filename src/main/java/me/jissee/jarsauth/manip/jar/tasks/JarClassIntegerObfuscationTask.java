package me.jissee.jarsauth.manip.jar.tasks;

import me.jissee.jarsauth.manip.jar.JarEntryWrapper;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Integer 常量混淆（可自定义嵌套层数）
 */
public class JarClassIntegerObfuscationTask implements Function<List<JarEntryWrapper>, List<JarEntryWrapper>> {

    private static final Random RANDOM = new Random();

    private static final int minDepth = 4;
    private static final int maxDepth = 5;

    @Override
    public List<JarEntryWrapper> apply(List<JarEntryWrapper> inputEntries) {
        List<JarEntryWrapper> result = new ArrayList<>();

        for (JarEntryWrapper wrapper : inputEntries) {
            String name = wrapper.getName();
            byte[] bytes = wrapper.getBytes();

            if (name.endsWith(".class")) {
                try {
                    ClassReader cr = new ClassReader(bytes);
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    ClassVisitor cv = new IntegerObfuscatorClassVisitor(cw);
                    cr.accept(cv, ClassReader.EXPAND_FRAMES);
                    result.add(new JarEntryWrapper(name, cw.toByteArray()));
                } catch (Exception ex) {
                    System.out.println("Error while obfuscating " + name + ": " + ex.getMessage());
                    result.add(wrapper);
                }
            } else {
                result.add(wrapper);
            }
        }
        return result;
    }

    // ---------------- ClassVisitor ----------------
    static class IntegerObfuscatorClassVisitor extends ClassVisitor {
        public IntegerObfuscatorClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new IntegerObfuscatorMethodVisitor(mv);
        }
    }

    // ---------------- 表达式节点 ----------------
    static class ExprNode {
        int value;
        List<ExprNode> children = new ArrayList<>();
        char op;
        int constant;

        boolean isLeaf() { return children.isEmpty(); }
    }

    // ---------------- MethodVisitor ----------------
    static class IntegerObfuscatorMethodVisitor extends MethodVisitor {
        public IntegerObfuscatorMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
                int value = (opcode == Opcodes.ICONST_M1) ? -1 : opcode - Opcodes.ICONST_0;
                generateIntegerExpression(value);
            } else {
                super.visitInsn(opcode);
            }
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                generateIntegerExpression(operand);
            } else {
                super.visitIntInsn(opcode, operand);
            }
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Integer) {
                generateIntegerExpression((Integer) cst);
            } else {
                super.visitLdcInsn(cst);
            }
        }

        private void generateIntegerExpression(int target) {
            int depth = minDepth + RANDOM.nextInt(maxDepth - minDepth + 1);
            ExprNode expr = generateNestedExpr(target, depth);

            if (expr.value != target) {
                int correction = target - expr.value;
                if (correction != 0) {
                    int corrDepth = Math.max(1, depth / 2);
                    ExprNode corrNode = generateNestedExpr(correction, corrDepth);
                    ExprNode wrapper = new ExprNode();
                    wrapper.op = '+';
                    wrapper.children.add(expr);
                    wrapper.children.add(corrNode);
                    wrapper.value = expr.value + corrNode.value;
                    expr = wrapper;
                }
            }
            emitExpr(expr);
        }

        private ExprNode generateNestedExpr(int target, int depth) {
            if (depth <= 0) return buildDirectLeaf(target);

            ExprNode node = new ExprNode();
            char[] ops = "+-^*".toCharArray();
            node.op = ops[RANDOM.nextInt(ops.length)];

            int a, b;
            switch (node.op) {
                case '+':
                    a = RANDOM.nextInt();
                    b = target - a;
                    break;
                case '-':
                    b = RANDOM.nextInt();
                    a = target + b;
                    break;
                case '^':
                    a = RANDOM.nextInt();
                    b = target ^ a;
                    break;
                case '*':
                    if (target != 0) {
                        int factor = RANDOM.nextInt(9) + 2;
                        if (target % factor == 0) {
                            a = target / factor;
                            b = factor;
                            break;
                        }
                    }
                    a = RANDOM.nextInt();
                    b = target - a;
                    node.op = '+';
                    break;
                default:
                    a = RANDOM.nextInt();
                    b = target - a;
                    node.op = '+';
            }

            node.children.add(generateNestedExpr(a, depth - 1));
            node.children.add(generateNestedExpr(b, depth - 1));
            node.value = compute(node.op, node.children.get(0).value, node.children.get(1).value);

            return node;
        }

        private int compute(char op, int a, int b) {
            switch (op) {
                case '+': return a + b;
                case '-': return a - b;
                case '*': return a * b;
                case '^': return a ^ b;
                default: return a;
            }
        }

        private ExprNode buildDirectLeaf(int value) {
            ExprNode leaf = new ExprNode();
            leaf.constant = value;
            leaf.value = value;
            return leaf;
        }

        private void emitExpr(ExprNode node) {
            if (node.isLeaf()) {
                mv.visitLdcInsn(Integer.valueOf(node.constant));
                return;
            }
            emitExpr(node.children.get(0));
            emitExpr(node.children.get(1));
            switch (node.op) {
                case '+': mv.visitInsn(Opcodes.IADD); break;
                case '-': mv.visitInsn(Opcodes.ISUB); break;
                case '*': mv.visitInsn(Opcodes.IMUL); break;
                case '^': mv.visitInsn(Opcodes.IXOR); break;
            }
        }
    }
}
