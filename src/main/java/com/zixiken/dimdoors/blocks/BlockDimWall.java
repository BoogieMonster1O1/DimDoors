package com.zixiken.dimdoors.blocks;

import java.util.List;
import java.util.Random;

import com.zixiken.dimdoors.DimDoors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import com.zixiken.dimdoors.client.PrivatePocketRender;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDimWall extends Block {
    public static final String ID = "blockDimWall";
    public static final PropertyInteger TYPE = PropertyInteger.create("type", 0, 2);

	private static final float SUPER_HIGH_HARDNESS = 10000000000000F;
	private static final float SUPER_EXPLOSION_RESISTANCE = 18000000F;
	
	public BlockDimWall() {
		super(Material.iron);
		this.setCreativeTab(DimDoors.dimDoorsCreativeTab);
		setLightLevel(1.0F);
        setHardness(0.1F);
        setUnlocalizedName(ID);
        setDefaultState(blockState.getBaseState().withProperty(TYPE, 0));
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
        if(meta >= 0 && meta <= 2) return getDefaultState().withProperty(TYPE, meta);
        else return getDefaultState();
    }

	@Override
	public int getMetaFromState(IBlockState state) {return state.getValue(TYPE);}

	@Override
	protected BlockState createBlockState() {return new BlockState(this, TYPE);}

	@Override
	public float getBlockHardness(World world, BlockPos pos) {
		if (world.getBlockState(pos).getValue(TYPE) != 1) return this.blockHardness;
		else return SUPER_HIGH_HARDNESS;
	}

	@Override
	public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
		if(world.getBlockState(pos).getValue(TYPE) != 1)
			return super.getExplosionResistance(world, pos, exploder, explosion);
        else return SUPER_EXPLOSION_RESISTANCE;
	}
	
	public int getRenderType() {
        return PrivatePocketRender.renderID;
    }
	
	@Override
	public int damageDropped(IBlockState state) {
        int metadata = state.getValue(TYPE);
		//Return 0 to avoid dropping Ancient Fabric even if the player somehow manages to break it
		return metadata == 1 ? 0 : metadata;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
		for (int ix = 0; ix < 3; ix++)
			subItems.add(new ItemStack(itemIn, 1, ix));
	}
    
    @Override
	public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player)
    {
        return true;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(Item.getItemFromBlock(this), 1, getMetaFromState(world.getBlockState(pos)));
    }

    @Override
	public int quantityDropped(Random par1Random)
    {
        return 0;
    }

    /**
     * replaces the block clicked with the held block, instead of placing the block on top of it. Shift click to disable. 
     */
    @Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
    	//Check if the metadata value is 0 -- we don't want the user to replace Ancient Fabric
        ItemStack playerEquip = player.getCurrentEquippedItem();
        if (playerEquip != null && state.getValue(TYPE) != 1) {
            Block block = Block.getBlockFromItem(playerEquip.getItem());
        	if (block != null) {
        		if (!block.isNormalCube(world, pos) || block instanceof BlockContainer || block == this)
        			return false;
        		if (!world.isRemote) {
            		if (!player.capabilities.isCreativeMode) playerEquip.stackSize--;
            		world.setBlockState(pos, block.getStateFromMeta(playerEquip.getItemDamage()));
        		}
        		return true;
        	}
        }
        return false;
    }
}