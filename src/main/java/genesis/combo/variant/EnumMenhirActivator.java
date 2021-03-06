package genesis.combo.variant;

public enum EnumMenhirActivator implements IMetadata<EnumMenhirActivator>
{
	LEYLINE_FEED_CRYSTAL("leyline_feed_crystal", "leylineFeedCrystal", true);
	
	final String name;
	final String unlocalizedName;
	final boolean forOverworld;
	
	EnumMenhirActivator(String name, String unlocalizedName, boolean forOverworld)
	{
		this.name = name;
		this.unlocalizedName = unlocalizedName;
		this.forOverworld = forOverworld;
	}
	
	EnumMenhirActivator(String name, boolean forOverworld)
	{
		this(name, name, forOverworld);
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
	
	public boolean isForOverworld()
	{
		return forOverworld;
	}
}
