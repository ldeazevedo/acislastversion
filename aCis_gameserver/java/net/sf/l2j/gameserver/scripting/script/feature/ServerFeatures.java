/*
 * Copyright (C) 2004-2014 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.scripting.script.feature;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.ASpawn;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.script.ai.boss.Antharas;
import net.sf.l2j.gameserver.scripting.script.ai.boss.Frintezza;
import net.sf.l2j.gameserver.scripting.script.ai.boss.Valakas;

public class ServerFeatures extends Quest
{
	private static final int[] BOSSES = {29001, 29006, 29014, 29022};
	private static final String SQL_BOSS = "SELECT boss_id, respawn_time, status, name, id, level FROM grandboss_data LEFT JOIN npc ON grandboss_data.boss_id=npc.id WHERE boss_id ORDER BY grandboss_data.status DESC";
	
	private static final int ARENA_NPC = 50095;
	private static final int CUSTOM_NPC = 50096;
	private static final int BUFFER = 50012;
	
	//Npcs para shops, teleports y $
	protected static final int[][] CUSTOM_SPAWNS =
	{
		{ 146743, 25861, -2008, 843 },  			// Aden
		{ 148018, -55242, -2728, 48573 },  			// Goddard
		{ 83459, 147911, -3400, 18939 },  			// Giran
		{ 82978, 53105, -1488, 31967 },  			// Oren
		{ 111345, 219415, -3544, 46596 },  			// Heine
		{ 15567, 142902, -2696, 16384 },  			// Dion
		{ -12798, 122805, -3112, 51707 },  			// Gludio
		{ 87139, -143441, -1288, 20021 },  			// Schuttgart
		{ 43897, -47676, -792, 32768 },  			// Rune
		{ -80710, 149831, -3040, 23630 },  			// Gludin
		{ -84076, 244556, -3728, 50874 },  			// Talking Island
		{ 46883, 51514, -2976, 40960 },  			// Elven
		{ 9706, 15493, -4568, 3968 },  				// Dark Elf
		{ -45253, -112561, -240, 3355 },  			// Orc
		{ 115076, -178222, -912, 6133 },  			// Dwarven
		{ 117102, 76966, -2696, 34826 }  			// Hunter
	}; 
	
	// Npcs para el Arena Fight
	protected static final int[][] ARENA_SPAWNS =
	{
		{ 83587, 147598, -3400, 16384 },  			// Giran
		{ 148102, -55695, -2744, 27280 },  			// Goddard
		{ 147074, 25630, -2008, 10495 }  			// Aden
	};
	
	public ServerFeatures()
	{
		super(-1, "feature");

		addTalkId(ARENA_NPC);
		addTalkId(CUSTOM_NPC);
		addTalkId(BUFFER);
		addFirstTalkId(ARENA_NPC);
		addFirstTalkId(CUSTOM_NPC);
		addFirstTalkId(BUFFER);
		
		for (NpcTemplate t : NpcData.getInstance().getTemplates(t -> t.isType("RaidBoss")))
			addMyDying(t.getIdTemplate());
		
		for (int[] loc : ARENA_SPAWNS)
			addSpawn(50095, loc[0], loc[1], loc[2], loc[3], false, 0, false);
		
		for (int[] loc : CUSTOM_SPAWNS)
			addSpawn(50096, loc[0], loc[1], loc[2], loc[3], false, 0, false);
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		final Player player = killer.getActingPlayer();
		if ((player.getStatus().getLevel() >= 75) && (npc.getStatus().getLevel() < 60))
			return;
		
		int score = Rnd.get(100, 250);
		player.updatePcBangScore(score);
		super.onMyDying(npc, killer);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "teleport.htm":
				if (player.getKarma() != 0)
					return "teleport-no.htm";
				break;
			case "boss":
				generateFirstWindow(npc, player);
				return "";
		}
		
		return event;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = "";
		switch (npc.getNpcId())
		{
			case CUSTOM_NPC:
				htmltext = "main.htm";
				break;
			case ARENA_NPC:
				if (player.getKarma() != 0)
					return "arena-karma.htm";
				htmltext = "arena_main.htm";
				break;
		}
		return htmltext;
	}
	
	private void generateFirstWindow(Npc npc, Player player)
	{
		if (npc == null || player == null)
			return;
		final StringBuilder sb = new StringBuilder();
		
		try (Connection con = ConnectionPool.getConnection();
			 PreparedStatement statement = con.prepareStatement(SQL_BOSS);
			 ResultSet result = statement.executeQuery())
		{
			while (result.next()) //Tomados del GrandBoss data.
			{
				NpcTemplate npcinfo = NpcData.getInstance().getTemplate(result.getInt("boss_id"));
				long respawnTime = (GrandBossManager.getInstance().getStatSet(npcinfo.getNpcId()).getLong("respawn_time"));

				int npcid = npcinfo.getNpcId();
				boolean diffentId = npcid == Valakas.VALAKAS || npcid == Frintezza.FRINTEZZA || npcid == Antharas.ANTHARAS;
				boolean isStatus = GrandBossManager.getInstance().getBossStatus(npcid) == (diffentId ? 3 : 2);
				String name = NpcData.getInstance().getTemplate(npcid).getName().toUpperCase();
				if (name.equalsIgnoreCase("Scarlet van Halisha"))
					continue;

				if (!isStatus)
					sb.append("" + name + "&nbsp;<font color=\"FFFF00\">IS ALIVE!</font><br1>");
				else
				{
					sb.append("&nbsp;" + name + "&nbsp;<font color=\"FF0000\">IS DEAD</font><br1>");
					sb.append("Time until respawn: " + name + "&nbsp;<font color=\"b09979\">:&nbsp;" + ConverTime(respawnTime - Calendar.getInstance().getTimeInMillis()) + "</font><br1>");
				}
			}
		} catch (Exception e)
		{
			log.error("There was an error when loading bosses: " + e.getMessage());
		}
		Arrays.stream(BOSSES).forEach(boss -> {
			NpcTemplate npcinfo = NpcData.getInstance().getTemplate(boss);
			if (npcinfo == null)
				return;
			ASpawn spawn = SpawnManager.getInstance().getSpawn(boss);
	
			if (spawn != null && spawn.getSpawnData() != null)
			{
				long delay = spawn.getSpawnData().getRespawnTime();
				String name = NpcData.getInstance().getTemplate(boss).getName().toUpperCase();
				
				if (delay == 0)
					sb.append("" + name + "&nbsp;<font color=\"FFFF00\">IS ALIVE!</font><br1>");
				else if (delay > 0)
				{
					sb.append("&nbsp;" + name + "&nbsp;<font color=\"FF0000\">IS DEAD</font><br1>");
					sb.append("Time until respawn: " + name + "&nbsp;<font color=\"b09979\">:&nbsp;" + ConverTime(delay - Calendar.getInstance().getTimeInMillis()) + "</font><br1>");
				}
			}
		});
		final List<NpcTemplate> mobs = NpcData.getInstance().getTemplates(t -> t.isType("RaidBoss"));
		String mainBossInfo = "";

		for (int i = 0; i < mobs.size(); i++)
		{
			ASpawn spawn = SpawnManager.getInstance().getSpawn(mobs.get(i).getNpcId());
			if (NpcData.getInstance().getTemplate(mobs.get(i).getNpcId()) == null || spawn == null || spawn.getSpawnData() == null)
				continue;
			if ((NpcData.getInstance().getTemplate(mobs.get(i).getNpcId()).getLevel() < 75))
				continue;
			long delay = spawn.getSpawnData().getRespawnTime();
			String name = NpcData.getInstance().getTemplate(mobs.get(i).getNpcId()).getName().toUpperCase();
			
			if (delay == 0)
				mainBossInfo += ("" + name + "&nbsp;<font color=\"FFFF00\">IS ALIVE!</font><br1>");
			else if (delay > 0)
			{
				mainBossInfo += ("&nbsp;" + name + "&nbsp;<font color=\"FF0000\">IS DEAD</font><br1>");
				mainBossInfo += ("Time until respawn: " +name+ "&nbsp;<font color=\"b09979\">:&nbsp;" + ConverTime(delay - Calendar.getInstance().getTimeInMillis()) + "</font><br1>");
			}
		}
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("./data/html/script/" + getDescr() + "/" + getName() + "/" + "boss.htm");
		html.replace("%objectId%", npc.getObjectId());
		html.replace("%bosslist%", sb.toString());
		html.replace("%mboss%", mainBossInfo);
		player.sendPacket(html);
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
}