package genesis.combo.variant;

import genesis.util.Constants;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.EnumHelper;

public enum EnumClothing implements IMetadata<EnumClothing>
{
	CHITIN("chitin", 10, new int[]{2, 4, 3, 2}, 10, SoundEvents.item_armor_equip_iron),	// TODO: Change to original sounds.
	CAMOUFLAGE("camouflage", 5, new int[]{0, 0, 0, 0}, 0, SoundEvents.item_armor_equip_leather);
	
	final String name;
	final String unlocalizedName;
	final ArmorMaterial material;
	
	EnumClothing(String name, String unlocalizedName,
			int durability, int[] reductionAmounts, int enchantability,
			SoundEvent equipSound)
	{
		this.name = name;
		this.unlocalizedName = unlocalizedName;
		this.material = EnumHelper.addArmorMaterial(Constants.MOD_ID.toUpperCase() + "_" + name(), Constants.ASSETS_PREFIX + name,
				durability, reductionAmounts, enchantability,
				equipSound);
	}
	
	EnumClothing(String name,
			int durability, int[] reductionAmounts, int enchantability,
			SoundEvent equipSound)
	{
		this(name, name, durability, reductionAmounts, enchantability, equipSound);
	}
	
	EnumClothing(String name, String unlocalizedName,
			int enchantability,
			SoundEvent equipSound)
	{
		this(name, unlocalizedName, 0, new int[]{0, 0, 0, 0}, enchantability, equipSound);
	}
	
	EnumClothing(String name,
			int enchantability,
			SoundEvent equipSound)
	{
		this(name, name, enchantability, equipSound);
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public String getUnlocalizedName()
	{
		return unlocalizedName;
	}
	
	public ArmorMaterial getMaterial()
	{
		return material;
	}
}
