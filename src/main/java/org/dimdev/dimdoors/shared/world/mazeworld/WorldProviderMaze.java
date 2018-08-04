package org.dimdev.dimdoors.shared.world.mazeworld;

import net.minecraft.client.audio.MusicTicker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.dimdev.ddutils.Location;
import org.dimdev.ddutils.render.CloudRenderBlank;
import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.shared.blocks.ModBlocks;
import org.dimdev.dimdoors.shared.sound.ModSounds;
import org.dimdev.dimdoors.shared.world.ModBiomes;
import org.dimdev.dimdoors.shared.world.ModDimensions;

public class WorldProviderMaze extends WorldProvider {
    @SideOnly(Side.CLIENT)
    public static MusicTicker.MusicType music;
    static {
        if (DimDoors.proxy.isClient()) {
            music = EnumHelper.addEnum(MusicTicker.MusicType.class, "limbo", new Class<?>[] {SoundEvent.class, int.class, int.class}, ModSounds.CREEPY, 0, 0);
        }
    }
    
    @Override
    public void init() {
        hasSkyLight = false;
        biomeProvider = new BiomeProviderSingle(ModBiomes.DANGEROUS_BLACK_VOID);
        DimDoors.proxy.setCloudRenderer(this, new CloudRenderBlank());
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    protected void generateLightBrightnessTable() {
        // Brightness decreases with light level in limbo
        for (int i = 0; i <= 15; ++i) {
            float f1 = 1.0F - i / 15.0F;
            lightBrightnessTable[i] = f1 / (f1 * 3.0F + 1.0F) * 1.0F * 3;
        }
    }

    @Override
    public BlockPos getSpawnPoint() {
        int x = MathHelper.clamp(world.rand.nextInt(), -500, 500);
        int z = MathHelper.clamp(world.rand.nextInt(), -500, 500);
        return new BlockPos(x, 700, z);
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getMoonPhase(long worldTime) {
        return 4;
    }

    @Override
    public boolean canCoordinateBeSpawn(int x, int z) {
        BlockPos pos = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
        return world.getBlockState(pos) == ModBlocks.UNRAVELLED_FABRIC.getDefaultState();
    }

    @Override
    public double getHorizon() {
        return (double) world.getHeight() / 4 - 800;
    }

    @Override
    public int getRespawnDimension(EntityPlayerMP player) {
        return 0;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new ChunkGeneratorMaze(world);
    }

    public static Location getLimboSkySpawn(Entity entity) { // TODO: move this somewhere else
        int x = (int) entity.posX + MathHelper.clamp(entity.world.rand.nextInt(), 100, 100);
        int z = (int) entity.posZ + MathHelper.clamp(entity.world.rand.nextInt(), -100, 100);
        return new Location(ModDimensions.LIMBO.getId(), x, 700, z);
    }

    @Override
    public BlockPos getRandomizedSpawnPoint() {
        int x = MathHelper.clamp(world.rand.nextInt(), -500, 500);
        int z = MathHelper.clamp(world.rand.nextInt(), -500, 500);
        return new BlockPos(x, 700, z);
    }

    @Override
    public DimensionType getDimensionType() {
        return ModDimensions.LIMBO;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getSkyColor(Entity cameraEntity, float partialTicks) {
        return Vec3d.ZERO;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        return new Vec3d(.2, .2, .2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean doesXZShowFog(int x, int z) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public MusicTicker.MusicType getMusicType() {
        return music;
    }
}
