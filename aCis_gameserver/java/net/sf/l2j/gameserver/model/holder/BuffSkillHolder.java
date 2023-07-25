package net.sf.l2j.gameserver.model.holder;

import java.util.List;
import java.util.ArrayList;

import net.sf.l2j.gameserver.model.buffer.BuffCategory;

/**
 * A container extending {@link IntIntHolder} used for schemes buffer.
 */
public final class BuffSkillHolder extends IntIntHolder
{
	private final int _price;
	private final String _oldtype;
	private final String _description;
	private final int _priceitemId;
	private final BuffCategory _type;
	private final int _order;
	private List<Integer> _stackingBuffs;
	
	public BuffSkillHolder(int id, int level, int priceItemId, int priceCount, BuffCategory buffCategory, String description, int order)
	{
		super(id, level);
		this._oldtype = "";
		_priceitemId = priceItemId;
		_price = priceCount;
		_type = buffCategory;
 		_description = description;
		_order = order;
		_stackingBuffs = new ArrayList<>();
	}
	
	public BuffSkillHolder(int id, int level, int price, String type, String description)
	{
		super(id, level);
		
		_price = price;
		_oldtype = type;
		_description = description;
		this._priceitemId = 0;
		this._type = null;
		this._order = 0;
	}
	
	
	@Override
	public String toString()
	{
		return "BuffSkillHolder [id=" + getId() + " value=" + getValue() + " price=" + _price + " type=" + _oldtype + " desc=" + _description + "]";
	}

	public void addStackingBuff(int buffId)
 	{
		_stackingBuffs.add(buffId);
 	}

	public final int getPrice()
	{
		return _price;
	}
	
	public boolean isStacking(int buffId)
	{
		return _stackingBuffs.contains(buffId);
	}
	
	public final String getType()
	{
		return _oldtype;
	}
	
	public final BuffCategory getBuffCategory()
	{
		return _type;
	}
	
	public int getOrder()
	{
		return _order;
	}

	public final String getDescription()
	{
		return _description;
	}

	public final int getPriceItemCount()
	{
		return _price;
	}
	
	public final int getPriceItemId()
	{
		return _priceitemId;
	}
	
	public final int getLevel()
	{
		return this.getValue();
	}
}