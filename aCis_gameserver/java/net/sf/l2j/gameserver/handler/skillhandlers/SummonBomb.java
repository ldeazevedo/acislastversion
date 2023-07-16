package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.skills.SkillType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Agathion;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.skills.L2Skill;

public class SummonBomb implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.SUMMON_CREATURE
	};
	
	@Override
	public void useSkill(Creature activeChar, L2Skill skill, WorldObject[] targets, ItemInstance itemInstance)
	{
		if (!(activeChar instanceof Player))
			return;
		
		final Player player = (Player) activeChar;
		
		// Check NpcTemplate validity.
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(37090);
		if (npcTemplate == null)
			return;
		
		if (npcTemplate.getType().equalsIgnoreCase("Agathion"))
		{
			if (player.getAgathion() != null)
				player.getAgathion().unSummon(player);
			
			Agathion summon;
			summon = new Agathion(IdFactory.getInstance().getNextId(), npcTemplate, player);
			player.setAgathion(summon);
			summon.setInstanceId(player.getInstanceId());
			
			summon.setName(npcTemplate.getName());
			summon.setTitle(player.getName());
			summon.getStatus().setMaxHpMp();
			summon.forceRunStance();
			
			final SpawnLocation spawnLoc = activeChar.getPosition().clone();
			spawnLoc.addStrictOffset(40);
			spawnLoc.setHeadingTo(activeChar.getPosition());
			spawnLoc.set(GeoEngine.getInstance().getValidLocation(activeChar, spawnLoc));
			
			summon.spawnMe(spawnLoc);
			summon.setInvul(true);
			summon.getAI().setFollowStatus(true);
			return;
		}
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}