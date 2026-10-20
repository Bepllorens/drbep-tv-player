package com.drbep.build;
import com.android.build.api.instrumentation.*;
import org.objectweb.asm.ClassVisitor;
public abstract class PesReaderFixFactory implements AsmClassVisitorFactory<InstrumentationParameters.None> {
    @Override public boolean isInstrumentable(ClassData data) {
        return data.getClassName().equals("androidx.media3.extractor.ts.PesReader");
    }
    @Override public ClassVisitor createClassVisitor(ClassContext context, ClassVisitor next) {
        return new PesReaderFix(new PesReaderFragmentFix(next));
    }
}
