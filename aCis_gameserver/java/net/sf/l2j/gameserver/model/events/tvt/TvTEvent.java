/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.events.tvt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.events.EventManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author FBIagent
 */
public class TvTEvent
{
	enum EventState
	{
		INACTIVE,
		INACTIVATING,
		PARTICIPATING,
		STARTING,
		STARTED,
		REWARDING
	}
	
	public static boolean isInProgress()
	{
		synchronized (_state)
		{
			return _state != EventState.INACTIVE;
		}
	}
	
	protected static final Logger _log = Logger.getLogger(TvTEvent.class.getName());
	/** html path **/
	private static final String htmlPath = "data/html/mods/TvTEvent/";
	/**
	 * The teams of the TvTEvent<br>
	 */
	private static TvTEventTeam[] _teams = new TvTEventTeam[2];
	
	/**
	 * The state of the TvTEvent<br>
	 */
	private static EventState _state = EventState.INACTIVE;
	/**
	 * The spawn of the participation npc<br>
	 */
	private static Spawn _npcSpawn = null;
	/**
	 * the npc instance of the participation npc<br>
	 */
	private static Npc _lastNpcSpawn = null;
	
	/**
	 * No instance of this class!<br>
	 */
	private TvTEvent()
	{
	}
	
	/**
	 * Teams initializing<br>
	 */
	public static void init()
	{
		_teams[0] = new TvTEventTeam(Config.TVT_EVENT_TEAM_1_NAME, Config.TVT_EVENT_TEAM_1_COORDINATES);
		_teams[1] = new TvTEventTeam(Config.TVT_EVENT_TEAM_2_NAME, Config.TVT_EVENT_TEAM_2_COORDINATES);
	}
	
