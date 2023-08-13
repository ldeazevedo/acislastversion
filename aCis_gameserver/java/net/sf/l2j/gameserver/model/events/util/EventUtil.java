package net.sf.l2j.gameserver.model.events.util;

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import java.util.List;
import java.util.logging.Logger;

public class EventUtil
{
	private static final Logger log = Logger.getLogger(EventUtil.class.getName());

	private EventUtil()
	{
	}

	public static void revertPlayer(Player player)
	{
		player.setIsInEvent(false);
		if (player.isDead())
			player.doRevive();
		player.getStatus().setMaxCpHpMp();
		player.broadcastUserInfo();

		player.teleportTo(player.getSavedLocation() != null ? player.getSavedLocation() : EventConstants.DEFAULT_LOCATION, 0);

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
		player.sendPacket(html);
	}

	public static String generateHtmlForInstances(List<Tuple<Player, Player>> instances)
	{
		StringBuilder stringBuilder = new StringBuilder("<html><body>");
		stringBuilder.append("<table width=270>");
		instances.forEach(i -> {
			stringBuilder.append("<tr>");
			stringBuilder.append("<td width=180 align=center>").append(i.left().getName()).append(" vs. ").append(i.right().getName()).append("</td><br>");
			stringBuilder.append("<td width=45><button value=\"Mirar\" action=\"bypass -h instance_ ").append(i.getInstanceId())
					.append("\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><br>");
			stringBuilder.append("</tr><br>");
		});
		stringBuilder.append("</table><br>");
		stringBuilder.append("</body></html>");
		return stringBuilder.toString();
	}

	public static void storeEventResults(Player killer)
	{
		try (var con = ConnectionPool.getConnection();
			 var statement = con.prepareStatement(EventConstants.QUERY_EVENT_INFO))
		{
			statement.setString(1, killer.getName());
			boolean existsRow = statement.executeQuery().first();
			String sql = existsRow ? EventConstants.UPDATE_EVENT_INFO : EventConstants.INSERT_EVENT_INFO;
			try (var statement2 = con.prepareStatement(sql))
			{
				statement2.setString(1, killer.getName());
				statement2.execute();
			}
		} catch (Exception e)
		{
			log.warning("Error when storing event results in database for: " + killer.getName() + " | " + e);
		}
	}
}