package org.dimdev.dimdoors.shared.tools;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.dimdev.ddutils.schem.Schematic;
import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.shared.blocks.ModBlocks;
import org.dimdev.dimdoors.shared.entities.EntityMonolith;
import org.dimdev.dimdoors.shared.rifts.destinations.AvailableLinkDestination;
import org.dimdev.dimdoors.shared.rifts.destinations.PocketEntranceDestination;
import org.dimdev.dimdoors.shared.rifts.destinations.PocketExitDestination;
import org.dimdev.dimdoors.shared.rifts.registry.LinkProperties;
import org.dimdev.dimdoors.shared.tileentities.TileEntityEntranceRift;

import java.util.*;

/**
 * @author Robijnvogel
 */
public final class SchematicConverter {

    public static Schematic convertSchematic(NBTTagCompound nbt, String schematicId, String name, String author) {
        Schematic schematic = new Schematic();

        schematic.version = 1; //already the default value
        schematic.author = author;
        schematic.name = name;
        schematic.creationDate = System.currentTimeMillis();
        schematic.requiredMods = new String[]{DimDoors.MODID};

        schematic.width = nbt.getShort("Width");
        schematic.height = nbt.getShort("Height");
        schematic.length = nbt.getShort("Length");
        schematic.offset = new int[]{0, 0, 0};

        // Schematic info
        int ironDoors = 0;
        int woodDoors = 0;
        int sandstoneDoors = 0;
        int monoliths = 0;
        int chests = 0;

        byte[] blockIdArray = nbt.getByteArray("Blocks");
        byte[] addId = nbt.getByteArray("AddBlocks");
        Map<Integer, Byte> palletteMap = new HashMap<>(); // block ID -> pallette index
        byte currentPalletteIndex = 0;
        for (int i = 0; i < blockIdArray.length; i++) {
            int id;
            if (i >> 1 >= addId.length) {
                id = (short) (blockIdArray[i] & 0xFF);
            } else if ((i & 1) == 0) {
                id = (short) (((addId[i >> 1] & 0x0F) << 8) + (blockIdArray[i] & 0xFF));
            } else {
                id = (short) (((addId[i >> 1] & 0xF0) << 4) + (blockIdArray[i] & 0xFF));
            }
            if (palletteMap.containsKey(id)) {
                blockIdArray[i] = palletteMap.get(id);
            } else {
                IBlockState block = id <= 159 ? Block.getBlockById(id).getDefaultState() : Blocks.AIR.getDefaultState();
                switch (id) {
                    case 1973:
                        block = ModBlocks.FABRIC.getDefaultState();
                        break;
                    case 1975:
                        block = ModBlocks.WARP_DIMENSIONAL_DOOR.getDefaultState();
                        break;
                    case 1970:
                        block = ModBlocks.DIMENSIONAL_DOOR.getDefaultState();
                        break;
                    case 1979:
                        block = ModBlocks.DIMENSIONAL_PORTAL.getDefaultState();
                        break;
                    case 220:
                        block = ModBlocks.ANCIENT_FABRIC.getDefaultState();
                        break;
                    case 95: // Locked chest's ID was replaced with stained glass in 1.7.2
                        DimDoors.log.error("Schematic contained a locked chest, which was removed in 1.7.2.");
                        block = Blocks.AIR.getDefaultState();
                        break;
                }
                if (id != 0 && block.getBlock().getRegistryName().toString().equals("minecraft:air")) {
                    throw new RuntimeException("Unknown ID " + id + " in schematic " + schematicId);
                }
                if (block.equals(Blocks.IRON_DOOR)) block = ModBlocks.DIMENSIONAL_DOOR.getDefaultState();
                if (block.equals(Blocks.OAK_DOOR)) block = ModBlocks.WARP_DIMENSIONAL_DOOR.getDefaultState();
                schematic.pallette.add(block);
                palletteMap.put(id, currentPalletteIndex);
                blockIdArray[i] = currentPalletteIndex;
                currentPalletteIndex++;
            }
        }

        NBTTagList tileEntitiesNBT = (NBTTagList) nbt.getTag("TileEntities");
        for (int i = 0; i < tileEntitiesNBT.tagCount(); i++) {
            NBTTagCompound tileEntityNBT = tileEntitiesNBT.getCompoundTagAt(i);
            switch (tileEntityNBT.getString("id")) {
                case "TileEntityDimDoor":
                case "TileEntityRift":
                    continue;
                case "Sign":
                    tileEntityNBT.setString("Text1", ITextComponent.Serializer.componentToJson(new TextComponentString(tileEntityNBT.getString("Text1"))));
                    tileEntityNBT.setString("Text2", ITextComponent.Serializer.componentToJson(new TextComponentString(tileEntityNBT.getString("Text2"))));
                    tileEntityNBT.setString("Text3", ITextComponent.Serializer.componentToJson(new TextComponentString(tileEntityNBT.getString("Text3"))));
                    tileEntityNBT.setString("Text4", ITextComponent.Serializer.componentToJson(new TextComponentString(tileEntityNBT.getString("Text4"))));
                    break;
                default:
                    break;
            }
            tileEntityNBT.setString("id", translateId(tileEntityNBT.getString("id")).toString());
            schematic.tileEntities.add(tileEntityNBT);
        }

        byte[] dataIntArray = nbt.getByteArray("Data");
        schematic.blockData = new int[schematic.width][schematic.height][schematic.length];
        for (int x = 0; x < schematic.width; x++) {
            for (int y = 0; y < schematic.height; y++) {
                for (int z = 0; z < schematic.length; z++) {
                    int blockInt = blockIdArray[x + z * schematic.width + y * schematic.width * schematic.length]; //according to the documentation on https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-1.md
                    int metadata = dataIntArray[x + z * schematic.width + y * schematic.width * schematic.length]; //according to the documentation on https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-1.md

                    IBlockState baseState = schematic.pallette.get(blockInt); //this is the default blockstate except for ancient fabric
                    if (baseState == baseState.getBlock().getDefaultState() || baseState.getBlock().equals(ModBlocks.FABRIC) || baseState.getBlock().equals(ModBlocks.ANCIENT_FABRIC)) { //should only be false if {@code baseState} is ancient fabric
                        IBlockState blockState;
                        if (baseState.getBlock().equals(ModBlocks.FABRIC) || baseState.getBlock().equals(ModBlocks.ANCIENT_FABRIC)) {
                            blockState = baseState;
                        } else {
                            blockState = baseState.getBlock().getStateFromMeta(metadata);
                        }
                        if (schematic.pallette.contains(blockState)) { //check whether or not this blockstate is already in the list
                            blockInt = schematic.pallette.indexOf(blockState);
                        } else {
                            schematic.pallette.add(blockState);
                            //DimDoors.log.info("New blockstate detected. Original blockInt = " + blockInt + " and blockState is " + blockState);
                            blockInt = schematic.pallette.size() - 1;
                        }

                        if (blockState.getBlock().equals(Blocks.CHEST)) chests++;

                        if (blockState.getBlock().equals(ModBlocks.DIMENSIONAL_DOOR) || blockState.getBlock().equals(ModBlocks.WARP_DIMENSIONAL_DOOR) || blockState.getBlock().equals(ModBlocks.DIMENSIONAL_PORTAL)) {
                            //DimDoors.log.info("Door found: " + blockState.getBlock().getUnlocalizedName());
                            if (blockState.getProperties().get(BlockDoor.HALF).equals(BlockDoor.EnumDoorHalf.LOWER)) {
                                TileEntityEntranceRift rift = (TileEntityEntranceRift) blockState.getBlock().createTileEntity(null, blockState);
                                rift.setPos(new BlockPos(x, y, z));

                                rift.setProperties(LinkProperties.builder()
                                        .groups(new HashSet<>(Arrays.asList(0, 1)))
                                        .linksRemaining(1).build());

                                if (blockState.getBlock().equals(ModBlocks.DIMENSIONAL_DOOR)) {
                                    ironDoors++;
                                    rift.setDestination(AvailableLinkDestination.builder()
                                            .acceptedGroups(Collections.singleton(0))
                                            .coordFactor(1)
                                            .negativeDepthFactor(10000)
                                            .positiveDepthFactor(80)
                                            .weightMaximum(100)
                                            .newRiftWeight(1).build());
                                } else if (blockState.getBlock().equals(ModBlocks.WARP_DIMENSIONAL_DOOR)) {
                                    IBlockState stateBelow = schematic.pallette.get(schematic.blockData[x][y - 1][z]);
                                    if (stateBelow.getBlock().equals(Blocks.SANDSTONE)) {
                                        sandstoneDoors++;
                                        rift.setProperties(null); // TODO: this should be removed once the linking equations are made symmetric
                                        rift.setDestination(AvailableLinkDestination.builder()
                                                .acceptedGroups(Collections.singleton(0))
                                                .coordFactor(1)
                                                .negativeDepthFactor(0.00000000001) // The division result is cast to an int, so Double.MIN_VALUE would cause an overflow
                                                .positiveDepthFactor(Double.POSITIVE_INFINITY)
                                                .weightMaximum(100)
                                                .newRiftWeight(1).build());
                                        //change the sandstone to the block below it.
                                        if (y >= 2) {
                                            schematic.blockData[x][y - 1][z] = schematic.blockData[x][y - 2][z];
                                        } else {
                                            DimDoors.log.error("Someone placed a door on a sandstone block at the bottom of a schematic. This causes problems and should be remedied. Schematic name: " + schematicId);
                                        }
                                    } else {
                                        woodDoors++;
                                        rift.setDestination(PocketEntranceDestination.builder()
                                                .weight(1)
                                                .ifDestination(PocketExitDestination.builder().build())
                                                .otherwiseDestination(AvailableLinkDestination.builder()
                                                        .acceptedGroups(Collections.singleton(0))
                                                        .coordFactor(1)
                                                        .negativeDepthFactor(80)
                                                        .positiveDepthFactor(10000)
                                                        .weightMaximum(100)
                                                        .newRiftWeight(1).build()).build());
                                    }
                                } else if (blockState.getBlock().equals(ModBlocks.DIMENSIONAL_PORTAL)) {
                                    rift.setProperties(LinkProperties.builder()
                                            .groups(new HashSet<>(Arrays.asList(0, 1)))
                                            .entranceWeight(50)
                                            .linksRemaining(1).build());
                                    rift.setDestination(AvailableLinkDestination.builder()
                                            .acceptedGroups(Collections.singleton(0))
                                            .coordFactor(1) // TODO: lower value?
                                            .negativeDepthFactor(Double.POSITIVE_INFINITY)
                                            .positiveDepthFactor(80) // TODO: lower value?
                                            .weightMaximum(300) // Link further away
                                            .newRiftWeight(1)
                                            .build());
                                }

                                schematic.tileEntities.add(rift.serializeNBT());
                            }
                        }

                        if (blockState.getBlock().equals(Blocks.END_PORTAL_FRAME)) {
                            monoliths++;
                            // I think it's safe to assume that air is present
                            blockInt = schematic.pallette.indexOf(Blocks.AIR.getDefaultState());
                            EntityMonolith monolith = new EntityMonolith(null);
                            EnumFacing facing = blockState.getValue(BlockEndPortalFrame.FACING);
                            monolith.setLocationAndAngles(x + 0.5d, y, z + 0.5d, facing.getHorizontalAngle(), 0);
                            schematic.entities.add(monolith.serializeNBT());
                        }
                    } else { // if this is ancient fabric
                        blockInt = schematic.pallette.indexOf(baseState);
                    }
                    assert blockInt >= 0;
                    schematic.blockData[x][y][z] = blockInt;
                }
            }
        }
        if (!nbt.getTag("Entities").hasNoTags())
            throw new RuntimeException("Schematic contains entities, but those aren't implemented in the conversion code");
        schematic.paletteMax = schematic.pallette.size() - 1;

        DimDoors.log.info(schematicId + "," + schematic.name + "," + ironDoors + "," + woodDoors + "," + sandstoneDoors + "," + monoliths + "," + chests);

        return schematic;
    }

    private static ResourceLocation translateId(String id) { // TODO
        ResourceLocation location;
        switch (id) {
            case "Sign":
                location = TileEntity.getKey(TileEntitySign.class);
                break;
            case "Music":
                location = TileEntity.getKey(TileEntityNote.class);
                break;
            case "Trap":
                location = TileEntity.getKey(TileEntityDispenser.class);
                break;
            case "Comparator":
                location = TileEntity.getKey(TileEntityComparator.class);
                break;
            case "Hopper":
                location = TileEntity.getKey(TileEntityHopper.class);
                break;
            case "Furnace":
                location = TileEntity.getKey(TileEntityFurnace.class);
                break;
            case "Chest":
                location = TileEntity.getKey(TileEntityChest.class);
                break;
            default:
                throw new RuntimeException("Tile entity ID " + id + " not supported by conversion code");
        }
        if (location == null) {
            DimDoors.log.error("Tile entity ID " + id + " is returning a null resource location.");
            location = new ResourceLocation(id.toLowerCase());
        }        
        return location;
    }
}