	/**
	 * Starts the participation of the TvTEvent<br>
	 * 1. Get NpcTemplate by Config.TVT_EVENT_PARTICIPATION_NPC_ID<br>
	 * 2. Try to spawn a new npc of it<br>
	 * <br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startParticipation()
	{
		NpcTemplate tmpl = NpcData.getInstance().getTemplate(Config.TVT_EVENT_PARTICIPATION_NPC_ID);
		
		if (tmpl == null)
		{
			_log.warning("TvTEventEngine[TvTEvent.startParticipation()]: NpcTemplate is a NullPointer -> Invalid npc id in configs?");
			return false;
		}
		
		try
		{
			_npcSpawn = new Spawn(tmpl);
			
			_npcSpawn.setLoc(Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0], Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1], Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2], Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[3]);
			_npcSpawn.setRespawnDelay(1);
			// later no need to delete spawn from db, we don't store it (false)
			SpawnManager.getInstance().addSpawn(_npcSpawn);
			_npcSpawn.doSpawn(false);
			_lastNpcSpawn = _npcSpawn.getNpc();
			//_lastNpcSpawn.setCurrentHp(_lastNpcSpawn.getMaxHp());
			_lastNpcSpawn.setTitle("TvT Event Participation");
			//_lastNpcSpawn.isAggressive();
			_lastNpcSpawn.decayMe();
			_lastNpcSpawn.spawnMe(_npcSpawn.getLocX(), _npcSpawn.getLocY(), _npcSpawn.getLocZ());
			_lastNpcSpawn.broadcastPacket(new MagicSkillUse(_lastNpcSpawn, _lastNpcSpawn, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "TvTEventEngine[TvTEvent.startParticipation()]: exception: " + e.getMessage(), e);
			return false;
		}
		
		setState(EventState.PARTICIPATING);
		return true;
	}
	
	private static int highestLevelPcInstanceOf(Map<Integer, Player> players)
	{
		int maxLevel = Integer.MIN_VALUE, maxLevelId = -1;
		for (Player player : players.values())
		{
			if (player.getStatus().getLevel() >= maxLevel)
			{
				maxLevel = player.getStatus().getLevel();
				maxLevelId = player.getObjectId();
			}
		}
		return maxLevelId;
	}
	
	/**
	 * Starts the TvTEvent fight<br>
	 * 1. Set state EventState.STARTING<br>
	 * 2. Close doors specified in configs<br>
	 * 3. Abort if not enought participants(return false)<br>
	 * 4. Set state EventState.STARTED<br>
	 * 5. Teleport all participants to team spot<br>
	 * <br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static boolean startFight()
	{
		// Set state to STARTING
		setState(EventState.STARTING);
		
		// Randomize and balance team distribution
		Map<Integer, Player> allParticipants = new HashMap<>();
		allParticipants.putAll(_teams[0].getParticipatedPlayers());
		allParticipants.putAll(_teams[1].getParticipatedPlayers());
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		
		Player player;
		Iterator<Player> iter;
		if (needParticipationFee())
		{
			iter = allParticipants.values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!hasParticipationFee(player))
					iter.remove();
			}
		}
		
		int balance[] =
		{
			0,
			0
		}, priority = 0, highestLevelPlayerId;
		Player highestLevelPlayer;
		// XXX: allParticipants should be sorted by level instead of using highestLevelPcInstanceOf for every fetch
		while (!allParticipants.isEmpty())
		{
			// Priority team gets one player
			highestLevelPlayerId = highestLevelPcInstanceOf(allParticipants);
			highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getStatus().getLevel();
			// Exiting if no more players
			if (allParticipants.isEmpty())
				break;
			
			// The other team gets one player
			// XXX: Code not dry
			priority = 1 - priority;
			highestLevelPlayerId = highestLevelPcInstanceOf(allParticipants);
			highestLevelPlayer = allParticipants.get(highestLevelPlayerId);
			allParticipants.remove(highestLevelPlayerId);
			_teams[priority].addPlayer(highestLevelPlayer);
			balance[priority] += highestLevelPlayer.getStatus().getLevel();
			// Recalculating priority
			priority = balance[0] > balance[1] ? 1 : 0;
		}
		
		// Check for enought participants
		if ((_teams[0].getParticipatedPlayerCount() < Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS) || (_teams[1].getParticipatedPlayerCount() < Config.TVT_EVENT_MIN_PLAYERS_IN_TEAMS))
		{
			// Set state INACTIVE
			setState(EventState.INACTIVE);
			// Cleanup of teams
			_teams[0].cleanMe();
			_teams[1].cleanMe();
			// Unspawn the event NPC
			unSpawnNpc();
			return false;
		}
		
		if (needParticipationFee())
		{
			iter = _teams[0].getParticipatedPlayers().values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!payParticipationFee(player))
					iter.remove();
			}
			iter = _teams[1].getParticipatedPlayers().values().iterator();
			while (iter.hasNext())
			{
				player = iter.next();
				if (!payParticipationFee(player))
					iter.remove();
			}
		}
		
		// Opens all doors specified in configs for tvt
		// openDoors(Config.TVT_DOORS_IDS_TO_OPEN);
		// Closes all doors specified in configs for tvt
		closeOpenDoors(Config.TVT_DOORS_IDS_TO_CLOSE, false);
		// Set state STARTED
		setState(EventState.STARTED);
		
		// Iterate over all teams
		for (TvTEventTeam team : _teams) // Iterate over all participated player instances in this team
			for (Player pc : team.getParticipatedPlayers().values())
				if (pc != null)
					new TvTEventTeleporter(pc, team.getCoordinates(), false, false); // Teleporter implements Runnable and starts itself
		
		return true;
	}
	
	/**
	 * Calculates the TvTEvent reward<br>
	 * 1. If both teams are at a tie(points equals), send it as system message to all participants, if one of the teams have 0 participants left online abort rewarding<br>
	 * 2. Wait till teams are not at a tie anymore<br>
	 * 3. Set state EvcentState.REWARDING<br>
	 * 4. Reward team with more points<br>
	 * 5. Show win html to wining team participants<br>
	 * <br>
	 * @return String: winning team name<br>
	 */
	public static String calculateRewards()
	{
		if (_teams[0].getPoints() == _teams[1].getPoints())
		{
			// Check if one of the teams have no more players left
			if ((_teams[0].getParticipatedPlayerCount() == 0) || (_teams[1].getParticipatedPlayerCount() == 0))
			{
				// set state to rewarding
				setState(EventState.REWARDING);
				// return here, the fight can't be completed
				return "TvT Event: Event has ended. No team won due to inactivity!";
			}
			
			// Both teams have equals points
			sysMsgToAllParticipants("TvT Event: Event has ended, both teams have tied.");
			if (Config.TVT_REWARD_TEAM_TIE)
			{
				rewardTeam(_teams[0]);
				rewardTeam(_teams[1]);
				return "TvT Event: Event has ended with both teams tying.";
			}
			return "TvT Event: Event has ended with both teams tying.";
		}
		
		// Set state REWARDING so nobody can point anymore
		setState(EventState.REWARDING);
		
		// Get team which has more points
		TvTEventTeam team = _teams[_teams[0].getPoints() > _teams[1].getPoints() ? 0 : 1];
		rewardTeam(team);
		return "TvT Event: Event finish. Team " + team.getName() + " won with " + team.getPoints() + " kills.";
	}
	
