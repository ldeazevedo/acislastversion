package net.sf.l2j.gameserver.communitybbs.module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.gameserver.data.xml.PlayerData;

public class TopPvpPk
{
	protected static final Logger log = Logger.getLogger(TopPvpPk.class.getName());
	private final StringBuilder html = new StringBuilder();

	public TopPvpPk()
	{
		loadFromDB();
	}

	private void loadFromDB()
	{
		try (Connection con = ConnectionPool.getConnection();
			 PreparedStatement statement = con.prepareStatement("SELECT char_name, base_class, pvpkills, pkkills, level, online, clan_name, ally_name FROM characters LEFT JOIN clan_data ON clan_id=clanid WHERE accesslevel=0 ORDER BY pvpkills DESC, char_name ASC LIMIT 10");
			 ResultSet result = statement.executeQuery())
		{
			int _posId = 0;
			while (result.next())
			{
				var status = result.getInt("online") == 1;
				var clan = result.getString("clan_name") == null ? "No Clan" : result.getString("clan_name");
				var ally = result.getString("ally_name") == null ? "No Ally" : result.getString("ally_name");
				var charName = result.getString("char_name");
				var classId = result.getInt("base_class");
				var level = result.getInt("level");
				var pvpKills = result.getInt("pvpkills");
				var pkKills = result.getInt("pkkills");
				addTopPvpPkList(++_posId, charName, clan, ally, classId, level, pvpKills, pkKills, status);
			}
		} catch (Exception e)
		{
			log.severe("There was an error when loading top pvp/pk: " + e.getMessage());
		}
	}

	public String getTopPvpPkHTML()
	{
		return html.toString();
	}

	private void addTopPvpPkList(int objId, String name, String clan, String alliance, int classId, int level, int points, int pkKills, boolean isOnline)
	{
		html.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		html.append("<tr>");
		html.append("<td FIXWIDTH=2></td>");
		html.append("<td FIXWIDTH=15>").append(objId).append(".</td>");
		html.append("<td FIXWIDTH=90>").append(name).append("</td>");
		html.append("<td FIXWIDTH=20>").append(level).append("</td>");
		html.append("<td FIXWIDTH=70>").append(PlayerData.getInstance().getClassNameById(classId)).append("</td>");
		html.append("<td FIXWIDTH=70>").append(clan).append("</td>");
		html.append("<td FIXWIDTH=70>").append(alliance).append("</td>");
		html.append("<td FIXWIDTH=25>").append(points).append("</td>");
		html.append("<td FIXWIDTH=25>").append(pkKills).append("</td>");
		html.append("<td FIXWIDTH=90>").append(((isOnline) ? "<font color=00FF00>Online</font>" : "<font color=CC0000>Offline</font>")).append("</td>");
		html.append("<td FIXWIDTH=2></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}
}