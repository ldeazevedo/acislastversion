package net.sf.l2j.gameserver.communitybbs.module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

public class StatusEpicBoss
{
	private StringBuilder StatusEpicBoss = new StringBuilder();
	private static String SQLBoss = "SELECT boss_id, respawn_time, status, name, id, level FROM grandboss_data LEFT JOIN npc ON grandboss_data.boss_id=npc.id WHERE boss_id ORDER BY grandboss_data.status DESC";

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
	
	public StatusEpicBoss()
	
	{
		loadFromDB();
	}
	
	private void loadFromDB()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement(SQLBoss);
			ResultSet result = statement.executeQuery())
		{
			while (result.next())
			{
				boolean status;
				NpcTemplate npc = NpcData.getInstance().getTemplate(result.getInt("boss_id"));
				for (int boss : BOSSES)
					if (boss == result.getInt("boss_id"))
						continue;
				long temp = result.getLong("respawn_time");
				status = temp <= System.currentTimeMillis();
				if (npc.getName().equalsIgnoreCase("Scarlet van Halisha"))
					continue;
				addEpicBossList(npc.getName(), npc.getLevel(), status ? false : true);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void addEpicBossList(String name, int level, boolean isStatus)
	{
		this.StatusEpicBoss.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		this.StatusEpicBoss.append("<tr>");
		this.StatusEpicBoss.append("<td FIXWIDTH=2></td>");
		this.StatusEpicBoss.append("<td FIXWIDTH=90>"+ name +"</td>");
		this.StatusEpicBoss.append("<td FIXWIDTH=90>"+ level +"</td>");
		this.StatusEpicBoss.append("<td FIXWIDTH=90>"+ ((isStatus) ? "<font color=CC0000≯Dead</font>" : "<font color=00FF00>Alive</font>") +"</td>");
		this.StatusEpicBoss.append("<td FIXWIDTH=2></td>");
		this.StatusEpicBoss.append("</tr>");
		this.StatusEpicBoss.append("</table>");
		this.StatusEpicBoss.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}
	
	public String loadStatusEpicBossList()
	{
		return StatusEpicBoss.toString();
	}
}