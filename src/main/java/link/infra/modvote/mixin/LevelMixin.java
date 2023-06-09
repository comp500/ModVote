package link.infra.modvote.mixin;

import link.infra.modvote.rules.RulesManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin {
	@Inject(method = "tickBlockEntities", at = @At("HEAD"))
	public void modvote$onTick(CallbackInfo ci) {
		// Only tick when in integrated server thread in singleplayer
		RulesManager.INSTANCE.levelTick((Level)(Object)this);
	}
}