	private static void rewardTeam(TvTEventTeam team)
	{
		// Iterate over all participated player instances of the winning team
		for (Player player : team.getParticipatedPlayers().values())
		{
			// Check for nullpointer
			if (player == null)
				continue;
			
			StatusUpdate statusUpdate = new StatusUpdate(player);
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

			statusUpdate.addAttribute(StatusType.CUR_LOAD, player.getCurrentWeight());
			npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Reward.htm"));
			player.sendPacket(statusUpdate);
			player.sendPacket(npcHtmlMessage);
		}
	}
	
	/**
	 * Stops the TvTEvent fight<br>
	 * 1. Set state EventState.INACTIVATING<br>
	 * 2. Remove tvt npc from world<br>
	 * 3. Open doors specified in configs<br>
	 * 4. Teleport all participants back to participation npc location<br>
	 * 5. Teams cleaning<br>
	 * 6. Set state EventState.INACTIVE<br>
	 */
	public static void stopFight()
	{
		// Set state INACTIVATING
		setState(EventState.INACTIVATING);
		// Unspawn event npc
		unSpawnNpc();
		// Opens all doors specified in configs for tvt
		closeOpenDoors(Config.TVT_DOORS_IDS_TO_CLOSE, true);
		// Closes all doors specified in Configs for tvt
		// closeDoors(Config.TVT_DOORS_IDS_TO_OPEN);
		
		// Iterate over all teams
		for (TvTEventTeam team : _teams)
			for (Player player : team.getParticipatedPlayers().values())
				if (player != null) 	// Check for nullpointer
					new TvTEventTeleporter(player, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES, false, false);
		
		// Cleanup of teams
		_teams[0].cleanMe();
		_teams[1].cleanMe();
		// Set state INACTIVE
		setState(EventState.INACTIVE);
	}
	
	/**
	 * Adds a player to a TvTEvent team<br>
	 * 1. Calculate the id of the team in which the player should be added<br>
	 * 2. Add the player to the calculated team<br>
	 * <br>
	 * @param player as Player<br>
	 * @return boolean: true if success, otherwise false<br>
	 */
	public static synchronized boolean addParticipant(Player player)
	{
		// Check for nullpoitner
		if (player == null)
			return false;
		
		byte teamId = 0;
		
		// Check to which team the player should be added
		if (_teams[0].getParticipatedPlayerCount() == _teams[1].getParticipatedPlayerCount())
			teamId = (byte) (Rnd.get(2));
		else
			teamId = (byte) (_teams[0].getParticipatedPlayerCount() > _teams[1].getParticipatedPlayerCount() ? 1 : 0);
		
		return _teams[teamId].addPlayer(player);
	}
	
	/**
	 * Removes a TvTEvent player from it's team<br>
	 * 1. Get team id of the player<br>
	 * 2. Remove player from it's team<br>
	 * <br>
	 * @param objtId
	 * @return boolean: true if success, otherwise false
	 */
	public static boolean removeParticipant(int objtId)
	{
		// Get the teamId of the player
		byte teamId = getParticipantTeamId(objtId);
		
		// Check if the player is participant
		if (teamId != -1)
		{
			// Remove the player from team
			_teams[teamId].removePlayer(objtId);
			return true;
		}
		
		return false;
	}
	
	public static boolean needParticipationFee()
	{
		return (Config.TVT_EVENT_PARTICIPATION_FEE[0] != 0) && (Config.TVT_EVENT_PARTICIPATION_FEE[1] != 0);
	}
	
	public static boolean hasParticipationFee(Player player)
	{
		return player.getInventory().getItemCount(Config.TVT_EVENT_PARTICIPATION_FEE[0], -1) >= Config.TVT_EVENT_PARTICIPATION_FEE[1]; //return player.addItem("TvT Reward", getParticipatedPlayersCount(), getParticipatedPlayersCount(), player, isInProgress())
	}
	
