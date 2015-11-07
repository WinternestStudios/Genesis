package genesis.block.tileentity.portal;

import java.util.List;

import genesis.block.tileentity.TileEntityBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class TileEntityGenesisPortal extends TileEntityBase implements IUpdatePlayerListBox
{
	protected double radius = 5 / 2.0;
	protected byte timer = 0;
	
	//Cached
	protected Vec3 center = null;
	protected AxisAlignedBB bounds = null;
	
	public TileEntityGenesisPortal()
	{
	}
	
	public void setPos(BlockPos pos)
	{
		super.setPos(pos);
		
		center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		bounds = new AxisAlignedBB(center.xCoord - radius, center.yCoord - radius, center.zCoord - radius,
									center.xCoord + radius, center.yCoord + radius, center.zCoord + radius);
	}
	
	@Override
	public void update()
	{
		/*if (!worldObj.isRemote)
		{
			timer--;
			
			if (timer <= 0)
			{
				GenesisPortal.fromPortalBlock(worldObj, pos).updatePortalStatus(worldObj);
				timer = GenesisPortal.PORTAL_CHECK_TIME;
			}
		}*/
		
		List<EntityLivingBase> entities = (List<EntityLivingBase>) worldObj.getEntitiesWithinAABB(EntityLivingBase.class, bounds);
		
		for (EntityLivingBase entity : entities)
		{
			EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
			
			if (player != null && player.capabilities.isFlying)
			{
				continue;
			}
			
			double diffX = entity.posX - center.xCoord;
			double diffY = entity.posY + (entity.getEyeHeight() / 2) - center.yCoord;
			double diffZ = entity.posZ - center.zCoord;
			double distance = diffX * diffX + diffY * diffY + diffZ * diffZ;
			
			if (distance < radius * radius)
			{
				distance = MathHelper.sqrt_double(distance);
				double speed = -(distance / radius) * 0.05;
				
				if (player != null && player.isSneaking())
				{
					speed *= 0.5;
				}
				
				entity.motionX += diffX * speed;
				entity.motionY += diffY * speed;
				entity.motionZ += diffZ * speed;
			}
		}
	}
	
	public void writeToNBT(NBTTagCompound compound)
	{
		super.writeToNBT(compound);
		
		compound.setDouble("radius", radius);
		//compound.setByte("timer", timer);
	}
	
	public void readFromNBT(NBTTagCompound compound)
	{
		super.readFromNBT(compound);

		radius = compound.getDouble("radius");
		//timer = compound.getByte("timer");
	}
	
	@Override
	protected void writeVisualData(NBTTagCompound compound, boolean save)
	{
	}
	
	@Override
	protected void readVisualData(NBTTagCompound compound, boolean save)
	{
	}
}