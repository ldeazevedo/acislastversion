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

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.scripting.Quest;

public class ServerFeatures extends Quest
{
	private static final int ARENA_NPC = 50095;
	private static final int CUSTOM_NPC = 50096;
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
		{ 83587, 147598, -3400, 16384 },
		{ 148102, -55695, -2744, 27280 },
		{ 147074, 25630, -2008, 10495 }
	};
	
	public ServerFeatures()
	{
		super(-1, "feature");

		addTalkId(ARENA_NPC);
		addTalkId(CUSTOM_NPC);
		addFirstTalkId(ARENA_NPC);
		addFirstTalkId(CUSTOM_NPC);
		
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
}