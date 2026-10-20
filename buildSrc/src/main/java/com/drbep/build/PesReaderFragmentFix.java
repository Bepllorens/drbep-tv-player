package com.drbep.build;
import org.objectweb.asm.*;
/** Media3 1.11.1: no input is not completion when extended-header bytes remain. */
public final class PesReaderFragmentFix extends ClassVisitor {
 private int methods;
 public PesReaderFragmentFix(ClassVisitor next){super(Opcodes.ASM9,next);}
 @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions){
  MethodVisitor next=super.visitMethod(access,name,desc,signature,exceptions);
  if(!name.equals("continueRead")||!desc.equals("(Landroidx/media3/common/util/ParsableByteArray;[BI)Z"))return next;
  methods++;
  return new MethodVisitor(Opcodes.ASM9,next){
   @Override public void visitCode(){
    super.visitCode();Label original=new Label();
    super.visitVarInsn(Opcodes.ALOAD,1);
    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"androidx/media3/common/util/ParsableByteArray","bytesLeft","()I",false);
    super.visitJumpInsn(Opcodes.IFNE,original);
    super.visitVarInsn(Opcodes.ALOAD,0);
    super.visitFieldInsn(Opcodes.GETFIELD,"androidx/media3/extractor/ts/PesReader","bytesRead","I");
    super.visitVarInsn(Opcodes.ILOAD,3);
    super.visitJumpInsn(Opcodes.IF_ICMPGE,original);
    super.visitInsn(Opcodes.ICONST_0);super.visitInsn(Opcodes.IRETURN);
    super.visitLabel(original);
   }
  };
 }
 @Override public void visitEnd(){
  if(methods!=1)throw new IllegalStateException("Review Media3 fragmented PES fix after dependency change");
  super.visitEnd();
 }
}
