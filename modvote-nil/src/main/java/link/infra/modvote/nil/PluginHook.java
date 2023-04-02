package link.infra.modvote.nil;

import nilloader.api.ClassTransformer;
import nilloader.api.NilLogger;
import nilloader.api.lib.asm.ClassReader;
import nilloader.api.lib.asm.ClassWriter;
import nilloader.api.lib.asm.Label;
import nilloader.api.lib.asm.Opcodes;
import nilloader.api.lib.asm.tree.ClassNode;
import nilloader.api.lib.asm.tree.MethodNode;

import static nilloader.api.lib.asm.Opcodes.*;

public class PluginHook implements ClassTransformer {
	private static final NilLogger log = NilLogger.get("MobVoteTransformer");
	private static final NilLogger log2 = NilLogger.get("V1ModMetadataImpl");

	@Override
	public byte[] transform(ClassLoader loader, String className, byte[] classBytes) {
		if (!className.equals("org/quiltmc/loader/impl/metadata/qmj/V1ModMetadataImpl")) {
			return classBytes;
		}

		log.info("the form they promised me is great but my transition shall be agonizing");
		// I'm so sorry, Quilt devs
		log2.info("oh god my flesh... is.... melting..");

		ClassReader reader = new ClassReader(classBytes);
		ClassNode clazz = new ClassNode();
		reader.accept(clazz, 0);

		// Add plugin() method
		MethodNode method = new MethodNode(ACC_PUBLIC, "plugin", "()Lorg/quiltmc/loader/api/plugin/ModMetadataExt$ModPlugin;", null, null);
		method.visitCode();
		Label label0 = new Label();
		method.visitLabel(label0);
		method.visitVarInsn(ALOAD, 0);
		method.visitFieldInsn(GETFIELD, "org/quiltmc/loader/impl/metadata/qmj/V1ModMetadataImpl", "id", "Ljava/lang/String;");
		method.visitLdcInsn("modvote");
		method.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
		Label label1 = new Label();
		method.visitJumpInsn(IFEQ, label1);
		method.visitFieldInsn(GETSTATIC, "link/infra/modvote/plugin/MetadataHolder", "PLUGIN", "Lorg/quiltmc/loader/api/plugin/ModMetadataExt$ModPlugin;");
		Label label2 = new Label();
		method.visitJumpInsn(GOTO, label2);
		method.visitLabel(label1);
		method.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		method.visitInsn(ACONST_NULL);
		method.visitLabel(label2);
		method.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"org/quiltmc/loader/api/plugin/ModMetadataExt$ModPlugin"});
		method.visitInsn(ARETURN);
		Label label3 = new Label();
		method.visitLabel(label3);
		method.visitLocalVariable("this", "Lorg/quiltmc/loader/impl/metadata/qmj/V1ModMetadataImpl;", null, label0, label3, 0);
		method.visitMaxs(2, 1);
		method.visitEnd();

		clazz.methods.add(method);

		int flags = ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES;
		ClassWriter writer = new ClassWriter(flags) {
			@Override
			protected ClassLoader getClassLoader() {
				return loader;
			}
		};
		clazz.accept(writer);

		return writer.toByteArray();
	}

	@Override
	public byte[] transform(String className, byte[] originalData) {
		return transform(ClassLoader.getSystemClassLoader(), className, originalData);
	}
}
