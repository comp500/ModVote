package link.infra.modvote.nil;

import nilloader.api.lib.asm.tree.LabelNode;
import nilloader.api.lib.mini.MiniTransformer;
import nilloader.api.lib.mini.PatchContext;
import nilloader.api.lib.mini.annotation.Patch;

import static link.infra.modvote.nil.ModVoteNil.fork;

@Patch.Class("org.quiltmc.loader.impl.launch.knot.KnotClient")
public class QuiltMainHook extends MiniTransformer {
	@Patch.Method("main([Ljava/lang/String;)V")
	@Patch.Method.AffectsControlFlow()
	public void patchMain(PatchContext ctx) {
		ctx.jumpToStart();

		ctx.add(ALOAD(0));
		ctx.add(INVOKESTATIC("link/infra/modvote/nil/QuiltMainHook$Hooks", "onMain", "([Ljava/lang/String;)Z"));
		LabelNode end = new LabelNode();
		ctx.add(IFEQ(end));
		ctx.add(RETURN());
		ctx.add(end);
	}

	public static class Hooks {
		public static boolean onMain(String[] mainArgs) {
			if (!"true".equals(System.getProperty("modvote.forked"))) {
				while (fork(mainArgs)) { }
				return true;
			}
			return false;
		}
	}
}
