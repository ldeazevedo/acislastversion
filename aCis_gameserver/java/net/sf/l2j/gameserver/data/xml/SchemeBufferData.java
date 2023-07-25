package net.sf.l2j.gameserver.data.xml;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.model.buffer.BuffCategory;
import net.sf.l2j.gameserver.model.holder.BuffSkillHolder;
import org.w3c.dom.Document;

public class SchemeBufferData implements IXmlReader
{
	private static final CLogger LOGGER = new CLogger(SchemeBufferData.class.getName());
	
	//Available Buffs
	private final Map<Integer,BuffSkillHolder> _availableBuffs = new HashMap<>();
	private final Map<Integer,String> _availableIcons = new HashMap<>();
	private final Map<BuffCategory,List<Integer>> _availableBuffsSorted = new HashMap<>();
	
	
	
	private int _mysticSchemePrice = 0;
	private int _warriorSchemePrice = 0;

	public static SchemeBufferData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public int getMysticSchemePrice()
	{
		return _mysticSchemePrice;
	}
	
	public int getWarriorSchemePrice()
	{
		return _warriorSchemePrice;
	}
		
	
	private void generateStackingBuffs()
	{
		for (var buffId : _availableBuffs.keySet())
		{
			var buffEffects = _availableBuffs.get(buffId).getSkill().getEffectTemplates();
			
			//lets check only First effect - Is there any buff that gives more than one effect?
			if (buffEffects.size() == 0)
			{
				continue;
			}
			
			for (var checkId : _availableBuffs.keySet())
			{
				if (checkId == buffId)
					continue;
					
				var checkEffects = _availableBuffs.get(checkId).getSkill().getEffectTemplates();
				//lets check only First effect - Is there any buff that gives more than one effect?
				if (checkEffects.size() == 0)
				{
					continue;
				}		
				
				if (buffEffects.get(0).getStackType().equals(checkEffects.get(0).getStackType()))
				{
					_availableBuffs.get(buffId).addStackingBuff(checkId);
				}
			}
		}
	}
	
	private void calcSchemesPrices()
	{
		for (int skillId : Config.BUFFS_MASTER_MYSTIC_SCHEME)
		{
			var buff = getBuff(skillId);
			
			if (buff != null) 
			{
				_mysticSchemePrice += buff.getPriceItemCount();
			}
		}
		
		for (int skillId : Config.BUFFS_MASTER_WARRIOR_SCHEME)
		{
			var buff = getBuff(skillId);
			
			if (buff != null) 
			{
				_warriorSchemePrice += buff.getPriceItemCount();
			}
		}
	}
	
	@Override
	public void load()
	{
		_availableBuffs.clear();
		_availableBuffsSorted.clear();
		_availableIcons.clear();

		parseFile("./data/xml/bufferBuffs.xml");
		LOGGER.info("Loaded {} Buffs data and {} Icons data.", _availableBuffs.size(), _availableIcons.size());
		
		calcSchemesPrices();
		generateStackingBuffs();
	}
	
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "icons", iconsNode -> forEach(iconsNode, "icon", iconNode ->
			{
				final StatSet set = parseAttributes(iconNode);
				_availableIcons.put(set.getInteger("id"), set.getString("name"));
			}));
			forEach(listNode, "buffs", buffsNode -> forEach(buffsNode, "buff", buffNode ->
			{
				final StatSet set = parseAttributes(buffNode);
				
				
				final int skillId = set.getInteger("skillId");
				int skillLevel = 0;
				
				if (set.containsKey("skill_level"))
				{
					skillLevel = set.getInteger("skill_level");
				}
				else 
				{
					skillLevel = SkillTable.getInstance().getMaxLevel(Integer.valueOf(skillId));
				}
				
				final int order = set.getInteger("order");
				final int priceItemId = set.getInteger("price_id");
				final int priceCount = set.getInteger("price_count");
				final String buffCategory = set.getString("group");
				
				BuffCategory category = BuffCategory.GetByName(buffCategory);
				
				if (category == null)
				{
					LOGGER.info( "Invalid buff category: " + buffCategory);
					category = BuffCategory.All;
				}
									
				final String buffDescription = set.getString("description");
				BuffSkillHolder newSkillHolder = new BuffSkillHolder(skillId, skillLevel,priceItemId, priceCount, category, buffDescription, order);
				_availableBuffs.put(skillId, newSkillHolder);
				
				//split by category
				if (!_availableBuffsSorted.containsKey(category))
				{
					_availableBuffsSorted.put(category, new ArrayList<>());
				}
				_availableBuffsSorted.get(category).add(skillId);
			}));
		});
	}
	
	public Map<Integer, String> getIcons()
	{
		return _availableIcons;
	}

	public boolean hasBuff(int buffId)
	{
		return _availableBuffs.containsKey(buffId);
	}
	
	public BuffSkillHolder getBuff(int buffId)
	{
		return _availableBuffs.get(buffId);
	}
	
	public Map<Integer, Integer> getBuffsOrders()
	{
		return _availableBuffs.values().stream().collect(Collectors.toMap(BuffSkillHolder::getId, BuffSkillHolder::getOrder));
	}
	
	
	public List<Integer> getAvailableBuffsByCategory(BuffCategory buffCategory)
	{
		if (!_availableBuffsSorted.containsKey(buffCategory))
		{
			return new ArrayList<>();
		}
		return _availableBuffsSorted.get(buffCategory);
	}
	
	protected SchemeBufferData()
	{
		load();
	}
	
	private static class SingletonHolder
	{
		protected static final SchemeBufferData _instance = new SchemeBufferData();
	}
}