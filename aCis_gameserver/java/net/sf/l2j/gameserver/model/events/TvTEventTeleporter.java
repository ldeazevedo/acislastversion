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
package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.status.PlayerStatus;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;

public class TvTEventTeleporter implements Runnable
{
	/** The instance of the player to teleport */
	private Player _playerInstance = null;
	/** Coordinates of the spot to teleport to */
	private int[] _coordinates = new int[3];
	/** Admin removed this player from event */
	private boolean _adminRemove = false;
	
	/**
	 * Initialize the teleporter and start the delayed task.
	 * @param playerInstance
	 * @param coordinates
	 * @param fastSchedule
	 * @param adminRemove
	 */
	public TvTEventTeleporter(Player playerInstance, int[] coordinates, boolean fastSchedule, boolean adminRemove)
	{
		_playerInstance = playerInstance;
		_coordinates = coordinates;
		_adminRemove = adminRemove;
		
		long delay = (TvTEvent.isStarted() ? Config.TVT_EVENT_RESPAWN_TELEPORT_DELAY : Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000;
		
		ThreadPool.schedule(this, fastSchedule ? 0 : delay);
	}
	
	/**
	 * The task method to teleport the player<br>
	 * 1. Unsummon pet if there is one<br>
	 * 2. Remove all effects<br>
	 * 3. Revive and full heal the player<br>
	 * 4. Teleport the player<br>
	 * 5. Broadcast status and user info
	 */
	@Override
	public void run()
	{
		if (_playerInstance == null)
		{
			return;
		}
		
		Summon summon = _playerInstance.getSummon();
		
		if (summon != null)
		{
			summon.unSummon(_playerInstance);
		}
		
		if ((Config.TVT_EVENT_EFFECTS_REMOVAL == 0) || ((Config.TVT_EVENT_EFFECTS_REMOVAL == 1) && ((_playerInstance.getTeam() == TeamType.NONE) || (_playerInstance.isInDuel() && (_playerInstance.getDuelState() != DuelState.INTERRUPTED)))))
		{
			_playerInstance.stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		if (_playerInstance.isInDuel())
		{
			_playerInstance.setDuelState(DuelState.INTERRUPTED);
		}
		
		_playerInstance.doRevive();
		
		_playerInstance.instantTeleportTo((_coordinates[0] + Rnd.get(101)) - 50, (_coordinates[1] + Rnd.get(101)) - 50, _coordinates[2], 0);
		
		if (TvTEvent.isStarted() && !_adminRemove)
		{
			//int team = TvTEvent.getParticipantTeamId(_playerInstance.getObjectId());
			//TeamType.
			//.setTeam(TeamType.team);
				
			//_playerInstance.setTeam(TvTEvent.getParticipantTeamId(_playerInstance.getObjectId()) + 1);
			//_playerInstance.setTeam(TvTEvent.getParticipantTeamId(_playerInstance.getObjectId()) == 0 ? TeamType.BLUE : TeamType.RED);
			//_playerInstance.setTeam(TvTEvent.getParticipantTeamId(_playerInstance.getObjectId()) == 0 ? TeamType.BLUE : TeamType.RED);
			int teamByte = TvTEvent.getParticipantTeamId(_playerInstance.getObjectId()) + 1;
			_playerInstance.setTeam(TeamType.getById(teamByte));
			TvTEvent.getParticipantTeamId(_playerInstance.getObjectId() + 1);
		}
		else
		{
			_playerInstance.setTeam(TeamType.NONE);
		}
		
		PlayerStatus st = _playerInstance.getStatus();
		//_playerInstance.setCurrentCp(st.getStatus().getMaxCp());
		//_playerInstance.setCurrentHp(st.getMaxHp());
		//_playerInstance.setCurrentMp(_playerInstance.getMaxMp());
		_playerInstance.getStatus().setCpHpMp(st.getMaxCp(), st.getMaxHp(), st.getMaxMp());
		_playerInstance.getStatus().broadcastStatusUpdate();
		//_PlayerInstance.broadcastStatusUpdate();
		_playerInstance.broadcastUserInfo();
	}
}
