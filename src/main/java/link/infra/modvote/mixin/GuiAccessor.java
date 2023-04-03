package link.infra.modvote.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
	@Accessor
	BossHealthOverlay getBossOverlay();
}
