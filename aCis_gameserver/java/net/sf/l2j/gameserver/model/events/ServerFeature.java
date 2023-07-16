package net.sf.l2j.gameserver.model.events;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

public class ServerFeature
{
	protected static final Logger LOGGER = Logger.getLogger(ServerFeature.class.getName());

	protected ServerFeature()
	{
	}
	
	public static void checkEnterWorld(Player player)
	{
		float points = 4 * (System.currentTimeMillis() - player.getLastAccess()) / 60000;
		if (points > 0)
			player.getStatus().updateVitalityPoints(points, false, true);
		player.showPcBangWindow();
	}
	
	public static void onCalculateRewards(Player attacker, long exp, int sp, int pcbandpoints, long damage, Npc npc)
	{
		if (pcbandpoints > 0)
			attacker.updatePcBangScore(pcbandpoints);
		if (exp > 0)
			attacker.getStatus().updateVitalityPoints(getVitalityPoints(npc, damage, attacker), true, false);
		if (attacker.isGM())
			LOGGER.info("onCalculateRewards() " + attacker.getName() +" - exp: "+exp+ " - Damage: "+damage +" - Name Npc: "+npc.getName());
	}
	
	/*
	 * Return vitality points decrease (if positive)
	 * or increase (if negative) based on damage.
	 * Maximum for damage = maxHp.
	 */
	public static float getVitalityPoints(Npc npc, long damage, Player player)
	{
		int level = player.getStatus().getLevel();
		// sanity check
		if (damage <= 0)
			return 0;

		float divider = npc.getTemplate().baseVitalityDivider;
		if (divider == 0 || npc.isRaidRelated() && npc.hasMaster())
			return 0;
		
		if (level < 70)
			divider *= 2;
		else if (level < 76 && level > 70)
			divider += (divider /= 3);
		// negative value - vitality will be consumed
		float ret = npc.isRaidBoss() ? Math.min(damage, npc.getStatus().getMaxHp()) / 100 : Math.min(damage, npc.getStatus().getMaxHp()) / divider;
		if (ret > 150)
			ret = 150;
		return - ret;/*(npc.isRaidBoss() ? Math.min(damage, npc.getStatus().getMaxHp()) / 100 : Math.min(damage, npc.getStatus().getMaxHp()) / divider /2)*/
	}

	public static void readChats(Player player, String text, L2GameServerPacket packet)
	{
		for (Player gm : AdminData.getInstance().getAllGms(true))
			if (gm.getReadChat())
				gm.sendPacket(new CreatureSay(player.getObjectId(), SayType.ALLIANCE, player.getName(), "[" + player.getClan().getName() + "]:" + text));
	}
	
	public static void sendScreenMessage(Player player, String message, int time, boolean sendMessage)
	{
		player.sendPacket(new ExShowScreenMessage(message, time, SMPOS.MIDDLE_LEFT, false));
		if (sendMessage)
			player.sendMessage(message);
	}
}