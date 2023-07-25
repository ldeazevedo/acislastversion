package net.sf.l2j.gameserver.model;

public class DressMe
{
	private final int itemId;
	private final int hair;
	private final int chest;
	private final int legs;
	private final int gloves;
	private final int feet;
	private boolean hairOn;
	
	public DressMe(int itemId, int chest, int legs, int hair, int gloves, int feet, boolean hairOn)
	{
		this.itemId = itemId;
		this.chest = chest;
		this.legs = legs;
		this.hair = hair;
		this.gloves = gloves;
		this.feet = feet;
		this.hairOn = hairOn;
	}
	
	public final int getItemId()
	{
		return itemId;
	}
	
	public int getChest()
	{
		return chest;
	}
	
	public int getLegs()
	{
		return legs;
	}
	
	public int getHair()
	{
		return hair;
	}
	
	public int getGloves()
	{
		return gloves;
	}
	
	public int getFeet()
	{
		return feet;
	}
	
	public boolean hairOn()
	{
		return hairOn;
	}
}