package link.infra.modvote.plugin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import link.infra.modvote.ModVote;
import link.infra.modvote.rules.RulesManager;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.voting.rules.Rule;
import net.minecraft.voting.rules.RuleAction;
import net.minecraft.voting.rules.RuleChange;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VotedModsRule implements Rule {
	// Always empty on start - server will sync/load changes, then both sides will update their mod lists to match
	// with a janky artificial delay to ensure all mod updates are done at once
	// (on disconnect/reconnect, rulechanges are repealed, so this should always be correct)
	private final Set<Mod> approvedMods = new HashSet<>();

	@Override
	public Codec<RuleChange> codec() {
		return Rule.<Mod>puntCodec(RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("id").forGetter(m -> m.id),
			Codec.STRING.fieldOf("name").forGetter(m -> m.name)
		).apply(instance, Mod::new)));
	}

	@Override
	public Stream<RuleChange> approvedChanges() {
		// Use pending data, since this list is used to save the rules
		return approvedMods.stream().map(r -> r);
	}

	@Override
	public Stream<RuleChange> randomApprovableChanges(MinecraftServer minecraftServer, RandomSource randomSource, int i) {
		List<RuleChange> changes = RulesManager.INSTANCE.scannedMods.stream()
			.map(r -> new Mod(r.id(), r.name()))
			.filter(m -> !approvedMods.contains(m)).collect(Collectors.toList());
		Util.shuffle(changes, randomSource);
		return changes.stream().limit(i);
	}

	public Set<String> getApprovedModIds() {
		return approvedMods.stream().map(m -> m.id).collect(Collectors.toUnmodifiableSet());
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
			boolean idIsAvailable = RulesManager.INSTANCE.scannedMods.stream().anyMatch(mod -> mod.id().equals(id));
			if (!idIsAvailable) {
				ModVote.LOGGER.warn("Failed to find voted mod " + id + " (do you have the same mods as the server?)");
				return;
			}
			boolean updated;
			if (ruleAction == RuleAction.APPROVE) {
				updated = approvedMods.add(this);
			} else {
				updated = approvedMods.remove(this);
			}
			if (updated) {
				RulesManager.INSTANCE.queueModUpdate();
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Mod mod = (Mod) o;
			return id.equals(mod.id) && name.equals(mod.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, name);
		}
	}
}
