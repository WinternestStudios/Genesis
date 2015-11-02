package genesis.block.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public abstract class TileEntityBase extends TileEntity
{
	public void sendDescriptionPacket()
	{
		if (worldObj != null)
		{
			worldObj.markBlockForUpdate(pos);
		}
	}
	
	protected abstract void writeVisualData(NBTTagCompound compound, boolean save);
	
	protected abstract void readVisualData(NBTTagCompound compound, boolean save);
	
	@Override
	public S35PacketUpdateTileEntity getDescriptionPacket()
	{
		NBTTagCompound compound = new NBTTagCompound();
		writeVisualData(compound, false);
		return new S35PacketUpdateTileEntity(pos, 0, compound);
	}
	
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet)
	{
		readVisualData(packet.getNbtCompound(), false);
	}

	@Override
	public void writeToNBT(NBTTagCompound compound)
	{
		super.writeToNBT(compound);
		
		writeVisualData(compound, true);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		super.readFromNBT(compound);
		
		readVisualData(compound, true);
	}
	
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
	{
		return !oldState.equals(newState);
	}
}
