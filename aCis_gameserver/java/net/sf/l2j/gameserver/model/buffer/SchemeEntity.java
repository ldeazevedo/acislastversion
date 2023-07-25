package net.sf.l2j.gameserver.model.buffer;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.SchemeBufferData;
import net.sf.l2j.gameserver.model.holder.BuffSkillHolder;

public final class SchemeEntity
{
	public Map<Integer, SchemeBuffEntity> Buffs = new HashMap<>(); //id, buffId
	
	private int _id;
	private int _playerId;
	private String _description;
	private String _name;
	private int _iconId;
	private long _creationDate;
	private String _shareCode;
	private boolean _isShareCodeLocked;
	
	private boolean _restoreHp;
	private boolean _restoreMp;
	private boolean _restoreCp;
	//--------------------//
	
	public int getId()
	{
		return _id;
	}
	
	public void setId(int id)
	{
		_id = id;
	}
	
	public long getCreationDate()
	{
		return _creationDate;
	}
	
	public void setCreationDate(long creationDate)
	{
		_creationDate = creationDate;
	}
	
	public int getIconId()
	{
		return _iconId;
	}
	
	public void setIconId(int iconId)
	{
		_iconId = iconId;
	}
	
	public String getDescription()
	{
		return  _description;
	} 
	
	public void setDescription(String description)
	{
		_description = description;
	}	
	
	public String getName()
	{
		return  _name;
	}
	
	public String getNameForUI()
	{
		if (_name == null)
			return " ";
		
		var result = _name;
		result = result.replace(">", "&gt;");
		result = result.replace("<", "&lt;");
		
		return  result;
	}
	
	public void setName(String name)
	{
		_name = name;
	}	
	
	public int getPlayerId()
	{
		return _playerId;
	}
	
	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}

	public void setShareCodeLocked(boolean locked)
	{
		_isShareCodeLocked = locked;
	}
	
	public boolean isShareCodeLocked() 
	{
		return _isShareCodeLocked;
	}
	
	public void setShareCode(String shareCode)
	{
		_shareCode = shareCode;
	}
	
	public String getShareCode() 
	{
		return _shareCode;
	}
	
	public void setRestoreHp(boolean restoreHp)
	{
		_restoreHp = restoreHp;
	}
	
	public void setRestoreMp(boolean restoreMp)
	{
		_restoreMp = restoreMp;
	}
	
	public void setRestoreCp(boolean restoreCp)
	{
		_restoreCp = restoreCp;
	}
	
	public boolean getRestoreHp()
	{
		return _restoreHp;
	}
	
	public boolean getRestoreMp()
	{
		return _restoreMp;
	}
	
	public boolean getRestoreCp()
	{
		return _restoreCp;
	}
	
	private int _schemePrice = 0;
	
	public void addBuffOnLastPosition(int buffId)
	{
		int last = 0;
		for (int i: Buffs.keySet())
		{
			if (Buffs.get(i).getOrder() > last)
			{
				last = Buffs.get(i).getOrder();
			}
		}
		SchemeBuffEntity newBuff = new SchemeBuffEntity();
		newBuff.setBuffId(buffId);
		newBuff.setOrder(last +1);
		Buffs.put(buffId, newBuff);
	}
	
	public int getPrice()
	{
		return _schemePrice;
	}
	
	public void recalculatePrice()
	{
		_schemePrice = 0;
		for (int i: Buffs.keySet())
		{
			BuffSkillHolder buff = SchemeBufferData.getInstance().getBuff(Buffs.get(i).getBuffId());
			if (buff != null)
			{
				_schemePrice = _schemePrice + buff.getPriceItemCount();
			}
		}
		
		if (_restoreCp)
		{
			_schemePrice += Config.BUFFS_MASTER_CP_RESTORE_PRICE;
		}
		if (_restoreHp)
		{
			_schemePrice += Config.BUFFS_MASTER_HP_RESTORE_PRICE;
		}
		if (_restoreMp)
		{
			_schemePrice += Config.BUFFS_MASTER_MP_RESTORE_PRICE;
		}
	}
	
	// use just for load from BD, for adding buff to existing scheme use AddBuffOnLastPosition
	public void addBuffOnLoad(int buffId)
	{
		SchemeBuffEntity newBuff = new SchemeBuffEntity();
		newBuff.setBuffId(buffId);
		newBuff.setOrder(Buffs.size() + 1);
		Buffs.put(buffId, newBuff);
	}
	
	public Boolean isBuffInScheme(int buffId)
	{
		return Buffs.containsKey(buffId);
	}
	
	public List<Integer> getBuffsSorted()
	{
		List<Integer> sorted =  Buffs.entrySet().stream()
			.sorted((e1,e2)-> Integer.compare(e1.getValue().getOrder(), e2.getValue().getOrder()))
			.map(e -> e.getKey())
			.collect(Collectors.toList());

		if (sorted.size()>0)
		{
			//FirstOrder = Buffs.get(sorted.get(0)).Order;
			//LastOrder = Buffs.get(sorted.get(sorted.size()-1)).Order;
		}
		
		return sorted;
	}
}
