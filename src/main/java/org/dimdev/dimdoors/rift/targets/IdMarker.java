package org.dimdev.dimdoors.rift.targets;

import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;

import org.dimdev.dimdoors.api.rift.target.EntityTarget;
import org.dimdev.dimdoors.api.util.EntityUtils;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;

public class IdMarker extends VirtualTarget implements EntityTarget {
	private final int id;

	public IdMarker(int id) {
		this.id = id;
	}

	@Override
	public VirtualTargetType<? extends VirtualTarget> getType() {
		return VirtualTargetType.ID_MARKER;
	}

	public static CompoundTag toTag(IdMarker target) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("id", target.id);
		return tag;
	}

	public static IdMarker fromTag(CompoundTag nbt) {
		return new IdMarker(nbt.getInt("id"));
	}

	public int getId() {
		return this.id;
	}

	@Override
	public boolean receiveEntity(Entity entity, Vec3d relativePos, EulerAngle relativeAngle, Vec3d relativeVelocity) {
		EntityUtils.chat(entity, Text.of("This rift is configured for pocket dungeons. Its id is " + this.id));
		return false;
	}
}
