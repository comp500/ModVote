package link.infra.modvote.plugin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import link.infra.modvote.rules.RulesManager;
import link.infra.modvote.scan.ModScanner;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.voting.rules.Rule;
import net.minecraft.voting.rules.RuleAction;
import net.minecraft.voting.rules.RuleChange;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VotedModsRule implements Rule {
	@Override
	public Codec<RuleChange> codec() {
		return Rule.<Mod>puntCodec(RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("id").forGetter(m -> m.id),
			Codec.STRING.fieldOf("name").forGetter(m -> m.name)
		).apply(instance, Mod::new)));
	}

	@Override
	public Stream<RuleChange> approvedChanges() {
		return RulesManager.INSTANCE.scannedMods.stream()
			// Use pending data, since this list is used before saving the rules
			.filter(r -> RulesManager.INSTANCE.pendingOrLoaded(r.id()))
			.map(r -> new Mod(r.id(), r.name()));
	}

	@Override
	public Stream<RuleChange> randomApprovableChanges(MinecraftServer minecraftServer, RandomSource randomSource, int i) {
		List<RuleChange> changes = RulesManager.INSTANCE.scannedMods.stream()
			.filter(ModScanner.Result::unloaded)
			.map(r -> new Mod(r.id(), r.name())).collect(Collectors.toList());
		Util.shuffle(changes, randomSource);
		return changes.stream().limit(i);
	}

	public class Mod implements RuleChange.Simple {
		public final String id;
		public final String name;

		public Mod(String id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public Component description() {
			return Component.translatable("rule.modvote.add", name);
		}

		@Override
		public Rule rule() {
			return VotedModsRule.this;
		}

		@Override
		public void update(RuleAction ruleAction) {
			RulesManager.INSTANCE.queueModUpdate(id, ruleAction == RuleAction.APPROVE);
		}
	}
}
