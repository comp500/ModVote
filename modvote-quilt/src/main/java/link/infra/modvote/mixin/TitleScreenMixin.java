package link.infra.modvote.mixin;

import link.infra.modvote.ModVote;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
	@Inject(method = "init", at = @At("TAIL"))
	public void modvote$onInit(CallbackInfo ci) {
		ModVote.LOGGER.info("Restarting the game...");
		System.exit(1337);
	}
}
