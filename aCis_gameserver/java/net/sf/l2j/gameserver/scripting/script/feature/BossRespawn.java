package net.sf.l2j.gameserver.scripting.script.feature;

import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.Quest;

public class BossRespawn extends Quest
{
	private static final int NPC_ID = 50010;
	private static final int[] BOSSES =
	{
		29001,
		29006,
		29014,
		29019,
		29020,
		29022,
		29028,
		29045
	};
	
	public BossRespawn()
	{
		super(-1, "feature");
		addFirstTalkId(NPC_ID);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player activeChar)
	{
		if (npc == null || activeChar == null)
			return null;
		
		if (npc.getNpcId() == NPC_ID)
			sendInfo(activeChar);
		return null;
	}
	
	private static void sendInfo(Player activeChar)
	{
		StringBuilder tb = new StringBuilder();
		tb.append("<html><title>Grand Boss Info</title><body><br><center>");
		tb.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br><br>");
		
		for (int boss : BOSSES)
		{
			String name = NpcData.getInstance().getTemplate(boss).getName();
			long delay = GrandBossManager.getInstance().getStatSet(boss).getLong("respawn_time");
			if (delay <= System.currentTimeMillis())
				tb.append("<font color=\"00C3FF\">" + name + "</color>: <font color=\"9CC300\">Is Alive</color><br1>");
			else
			{
				int hours = (int) ((delay - System.currentTimeMillis()) / 1000 / 60 / 60);
				int mins = (int) (((delay - (hours * 60 * 60 * 1000)) - System.currentTimeMillis()) / 1000 / 60);
				int seconts = (int) (((delay - ((hours * 60 * 60 * 1000) + (mins * 60 * 1000))) - System.currentTimeMillis()) / 1000);
				tb.append("<font color=\"00C3FF\">" + name + "</color><font color=\"FFFFFF\"> Respawn in : </color><font color=\"32C332\">" + hours + ":" + mins + ":" + seconts + "</color><br1>");
			}
		}
		
		tb.append("<br><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		tb.append("</center></body></html>");
		
		NpcHtmlMessage msg = new NpcHtmlMessage(NPC_ID);
		msg.setHtml(tb.toString());
		activeChar.sendPacket(msg);
	}
}