package genesis.combo.variant;

import java.util.List;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.EnumPlantType;

public interface IPlantMetadata<T extends IPlantMetadata<T>> extends IMetadata<T>
{
	int getColorMultiplier(IBlockAccess world, BlockPos pos);
	
	boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos);
	
	List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, List<ItemStack> normalDrop);
	
	boolean isReplaceable(IBlockAccess world, BlockPos pos);
	
	List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, Random rand, List<ItemStack> normalDrop);
	
	EnumPlantType[] getSoilTypes();
	
	int getWaterDistance();
}
