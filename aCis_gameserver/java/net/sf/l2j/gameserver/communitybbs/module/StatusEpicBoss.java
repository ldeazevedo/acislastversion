package net.sf.l2j.gameserver.communitybbs.module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.ASpawn;

public class StatusEpicBoss
{
	private StringBuilder StatusEpicBoss = new StringBuilder();
	private static String SQLBoss = "SELECT boss_id, respawn_time, status, name, id, level FROM grandboss_data LEFT JOIN npc ON grandboss_data.boss_id=npc.id WHERE boss_id ORDER BY grandboss_data.status DESC";

	private static final int[] BOSSES =
	{
		29001,
		29006,
		29014,
		29022
	};
	
	public StatusEpicBoss()
	{
		loadFromDB();
	}
	
	private void loadFromDB()
	{
		Calendar tmpDate = Calendar.getInstance();
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement(SQLBoss);
			ResultSet result = statement.executeQuery())
		{
			while (result.next()) //Tomados del GrandBoss data.
			{
				NpcTemplate npc = NpcData.getInstance().getTemplate(result.getInt("boss_id"));
				final long respawnTime = (GrandBossManager.getInstance().getStatSet(npc.getNpcId()).getLong("respawn_time") - System.currentTimeMillis());
				tmpDate.setTimeInMillis(respawnTime);
				addEpicBossList(npc.getName(), npc.getLevel(), /*tmpDate.getTimeInMillis(),  */GrandBossManager.getInstance().getBossStatus(npc.getNpcId()) == 2);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		for (int boss : BOSSES) // Estos habia que agregarlos a mano por que cambio a SpawnManager.
		{
			ASpawn spawn = SpawnManager.getInstance().getSpawn(boss);
			if (spawn != null)
			{
				tmpDate.setTimeInMillis(spawn.getRespawnDelay() * 1000);
				addEpicBossList(spawn.getTemplate().getName(), spawn.getTemplate().getLevel(), /*tmpDate.getTimeInMillis(), */spawn.getSpawnData().checkDead());
			}
		}
	}
	
	private void addEpicBossList(String name, int level, /*long respawn, */boolean isStatus)
	{
		if (name.equalsIgnoreCase("Scarlet van Halisha"))
			return;
		this.StatusEpicBoss.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		this.StatusEpicBoss.append("<tr>");
		this.StatusEpicBoss.append("<td FIXWIDTH=2></td>");
		this.StatusEpicBoss.append("<td FIXWIDTH=90>"+ name +"</td>");
		this.StatusEpicBoss.append("<td FIXWIDTH=90>"+ level +"</td>");
		
        //SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		//this.StatusEpicBoss.append("<td FIXWIDTH=90>"+ /*sdf.format(respawn)      String.valueOf*/(format.format(respawn)) +"</td>");
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