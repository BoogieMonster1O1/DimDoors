package org.dimdev.dimdoors.pockets.modifier;

import net.minecraft.nbt.CompoundTag;

import com.google.common.base.MoreObjects;
import org.dimdev.dimdoors.rift.targets.PocketEntranceMarker;
import org.dimdev.dimdoors.rift.targets.PocketExitMarker;
import org.dimdev.dimdoors.pockets.PocketGenerationContext;
import org.dimdev.dimdoors.world.pocket.type.Pocket;

public class PocketEntranceModifier implements Modifier {
	public static final String KEY = "pocket_entrance";

	private int id;

	public PocketEntranceModifier(int id) {
		this.id = id;
	}

	public PocketEntranceModifier() {

	}

	@Override
	public Modifier fromTag(CompoundTag tag) {
		return new PocketEntranceModifier(tag.getInt("id"));
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		Modifier.super.toTag(tag);

		tag.putInt("id", id);

		return tag;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.toString();
	}

	@Override
	public ModifierType<? extends Modifier> getType() {
		return ModifierType.PUBLIC_MODIFIER_TYPE;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public void apply(PocketGenerationContext parameters, RiftManager manager) {
		manager.consume(id, rift -> {
			rift.setDestination(PocketEntranceMarker.builder().ifDestination(new PocketExitMarker()).weight(1.0f).build());
			return true;
		});
	}

	@Override
	public void apply(PocketGenerationContext parameters, Pocket.PocketBuilder<?, ?> builder) {

	}
}