	public static boolean payParticipationFee(Player player)
	{
		return player.destroyItemByItemId("TvT Participation Fee", Config.TVT_EVENT_PARTICIPATION_FEE[0], Config.TVT_EVENT_PARTICIPATION_FEE[1], _lastNpcSpawn, true);
	}
	
	public static String getParticipationFee()
	{
		int itemId = Config.TVT_EVENT_PARTICIPATION_FEE[0];
		int itemNum = Config.TVT_EVENT_PARTICIPATION_FEE[1];
		
		if ((itemId == 0) || (itemNum == 0))
			return "-";
		
		return String.valueOf(itemNum) + " " + ItemData.getInstance().getTemplate(itemId).getName();
	}
	
	/**
	 * Send a SystemMessage to all participated players<br>
	 * 1. Send the message to all players of team number one<br>
	 * 2. Send the message to all players of team number two<br>
	 * <br>
	 * @param message as String<br>
	 */
	public static void sysMsgToAllParticipants(String message)
	{
		for (Player player : _teams[0].getParticipatedPlayers().values())
			if (player != null)
				player.sendMessage(message);
		
		for (Player player : _teams[1].getParticipatedPlayers().values())
			if (player != null)
				player.sendMessage(message);
	}

	/**
	 * Close Open doors specified in configs
	 * @param doors
	 * @param open 
	 */
	private static void closeOpenDoors(List<Integer> doors, boolean open)
	{
		for (int doorId : doors)
		{
			Door Door = DoorData.getInstance().getDoor(doorId);
			
			if (Door != null)
				if (open)
					Door.openMe();
				else Door.closeMe();
		}
	}
	
	/**
	 * UnSpawns the TvTEvent npc
	 */
	private static void unSpawnNpc()
	{
		// Delete the npc
		_lastNpcSpawn.cancelRespawn();
		_lastNpcSpawn.deleteMe();
		//SpawnManager.getInstance().deleteSpawn(_lastNpcSpawn.getSpawn());
		// Stop respawning of the npc
		//SpawnManager.getInstance().deleteSpawn(_npcSpawn, false);
		_npcSpawn = null;
		_lastNpcSpawn = null;
	}
	
	/**
	 * Called when a player logs in<br>
	 * <br>
	 * @param player as Player<br>
	 */
	public static void onLogin(Player player)
	{
		if ((player == null) || (!isStarting() && !isStarted()))
			return;
		
		byte teamId = getParticipantTeamId(player.getObjectId());
		
		if (teamId == -1)
			return;
		
		_teams[teamId].addPlayer(player);
		new TvTEventTeleporter(player, _teams[teamId].getCoordinates(), true, false);
	}
	
