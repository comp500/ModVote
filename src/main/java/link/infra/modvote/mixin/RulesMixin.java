package link.infra.modvote.mixin;

import link.infra.modvote.rules.RulesManager;
import net.minecraft.voting.rules.Rules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Rules.class)
public class RulesMixin {
	@Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/random/SimpleWeightedRandomList$Builder;build()Lnet/minecraft/util/random/SimpleWeightedRandomList;"))
	private static void modvote$beforeListBuild(CallbackInfo ci) {
		RulesManager.INSTANCE.registerRules();
	}
}
