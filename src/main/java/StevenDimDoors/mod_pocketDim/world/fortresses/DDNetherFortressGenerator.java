package StevenDimDoors.mod_pocketDim.world.fortresses;

import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureStart;

public class DDNetherFortressGenerator extends MapGenNetherBridge
{
    public DDNetherFortressGenerator()
    {
    	super();
    	
    	// Register our custom StructureStart class with MapGenStructureIO
    	// If we don't do this, Minecraft will crash when a fortress tries to generate.
    	// Moreover, use Fortress as our structure identifier so that if DD is removed,
    	// fortresses will generate properly using Vanilla code.
    	MapGenStructureIO.func_143034_b(DDStructureNetherBridgeStart.class, "Fortress");
    }

    protected StructureStart getStructureStart(int chunkX, int chunkZ)
    {
        return new DDStructureNetherBridgeStart(this.worldObj, this.rand, chunkX, chunkZ);
    }
}