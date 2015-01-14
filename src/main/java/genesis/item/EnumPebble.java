package genesis.item;

import genesis.common.GenesisItems;
import net.minecraft.item.ItemStack;

public enum EnumPebble implements IMetadataItem {
    GRANITE("granite", "granite"),
    RHYOLITE("rhyolite", "rhyolite"),
    DOLERITE("dolerite", "dolerite"),
    QUARTZITE("quartzite", "quartzite"),
    BROWN_FLINT("brown_flint", "brownFlint");

    private final String name;
    private final String unlocalizedName;

    EnumPebble(String name, String unlocalizedName) {
        this.name = name;
        this.unlocalizedName = unlocalizedName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUnlocalizedName() {
        return unlocalizedName;
    }

    @Override
    public int getMetadata() {
        return ordinal();
    }

    @Override
    public ItemStack createStack(int amount) {
        return new ItemStack(GenesisItems.pebble, amount, getMetadata());
    }
}