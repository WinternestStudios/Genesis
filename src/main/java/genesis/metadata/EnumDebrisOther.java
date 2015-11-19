package genesis.metadata;

public enum EnumDebrisOther implements IMetadata
{
	CALAMITES("calamites"), epidexipteryx_feather("epidexipteryx_feather", "epidexipteryxFeather");
	
	final String name;
	final String unlocalizedName;
	
	EnumDebrisOther(String name)
	{
		this(name, name);
	}
	
	EnumDebrisOther(String name, String unlocalizedName)
	{
		this.name = name;
		this.unlocalizedName = unlocalizedName;
	}
	
	@Override
	public String getUnlocalizedName()
	{
		return null;
	}
	
	@Override
	public String getName()
	{
		return null;
	}
	
	@Override
	public String toString()
	{
		return super.toString();
	}
}