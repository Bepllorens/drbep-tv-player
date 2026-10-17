package com.drbep.build;

import org.objectweb.asm.*;

/** Media3 1.11.1: cache header validity before pesScratch is reused for PTS/DTS.
 * Never re-parse that scratch buffer at HLS EOF (which also corrupts payloadSize).
 * Remove when Media3 fixes canConsumeSynthesizedEmptyPusi upstream.
 */
public final class PesReaderFix extends ClassVisitor {
    private static final String OWNER = "androidx/media3/extractor/ts/PesReader";
    private static final String VALID = "drbepHeaderValid";
    private int replacements;
    public PesReaderFix(ClassVisitor next) { super(Opcodes.ASM9, next); }
    @Override public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("parseHeader") && desc.equals("()Z")) {
            return new MethodVisitor(Opcodes.ASM9, next) {
                @Override public void visitInsn(int opcode) {
                    if (opcode == Opcodes.IRETURN) {
                        // Preserve the return value and save it on this instance.
                        super.visitInsn(Opcodes.DUP);
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitInsn(Opcodes.SWAP);
                        super.visitFieldInsn(Opcodes.PUTFIELD, OWNER, VALID, "Z");
                    }
                    super.visitInsn(opcode);
                }
            };
        }
        if (name.equals("seek") && desc.equals("()V")) {
            return new MethodVisitor(Opcodes.ASM9, next) {
                @Override public void visitCode() {
                    super.visitCode();
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitInsn(Opcodes.ICONST_0);
                    super.visitFieldInsn(Opcodes.PUTFIELD, OWNER, VALID, "Z");
                }
            };
        }
        if (name.equals("canConsumeSynthesizedEmptyPusi") && desc.equals("(Z)Z")) {
            return new MethodVisitor(Opcodes.ASM9, next) {
                @Override public void visitMethodInsn(int opcode, String owner, String method,
                        String descriptor, boolean isInterface) {
                    if (owner.equals(OWNER) && method.equals("parseHeader") && descriptor.equals("()Z")) {
                        replacements++;
                        super.visitFieldInsn(Opcodes.GETFIELD, OWNER, VALID, "Z");
                    } else super.visitMethodInsn(opcode, owner, method, descriptor, isInterface);
                }
            };
        }
        return next;
    }
    @Override public void visitEnd() {
        if (replacements != 1) throw new IllegalStateException("Review Media3 PesReader EOF fix after dependency change");
        super.visitField(Opcodes.ACC_PRIVATE, VALID, "Z", null, null).visitEnd();
        super.visitEnd();
    }
}
