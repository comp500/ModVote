package link.infra.modvote.mixin;

import link.infra.modvote.rules.RulesManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
	@Shadow public abstract boolean isLocalServer();

	@Inject(method = "tick", at = @At("HEAD"))
	public void modvote$onTick(CallbackInfo ci) {
		if (!isLocalServer()) {
			RulesManager.INSTANCE.tick();
		}
	}
}
