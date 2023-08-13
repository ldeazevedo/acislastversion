package net.sf.l2j.gameserver.communitybbs.module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.ASpawn;

public class EpicBossStatus
{
	protected static final Logger log = Logger.getLogger(EpicBossStatus.class.getName());
	private final StringBuilder html = new StringBuilder();
	private static final String SQL_BOSS = "SELECT boss_id, respawn_time, status, name, id, level FROM grandboss_data LEFT JOIN npc ON grandboss_data.boss_id=npc.id WHERE boss_id ORDER BY grandboss_data.status DESC";
	private static final int[] BOSSES = {29001, 29006, 29014, 29022};

	public EpicBossStatus()
	{
		loadFromDB();
	}
	
	private void loadFromDB()
	{
		Calendar tmpDate = Calendar.getInstance();
		try (Connection con = ConnectionPool.getConnection();
			 PreparedStatement statement = con.prepareStatement(SQL_BOSS);
			 ResultSet result = statement.executeQuery())
		{
			while (result.next()) //Tomados del GrandBoss data.
			{
				NpcTemplate npc = NpcData.getInstance().getTemplate(result.getInt("boss_id"));
				final long respawnTime = (GrandBossManager.getInstance().getStatSet(npc.getNpcId()).getLong("respawn_time") - System.currentTimeMillis());
				tmpDate.setTimeInMillis(respawnTime);
				//populateHTML(npc.getName(), npc.getLevel(), GrandBossManager.getInstance().getBossStatus(npc.getNpcId()) == 2);
				populateHTML(npc.getName(), npc.getLevel(), GrandBossManager.getInstance().getStatSet(npc.getNpcId()).getLong("respawn_time"), GrandBossManager.getInstance().getBossStatus(npc.getNpcId()) == 2);
			}
		} catch (Exception e)
		{
			log.severe("There was an error when loading bosses: " + e.getMessage());
		}
		Arrays.stream(BOSSES).forEach(boss -> {
			ASpawn spawn = SpawnManager.getInstance().getSpawn(boss);
			if (spawn != null)
				populateHTML(spawn.getTemplate().getName(), spawn.getTemplate().getLevel(), spawn.getSpawnData().getRespawnTime(), spawn.getSpawnData().checkDead());
		});
		List<NpcTemplate> mobs = NpcData.getInstance().getTemplates(t -> t.isType("RaidBoss")).stream().sorted(Comparator.comparingInt(p -> p.getLevel())).collect(Collectors.toList());
		
		for (int i = 0; i < mobs.size(); i++)
		{
			ASpawn spawn = SpawnManager.getInstance().getSpawn(mobs.get(i).getNpcId());
			if (NpcData.getInstance().getTemplate(mobs.get(i).getNpcId()) == null || spawn == null || spawn.getSpawnData() == null)
				continue;
			if ((NpcData.getInstance().getTemplate(mobs.get(i).getNpcId()).getLevel() < 75))
				continue;
			populateHTML(spawn.getTemplate().getName(), spawn.getTemplate().getLevel(), spawn.getSpawnData().getRespawnTime(), spawn.getSpawnData().checkDead());
		}
		/*
		Arrays.stream(BOSSES).forEach(boss -> {
			ASpawn spawn = SpawnManager.getInstance().getSpawn(boss);
			if (spawn != null)
				populateHTML(spawn.getTemplate().getName(), spawn.getTemplate().getLevel(), spawn.getSpawnData().getRespawnTime(), spawn.getSpawnData().checkDead());
		});*/
	}
	
	private void populateHTML(String name, int level, long delay, boolean isStatus)
	{
		if (name.equalsIgnoreCase("Scarlet van Halisha"))
			return;
		html.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		html.append("<tr>");
		html.append("<td FIXWIDTH=2></td>");
		html.append("<td FIXWIDTH=90>").append(name).append("</td>");
		html.append("<td FIXWIDTH=90>").append(level).append("</td>");
		html.append("<td FIXWIDTH=90>").append(((isStatus) ? "<font color=CC0000≯Dead</font>" : "<font color=00FF00>Alive</font>")).append("</td>");
		html.append("<td FIXWIDTH=90>").append(((isStatus) ? ConverTime(delay - Calendar.getInstance().getTimeInMillis()) : 0)).append("</td>");
		html.append("<td FIXWIDTH=2></td>");
		html.append("</tr></table>");
		html.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}
	
	private static String ConverTime(long mseconds)
	{
		long remainder = mseconds;
		
		long hours = (long) Math.ceil((mseconds / (60 * 60 * 1000)));
		remainder = mseconds - (hours * 60 * 60 * 1000);
		
		long minutes = (long) Math.ceil((remainder / (60 * 1000)));
		remainder = remainder - (minutes * (60 * 1000));
		
		long seconds = (long) Math.ceil((remainder / 1000));
		
		return hours + ":" + minutes + ":" + seconds;
	}

	public String getInHTMLFormat()
	{
		return html.toString();
	}
}