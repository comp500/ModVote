package link.infra.modvote.ui;

import link.infra.modvote.mixin.BossHealthOverlayAccessor;
import link.infra.modvote.mixin.GuiAccessor;
import link.infra.modvote.rules.RulesManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

import java.util.Map;
import java.util.UUID;

public class BossEventUpdater {
	private static final UUID evtId = UUID.randomUUID();

	public static void update(int ticks) {
		BossHealthOverlay healthOverlay = ((GuiAccessor) Minecraft.getInstance().gui).getBossOverlay();
		Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor)healthOverlay).getEvents();
		LerpingBossEvent evt = events.get(evtId);
		Component text = Component.translatable("boss.modvote.reloading", ticks / 20);
		if (evt == null) {
			evt = new LerpingBossEvent(evtId,
				text, 0,
				BossEvent.BossBarColor.PURPLE,
				BossEvent.BossBarOverlay.PROGRESS, true, true, true);
			events.put(evtId, evt);
		}

		evt.setName(text);
		evt.setProgress((RulesManager.UPDATE_DELAY - (float)ticks) / RulesManager.UPDATE_DELAY);
	}

	public static void clear() {
		BossHealthOverlay healthOverlay = ((GuiAccessor) Minecraft.getInstance().gui).getBossOverlay();
		Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor)healthOverlay).getEvents();
		events.remove(evtId);
	}
}
