package org.dimdev.dimdoors.shared.pockets;

import org.dimdev.ddutils.nbt.INBTStorable;
import org.dimdev.dimdoors.shared.VirtualLocation;
import org.dimdev.dimdoors.shared.rifts.*;
import org.dimdev.dimdoors.shared.tileentities.TileEntityEntranceRift;
import org.dimdev.ddutils.Location;

import java.util.*;

import org.dimdev.ddutils.math.MathUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class Pocket implements INBTStorable{ // TODO: better visibilities

    @Getter private int id;
    @Getter private int x; // Grid x TODO: rename to gridX and gridY
    @Getter private int z; // Grid y
    @Getter @Setter private int size; // In chunks TODO: non chunk-based size, better bounds such as minX, minZ, maxX, maxZ, etc.
    @Getter @Setter private VirtualLocation virtualLocation; // The non-pocket dimension from which this dungeon was created
    @Getter @Setter Location entrance;
    @Getter List<Location> riftLocations;

    @Getter int dimID; // Not saved

    public Pocket() {}

    public Pocket(int id, int dimID, int x, int z) {
        this.id = id;
        this.dimID = dimID;
        this.x = x;
        this.z = z;
        riftLocations = new ArrayList<>();
    }

    @Override
    public /*static Pocket*/ void readFromNBT(NBTTagCompound nbt) {
        id = nbt.getInteger("id");
        x = nbt.getInteger("x");
        z = nbt.getInteger("z");
        size = nbt.getInteger("size");
        if (nbt.hasKey("virtualLocation")) virtualLocation = VirtualLocation.readFromNBT(nbt.getCompoundTag("virtualLocation"));
        if (nbt.hasKey("entrance")) entrance = Location.readFromNBT(nbt.getCompoundTag("entrance"));

        riftLocations = new ArrayList<>();
        NBTTagList riftLocationsNBT = (NBTTagList) nbt.getTag("riftLocations");
        for (NBTBase riftLocationNBT : riftLocationsNBT) {
            riftLocations.add(Location.readFromNBT((NBTTagCompound) riftLocationNBT));
        }
    }

    @Override
    public /*static*/ NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("id", id);
        nbt.setInteger("x", x);
        nbt.setInteger("z", z);
        nbt.setInteger("size", size);
        if (virtualLocation != null) nbt.setTag("virtualLocation", virtualLocation.writeToNBT());
        if (entrance != null) nbt.setTag("entrance", Location.writeToNBT(entrance));

        NBTTagList riftLocationsNBT = new NBTTagList();
        for (Location loc : riftLocations) {
            riftLocationsNBT.appendTag(Location.writeToNBT(loc));
        }
        nbt.setTag("riftLocations", riftLocationsNBT);

        return nbt;
    }

    boolean isInBounds(BlockPos pos) {
        // pocket bounds
        int gridSize = PocketRegistry.getForDim(dimID).getGridSize();
        int pocMinX = x * gridSize;
        int pocMinZ = z * gridSize;
        int pocMaxX = pocMinX + (size + 1) * 16;
        int pocMaxZ = pocMinX + (size + 1) * 16;
        return pocMinX <= pos.getX() && pocMinZ <= pos.getZ() && pos.getX() < pocMaxX && pos.getZ() < pocMaxZ;
    }

    public List<TileEntityRift> getRifts() {
        List<TileEntityRift> rifts = new ArrayList<>(riftLocations.size()); // TODO: make immutable
        for (Location riftLocation : riftLocations) {
            TileEntity tileEntity = riftLocation.getWorld().getTileEntity(riftLocation.getPos());
            if (tileEntity instanceof TileEntityRift) {
                rifts.add((TileEntityRift) tileEntity);
            } else {
                throw new RuntimeException("The rift locations array was wrong, check the schematic placing function to see why!");
            }
        }
        return rifts;
    }

    public void setup() { // Always call after creating a pocket except when building the pocket
        List<TileEntityRift> rifts = getRifts();

        HashMap<Integer, Float> entranceIndexWeights = new HashMap<>();

        int index = 0;
        for (TileEntityRift rift : rifts) { // Find an entrance
            for (WeightedRiftDestination weightedPocketEntranceDest : rift.getDestinations()) {
                if (weightedPocketEntranceDest.getDestination() instanceof PocketEntranceDestination) {
                    entranceIndexWeights.put(index, weightedPocketEntranceDest.getWeight());
                    rift.markDirty();
                    index++;
                }
            }
        }
        if (entranceIndexWeights.size() == 0) return;
        int selectedEntranceIndex = MathUtils.weightedRandom(entranceIndexWeights);

        // Replace entrances with appropriate destinations
        index = 0;
        for (TileEntityRift rift : rifts) {
            ListIterator<WeightedRiftDestination> destIterator = rift.getDestinations().listIterator();
            while (destIterator.hasNext()) {
                WeightedRiftDestination wdest = destIterator.next();
                RiftDestination dest = wdest.getDestination();
                if (dest instanceof PocketEntranceDestination) {
                    destIterator.remove();
                    if (index == selectedEntranceIndex) {
                        entrance = new Location(rift.getWorld(), rift.getPos());
                        PocketRegistry.getForDim(dimID).markDirty();
                        List<WeightedRiftDestination> ifDestinations = ((PocketEntranceDestination) dest).getIfDestinations();
                        for (WeightedRiftDestination ifDestination : ifDestinations) {
                            destIterator.add(new WeightedRiftDestination(ifDestination.getDestination(), ifDestination.getWeight() / wdest.getWeight(), ifDestination.getGroup()));
                            destIterator.previous(); // An entrance destination shouldn't be in an if/otherwise destination, but just in case, pass over it too
                        }
                    } else {
                        List<WeightedRiftDestination> otherwiseDestinations = ((PocketEntranceDestination) dest).getOtherwiseDestinations();
                        for (WeightedRiftDestination otherwiseDestination : otherwiseDestinations) {
                            destIterator.add(new WeightedRiftDestination(otherwiseDestination.getDestination(), otherwiseDestination.getWeight() / wdest.getWeight(), otherwiseDestination.getGroup()));
                            destIterator.previous(); // An entrance destination shouldn't be in an if/otherwise destination, but just in case, pass over it too
                        }
                    }
                    index++;
                }
            }
        }

        // set virtual locations and register rifts
        for (TileEntityRift rift : rifts) {
            rift.setVirtualLocation(virtualLocation);
            rift.register();
        }
    }

    public void linkPocketTo(RiftDestination linkTo) {
        List<TileEntityRift> rifts = getRifts();

        // Link pocket exits back
        for (TileEntityRift rift : rifts) {
            ListIterator<WeightedRiftDestination> destIterator = rift.getDestinations().listIterator();
            while (destIterator.hasNext()) {
                WeightedRiftDestination wdest = destIterator.next();
                RiftDestination dest = wdest.getDestination();
                if (dest instanceof PocketExitDestination) {
                    destIterator.remove();
                    if (rift.isRegistered()) dest.unregister(rift);
                    destIterator.add(new WeightedRiftDestination(linkTo, wdest.getWeight(), wdest.getGroup(), dest));
                    if (rift.isRegistered()) linkTo.register(rift);
                    if (rift instanceof TileEntityEntranceRift && !rift.isAlwaysDelete()) {
                        ((TileEntityEntranceRift) rift).setPlaceRiftOnBreak(true); // We modified the door's state
                    }
                    rift.markDirty();
                }
            }
        }
    }

    public void unlinkPocket() {
        // TODO
    }

    public BlockPos getOrigin() {
        int gridSize = PocketRegistry.getForDim(dimID).getGridSize();
        return new BlockPos(x * gridSize, 0, z * gridSize); // TODO: configurable yBase?
    }

    // TODO: method to erase a pocket
}