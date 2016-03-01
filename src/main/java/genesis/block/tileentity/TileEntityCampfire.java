package genesis.block.tileentity;

import java.util.*;

import com.google.common.collect.ImmutableList;

import genesis.block.tileentity.crafting.CookingPotRecipeRegistry;
import genesis.block.tileentity.crafting.CookingPotRecipeRegistry.InventoryCookingPot;
import genesis.block.tileentity.gui.ContainerCampfire;
import genesis.block.tileentity.render.TileEntityCampfireRenderer;
import genesis.util.*;
import genesis.util.Constants.Unlocalized;
import genesis.util.gui.RestrictedDisabledSlot.IInventoryDisabledSlots;
import genesis.util.random.FloatRange;
import net.minecraft.block.*;
import net.minecraft.block.state.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.play.server.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraftforge.fluids.*;

public class TileEntityCampfire extends TileEntityLockable implements ISidedInventory, IInventoryDisabledSlots, InventoryCookingPot, ITickable
{
	public static int getItemBurnTime(ItemStack stack)
	{
		int time = TileEntityFurnace.getItemBurnTime(stack);
		
		// Exclude lava.
		FluidStack lava = FluidContainerRegistry.getFluidForFilledItem(new ItemStack(Items.lava_bucket));
		
		if (FluidContainerRegistry.containsFluid(stack, lava))
		{
			time = 0;
		}
		
		return (int)(time * 1.125F);
	}
	
	public static boolean isItemFuel(ItemStack stack)
	{
		return getItemBurnTime(stack) > 0;
	}
	
	public static boolean isCookingPot(ItemStack stack)
	{
		return CookingPotRecipeRegistry.isCookingPotItem(stack);
	}
	
	private static final Set<ItemStackKey> allowedOutputs = new HashSet<ItemStackKey>();
	
	/**
	 * Used to allow outputs that aren't foods to be created using the campfire.
	 */
	public static ItemStack registerAllowedOutput(ItemStack output)
	{
		allowedOutputs.add(new ItemStackKey(output));
		return output;
	}
	
	public static boolean isAllowedOutput(ItemStack output)
	{
		if (output == null)
		{
			return false;
		}
		
		return allowedOutputs.contains(new ItemStackKey(output));
	}
	
	public static final int SLOT_INPUT = 0;
	public static final int SLOT_FUEL = SLOT_INPUT + 1;
	public static final int SLOT_OUTPUT = SLOT_FUEL + 1;
	public static final int SLOTS_INGREDIENTS_START = SLOT_OUTPUT + 1;
	public static final int SLOTS_INGREDIENTS_COUNT = 3;
	public static final int SLOTS_INGREDIENTS_END = SLOTS_INGREDIENTS_START + SLOTS_INGREDIENTS_COUNT - 1;
	public static final int SLOT_INPUT_WASTE = SLOTS_INGREDIENTS_END + 1;
	public static final int SLOT_COUNT = SLOT_INPUT_WASTE + 1;
	
	protected static final int WET_TIME = 200;
	protected static final int COOK_TIME = 200;
	
	protected final ItemStack[] inventory = new ItemStack[SLOT_COUNT];
	
	protected boolean wasBurning = false;
	protected boolean waterAround = false;
	
	public float prevRot = 0;
	public float rot = 0;
	public float rotVel = 0;
	
	public String customName;
	
	public int burnTime;
	public int totalBurnTime;
	public int cookTime;
	
	protected int fireSoundTime = 30;
	protected int fireSoundCounter;
	
	private final int[] SLOTS_TOP;
	private final int[] SLOTS_SIDE;
	private final int[] SLOTS_FRONT;
	private final int[] SLOTS_BOTTOM;

	private final SlotModifier input = SlotModifier.from(this, SLOT_INPUT);
	private final SlotModifier inputWaste = SlotModifier.from(this, SLOT_INPUT_WASTE);
	private final List<SlotModifier> ingredients;
	private final SlotModifier fuel = SlotModifier.from(this, SLOT_FUEL);
	private final SlotModifier output = SlotModifier.from(this, SLOT_OUTPUT);
	
