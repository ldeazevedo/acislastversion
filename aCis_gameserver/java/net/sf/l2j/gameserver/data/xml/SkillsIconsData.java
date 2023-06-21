package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.icon.SkillIcon;

import org.w3c.dom.Document;

public class SkillsIconsData implements IXmlReader
{

	private static Map<Integer, Map<Integer, SkillIcon>> _skillIcons = new HashMap<>();
	
	protected SkillsIconsData()
	{
		_skillIcons = new HashMap<>();
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/skillsIcons.xml");
		LOGGER.info("Loaded {} skills icons.", _skillIcons.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "skill", iconNode ->
		{
			
			final StatSet set = parseAttributes(iconNode);
			
			int skillId = set.getInteger("ID");
			int skillLevel = set.getInteger("level");
			String iconName = set.getString("icon");
			
			addIcon(skillId, skillLevel, iconName);

		}));
	}
	
	private static void addIcon(int skillId, int level, String iconName)
	{
		SkillIcon iconData = new SkillIcon();
		iconData.SkillID = skillId;
		iconData.Level = level;
		iconData.IconName = iconName;
				
		if (!_skillIcons.containsKey(skillId))
		{
			_skillIcons.put(skillId, new HashMap<>());
		}
		_skillIcons.get(skillId).put(skillId, iconData);
	}
	
	public String getIcon(int skillId)
	{
		if (!_skillIcons.containsKey(skillId))
		{
			
			return "Icon.NOIMAGE";
		}
		// if level is not provided, get hightes level
		Set<Integer> skills = _skillIcons.get(skillId).keySet();
		int maxSkill = Collections.max(skills);
		
		return _skillIcons.get(skillId).get(maxSkill).IconName;
	}
	
	public static SkillsIconsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillsIconsData INSTANCE = new SkillsIconsData();
	}
}