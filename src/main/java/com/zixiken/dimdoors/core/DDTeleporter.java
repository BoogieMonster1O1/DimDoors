package com.zixiken.dimdoors.core;

import java.util.ArrayList;
import java.util.Random;

import com.zixiken.dimdoors.helpers.EnumFacingHelper;
import com.zixiken.dimdoors.world.LimboProvider;
import com.zixiken.dimdoors.blocks.BaseDimDoor;
import com.zixiken.dimdoors.blocks.IDimDoor;
import com.zixiken.dimdoors.config.DDProperties;
import com.zixiken.dimdoors.helpers.yCoordHelper;
import com.zixiken.dimdoors.DimDoors;
import com.zixiken.dimdoors.schematic.BlockRotator;
import com.zixiken.dimdoors.tileentities.TileEntityDimDoor;
import com.zixiken.dimdoors.util.Point4D;
import com.zixiken.dimdoors.world.PocketBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemDoor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import com.zixiken.dimdoors.watcher.ClientDimData;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class DDTeleporter {
    private static final Random random = new Random();
    private static final int NETHER_DIMENSION_ID = -1;
    private static final int OVERWORLD_DIMENSION_ID = 0;
    private static final int END_DIMENSION_ID = 1;
    private static final int MAX_NETHER_EXIT_CHANCE = 100;
    private static final int NETHER_EXIT_CHANCE = 20; //20% chance to compensate for frequent exit failures - the Nether often doesn't have enough space for an exit
    private static final int MAX_OVERWORLD_EXIT_CHANCE = 100;
    private static final int OVERWORLD_EXIT_CHANCE = 15;
    private static final int MAX_ROOT_SHIFT_CHANCE = 100;
    private static final int START_ROOT_SHIFT_CHANCE = 0;
    private static final int ROOT_SHIFT_CHANCE_PER_LEVEL = 5;
    private static final String SPIRIT_WORLD_NAME = "Spirit World";

    public static int cooldown = 0;

    private DDTeleporter() { }

    /**
     * Checks if the destination supplied is safe (i.e. filled by any replaceable or non-opaque blocks)
     */
    private static boolean checkDestination(WorldServer world, Point4D destination, EnumFacing orientation) {
        int x = destination.getX();
        int y = destination.getY();
        int z = destination.getZ();
        Block blockTop;
        Block blockBottom;
        BlockPos point;

        switch (orientation) {
            case SOUTH:
                point = new BlockPos(x - 1, y - 1, z);
                break;
            case WEST:
                point = new BlockPos(x, y - 1, z - 1);
                break;
            case NORTH:
                point =  new BlockPos(x + 1, y - 1, z);
                break;
            case EAST:
                point =  new BlockPos(x, y - 1, z + 1);
                break;
            default:
                point =  new BlockPos(x, y - 1, z);
                break;
        }

        blockBottom = world.getBlockState(point).getBlock();
        blockTop = world.getBlockState(point.up()).getBlock();

        if (blockBottom != null) {
            if (!blockBottom.isReplaceable(world, point) && world.isBlockNormalCube(point, false)) {
                return false;
            }
        } if (blockTop != null) {
            if (!blockTop.isReplaceable(world, point.up())) {
                return false;
            }
        }

        return true;
    }

    private static void placeInPortal(Entity entity, WorldServer world, Point4D destination, DDProperties properties, boolean checkOrientation) {
        int x = destination.getX();
        int y = destination.getY();
        int z = destination.getZ();

        EnumFacing orientation;
        if (checkOrientation) {
            orientation = getDestinationOrientation(destination, properties);
            entity.rotationYaw = (orientation.getIndex() * 90) + 90;
        } else {
            // Teleport the entity to the precise destination point
            orientation = EnumFacing.SOUTH;
        }

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (checkDestination(world, destination, orientation)) {
                switch (orientation) {
                    case SOUTH:
                        player.setPositionAndUpdate(x - 0.5, y - 1, z + 0.5);
                        break;
                    case WEST:
                        player.setPositionAndUpdate(x + 0.5, y - 1, z - 0.5);
                        break;
                    case NORTH:
                        player.setPositionAndUpdate(x + 1.5, y - 1, z + 0.5);
                        break;
                    case EAST:
                        player.setPositionAndUpdate(x + 0.5, y - 1, z + 1.5);
                        break;
                    default:
                        player.setPositionAndUpdate(x + 0.5, y - 1, z + 0.5);
                        break;
                }
            }
            else {
                player.setPositionAndUpdate(x + 0.5, y - 1, z + 0.5);
            }
        } else if (entity instanceof EntityMinecart) {
            entity.motionX = 0;
            entity.motionZ = 0;
            entity.motionY = 0;
            switch (orientation) {
                case SOUTH:
                    DDTeleporter.setEntityPosition(entity, x - 0.5, y, z + 0.5);
                    entity.motionX = -0.39;
                    entity.worldObj.updateEntityWithOptionalForce(entity, false);
                    break;
                case WEST:
                    DDTeleporter.setEntityPosition(entity, x + 0.5, y, z - 0.5);
                    entity.motionZ = -0.39;
                    entity.worldObj.updateEntityWithOptionalForce(entity, false);
                    break;
                case NORTH:
                    DDTeleporter.setEntityPosition(entity, x + 1.5, y, z + 0.5);
                    entity.motionX = 0.39;
                    entity.worldObj.updateEntityWithOptionalForce(entity, false);
                    break;
                case EAST:
                    DDTeleporter.setEntityPosition(entity, x + 0.5, y, z + 1.5);
                    entity.motionZ = 0.39;
                    entity.worldObj.updateEntityWithOptionalForce(entity, false);
                    break;
                default:
                    DDTeleporter.setEntityPosition(entity, x + 0.5, y, z + 0.5);
                    entity.worldObj.updateEntityWithOptionalForce(entity, false);
                    break;
            }
        } else {
            switch (orientation) {
                case SOUTH:
                    setEntityPosition(entity, x - 0.5, y, z + 0.5);
                    break;
                case WEST:
                    setEntityPosition(entity, x + 0.5, y, z - 0.5);
                    break;
                case NORTH:
                    setEntityPosition(entity, x + 1.5, y, z + 0.5);
                    break;
                case EAST:
                    setEntityPosition(entity, x + 0.5, y, z + 1.5);
                    break;
                default:
                    setEntityPosition(entity, x + 0.5, y, z + 0.5);
                    break;
            }
        }
    }

    private static void setEntityPosition(Entity entity, double x, double y, double z) {
        entity.lastTickPosX = entity.prevPosX = entity.posX = x;
        entity.lastTickPosY = entity.prevPosY = entity.posY = y + entity.getYOffset();
        entity.lastTickPosZ = entity.prevPosZ = entity.posZ = z;
        entity.setPosition(x, y, z);
    }

    private static EnumFacing getDestinationOrientation(Point4D door, DDProperties properties) {
        World world = DimensionManager.getWorld(door.getDimension());
        if (world == null) {
            throw new IllegalStateException("The destination world should be loaded!");
        }

        //Check if the block below that point is actually a door
        IBlockState block = world.getBlockState(door.toBlockPos().down());
        if (block==null || !(block instanceof IDimDoor)) {
            //Return the pocket's orientation instead
            return PocketManager.createDimensionData(world).orientation();
        }


        //Return the orientation portion of its metadata
        return world.getBlockState(door.toBlockPos().down()).getValue(BlockDoor.FACING);
    }

    public static Entity teleportEntity(Entity entity, Point4D destination, boolean checkOrientation) {
        if (entity == null) {
            throw new IllegalArgumentException("entity cannot be null.");
        } if (destination == null) {
            throw new IllegalArgumentException("destination cannot be null.");
        }
        //This beautiful teleport method is based off of xCompWiz's teleport function.

        WorldServer oldWorld = (WorldServer) entity.worldObj;
        WorldServer newWorld;
        EntityPlayerMP player = (entity instanceof EntityPlayerMP) ? (EntityPlayerMP) entity : null;
        DDProperties properties = DDProperties.instance();

        // Is something riding? Handle it first.
        if (entity.riddenByEntity != null) {
            return teleportEntity(entity.riddenByEntity, destination, checkOrientation);
        }

        // Are we riding something? Dismount and tell the mount to go first.
        Entity cart = entity.ridingEntity;
        if (cart != null) {
            entity.mountEntity(null);
            cart = teleportEntity(cart, destination, checkOrientation);
            // We keep track of both so we can remount them on the other side.
        }

        // Determine if our destination is in another realm.
        boolean difDest = entity.dimension != destination.getDimension();
        if (difDest) {
            // Destination isn't loaded? Then we need to load it.
            newWorld = PocketManager.loadDimension(destination.getDimension());
        } else {
            newWorld = oldWorld;
        }


        // GreyMaria: What is this even accomplishing? We're doing the exact same thing at the end of this all.
        // TODO Check to see if this is actually vital.
        DDTeleporter.placeInPortal(entity, newWorld, destination, properties, checkOrientation);

        if (difDest) // Are we moving our target to a new dimension?{
            if(player != null) // Are we working with a player?{
                // We need to do all this special stuff to move a player between dimensions.
                //Give the client the dimensionData for the destination

                // FIXME: This violates the way we assume PocketManager works. DimWatcher should not be exposed
                // to prevent us from doing bad things. Moreover, no dimension is being created, so if we ever
                // tie code to that, it could cause confusing bugs.
                // No hacky for you! ~SenseiKiwi
                PocketManager.getDimwatcher().onCreated(new ClientDimData(PocketManager.createDimensionData(newWorld)));

                // Set the new dimension and inform the client that it's moving to a new world.
                player.dimension = destination.getDimension();
                player.playerNetServerHandler.sendPacket(new S07PacketRespawn(player.dimension, player.worldObj.getDifficulty(), newWorld.getWorldInfo().getTerrainType(), player.theItemInWorldManager.getGameType()));

                // GreyMaria: Used the safe player entity remover before.
                // This should fix an apparently unreported bug where
                // the last non-sleeping player leaves the Overworld
                // for a pocket dimension, causing all sleeping players
                // to remain asleep instead of progressing to day.
                ((WorldServer)entity.worldObj).getPlayerManager().removePlayer(player);
                oldWorld.removePlayerEntityDangerously(player);
                player.isDead = false;

                // Creates sanity by ensuring that we're only known to exist where we're supposed to be known to exist.
                oldWorld.getPlayerManager().removePlayer(player);
                newWorld.getPlayerManager().addPlayer(player);

                player.theItemInWorldManager.setWorld(newWorld);

                // Synchronize with the server so the client knows what time it is and what it's holding.
                player.mcServer.getConfigurationManager().updateTimeAndWeatherForPlayer(player, newWorld);
                player.mcServer.getConfigurationManager().syncPlayerInventory(player);
                for(Object potionEffect : player.getActivePotionEffects())
                {
                    PotionEffect effect = (PotionEffect)potionEffect;
                    player.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(player.getEntityId(), effect));
                }

                player.playerNetServerHandler.sendPacket(new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));


            // Creates sanity by removing the entity from its old location's chunk entity list, if applicable.
            int entX = entity.chunkCoordX;
            int entZ = entity.chunkCoordZ;
            if ((entity.addedToChunk) && (oldWorld.getChunkProvider().chunkExists(entX, entZ))) {
                oldWorld.getChunkFromChunkCoords(entX, entZ).removeEntity(entity);
                oldWorld.getChunkFromChunkCoords(entX, entZ).setModified(true);
            }

            // Memory concerns.
            oldWorld.onEntityRemoved(entity);

            if (player == null) { // Are we NOT working with a player?
                NBTTagCompound entityNBT = new NBTTagCompound();
                entity.isDead = false;
                entity.writeMountToNBT(entityNBT);

                if(entityNBT.hasNoTags()) {
                    return entity;
                }

                entity.isDead = true;
                entity = EntityList.createEntityFromNBT(entityNBT, newWorld);

                if (entity == null) {
                    // TODO FIXME IMPLEMENT NULL CHECKS THAT ACTUALLY DO SOMETHING.
					/* 
					 * shit ourselves in an organized fashion, preferably
					 * in a neat pile instead of all over our users' games
					 */
                }

            }

            // Finally, respawn the entity in its new home.
            newWorld.spawnEntityInWorld(entity);
            entity.setWorld(newWorld);


        entity.worldObj.updateEntityWithOptionalForce(entity, false);

        // Hey, remember me? It's time to remount.
        if (cart != null) {
            // Was there a player teleported? If there was, it's important that we update shit.
            if (player != null) {
                entity.worldObj.updateEntityWithOptionalForce(entity, true);
            }
            entity.mountEntity(cart);
        }

        // Did we teleport a player? Load the chunk for them.
        if (player != null) {
            newWorld.getChunkProvider().provideChunk(MathHelper.floor_double(entity.posX) >> 4, MathHelper.floor_double(entity.posZ) >> 4);
            // Tell Forge we're moving its players so everyone else knows.
            // Let's try doing this down here in case this is what's killing NEI.
            FMLCommonHandler.instance().firePlayerChangedDimensionEvent((EntityPlayer) entity, oldWorld.provider.getDimensionId(), newWorld.provider.getDimensionId());
        }

        DDTeleporter.placeInPortal(entity, newWorld, destination, properties, checkOrientation);
        return entity;
    }

    /**
     * Primary function used to teleport the player using doors. Performs numerous null checks, and also generates the destination door/pocket if it has not done so already.
     * Also ensures correct orientation relative to the door.
     * @param world - world the player is currently in
     * @param link - the link the player is using to teleport; sends the player to its destination
     * @param entity - the instance of the player to be teleported
     */
    public static void traverseDimDoor(World world, DimLink link,
                                       Entity entity, Block door) throws IllegalArgumentException {
        if(world.isRemote) return;
        if(world == null) throw new IllegalArgumentException("world cannot be null.");
        if(link == null) throw new IllegalArgumentException("link cannot be null.");
        if(entity == null) throw new IllegalArgumentException("entity cannot be null.");

        if (cooldown == 0 || entity instanceof EntityPlayer) {
            // According to Steven, we increase the cooldown by a random amount so that if someone
            // makes a loop with doors and throws items in them, the slamming sounds won't all
            // sync up and be very loud. The cooldown itself prevents server-crashing madness.
            cooldown = 2 + random.nextInt(2);
        } else {
            return;
        }

        if (!initializeDestination(link, DDProperties.instance(),entity,door)) {
            return;
        } if (link.linkType() == LinkType.RANDOM) {
            Point4D randomDestination = getRandomDestination();
            if (randomDestination != null) {
                entity = teleportEntity(entity, randomDestination, true);
                entity.worldObj.playSoundEffect(entity.posX, entity.posY, entity.posZ, "mob.endermen.portal", 1.0F, 1.0F);
            }
        } else {
            buildExitDoor(door, link, DDProperties.instance());
            entity = teleportEntity(entity, link.destination(), link.linkType() != LinkType.UNSAFE_EXIT);
            entity.worldObj.playSoundEffect(entity.posX, entity.posY, entity.posZ, "mob.endermen.portal", 1.0F, 1.0F);
        }
    }

    private static boolean initializeDestination(DimLink link, DDProperties properties, Entity entity, Block door) {
        if (link.hasDestination()&&link.linkType()!=LinkType.PERSONAL) {
            if (PocketManager.isBlackListed(link.destination().getDimension())) {

                // This link leads to a dimension that has been blacklisted.
                // That means that it was a pocket and it was deleted.
                // Depending on the link type, we must overwrite it or cancel
                // the teleport operation. We don't need to assign 'link' with
                // a different value. DimData will overwrite it in-place.
                DimData start = PocketManager.getDimensionData(link.source().getDimension());
                if (link.linkType() == LinkType.DUNGEON) {
                    // Ovewrite the link into a dungeon link with no destination
                    start.createLink(link.source(), LinkType.DUNGEON, link.orientation(), null);
                } else {
                    if (start.isPocketDimension()) {
                        // Ovewrite the link into a safe exit link, because
                        // this could be the only way out from a pocket.
                        start.createLink(link.source(), LinkType.SAFE_EXIT, link.orientation(), null);
                    } else {
                        // Cancel the teleport attempt
                        return false;
                    }
                }
            } else {
                return true;
            }
        }

        // Check the destination type and respond accordingly
        switch (link.linkType()) {
            case DUNGEON:
                return PocketBuilder.generateNewDungeonPocket(link, properties);
            case POCKET:
                return PocketBuilder.generateNewPocket(link, properties, door, DimensionType.POCKET);
            case PERSONAL:
                return setupPersonalLink(link, properties, entity, door);
            case SAFE_EXIT:
                return generateSafeExit(link, properties);
            case DUNGEON_EXIT:
                return generateDungeonExit(link, properties);
            case UNSAFE_EXIT:
                return generateUnsafeExit(link);
            case LIMBO:
                if(!(entity instanceof EntityPlayer)) {
                    return false;
                }

                Point4D dest = LimboProvider.getLimboSkySpawn((EntityPlayer)entity, DDProperties.instance());
                link.tail.setDestination(dest);
                return true;
            case NORMAL:
            case REVERSE:
            case RANDOM:
                return true;
            default:
                throw new IllegalArgumentException("link has an unrecognized link type.");
        }
    }

    private static boolean setupPersonalLink(DimLink link, DDProperties properties,Entity entity, Block door) {
        if(!(entity instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer player = (EntityPlayer)entity;
        DimData dim = PocketManager.getPersonalDimensionForPlayer(player.getGameProfile().getId().toString());
        if(dim == null) {
            return PocketBuilder.generateNewPersonalPocket(link, properties, player, door);
        }

        DimLink personalHomeLink = dim.getLink(dim.origin());
        if(personalHomeLink!=null) {
            link.tail.setDestination(personalHomeLink.source());
            PocketManager.getDimensionData(link.source().getDimension()).setLinkDestination(personalHomeLink, link.source().toBlockPos());
        }

        return true;
    }

    private static Point4D getRandomDestination() {
        // Our aim is to return a random link's source point
        // so that a link of type RANDOM can teleport a player there.

        // Restrictions:
        // 1. Ignore links with their source inside a pocket dimension.
        // 2. Ignore links with link type RANDOM.

        // Iterate over the root dimensions. Pocket dimensions cannot be roots.
        // Don't just pick a random root and a random link within that root
        // because we want to have unbiased selection among all links.
        ArrayList<Point4D> matches = new ArrayList<Point4D>();
        for (DimData dimension : PocketManager.getRootDimensions()) {
            for (DimLink link : dimension.getAllLinks()) {
                if (link.linkType() != LinkType.RANDOM) {
                    matches.add(link.source());
                }
            }
        }

        // Pick a random point, if any is available
        if (!matches.isEmpty()) {
            return matches.get( random.nextInt(matches.size()) );
        }
        return null;
    }

    private static boolean generateUnsafeExit(DimLink link) {
        // An unsafe exit teleports the user to the first available air space
        // in the pocket's root dimension. X and Z are kept roughly the same
        // as the source location, but Y is set by searching down. We don't
        // place a platform at the destination. We also don't place a reverse
        // link at the destination, so it's a one-way trip. Good luck!

        // To avoid loops, don't generate a destination if the player is
        // already in a non-pocket dimension.

        DimData current = PocketManager.getDimensionData(link.point.getDimension());

        if (current.isPocketDimension()) {
            Point4D source = link.source();
            World world = PocketManager.loadDimension(current.root().id());
            if (world == null) {
                return false;
            }

            BlockPos destination = yCoordHelper.findDropPoint(world, source.getX(), source.getY() + 1, source.getZ());
            if (destination != null) {
                current.root().setLinkDestination(link, destination);
                return true;
            }
        }

        return false;
    }

    private static void buildExitDoor(Block door,DimLink link, DDProperties prop) {
        World startWorld = PocketManager.loadDimension(link.source().getDimension());
        World destWorld = PocketManager.loadDimension(link.destination().getDimension());
        TileEntity doorTE = startWorld.getTileEntity(link.source().toBlockPos());
        if(doorTE instanceof TileEntityDimDoor) {
            if((TileEntityDimDoor.class.cast(doorTE).hasGennedPair)) {
                return;
            }
            TileEntityDimDoor.class.cast(doorTE).hasGennedPair=true;
            Block blockToReplace = destWorld.getBlockState(link.destination().toBlockPos()).getBlock();

            if(!destWorld.isAirBlock(link.destination().toBlockPos())) {
                if(!blockToReplace.isReplaceable(destWorld, link.destination().toBlockPos())) {
                    return;
                }
            }

            ItemDoor.placeDoor(destWorld, link.destination().toBlockPos().down(), link.getDestinationOrientation(), door);

            TileEntity 	doorDestTE = ((BaseDimDoor)door).initDoorTE(destWorld, link.destination().toBlockPos());


            if(doorDestTE instanceof TileEntityDimDoor) {
                TileEntityDimDoor.class.cast(doorDestTE).hasGennedPair=true;

            }
        }
    }

    private static boolean generateSafeExit(DimLink link, DDProperties properties) {
        DimData current = PocketManager.getDimensionData(link.point.getDimension());
        return generateSafeExit(current.root(), link, properties);
    }

    private static boolean generateDungeonExit(DimLink link, DDProperties properties) {
        // A dungeon exit acts the same as a safe exit, but has the chance of
        // taking the user to any non-pocket dimension, excluding Limbo and The End.
        // There is a chance of choosing the Nether first before other root dimensions
        // to compensate for servers with many Mystcraft ages or other worlds.

        DimData current = PocketManager.getDimensionData(link.point.getDimension());

        ArrayList<DimData> roots = PocketManager.getRootDimensions();
        int shiftChance = START_ROOT_SHIFT_CHANCE + ROOT_SHIFT_CHANCE_PER_LEVEL * (current.packDepth() - 1);

        if (random.nextInt(MAX_ROOT_SHIFT_CHANCE) < shiftChance) {
            if (current.root().id() != OVERWORLD_DIMENSION_ID && random.nextInt(MAX_OVERWORLD_EXIT_CHANCE) < OVERWORLD_EXIT_CHANCE) {
                return generateSafeExit(PocketManager.createDimensionDataDangerously(OVERWORLD_DIMENSION_ID), link, properties);
            } if (current.root().id() != NETHER_DIMENSION_ID && random.nextInt(MAX_NETHER_EXIT_CHANCE) < NETHER_EXIT_CHANCE) {
                return generateSafeExit(PocketManager.createDimensionDataDangerously(NETHER_DIMENSION_ID), link, properties);
            }

            for (int attempts = 0; attempts < 10; attempts++) {
                DimData selection = roots.get( random.nextInt(roots.size()) );
                if (selection != current.root() && isValidForDungeonExit(selection, properties)) {
                    return generateSafeExit(selection, link, properties);
                }
            }
        }

        // Yes, this could lead you back into Limbo. That's intentional.
        return generateSafeExit(current.root(), link, properties);
    }

    private static boolean isValidForDungeonExit(DimData destination, DDProperties properties)
    {
        // Prevent exits to The End and Limbo
        if (destination.id() == END_DIMENSION_ID || destination.id() == properties.LimboDimensionID) {
            return false;
        }

        // Prevent exits to Witchery's Spirit World; we need to load the dimension to retrieve its name.
        // This is okay because the dimension would have to be loaded subsequently by generateSafeExit().
        World world = PocketManager.loadDimension(destination.id());
        return (world != null && !SPIRIT_WORLD_NAME.equals(world.provider.getDimensionName()));
    }

    private static boolean generateSafeExit(DimData destinationDim, DimLink link, DDProperties properties) {
        // A safe exit attempts to place a Warp Door in a dimension with
        // some precautions to protect the player. The X and Z coordinates
        // are fixed to match the source (mostly - may be shifted a little),
        // but the Y coordinate is chosen by searching for the nearest
        // a safe location to place the door.

        Point4D source = link.source();
        World world = PocketManager.loadDimension(destinationDim.id());
        if (world == null) {
            return false;
        }

        int startY = source.getY() - 2;
        BlockPos destination;
        BlockPos locationUp = yCoordHelper.findSafeCubeUp(world, source.getX(), startY, source.getZ());
        BlockPos locationDown = yCoordHelper.findSafeCubeDown(world, source.getX(), startY, source.getZ());

        if (locationUp == null) {
            destination = locationDown;
        } else if (locationDown == null) {
            destination = locationUp;
        } else if (locationUp.getY() - startY <= startY - locationDown.getY()) {
            destination = locationUp;
        } else {
            destination = locationDown;
        }

        if (destination != null) {
            // Set up a 3x3 platform at the destination
            // Only place fabric of reality if the block is replaceable or air
            // Don't cause block updates
            BlockPos pos = destination;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // Checking if the block is not an opaque solid is equivalent
                    // checking for a replaceable block, because we only allow
                    // exits intersecting blocks on those two surfaces.
                    if (!world.isBlockNormalCube(pos.add(dx, 0, dz), false)) {
                        world.setBlockState(pos.add(dx, 0, dz), DimDoors.blockDimWall.getDefaultState());
                    }
                }
            }

            // Clear out any blocks in the space above the platform layer
            // This removes any potential threats like replaceable Poison Ivy from BoP
            // Remember to avoid block updates to keep gravel from collapsing
            for (int dy = 1; dy <= 2; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        world.setBlockState(pos.add(dx, dy, dz), Blocks.air.getDefaultState());
                    }
                }
            }

            // Create a reverse link for returning
            EnumFacing orientation = getDestinationOrientation(source, properties);
            DimData sourceDim = PocketManager.getDimensionData(link.source().getDimension());
            DimLink reverse = destinationDim.createLink(pos.up(2), LinkType.REVERSE,orientation);

            sourceDim.setLinkDestination(reverse, source.toBlockPos());

            // Set up the warp door at the destination
            orientation = EnumFacingHelper.getFacingFromBlockState(BlockRotator.transform(DimDoors.warpDoor.getDefaultState(), 2));
            ItemDoor.placeDoor(world, pos.up(), orientation, DimDoors.warpDoor);

            // Complete the link to the destination
            // This comes last so the destination isn't set unless everything else works first
            destinationDim.setLinkDestination(link, pos.up(2));
        }

        return (destination != null);
    }
}
