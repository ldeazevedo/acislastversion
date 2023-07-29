/*
 * Copyright © 2004-2023 L2J DataPack
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
package net.sf.l2j.gameserver.scripting.script.instances;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.InstanceManager;
import net.sf.l2j.gameserver.data.manager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.L2Skill;

public final class Kamaloka extends Quest
{
	/*
	 * Reset time for all kamaloka Default: 6:30AM on server time
	 */
	private static final int RESET_HOUR = 6;
	private static final int RESET_MIN = 30;

	/*
	 * Time after which instance without players will be destroyed Default: 5
	 * minutes
	 */
	private static final int EMPTY_DESTROY_TIME = 5;

	/*
	 * Time to destroy instance (and eject players away) after boss defeat Default:
	 * 5 minutes
	 */
	private static final int EXIT_TIME = 5;

	/*
	 * Maximum level difference between players level and kamaloka level Default: 5
	 */
	private static final int MAX_LEVEL_DIFFERENCE = 5;

	/*
	 * If true shaman in the first room will have same npcId as other mobs, making
	 * radar useless Default: true (but not retail like)
	 */
	private static final boolean STEALTH_SHAMAN = true;
	// Template IDs for Kamaloka
	// @formatter:off
	private static final int[] TEMPLATE_IDS =
	{
		57, 58, 73, 60, 61, 74, 63, 64, 75, 66, 67, 76, 69, 70, 77, 72, 78, 79, 134
	};
	// Level of the Kamaloka
	private static final int[] LEVEL =
	{
		23, 26, 29, 33, 36, 39, 43, 46, 49, 53, 56, 59, 63, 66, 69, 73, 78, 81, 83
	};
	// Duration of the instance, minutes
	private static final int[] DURATION =
	{
		30, 30, 45, 30, 30, 45, 30, 30, 45, 30, 30, 45, 30, 30, 45, 30, 45, 45, 45
	};
	// Maximum party size for the instance
	private static final int[] MAX_PARTY_SIZE =
	{
		6, 6, 9, 6, 6, 9, 6, 6, 9, 6, 6, 9, 6, 6, 9, 6, 9, 9, 9
	};
	
	/**
	 * List of buffs NOT removed on enter from player and pet<br>
	 * On retail only newbie guide buffs not removed<br>
	 * CAUTION: array must be sorted in ascension order!
	 */
	protected static final int[] BUFFS_WHITELIST =
	{
		4322, 4323, 4324, 4325, 4326, 4327, 4328, 4329, 4330, 4331, 5632, 5637, 5950
	};
	// @formatter:on
	// Teleport points into instances x, y, z
	private static final Location[] TELEPORTS = { 
			new Location(-88429, -220629, -7903),
			new Location(-82464, -219532, -7899), 
			new Location(-10700, -174882, -10936), // -76280, -185540, -10936
			new Location(-89683, -213573, -8106), 
			new Location(-81413, -213568, -8104),
			new Location(-10700, -174882, -10936), // -76280, -174905, -10936
			new Location(-89759, -206143, -8120), 
			new Location(-81415, -206078, -8107),
			new Location(-10700, -174882, -10936), 
			new Location(-56999, -219856, -8117),
			new Location(-48794, -220261, -8075), 
			new Location(-10700, -174882, -10936),
			new Location(-56940, -212939, -8072), 
			new Location(-55566, -206139, -8120),
			new Location(-10700, -174882, -10936), 
			new Location(-49805, -206139, -8117),
			new Location(-10700, -174882, -10936), 
			new Location(-10700, -174882, -10936),
			new Location(22003, -174886, -10900), };

	// Respawn delay for the mobs in the first room, seconds Default: 25
	private static final int FIRST_ROOM_RESPAWN_DELAY = 25;

	/**
	 * First room information, null if room not spawned.<br>
	 * Skill is casted on the boss when shaman is defeated and mobs respawn
	 * stopped<br>
	 * Default: 5699 (decrease pdef)<br>
	 * shaman npcId, minions npcId, skillId, skillLvl
	 */
	private static final int[][] FIRST_ROOM = { null, null, 
			{ 22485, 22486, 5699, 1 }, null, null,
			{ 22488, 22489, 5699, 2 }, null, null, 
			{ 22491, 22492, 5699, 3 }, null, null, 
			{ 22494, 22495, 5699, 4 }, null, null, 
			{ 22497, 22498, 5699, 5 }, null, 
			{ 22500, 22501, 5699, 6 }, 
			{ 22503, 22504, 5699, 7 },
			{ 25706, 25707, 5699, 7 } };

	/*
	 * First room spawns, null if room not spawned x, y, z
	 */
	private static final int[][][] FIRST_ROOM_SPAWNS = { null, null, { 
			{ -12381, -174973, -10955 }, 
			{ -12413, -174905, -10955 }, 
			{ -12377, -174838, -10953 },
			{ -12316, -174903, -10953 }, 
			{ -12326, -174786, -10953 }, 
			{ -12330, -175024, -10953 },
			{ -12211, -174900, -10955 }, 
			{ -12238, -174849, -10953 }, 
			{ -12233, -174954, -10953 } }, null, null, { 
			{ -12381, -174973, -10955 }, 
			{ -12413, -174905, -10955 }, 
			{ -12377, -174838, -10953 },
			{ -12316, -174903, -10953 }, 
			{ -12326, -174786, -10953 }, 
			{ -12330, -175024, -10953 },
			{ -12211, -174900, -10955 }, 
			{ -12238, -174849, -10953 }, 
			{ -12233, -174954, -10953 } }, null, null, { 
			{ -12381, -174973, -10955 }, 
			{ -12413, -174905, -10955 }, 
			{ -12377, -174838, -10953 },
			{ -12316, -174903, -10953 }, 
			{ -12326, -174786, -10953 }, 
			{ -12330, -175024, -10953 },
			{ -12211, -174900, -10955 }, 
			{ -12238, -174849, -10953 }, 
			{ -12233, -174954, -10953 } }, null, null, { 
			{ -12381, -174973, -10955 }, 
			{ -12413, -174905, -10955 }, 
			{ -12377, -174838, -10953 },
			{ -12316, -174903, -10953 }, 
			{ -12326, -174786, -10953 }, 
			{ -12330, -175024, -10953 },
			{ -12211, -174900, -10955 }, 
			{ -12238, -174849, -10953 }, 
			{ -12233, -174954, -10953 } }, null, null, { 
			{ -12381, -174973, -10955 }, 
			{ -12413, -174905, -10955 }, 
			{ -12377, -174838, -10953 },
			{ -12316, -174903, -10953 }, 
			{ -12326, -174786, -10953 }, 
			{ -12330, -175024, -10953 },
			{ -12211, -174900, -10955 }, 
			{ -12238, -174849, -10953 }, 
			{ -12233, -174954, -10953 } }, null, { 
			{ -12381, -174973, -10955 }, 
			{ -12413, -174905, -10955 }, 
			{ -12377, -174838, -10953 },
			{ -12316, -174903, -10953 }, 
			{ -12326, -174786, -10953 }, 
			{ -12330, -175024, -10953 },
			{ -12211, -174900, -10955 }, 
			{ -12238, -174849, -10953 }, 
			{ -12233, -174954, -10953 } }, { 
			{ -12381, -174973, -10955 }, 
			{ -12413, -174905, -10955 }, 
			{ -12377, -174838, -10953 },
			{ -12316, -174903, -10953 }, 
			{ -12326, -174786, -10953 }, 
			{ -12330, -175024, -10953 },
			{ -12211, -174900, -10955 }, 
			{ -12238, -174849, -10953 }, 
			{ -12233, -174954, -10953 } }, { 
			{ 20409, -174827, -10912 }, 
			{ 20409, -174947, -10912 }, 
			{ 20494, -174887, -10912 },
			{ 20494, -174767, -10912 }, 
			{ 20614, -174887, -10912 }, 
			{ 20579, -174827, -10912 },
			{ 20579, -174947, -10912 }, 
			{ 20494, -175007, -10912 }, 
			{ 20374, -174887, -10912 } } };

	/*
	 * Second room information, null if room not spawned Skill is casted on the boss
	 * when all mobs are defeated Default: 5700 (decrease mdef) npcId, skillId,
	 * skillLvl
	 */
	private static final int[][] SECOND_ROOM = { null, null, 
			{ 22487, 5700, 1 }, null, null, 
			{ 22490, 5700, 2 }, null, null, 
			{ 22493, 5700, 3 }, null, null, 
			{ 22496, 5700, 4 }, null, null, 
			{ 22499, 5700, 5 }, null,
			{ 22502, 5700, 6 }, 
			{ 22505, 5700, 7 }, 
			{ 25708, 5700, 7 }

	};

	/*
	 * Spawns for second room, null if room not spawned x, y, z
	 */
	private static final int[][][] SECOND_ROOM_SPAWNS = { null, null,
			{ 
		{ -14547, -174901, -10690 }, 
		{ -14543, -175030, -10690 }, 
		{ -14668, -174900, -10690 },
		{ -14538, -174774, -10690 }, 
		{ -14410, -174904, -10690 } }, null, null, { 
		{ -14547, -174901, -10690 }, 
		{ -14543, -175030, -10690 }, 
		{ -14668, -174900, -10690 },
		{ -14538, -174774, -10690 }, 
		{ -14410, -174904, -10690 } }, null, null, { 
		{ -14547, -174901, -10690 }, 
		{ -14543, -175030, -10690 }, 
		{ -14668, -174900, -10690 },
		{ -14538, -174774, -10690 }, 
		{ -14410, -174904, -10690 } }, null, null, { 
		{ -14547, -174901, -10690 }, 
		{ -14543, -175030, -10690 }, 
		{ -14668, -174900, -10690 },
		{ -14538, -174774, -10690 }, 
		{ -14410, -174904, -10690 } }, null, null, { 
		{ -14547, -174901, -10690 }, 
		{ -14543, -175030, -10690 }, 
		{ -14668, -174900, -10690 },
		{ -14538, -174774, -10690 }, 
		{ -14410, -174904, -10690 } }, null, { 
		{ -14547, -174901, -10690 }, 
		{ -14543, -175030, -10690 }, 
		{ -14668, -174900, -10690 },
		{ -14538, -174774, -10690 }, 
		{ -14410, -174904, -10690 } }, { 
		{ -14547, -174901, -10690 }, 
		{ -14543, -175030, -10690 }, 
		{ -14668, -174900, -10690 },
		{ -14538, -174774, -10690 }, 
		{ -14410, -174904, -10690 } }, { 
		{ 18175, -174991, -10653 }, 
		{ 18070, -174890, -10655 }, 
		{ 18157, -174886, -10655 },
		{ 18249, -174885, -10653 }, 
		{ 18144, -174821, -10648 } } };

	// miniboss info
	// skill is casted on the boss when miniboss is defeated
	// npcId, x, y, z, skill id, skill level
	/*
	 * Miniboss information, null if miniboss not spawned Skill is casted on the
	 * boss when miniboss is defeated Default: 5701 (decrease patk) npcId, x, y, z,
	 * skillId, skillLvl
	 */
	private static final int[][] MINIBOSS = { null, null, 
			{ 25616, -16874, -174900, -10427, 5701, 1 }, null, null,
			{ 25617, -16874, -174900, -10427, 5701, 2 }, null, null, 
			{ 25618, -16874, -174900, -10427, 5701, 3 }, null, null, 
			{ 25619, -16874, -174900, -10427, 5701, 4 }, null, null, 
			{ 25620, -16874, -174900, -10427, 5701, 5 }, null, 
			{ 25621, -16874, -174900, -10427, 5701, 6 }, 
			{ 25622, -16874, -174900, -10427, 5701, 7 },
			{ 25709, 15828, -174885, -10384, 5701, 7 } };

	/*
	 * Bosses of the kamaloka Instance ends when boss is defeated npcId, x, y, z
	 */
	private static final int[][] BOSS = { 
			{ 18554, -88998, -220077, -7892 }, 
			{ 18555, -81891, -220078, -7893 },
			{ 29129, -20659, -174903, -9983 }, 
			{ 18558, -89183, -213564, -8100 }, 
			{ 18559, -81937, -213566, -8100 },
			{ 29132, -20659, -174903, -9983 }, 
			{ 18562, -89054, -206144, -8115 }, 
			{ 18564, -81937, -206077, -8100 },
			{ 29135, -20659, -174903, -9983 }, 
			{ 18566, -56281, -219859, -8115 }, 
			{ 18568, -49336, -220260, -8068 },
			{ 29138, -20659, -174903, -9983 }, 
			{ 18571, -56415, -212939, -8068 }, 
			{ 18573, -56281, -206140, -8115 },
			{ 29141, -20659, -174903, -9983 }, 
			{ 18577, -49084, -206140, -8115 }, 
			{ 29144, -20659, -174903, -9983 },
			{ 29147, -20659, -174903, -9983 }, 
			{ 25710, 12047, -174887, -9944 } };

	/*
	 * Escape telepoters spawns, null if not spawned x, y, z
	 */
	private static final int[][] TELEPORTERS = { null, null, 
			{ -10865, -174905, -10944 }, null, null,
			{ -10865, -174905, -10944 }, null, null, 
			{ -10865, -174905, -10944 }, null, null,
			{ -10865, -174905, -10944 }, null, null, 
			{ -10865, -174905, -10944 }, null, 
			{ -10865, -174905, -10944 },
			{ -10865, -174905, -10944 }, 
			{ 21837, -174885, -10904 } };

	/*
	 * Escape teleporter npcId
	 */
	private static final int TELEPORTER = 32496;

	/** Kamaloka captains (start npc's) npcIds. */
	private static final int[] CAPTAINS = { 30332, 30071, 30916, 30196, 31981, 31340 };

	protected class KamaWorld extends InstanceWorld
	{
		public int index; // 0-18 index of the kama type in arrays
		public int shaman = 0; // objectId of the shaman
		public List<Spawn> firstRoom; // list of the spawns in the first room (excluding shaman)
		public List<Integer> secondRoom;// list of objectIds mobs in the second room
		public int miniBoss = 0; // objectId of the miniboss
		public Npc boss = null; // boss

		public KamaWorld()
		{
			InstanceManager.getInstance().super();
		}
	}

	public Kamaloka()
	{
		super(-1, "instances");
		// super(Kamaloka.class.getSimpleName());

		addFirstTalkId(TELEPORTER);
		addTalkId(TELEPORTER);
		// addStartNpc(CAPTAINS);
		addTalkId(CAPTAINS);

		addTalkId(30009, 30019, 30131, 30400, 30530, 30575, 30008, 30017, 30129, 30370, 30528, 30573, 30598, 30599, 30600, 30601, 30602, 31076, 31077);
		addFirstTalkId(30009, 30019, 30131, 30400, 30530, 30575, 30008, 30017, 30129, 30370, 30528, 30573, 30598, 30599, 30600, 30601, 30602, 31076, 31077);

		addMyDying(18342);

		for (int[] mob : FIRST_ROOM)
			if (mob != null)
				if (STEALTH_SHAMAN)
					addMyDying(mob[1]);
				else
					addMyDying(mob[0]);
		for (int[] mob : SECOND_ROOM)
			if (mob != null)
				addMyDying(mob[0]);
		for (int[] mob : MINIBOSS)
			if (mob != null)
				addMyDying(mob[0]);
		for (int[] mob : BOSS)
			addMyDying(mob[0]);
	}

	/**
	 * Check if party with player as leader allowed to enter
	 * 
	 * @param player party leader
	 * @param index  (0-18) index of the kamaloka in arrays
	 * @return true if party allowed to enter
	 */
	private static boolean checkPartyConditions(Player player, int index)
	{
		final Party party = player.getParty();
		// player must be in party
		if (party == null)
		{
			// player.sendPacket(SystemMessageId.NOT_IN_PARTY_CANT_ENTER);
			player.sendMessage("Not in party cant enter");
			return false;
		}
		// ...and be party leader
		if (party.getLeader() != player)
		{
			// player.sendPacket(SystemMessageId.ONLY_PARTY_LEADER_CAN_ENTER);
			player.sendMessage("Only party leader can enter");
			return false;
		}
		// party must not exceed max size for selected instance
		if (party.getMembersCount() > MAX_PARTY_SIZE[index])
		{
			// player.sendPacket(SystemMessageId.PARTY_EXCEEDED_THE_LIMIT_CANT_ENTER);
			player.sendMessage("Only party leader can enter");
			return false;
		}

		// get level of the instance
		final int level = LEVEL[index];
		// and client name
		final String instanceName = InstanceManager.getInstanceIdName(TEMPLATE_IDS[index]);

		Map<Integer, Long> instanceTimes;
		// for each party member
		for (Player partyMember : party.getMembers())
		{
			// player level must be in range
			if (Math.abs(partyMember.getStatus().getLevel() - level) > MAX_LEVEL_DIFFERENCE)
			{
				// SystemMessage sm =
				// SystemMessage.getSystemMessage(SystemMessageId.C1_S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED);
				// sm.addPcName(partyMember);
				// player.sendPacket(sm); //TODO:
				return false;
			}
			// player must be near party leader
			if (!isInsideRadius(partyMember, player, 1000, true, true))
			{
				// TODO: SystemMessage sm =
				// SystemMessage.getSystemMessage(SystemMessageId.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED);
				// sm.addPcName(partyMember);
				// player.sendPacket(sm);
				return false;
			}
			// get instances reenter times for player
			instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(partyMember.getObjectId());
			if (instanceTimes != null)
			{
				for (int id : instanceTimes.keySet())
				{
					// find instance with same name (kamaloka or labyrinth)
					// TODO: Zoey76: Don't use instance name, use other system.
					if (!instanceName.equals(InstanceManager.getInstanceIdName(id)))
					{
						continue;
					}
					// if found instance still can't be reentered - exit
					if (System.currentTimeMillis() < instanceTimes.get(id))
					{
						// SystemMessage sm =
						// SystemMessage.getSystemMessage(SystemMessageId.C1_MAY_NOT_RE_ENTER_YET);
						// sm.addPcName(partyMember);
						// player.sendPacket(sm); //TODO:
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Removing all buffs from player and pet except BUFFS_WHITELIST
	 * 
	 * @param ch player
	 */
	private static void removeBuffs(Creature ch)
	{
	//	  final Function<L2Skill, Boolean> removeBuffs = info ->
	//	  { 
	//		  if ((info != null) && !info.getSkill().isStayAfterDeath() && (Arrays.binarySearch(BUFFS_WHITELIST, info.getSkill().getId()) < 0))
	//		  {
	//			  info.getEffected().getEffectList().stopSkillEffects(true, info.getSkill());
	//			  return true;
	//		  }
	//		  return false;
	//	  };
		  
	//	  ch.getEffectList().forEach(removeBuffs, false);
		  
	//	  if (ch.getSummon() != null)
	//		  ch.getSummon().getEffectList().forEach(removeBuffs, false); 
		  
	}

	/**
	 * Handling enter of the players into kamaloka
	 * 
	 * @param player party leader
	 * @param index  (0-18) kamaloka index in arrays
	 */
	private synchronized void enterInstance(Player player, int index)
	{
		int templateId;
		try
		{
			templateId = TEMPLATE_IDS[index];
		} catch (ArrayIndexOutOfBoundsException e)
		{
			throw e;
		}

		// check for existing instances for this player
		InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		// player already in the instance
		if (world != null)
		{
			// but not in kamaloka
			if (!(world instanceof KamaWorld) || (world.getTemplateId() != templateId))
			{
				// player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANT_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON);
				player.sendMessage(
						"You have entered another instant zone therefore you cannot enter corresponding dungeon");
				return;
			}
			// check for level difference again on reenter
			if (Math.abs(player.getStatus().getLevel() - LEVEL[((KamaWorld) world).index]) > MAX_LEVEL_DIFFERENCE)
			{
				// SystemMessage sm =
				// SystemMessage.getSystemMessage(SystemMessageId.C1_S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED);
				// sm.addPcName(player);
				// player.sendPacket(sm); //TODO:
				return;
			}
			// check what instance still exist
			Instance inst = InstanceManager.getInstance(world.getInstanceId());
			if (inst != null)
			{
				removeBuffs(player);
				teleportPlayer(player, TELEPORTS[index], world.getInstanceId());
			}
			return;
		}
		// Creating new kamaloka instance
		if (!checkPartyConditions(player, index))
		{
			return;
		}

		// Creating dynamic instance without template
		final int instanceId = InstanceManager.getInstance().createDynamicInstance(null);
		final Instance inst = InstanceManager.getInstance(instanceId);
		// set name for the kamaloka
		inst.setName(InstanceManager.getInstanceIdName(templateId));
		// set return location
		inst.setExitLoc(new Location(player));
		// disable summon friend into instance
		inst.setAllowSummon(false);
		// set duration and empty destroy time
		inst.setDuration(DURATION[index] * 60000);
		inst.setEmptyDestroyTime(EMPTY_DESTROY_TIME * 60000);

		// Creating new instanceWorld, using our instanceId and templateId
		world = new KamaWorld();
		world.setInstanceId(instanceId);
		world.setTemplateId(templateId);
		// set index for easy access to the arrays
		((KamaWorld) world).index = index;
		InstanceManager.getInstance().addWorld(world);
		world.setStatus(0);
		// spawn npcs
		spawnKama((KamaWorld) world);

		// and finally teleport party into instance
		final Party party = player.getParty();
		for (Player partyMember : party.getMembers())
		{
			world.addAllowed(partyMember.getObjectId());
			removeBuffs(partyMember);
			teleportPlayer(partyMember, TELEPORTS[index], instanceId);
		}
		return;
	}

	/**
	 * Called on instance finish and handles reenter time for instance
	 * 
	 * @param world instanceWorld
	 */
	protected static void finishInstance(InstanceWorld world)
	{
		if (world instanceof KamaWorld)
		{
			Calendar reenter = Calendar.getInstance();
			reenter.set(Calendar.MINUTE, RESET_MIN);
			// if time is >= RESET_HOUR - roll to the next day
			if (reenter.get(Calendar.HOUR_OF_DAY) >= RESET_HOUR)
				reenter.add(Calendar.DATE, 1);
			
			reenter.set(Calendar.HOUR_OF_DAY, RESET_HOUR);

			// SystemMessage sm =
			// SystemMessage.getSystemMessage(SystemMessageId.INSTANT_ZONE_FROM_HERE_S1_S_ENTRY_HAS_BEEN_RESTRICTED);
			// sm.addInstanceName(world.getTemplateId());
			String smm = "Instance zone from here " + world.getTemplateId() + " entry has been restricted";

			// set instance reenter time for all allowed players
			for (int objectId : world.getAllowed())
			{
				Player obj = World.getInstance().getPlayer(objectId);
				if ((obj != null) && obj.isOnline())
				{
					InstanceManager.getInstance().setInstanceTime(objectId, world.getTemplateId(), reenter.getTimeInMillis());
					// obj.sendPacket(sm);
					obj.sendMessage(smm);
				}
			}

			// destroy instance after EXIT_TIME
			Instance inst = InstanceManager.getInstance(world.getInstanceId());
			inst.setDuration(EXIT_TIME * 60000);
			inst.setEmptyDestroyTime(0);
		}
	}

	/**
	 * Spawn all NPCs in kamaloka
	 * 
	 * @param world instanceWorld
	 */
	private void spawnKama(KamaWorld world)
	{
		int[] npcs;
		int[][] spawns;
		Npc npc;
		final int index = world.index;

		// first room
		npcs = FIRST_ROOM[index];
		spawns = FIRST_ROOM_SPAWNS[index];
		if (npcs != null)
		{
			world.firstRoom = new ArrayList<>(spawns.length - 1);
			int shaman = Rnd.get(spawns.length); // random position for shaman

			for (int i = 0; i < spawns.length; i++)
			{
				if (i == shaman)
				{
					// stealth shaman use same npcId as other mobs
					npc = addSpawn(STEALTH_SHAMAN ? npcs[1] : npcs[0], spawns[i][0], spawns[i][1], spawns[i][2], 0,
							false, 0, false, world.getInstanceId());
					world.shaman = npc.getObjectId();
				} else
				{
					npc = addSpawn(npcs[1], spawns[i][0], spawns[i][1], spawns[i][2], 0, false, 0, false,
							world.getInstanceId());
					Spawn spawn = (Spawn) npc.getSpawn();
					spawn.setRespawnDelay(FIRST_ROOM_RESPAWN_DELAY);
					// spawn.setAmount(1);
					spawn.setRespawnState(true);// .startRespawn();
					world.firstRoom.add(spawn); // store mobs spawns
				}
				npc.moveUsingRandomOffset(20);// .setIsNoRndWalk(true);
			}
		}

		// second room
		npcs = SECOND_ROOM[index];
		spawns = SECOND_ROOM_SPAWNS[index];
		if (npcs != null)
		{
			world.secondRoom = new ArrayList<>(spawns.length);

			for (int[] spawn : spawns)
			{
				npc = addSpawn(npcs[0], spawn[0], spawn[1], spawn[2], 0, false, 0, false, world.getInstanceId());
				npc.moveUsingRandomOffset(20);// .setIsNoRndWalk(true);
				world.secondRoom.add(npc.getObjectId());
			}
		}

		// miniboss
		if (MINIBOSS[index] != null)
		{
			npc = addSpawn(MINIBOSS[index][0], MINIBOSS[index][1], MINIBOSS[index][2], MINIBOSS[index][3], 0, false, 0, false, world.getInstanceId());
			npc.moveUsingRandomOffset(20);// .setIsNoRndWalk(true);
			world.miniBoss = npc.getObjectId();
		}

		// escape teleporter
		if (TELEPORTERS[index] != null)
			addSpawn(TELEPORTER, TELEPORTERS[index][0], TELEPORTERS[index][1], TELEPORTERS[index][2], 0, false, 0, false, world.getInstanceId());

		// boss
		npc = addSpawn(BOSS[index][0], BOSS[index][1], BOSS[index][2], BOSS[index][3], 0, false, 0, false,
				world.getInstanceId());
		// ((Monster) npc).setOnKillDelay(100);
		world.boss = npc;
	}

	/**
	 * Handles only player's enter, single parameter - integer kamaloka index
	 */
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (npc == null)
			return "";
		
		try
		{
			enterInstance(player, Integer.parseInt(event));
		} catch (Exception e)
		{
			log.error(Level.WARNING, "", e);
		}
		return "";
	}

	/**
	 * Talk with captains and using of the escape teleporter
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final int npcId = npc.getNpcId();

		if (npcId == TELEPORTER)
		{
			final Party party = player.getParty();
			// only party leader can talk with escape teleporter
			if ((party != null) && party.isLeader(player))
			{
				final InstanceWorld world = InstanceManager.getInstance().getWorld(npc.getInstanceId());
				if (world instanceof KamaWorld)
				{
					// party members must be in the instance
					if (world.isAllowed(player.getObjectId()))
					{
						Instance inst = InstanceManager.getInstance(world.getInstanceId());

						// teleports entire party away
						for (Player partyMember : party.getMembers())
							if ((partyMember != null) && (partyMember.getInstanceId() == world.getInstanceId()))
								teleportPlayer(partyMember, inst.getExitLoc(), 0);
					}
				}
			}
		} else
			return npcId + ".htm";

		return "";
	}

	/**
	 * @param p
	 * @param exitLoc
	 * @param i
	 */
	@Override
	public void teleportPlayer(Player p, Location exitLoc, int i)
	{
		p.teleportTo(exitLoc, i);
	}

	/**
	 * Only escape teleporters first talk handled
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getNpcId() == TELEPORTER)
		{
			if (player.isInParty() && player.getParty().isLeader(player))
				return "32496.htm";
			return "32496-no.htm";
		}
		return "";
	}

	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		final InstanceWorld tmpWorld = InstanceManager.getInstance().getWorld(npc.getInstanceId());
		if (tmpWorld instanceof KamaWorld)
		{
			final KamaWorld world = (KamaWorld) tmpWorld;
			final int objectId = npc.getObjectId();

			// first room was spawned ?
			if (world.firstRoom != null)
			{
				// is shaman killed ?
				if ((world.shaman != 0) && (world.shaman == objectId))
				{
					world.shaman = 0;
					// stop respawn of the minions
					for (Spawn spawn : world.firstRoom)
						if (spawn != null)
							spawn.setRespawnState(false);// .stopRespawn();
					world.firstRoom.clear();
					world.firstRoom = null;

					if (world.boss != null)
					{
						final int skillId = FIRST_ROOM[world.index][2];
						final int skillLvl = FIRST_ROOM[world.index][3];
						if ((skillId != 0) && (skillLvl != 0))
						{
							final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
							if (skill != null)
								skill.getEffects(world.boss, world.boss);
						}
					}

					super.onMyDying(npc, killer);
				}
			}

			// second room was spawned ?
			if (world.secondRoom != null)
			{
				boolean all = true;
				// check for all mobs in the second room
				for (int i = 0; i < world.secondRoom.size(); i++)
				{
					// found killed now mob
					if (world.secondRoom.get(i) == objectId)
						world.secondRoom.set(i, 0);
					 else if (world.secondRoom.get(i) != 0)
						all = false;
				}
				// all mobs killed ?
				if (all)
				{
					world.secondRoom.clear();
					world.secondRoom = null;

					if (world.boss != null)
					{
						final int skillId = SECOND_ROOM[world.index][1];
						final int skillLvl = SECOND_ROOM[world.index][2];
						if ((skillId != 0) && (skillLvl != 0))
						{
							final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
							if (skill != null)
								skill.getEffects(world.boss, world.boss);
						}
					}

					super.onMyDying(npc, killer);
				}
			}

			// miniboss spawned ?
			if ((world.miniBoss != 0) && (world.miniBoss == objectId))
			{
				world.miniBoss = 0;

				if (world.boss != null)
				{
					final int skillId = MINIBOSS[world.index][4];
					final int skillLvl = MINIBOSS[world.index][5];
					if ((skillId != 0) && (skillLvl != 0))
					{
						final L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
						if (skill != null)
							skill.getEffects(world.boss, world.boss);
					}
				}

				super.onMyDying(npc, killer);
			}

			// boss was killed, finish instance
			if ((world.boss != null) && (world.boss == npc))
			{
				world.boss = null;
				finishInstance(world);
			}
		}
		super.onMyDying(npc, killer);
	}

	public final static boolean isInsideRadius(WorldObject object, int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		double dx = x - object.getX();
		double dy = y - object.getY();
		double dz = z - object.getZ();

		if (strictCheck)
		{
			if (checkZ)
				return (dx * dx + dy * dy + dz * dz) < radius * radius;
			return (dx * dx + dy * dy) < radius * radius;
		}
		if (checkZ)
			return (dx * dx + dy * dy + dz * dz) <= radius * radius;
		return (dx * dx + dy * dy) <= radius * radius;
	}

	public final static boolean isInsideRadius(WorldObject obj, WorldObject object, int radius, boolean checkZ, boolean strictCheck)
	{
		return isInsideRadius(obj, object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}
}
/*








loc inicial teleport 1ra kama
	-76433, -185542, -11000, 33009

primera sala centro
	-77770, -185542, -11008, 65444
primera sala derecha
	-77774, -185898, -10992, 15990
primera sala izquierda
	-77775, -185177, -10984, 49041

centro segunda sala
	-80106, -185542, -10744, 2555
derecha
	-80111, -185912, -10728, 16731
izquierda
	-80111, -185190, -10728, 49122

tercera sala centro
	-82430, -185545, -10480, 64314
derecha
	-82441, -185891, -10464, 16384
izquierda
	-82443, -185185, -10464, 49190

sala boss
centro
	-86217, -185544, -10040, 5636
derecha
	-86222, -185943, -10056, 16418
izquierda
	-86224, -185139, -10056, 49185




-------------------------------------------------------------------
##############################################################################################################

loc inicial teleport 2da kama
	-76443, -174918, -11000, 31470

sala centro 1ra
	-77763, -174920, -11008, 800
derecha
	-77776, -175276, -10992, 16384
izquierda
	-77777, -174562, -10992, 48982

sala centro 2da
	-80106, -174922, -10744, 65346
	
derecha
	-80107, -175279, -10728, 16384
	
izquierda
	-80110, -174570, -10728, 49231

sala centro 3ra
	-82443, -174917, -10480, 366
derecha
	-82444, -175271, -10464, 21220
izquierda
	-82441, -174566, -10464, 49179

sala centro boss
	-86217, -174920, -10040, 105
derecha
	-86222, -175319, -10056, 16384
izquierda
	-86222, -174521, -10056, 49272

############################################################################################################## Varias salas



############################################################################################################## Varias salas

loc inicial teleport 3ra kama
	-43640, -185509, -10968, 32768

sala centro 1ra
	-44972, -185513, -10976, 0
derecha
	-44977, -185860, -10960, 16384
izquierda
	-44975, -185157, -10960, 49152

sala centro 2da
	-47309, -185511, -10712, 234
derecha
	-47306, -185866, -10696, 16275
izquierda
	-47310, -185152, -10696, 49194

sala centro 3ra
	-49642, -185511, -10448, 168
	
derecha
	-49641, -185862, -10432, 16436
	
izquierda
	-49645, -185158, -10432, 49152

sala centro boss
	-53418, -185512, -10008, 50
	
derecha
	-53423, -185853, -10024, 16837
	
izquierda
	-53424, -185212, -10024, 49017
	

############################################################################################################## Varias salas

loc inicial teleport 4ra kama
	-43633, -174888, -10968, 32768

sala centro 1ra
	-44974, -174888, -10976, 66
derecha
	-44975, -175245, -10960, 16337
izquierda
	-44977, -174531, -10960, 49152

sala centro 2da
	-47300, -174890, -10712, 61312
derecha
	-47308, -175235, -10696, 16443
izquierda
	-47311, -174537, -10696, 50632

sala centro 3ra
	-49642, -174886, -10448, 31
derecha
	-49641, -175240, -10432, 16339
izquierda
	-49643, -174533, -10432, 49109

sala centro boss
	-53414, -174887, -10008, 376
derecha
	-53422, -175211, -10024, 16440
izquierda
	-53421, -174561, -10024, 49280

############################################################################################################## Varias salas

loc inicial teleport 5ta kama
-10872, -185524, -10936, 32768

sala centro 1ra
-12206, -185532, -10944, 65467

sala centro 2da
-14531, -185529, -10680, 64496

sala centro 3ra
-16869, -185527, -10416, 0

sala centro boss
-20646, -185529, -9976, 51

############################################################################################################## Varias salas

loc inicial teleport 6ta kama
-10872, -174901, -10936, 29412

sala centro 1ra
-12200, -174903, -10944, 65268

sala centro 2da
-14530, -174904, -10680, 65329

sala centro 3ra
-16867, -174903, -10416, 63170

sala centro boss
-20651, -174903, -9976, 3355

############################################################################################################## Varias salas

############################################################################################################## Varias salas

loc inicial teleport 7ta kama
21835, -185512, -10904, 49152

sala centro 1ra
20489, -185509, -10912, 33371

sala centro 2da
18164, -185512, -10648, 32049

sala centro 3ra
15828, -185510, -10384, 32669

sala centro boss
12047, -185513, -9944, 32926

############################################################################################################## Varias salas

############################################################################################################## Varias salas

loc inicial teleport 8va kama
21831, -174886, -10904, 32768

sala centro 1ra
20492, -174887, -10912, 32525
sala centro 2da
18158, -174886, -10648, 32073
sala centro 3ra
15822, -174886, -10384, 32768
sala centro boss
12046, -174887, -9944, 32768
############################################################################################################## Varias salas

############################################################################################################## Circular
Sala unica centro circular
-56279, -219856, -8112, 31287

sala unica centro circular
-42032, -219858, -8112, 32768

sala unica centro circular
-42031, -213149, -8112, 55285

Sala unica centro circular
-42034, -206133, -8112, 16384

Sala unica centro circular
-49088, -206139, -8112, 24576

Sala unica centro circular
-56269, -206141, -8112, 694

sala unica centro circular
-23494, -219853, -8048, 32571
############################################################################################################## Circular




############################################################################################################## cuadrada unica 
sala cuadrada unica 
-49338, -220247, -8064, 15862

sala cuadrada unica 
-56419, -212951, -8064, 46038

sala cuadrada unica
-49338, -212946, -8064, 49344
############################################################################################################## cuadrada unica 


############################################################################################################## cuadrada unica diferente 
sala cuadrada diferente 
unica sala
49088, -219718, -8752, 16384

sala cuadrada diferente 
unica sala
42359, -219693, -8752, 57344
############################################################################################################## cuadrada unica diferente 





















 */
