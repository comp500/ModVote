package link.infra.modvote.mixin;

import link.infra.modvote.rules.RulesManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@Inject(method = "tickChildren", at = @At("HEAD"))
	public void modvote$onTick(CallbackInfo ci) {
		RulesManager.INSTANCE.tick();
	}
}