	/**
	 * Called when a player logs out<br>
	 * <br>
	 * @param player as Player<br>
	 */
	public static void onLogout(Player player)
	{
		if ((player != null) && (isStarting() || isStarted() || isParticipating()))
			if (removeParticipant(player.getObjectId()))
				player.setXYZInvisible((Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0] + Rnd.get(101)) - 50, (Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1] + Rnd.get(101)) - 50, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2]);
	}
	
	/**
	 * Called on every bypass by npc of type L2TvTEventNpc<br>
	 * Needs synchronization cause of the max player check<br>
	 * <br>
	 * @param command as String<br>
	 * @param player as Player<br>
	 */
	public static synchronized void onBypass(String command, Player player)
	{
		if ((player == null) || !isParticipating())
			return;
		
		final String htmContent;
		
		if (command.equals("tvt_event_participation"))
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			int playerLevel = player.getStatus().getLevel();
			
			if (player.isCursedWeaponEquipped())
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "CursedWeaponEquipped.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if (EventManager.getInstance().containsPlayer(player))
			{
				player.sendMessage("Ya estas registrado en otro evento.");
				return;
			}
			else if (OlympiadManager.getInstance().isRegistered(player))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Olympiad.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if (player.getKarma() > 0)
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Karma.htm");
				if (htmContent != null)
					npcHtmlMessage.setHtml(htmContent);
			}
			else if ((playerLevel < Config.TVT_EVENT_MIN_LVL) || (playerLevel > Config.TVT_EVENT_MAX_LVL))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "Level.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%min%", String.valueOf(Config.TVT_EVENT_MIN_LVL));
					npcHtmlMessage.replace("%max%", String.valueOf(Config.TVT_EVENT_MAX_LVL));
				}
			}
			else if ((_teams[0].getParticipatedPlayerCount() == Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS) && (_teams[1].getParticipatedPlayerCount() == Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "TeamsFull.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%max%", String.valueOf(Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS));
				}
			}
			else if (needParticipationFee() && !hasParticipationFee(player))
			{
				htmContent = HtmCache.getInstance().getHtm(htmlPath + "ParticipationFee.htm");
				if (htmContent != null)
				{
					npcHtmlMessage.setHtml(htmContent);
					npcHtmlMessage.replace("%fee%", getParticipationFee());
				}
			}
			else if (addParticipant(player))
				npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Registered.htm"));
			else
				return;
			
			player.sendPacket(npcHtmlMessage);
		}
		else if (command.equals("tvt_event_remove_participation"))
		{
			removeParticipant(player.getObjectId());
			
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
			
			npcHtmlMessage.setHtml(HtmCache.getInstance().getHtm(htmlPath + "Unregistered.htm"));
			player.sendPacket(npcHtmlMessage);
		}
	}
	
	/**
	 * Called on every onAction in L2PcIstance<br>
	 * <br>
	 * @param player
	 * @param targetedobjtId
	 * @return boolean: true if player is allowed to target, otherwise false
	 */
	public static boolean onAction(Player player, int targetedobjtId)
	{
		if ((player == null) || !isStarted() || player.isGM())
			return true;
		
		byte playerTeamId = getParticipantTeamId(player.getObjectId());
		byte targetedPlayerTeamId = getParticipantTeamId(targetedobjtId);
		
		if ((playerTeamId != -1 && targetedPlayerTeamId == -1) || (playerTeamId == -1 && targetedPlayerTeamId != -1) || (playerTeamId != -1 && targetedPlayerTeamId != -1 && playerTeamId == targetedPlayerTeamId && player.getObjectId() != targetedobjtId && !Config.TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED))
			return false;
		
		return true;
	}
	
	/**
	 * Called on every scroll use<br>
	 * <br>
	 * @param objId
	 * @return boolean: true if player is allowed to use scroll, otherwise false
	 */
	public static boolean onScrollUse(int objId) // if (!isStarted()) return true; if (isPlayerParticipant(objtId) && !Config.TVT_EVENT_SCROLL_ALLOWED) return false; return true;
	{
		return (!isStarted() ? true : isPlayerParticipant(objId) && !Config.TVT_EVENT_SCROLL_ALLOWED ? false : true);
	}
	
	/**
	 * Called on every potion use
	 * @param objtId
	 * @return boolean: true if player is allowed to use potions, otherwise false
	 */
	public static boolean onPotionUse(int objtId)//		if (!isStarted()) { return true; } if (isPlayerParticipant(objtId) && !Config.TVT_EVENT_POTIONS_ALLOWED) { return false; }
	{
		return (!isStarted() ? true : isPlayerParticipant(objtId) && !Config.TVT_EVENT_POTIONS_ALLOWED ? false : true);
	}
	
	/**
	 * Called on every escape use(thanks to nbd)
	 * @param objtId
	 * @return boolean: true if player is not in tvt event, otherwise false
	 */
	public static boolean onEscapeUse(int objtId) //	if (!isStarted()) return true; if (isPlayerParticipant(objtId)) return false;
	{
		return (!isStarted() ? true : isPlayerParticipant(objtId) ? false : true);
	}
	
	/**
	 * Called on every summon item use
	 * @param objtId
	 * @return boolean: true if player is allowed to summon by item, otherwise false
	 */
	public static boolean onItemSummon(int objtId) //if (!isStarted()) return true; if (isPlayerParticipant(objtId) && !Config.TVT_EVENT_SUMMON_BY_ITEM_ALLOWED) return false; return true;
	{
		return (!isStarted() ? true : isPlayerParticipant(objtId) && !Config.TVT_EVENT_SUMMON_BY_ITEM_ALLOWED ? false : true);
	}
	
	/**
	 * Is called when a player is killed<br>
	 * <br>
	 * @param killerCharacter as Creature<br>
	 * @param killedPlayer as Player<br>
	 */
	public static void onKill(Creature killerCharacter, Player killedPlayer)
	{
		if ((killedPlayer == null) || !isStarted())
			return;
		byte killedTeamId = getParticipantTeamId(killedPlayer.getObjectId());
		
		if (killedTeamId == -1)
			return;
		
		new TvTEventTeleporter(killedPlayer, _teams[killedTeamId].getCoordinates(), false, false);
		
		if (killerCharacter == null)
			return;
		
		Player killerplayer = null;
		
		if ((killerCharacter instanceof Pet) || (killerCharacter instanceof Servitor))
		{
			killerplayer = ((Summon) killerCharacter).getOwner();
			
			if (killerplayer == null)
				return;
		}
		else if (killerCharacter instanceof Player)
			killerplayer = (Player) killerCharacter;
		else
			return;
		
		byte killerTeamId = getParticipantTeamId(killerplayer.getObjectId());
		
		if ((killerTeamId != -1) && (killedTeamId != -1) && (killerTeamId != killedTeamId))
		{
			TvTEventTeam killerTeam = _teams[killerTeamId];
			
			killerTeam.increasePoints();
			
			CreatureSay cs = new CreatureSay(killerplayer.getObjectId(), SayType.TELL, killerplayer.getName(), "I have killed " + killedPlayer.getName() + "!");
			
			for (Player player : _teams[killerTeamId].getParticipatedPlayers().values())
				if (player != null)
					player.sendPacket(cs);
		}
	}
	
	/**
	 * Called on Appearing packet received (player finished teleporting)
	 * @param player
	 */
	public static void onTeleported(Player player)
	{
		if (!isStarted() || (player == null) || !isPlayerParticipant(player.getObjectId()))
			return;
		
		if (player.isMageClass() && (Config.TVT_EVENT_MAGE_BUFFS != null) && !Config.TVT_EVENT_MAGE_BUFFS.isEmpty())
		{
			for (Entry<Integer, Integer> e : Config.TVT_EVENT_MAGE_BUFFS.entrySet())
			{
				L2Skill skill = SkillTable.getInstance().getInfo(e.getKey(), e.getValue());
				if (skill != null)
					skill.getEffects(player, player);
			}
		}
		else if ((Config.TVT_EVENT_FIGHTER_BUFFS != null) && !Config.TVT_EVENT_FIGHTER_BUFFS.isEmpty())
			for (Entry<Integer, Integer> e : Config.TVT_EVENT_FIGHTER_BUFFS.entrySet())
			{
				L2Skill skill = SkillTable.getInstance().getInfo(e.getKey(), e.getValue());
				if (skill != null)
					skill.getEffects(player, player);
			}
	}
	
	/**
	 * @param source
	 * @param target
	 * @param skill
	 * @return true if player valid for skill
	 */
	public static final boolean checkForTvTSkill(Player source, Player target, L2Skill skill)
	{
		if (!isStarted())
			return true;
		// TvT is started
		final int sourcePlayerId = source.getObjectId();
		final int targetPlayerId = target.getObjectId();
		final boolean isSourceParticipant = isPlayerParticipant(sourcePlayerId);
		final boolean isTargetParticipant = isPlayerParticipant(targetPlayerId);
		
		// both players not participating
		if (!isSourceParticipant && !isTargetParticipant)
			return true;
		// one player not participating
		if (!(isSourceParticipant && isTargetParticipant))
			return false;
		// players in the different teams ?
		if (getParticipantTeamId(sourcePlayerId) != getParticipantTeamId(targetPlayerId))
			if (!skill.isOffensive())
				return false;
		
		return true;
	}
	
	/**
	 * Sets the TvTEvent state<br>
	 * <br>
	 * @param state as EventState<br>
	 */
	private static void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}
	
	/**
	 * Is TvTEvent inactive?<br>
	 * <br>
	 * @return boolean: true if event is inactive(waiting for next event cycle), otherwise false<br>
	 */
	public static boolean isInactive() // boolean isInactive; return isInactive;
	{
		synchronized (_state)
		{
			return _state == EventState.INACTIVE;
		}
	}
	
	/**
	 * Is TvTEvent in inactivating?<br>
	 * <br>
	 * @return boolean: true if event is in inactivating progress, otherwise false<br>
	 */
	public static boolean isInactivating() // boolean isInactivating; return isInactivating;
	{
		synchronized (_state)
		{
			return _state == EventState.INACTIVATING;
		}
	}
	
	/**
	 * Is TvTEvent in participation?<br>
	 * <br>
	 * @return boolean: true if event is in participation progress, otherwise false<br>
	 */
	public static boolean isParticipating() // boolean isParticipating;  return isParticipating;
	{
		synchronized (_state)
		{
			return _state == EventState.PARTICIPATING;
		}
	}
	
	/**
	 * Is TvTEvent starting?<br>
	 * <br>
	 * @return boolean: true if event is starting up(setting up fighting spot, teleport players etc.), otherwise false<br>
	 */
	public static boolean isStarting() // boolean isStarting; return isStarting;
	{
		synchronized (_state)
		{
			return _state == EventState.STARTING;
		}
	}
	
	/**
	 * Is TvTEvent started?<br>
	 * <br>
	 * @return boolean: true if event is started, otherwise false<br>
	 */
	public static boolean isStarted() // boolean isStarted; return isStarted;
	{
		synchronized (_state)
		{
			return _state == EventState.STARTED;
		}
	}
	
	/**
	 * Is TvTEvent rewarding?<br>
	 * <br>
	 * @return boolean: true if event is currently rewarding, otherwise false<br>
	 */
	public static boolean isRewarding() // boolean isRewarding;	return isRewarding;
	{
		synchronized (_state)
		{
			return _state == EventState.REWARDING;
		}
	}
	
	/**
	 * Returns the team id of a player, if player is not participant it returns -1
	 * @param objtId
	 * @return byte: team name of the given playerName, if not in event -1
	 */
	public static byte getParticipantTeamId(int objtId)
	{
		return (byte) (_teams[0].containsPlayer(objtId) ? 0 : (_teams[1].containsPlayer(objtId) ? 1 : -1));
	}
	
	/**
	 * Returns the team of a player, if player is not participant it returns null
	 * @param objtId
	 * @return TvTEventTeam: team of the given objtId, if not in event null
	 */
	public static TvTEventTeam getParticipantTeam(int objtId)
	{
		return (_teams[0].containsPlayer(objtId) ? _teams[0] : (_teams[1].containsPlayer(objtId) ? _teams[1] : null));
	}
	
	/**
	 * Returns the enemy team of a player, if player is not participant it returns null
	 * @param objtId
	 * @return TvTEventTeam: enemy team of the given objtId, if not in event null
	 */
	public static TvTEventTeam getParticipantEnemyTeam(int objtId)
	{
		return (_teams[0].containsPlayer(objtId) ? _teams[1] : (_teams[1].containsPlayer(objtId) ? _teams[0] : null));
	}
	
	/**
	 * Returns the team coordinates in which the player is in, if player is not in a team return null
	 * @param objtId
	 * @return int[]: coordinates of teams, 2 elements, index 0 for team 1 and index 1 for team 2
	 */
	public static int[] getParticipantTeamCoordinates(int objtId)
	{
		return _teams[0].containsPlayer(objtId) ? _teams[0].getCoordinates() : (_teams[1].containsPlayer(objtId) ? _teams[1].getCoordinates() : null);
	}
	
	/**
	 * Is given player participant of the event?
	 * @param objtId
	 * @return boolean: true if player is participant, ohterwise false
	 */
	public static boolean isPlayerParticipant(int objtId)
	{
		return (!isParticipating() && !isStarting() && !isStarted()) ? false : (_teams[0].containsPlayer(objtId) || _teams[1].containsPlayer(objtId));	
	}
	
	/**
	 * Returns participated player count<br>
	 * <br>
	 * @return int: amount of players registered in the event<br>
	 */
	public static int getParticipatedPlayersCount()
	{
		return (!isParticipating() && !isStarting() && !isStarted()) ? 0 : _teams[0].getParticipatedPlayerCount() + _teams[1].getParticipatedPlayerCount();
	}
	
	/**
	 * Returns teams names<br>
	 * <br>
	 * @return String[]: names of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
	 */
	public static String[] getTeamNames()
	{
		return new String[]
		{
			_teams[0].getName(),
			_teams[1].getName()
		};
	}
	
	/**
	 * Returns player count of both teams<br>
	 * <br>
	 * @return int[]: player count of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
	 */
	public static int[] getTeamsPlayerCounts()
	{
		return new int[]
		{
			_teams[0].getParticipatedPlayerCount(),
			_teams[1].getParticipatedPlayerCount()
		};
	}
	
	/**
	 * Returns points count of both teams
	 * @return int[]: points of teams, 2 elements, index 0 for team 1 and index 1 for team 2<br>
	 */
	public static int[] getTeamsPoints()
	{
		return new int[]
		{
			_teams[0].getPoints(),
			_teams[1].getPoints()
		};
	}
	
	public static TvTEventTeam[] getTeams()
	{
		return _teams;
	}
}
