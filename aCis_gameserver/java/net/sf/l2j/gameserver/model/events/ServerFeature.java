package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;

import java.util.logging.Logger;

public class ServerFeature {
	protected static final Logger log = Logger.getLogger(ServerFeature.class.getName());

	private ServerFeature() {
	}

	public static void checkEnterWorld(Player player) {
		float points = 4 * (System.currentTimeMillis() - player.getLastAccess()) / 60000f;
		if (points > 0)
			player.getStatus().updateVitalityPoints(points, false, true);
		player.showPcBangWindow();
	}

	public static void onCalculateRewards(Player attacker, long exp, int pcBangPoints, long damage, Npc npc) {
		log.info("ServerFeature::onCalculateRewards() " + attacker.getName() + " - exp: " + exp + " - Damage: " + damage + " - Name Npc: " + npc.getName());

		if (pcBangPoints > 0)
			attacker.updatePcBangScore(pcBangPoints);
		if (exp > 0)
			attacker.getStatus().updateVitalityPoints(getVitalityPoints(npc, damage, attacker), true, false);

		//	if (attacker.isGM())
	}

	/*
	 * Return vitality points decrease (if positive)
	 * or increase (if negative) based on damage.
	 * Maximum for damage = maxHp.
	 */
	public static float getVitalityPoints(Npc npc, long damage, Player player) {
		int level = player.getStatus().getLevel();
	    
	    // Sanity check
	    if (damage <= 0)
	        return 0;
	    
	    float divider = npc.getTemplate().getBaseVitalityDivider();
	    if (divider == 0 || (npc.isRaidRelated() && npc.hasMaster()))
	        return 0;
	    
	    if (level >= 70 && level < 76)
	        divider += divider / 3;
	    else if (level < 70)
	        divider *= 2;
	    
	    long maxHp = npc.getStatus().getMaxHp();
	    
	    // Calculate vitality points
	    float ret = npc.isRaidBoss() ? Math.min(damage, maxHp) / 100f : Math.min(damage, maxHp) / divider;
	    
	    // Limit addition of 50% after 100
	    if (ret > 100) {
	        float excess = ret - 100;
	        ret = 100 + (excess * 0.5f);
	    }
	    
	    return -ret;
	}
	
	public static void readChats(Player player, String text) {
		for (Player gm : AdminData.getInstance().getAllGms(true))
			if (gm.getReadChat()) {
				var clanText = player.getClan() != null ? "[" + player.getClan().getName() + "]:" : "";
				gm.sendPacket(new CreatureSay(player.getObjectId(), SayType.ALLIANCE, player.getName(), clanText + text));
			}
	}

	public static void sendScreenMessage(Player player, String message, int time, boolean sendMessage) {
		player.sendPacket(new ExShowScreenMessage(message, time, SMPOS.MIDDLE_LEFT, false));
		if (sendMessage)
			player.sendMessage(message);
	}
}