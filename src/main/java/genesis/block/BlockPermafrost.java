package genesis.block;

import genesis.common.GenesisSounds;
import net.minecraft.block.material.Material;

public class BlockPermafrost extends BlockGenesis
{
	public BlockPermafrost()
	{
		super(Material.rock, GenesisSounds.PERMAFROST);
		
		slipperiness = 0.98F;
		setHardness(0.5F);
		setSoundType(GenesisSounds.PERMAFROST);
		setHarvestLevel("pickaxe", 0);
	}
}
