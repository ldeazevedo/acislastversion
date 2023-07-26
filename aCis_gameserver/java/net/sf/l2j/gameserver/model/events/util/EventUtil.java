package net.sf.l2j.gameserver.model.events.util;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import java.util.List;

public class EventUtil
{

	public static void revertPlayer(Player player)
	{
		if (player.getInEvent())
			player.setIsInEvent(false);
		if (player.isDead())
			player.doRevive();
		player.getStatus().setMaxCpHpMp();
		player.broadcastUserInfo();

		if (player.getSavedLocation() != null)
			player.teleportTo(player.getSavedLocation(), 0);
		else
			player.teleportTo(82698, 148638, -3473, 0);

		if (player.getKarma() > 0)
			player.setKarma(0);

		player.setPvpFlag(0);
		player.setTeam(TeamType.NONE);
		player.setInstanceId(0);
	}

	public static void sendHtmlMessage(Player player, String fileOrHtml, boolean isFile)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		if (isFile) html.setFile(fileOrHtml);
		else html.setHtml(fileOrHtml);
		html.replace("%objectId%", player.getObjectId());
		html.replace("%adenasCost%", StringUtil.formatNumber(Config.WEDDING_PRICE));
		html.replace("%needOrNot%", Config.WEDDING_FORMALWEAR ? "will" : "won't");
		player.sendPacket(html);
	}

	public static String generateHtmlForInstances(List<Integer> instances)
	{
		//TODO: cambiar esto a como quede mejor
		StringBuilder stringBuilder = new StringBuilder("<html><body>");
		stringBuilder.append("<table width=270>");
		stringBuilder.append("<tr>");
		instances.forEach(i -> {
			stringBuilder.append("<td width=180 align=center>Mirar ").append(i).append("</td><br>");
			stringBuilder.append("<td width=45><button value=\"Main\" action=\"bypass -h instance_ "+i+"\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><br>");
		});
	//	instances.forEach(i -> stringBuilder.append("<td width=45><button value=\"Main\" action=\"bypass -h instance_ "+i+ "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>"));
	//	stringBuilder.append("<td width=45><button value=\"Main\" action=\"bypass -h instance_ \" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		stringBuilder.append("</tr>");
		stringBuilder.append("</table><br>");
		stringBuilder.append("</body></html>");
		return stringBuilder.toString();
	}
}