	public TileEntityCampfire()
	{
		super();
		
		SLOTS_TOP = new int[]{SLOT_INPUT};
		SLOTS_FRONT = new int[]{SLOT_FUEL};
		SLOTS_BOTTOM = new int[]{SLOT_OUTPUT};
		SLOTS_SIDE = new int[SLOTS_INGREDIENTS_END - SLOTS_INGREDIENTS_START + 1];
		
		for (int i = 0; i < SLOTS_SIDE.length; i++)
			SLOTS_SIDE[i] = SLOTS_INGREDIENTS_START + i;
		
		ImmutableList.Builder<SlotModifier> builder = ImmutableList.builder();
		
		for (int i = 0; i < SLOTS_INGREDIENTS_COUNT; i++)
			builder.add(SlotModifier.from(this, SLOTS_INGREDIENTS_START + i));
		
		ingredients = builder.build();
	}
	
	@Override
	public BlockCampfire getBlockType()
	{
		return (BlockCampfire) super.getBlockType();
	}
	
	public boolean hasCookingPot()
	{
		return isCookingPot(getInput().getStack());
	}
	
	public boolean canSmeltItemType(ItemStack stack)
	{
		if (stack != null)
		{
			if (stack.getItemUseAction() == EnumAction.EAT)
			{
				return true;
			}
			
			if (Block.getBlockFromItem(stack.getItem()) == Blocks.cactus)
			{
				return true;
			}
			
			ItemStack smeltResult = FurnaceRecipes.instance().getSmeltingResult(stack);
			
			if (smeltResult != null)
			{
				if (isAllowedOutput(smeltResult))
				{
					return true;
				}
				
				if (smeltResult.getItemUseAction() == EnumAction.EAT)
				{
					return true;
				}
				
				if (smeltResult.getItem() == Items.coal)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	public boolean canOutputAccept(ItemStack stack)
	{
		if (stack == null)
		{
			return false;
		}
		
		ItemStack output = getOutput().getStack();
		
		if (output == null)
		{
			return true;
		}
		
		if (!output.isItemEqual(stack) || !ItemStack.areItemStackTagsEqual(output, stack))
		{
			return false;
		}
		
		int newOutputSize = output.stackSize + stack.stackSize;
		
		return (newOutputSize <= getInventoryStackLimit() && newOutputSize <= output.getMaxStackSize());
	}
	
	public boolean canSmelt()
	{
		ItemStack smeltingItem = getInput().getStack();
		
		if (!canSmeltItemType(smeltingItem) && !hasCookingPot())
		{
			return false;
		}
		
		if (CookingPotRecipeRegistry.hasRecipe(this))
		{
			return true;
		}
		
		return canOutputAccept(FurnaceRecipes.instance().getSmeltingResult(smeltingItem));
	}
	
	public void smeltItem()
	{
		if (canSmelt())
		{
			ItemStack smeltingItem = getInput().getStack();
			ItemStack outputItem = getOutput().getStack();
			
			if (CookingPotRecipeRegistry.hasRecipe(this))
			{
				CookingPotRecipeRegistry.getRecipe(this).craft(this);
			}
			else
			{
				ItemStack result = FurnaceRecipes.instance().getSmeltingResult(smeltingItem);
				
				if (outputItem == null)
				{
					outputItem = result.copy();
				}
				else if (outputItem.isItemEqual(result))
				{
					outputItem.stackSize += result.stackSize;
				}
				
				smeltingItem.stackSize--;
				
				getOutput().set(outputItem);
			}
			
			for (int i = 0; i < inventory.length; i++)
			{
				if (inventory[i] != null && inventory[i].stackSize <= 0)
				{
					inventory[i] = null;
				}
			}
		}
	}
	
	public boolean isBurning()
	{
		return this.burnTime > 0;
	}
	
	protected boolean isRainingOn()
	{
		return worldObj.getRainStrength(1) >= 0.9F && worldObj.canLightningStrike(pos.up());
	}
	
	public void setWaterAround(boolean water)
	{
		waterAround = water;
		
		sendDescriptionPacket();
	}
	
	public boolean isWaterAround()
	{
		return waterAround;
	}
	
	public boolean isWet()
	{
		return this.burnTime < 0;
	}
	
	public void setWet()
	{
		getBlockType().randomDisplayTick(worldObj, pos, worldObj.getBlockState(pos), worldObj.rand);
		
		if (isBurning())
		{
			worldObj.playAuxSFX(1004, pos, 0);
		}
		
		burnTime = Math.min(burnTime, -WET_TIME);
		updateBurningValue();
	}
	
	public boolean updateBurningValue()
	{
		boolean burning = isBurning();
		
		if (wasBurning != burning)
		{
			getBlockType().setBurning(worldObj, pos, burning);
			wasBurning = burning;
			return true;
		}
		
		return false;
	}
	
	public boolean burnFuelIfNotBurning()
	{
		if (!isBurning() && !isWet())
		{
			ItemStack burningItem = getFuel().getStack();
			totalBurnTime = burnTime = getItemBurnTime(burningItem);
			
			if (burnTime > 0 && burningItem != null)
			{
				ItemStack container = burningItem.getItem().getContainerItem(burningItem);
				burningItem = getFuel().consume(1);
				
				if (burningItem == null)
					getFuel().set(container);
				else
					WorldUtils.spawnItemsAt(worldObj, pos, WorldUtils.DropType.CONTAINER, container);
				
				updateBurningValue();
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void update()
	{
		// Get the block type for use in the update method.
		getBlockType();
		
		if (worldObj.isRemote && !TileEntityCampfireRenderer.hasCookingItemModel(getInput().getStack()))
		{
			final int fullAngle = 360;
			final int increments = 360;
			final float increment = fullAngle / (float) increments;
			prevRot = rot;
			
			boolean cookingPot = hasCookingPot();
			float newVel = rotVel;
			
			if (cookingPot)
			{
				float swungPos = MathHelper.sin(rot * (float) Math.PI / 180);
				newVel -= swungPos * 4;
				
				Random rand = worldObj.rand;
				
				if (wasBurning && rand.nextInt(20) == 0)
				{
					float swing = rand.nextFloat() * increment;
					
					if (newVel == 0)
					{
						newVel += rand.nextInt() == 0 ? swing : -swing;
					}
					else if (newVel > 0)
					{
						newVel += swing;
					}
					else
					{
						newVel -= swing;
					}
				}
			}
			else if (canSmelt())
			{
				float target = 0;
				
				if (wasBurning)
				{
					target = increment;
				}
				
				newVel += (target - newVel) * 0.5F;
			}
			
			newVel *= 0.9F;
			rot += newVel;
			rotVel = newVel;
			
			float sub = rot - (rot % fullAngle);
			
			if (sub > 0)
			{
				prevRot -= sub;
				rot -= sub;
			}
		}
		
		boolean updateBlock = false;
		
		if (isRainingOn() || isWaterAround())
		{
			// Stop burning if the fire is rained on.
			setWet();
		}
		else if (wasBurning)	// wasBurning (which is the value of isBurning() at the end of last tick) tells us whether burnTime > 0
		{
			burnTime--;
			// If the campfire is not burning, start an item burning.
			burnFuelIfNotBurning();
			
			if (worldObj.isRemote)
			{	// Play fire sound effect every 1.5 secs on client.
				if (fireSoundCounter > 0)
				{
					fireSoundCounter--;
				}
				else
				{
					FloatRange volumeRange = FloatRange.create(1, 2);
					FloatRange pitchRange = FloatRange.create(0.3F, 0.7F);
					worldObj.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, "fire.fire",
							volumeRange.get(worldObj.rand), pitchRange.get(worldObj.rand), false);
					
					if (hasCookingPot() && canSmelt())
					{
						worldObj.playSound(pos.getX() + 0.5, pos.getY() + 0.625, pos.getZ() + 0.5, Constants.ASSETS_PREFIX + "ambient.cookingpot",
								volumeRange.get(worldObj.rand), pitchRange.get(worldObj.rand), false);
					}
					
					fireSoundCounter = fireSoundTime;
				}
			}
		}
		else if (isWet())
		{
			burnTime++;
		}
		
		updateBlock = updateBlock || updateBurningValue();
		
		if (wasBurning && canSmelt())
		{
			cookTime++;
			
			if (cookTime >= COOK_TIME)
			{
				cookTime = 0;
				smeltItem();
				updateBlock = true;
			}
		}
		else
		{
			cookTime = 0;
		}
		
		if (updateBlock)
		{
			markDirty();
		}
	}
	
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
	{
		return oldState.getBlock() != newState.getBlock();
	}
	
	public int getBurnTimeLeftScaled(int pixels)
	{
		float total = totalBurnTime;
		
		if (isWet())
		{
			total = -WET_TIME;
		}
		
		return (int) Math.ceil((burnTime / total) * pixels);
	}
	
	public int getCookProgressScaled(int pixels)
	{
		return Math.round((cookTime / (float) COOK_TIME) * pixels);
	}
	
	/*@Override
	public void onInventoryChanged()
	{
		super.onInventoryChanged();
		
		if (worldObj != null)
		{
			worldObj.markBlockForUpdate(pos);
		}
	}*/
	
	@Override
	public int getSizeInventory()
	{
		return inventory.length;
	}
	
	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return inventory[slot];
	}
	
	public boolean isIngredientSlot(int slot)
	{
		return slot >= SLOTS_INGREDIENTS_START && slot <= SLOTS_INGREDIENTS_END;
	}
	
	@Override
	public boolean isSlotDisabled(int index)
	{
		if (isIngredientSlot(index))
		{
			return hasCookingPot();
		}
		
		return false;
	}
	
	@Override
	public void clear()
	{
		for (int i = 0; i < inventory.length; ++i)
		{
			inventory[i] = null;
		}
	}
	
	@Override
	public void setInventorySlotContents(int slot, ItemStack stack)
	{
		inventory[slot] = stack;
		sendDescriptionPacket();
	}
	
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack)
	{
		if (stack == null)
			return true;
		
		switch (slot)
		{
		case SLOT_INPUT:
			if (canSmeltItemType(stack))
			{
				return true;
			}
			
			if (isCookingPot(stack))
			{
				return true;
			}
			
			return false;
		case SLOT_FUEL:
			return TileEntityFurnace.isItemFuel(stack);
		default:
			break;
		}
		
		if (isIngredientSlot(slot))
			return hasCookingPot() && CookingPotRecipeRegistry.isRecipeIngredient(stack, this);
		
		return false;
	}
	
	@Override
	public ItemStack removeStackFromSlot(int slot)
	{
		ItemStack stack = getStackInSlot(slot);
		
		if (stack != null)
		{
			setInventorySlotContents(slot, null);
		}
		
		return stack;
	}
	
	@Override
	public ItemStack decrStackSize(int slot, int amount)
	{
		ItemStack stack = getStackInSlot(slot);
		
		if (stack != null)
		{
			if (stack.stackSize <= amount)
			{
				setInventorySlotContents(slot, null);
			}
			else
			{
				stack = stack.splitStack(amount);
				
				if (stack.stackSize <= 0)
				{
					setInventorySlotContents(slot, null);
				}
			}
		}
		
		return stack;
	}
	
	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}
	
	public void setCustomInventoryName(String name)
	{
		customName = name;
	}
	
	@Override
	public boolean hasCustomName()
	{
		return customName != null && customName.length() > 0;
	}
	
	@Override
	public String getName()
	{
		return hasCustomName() ? customName : Unlocalized.CONTAINER_UI + "campfire";
	}
	
	protected void sendDescriptionPacket()
	{
		worldObj.markBlockForUpdate(pos);
	}
	
	@Override
	public S35PacketUpdateTileEntity getDescriptionPacket()
	{
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("burnTime", burnTime);
		compound.setBoolean("waterAround", waterAround);
		
		ItemStack input = getInput().getStack();
		
		if (input != null)
		{
			compound.setTag("input", input.writeToNBT(new NBTTagCompound()));
		}
		
		ItemStack fuel = getFuel().getStack();
		
		if (fuel != null)
		{
			compound.setTag("fuel", fuel.writeToNBT(new NBTTagCompound()));
		}
		
		return new S35PacketUpdateTileEntity(pos, 0, compound);
	}
	
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet)
	{
		NBTTagCompound compound = packet.getNbtCompound();
		burnTime = compound.getInteger("burnTime");
		waterAround = compound.getBoolean("waterAround");
		
		getInput().set(compound.hasKey("input") ? ItemStack.loadItemStackFromNBT(compound.getCompoundTag("input")) : null);
		
		getFuel().set(compound.hasKey("fuel") ? ItemStack.loadItemStackFromNBT(compound.getCompoundTag("fuel")) : null);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		super.readFromNBT(compound);
		
		NBTTagList tagList = compound.getTagList("items", 10);
		
		for (int i = 0; i < tagList.tagCount(); ++i)
		{
			NBTTagCompound itemCompound = (NBTTagCompound) tagList.get(i);
			byte slot = itemCompound.getByte("slot");
			
			if (slot >= 0 && slot < inventory.length)
			{
				inventory[slot] = ItemStack.loadItemStackFromNBT(itemCompound);
			}
		}
		
		burnTime = compound.getInteger("burnTime");
		cookTime = compound.getInteger("cookTime");
		totalBurnTime = compound.getInteger("totalBurnTime");
		
		waterAround = compound.getBoolean("waterAround");
		
		if (compound.hasKey("customName"))
		{
			customName = compound.getString("customName");
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound compound)
	{
		super.writeToNBT(compound);
		
		NBTTagList itemList = new NBTTagList();
		int i = 0;
		
		for (ItemStack stack : inventory)
		{
			if (stack != null)
			{
				NBTTagCompound itemComp = new NBTTagCompound();
				itemComp.setByte("slot", (byte)i);
				stack.writeToNBT(itemComp);
				
				itemList.appendTag(itemComp);
			}
			
			i++;
		}
		
		compound.setTag("items", itemList);
		
		compound.setInteger("burnTime", burnTime);
		compound.setInteger("totalBurnTime", totalBurnTime);
		compound.setInteger("cookTime", cookTime);
		
		compound.setBoolean("waterAround", waterAround);
		
		if (hasCustomName())
		{
			compound.setString("customName", customName);
		}
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return new AxisAlignedBB(pos.getX() - 0.25, pos.getY(), pos.getZ() - 0.25,
				pos.getX() + 1.25, pos.getY() + 1.25, pos.getZ() + 1.25);
	}
	
	@Override
	public SlotModifier getInput()
	{
		return input;
	}
	
	@Override
	public SlotModifier getInputWaste()
	{
		return inputWaste;
	}
	
	@Override
	public List<? extends SlotModifier> getIngredients()
	{
		return ingredients;
	}
	
	@Override
	public SlotModifier getFuel()
	{
		return fuel;
	}
	
	@Override
	public SlotModifier getOutput()
	{
		return output;
	}
	
	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		return true;
	}
	
	@Override
	public int getField(int id)
	{
		return 0;
	}
	
	@Override
	public void setField(int id, int value)
	{
	}
	
	@Override
	public int getFieldCount()
	{
		return 0;
	}
	
	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		if (side.getAxis() != EnumFacing.Axis.Y && worldObj.getBlockState(pos).getValue(BlockCampfire.FACING) == EnumAxis.X)
			side = side.rotateY();
		
		switch (side)
		{
		case NORTH:
		case SOUTH:
			return SLOTS_FRONT;
		case EAST:
		case WEST:
			return SLOTS_SIDE;
		case UP:
			return SLOTS_TOP;
		case DOWN:
			return SLOTS_BOTTOM;
		}
		
		throw new RuntimeException("Somehow TileEntityCampfire.getSlotsForFace got an EnumFacing that doesn't exist.");
	}
	
	@Override
	public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction)
	{
		return true;
	}
	
	@Override
	public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
	{
		return true;
	}
	
	@Override
	public ContainerCampfire createContainer(InventoryPlayer playerInventory, EntityPlayer player)
	{
		return new ContainerCampfire(player, this);
	}
	
	@Override
	public String getGuiID()
	{
		return Constants.ASSETS_PREFIX + "campfire";
	}
	
	@Override
	public void openInventory(EntityPlayer player)
	{
	}
	
	@Override
	public void closeInventory(EntityPlayer player)
	{
	}
	
	@Override
	public boolean canRenderBreaking()
	{
		return false;
	}
}
