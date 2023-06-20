/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.scripting.script.ai.area;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;

/**
 * Manages monsters in locations such as Abandoned Camp or Orc Barracks.
 * @author savormix
 */
public class FleeingClanMembers extends AttackableAIScript
{
	private static final boolean	DEBUG = false;
	private static final int		RUNAWAY_CHANCE = 10;
	private static final int		MAX_GEO_PLAN_DIST = 100 * 100;
	private static final String[]	ATTACKED = {
		"Let's see about that!",
		"I will definitely repay this humiliation!",
		"Retreat!",
		"Tactical retreat!",
		"Mass fleeing!",
		"It's stronger than expected!",
		"I'll kill you next time!",
		"I'll definitely kill you next time!",
		"Oh! How strong!",
		"Invader!",
		"You can't get anything by killing me.",
		"Someday you'll pay!",
		"I won't just stand still while you hit me.",
		"Stop hitting!",
		"It hurts to the bone!",
		"Am I the neighborhood drum for beating!",
		"Follow me if you want!",
		"Surrender!",
		"Oh, I'm dead!",
		"I'll be back!",
		"I'll give you ten million adena if you let me live!"
	};
	private static final int[] NPC = {
		// abandoned camp
		20053, 20058, 20061, 20063, 20066, 20076, 20436, 20437, 20438, 20439,
		// orc barracks
		20495, 20496, 20497, 20498, 20499, 20500, 20501, 20546
	};
	private static final Location[] CLAN_LOC = {
		// inside AC
		new Location(-54463, 146508, -2879),
		new Location(-51888, 143760, -2893),
		new Location(-50630, 140454, -2856),
		new Location(-52615, 138035, -2924),
		new Location(-54604, 136280, -2752),
		new Location(-58019, 137811, -2786),
		new Location(-56703, 140548, -2623),
		// outside AC
		new Location(-44465, 140426, -2918),
		new Location(-47918, 146799, -2881),
		// inside OB (hopefully all)
		new Location(-93416, 107999, -3872),
		new Location(-93424, 106288, -3688),
		new Location(-94710, 110068, -3547),
		new Location(-90528, 111488, -3456),
		new Location(-93880, 112428, -3696),
		new Location(-91526, 110105, -3544),
		new Location(-90480, 111520, -3448),
		new Location(-91027, 114750, -3544),
		new Location(-93899, 117600, -3614),
		new Location(-97228, 117968, -3434),
		new Location(-97120, 114272, -3560),
		new Location(-97127, 110636, -3472),
		new Location(-97280, 106816, -3392),
		// outside OB
		new Location(-95159, 102351, -3560),
		new Location(-94154, 100252, -3512),
		new Location(-90482, 100340, -3560),
		new Location(-88240, 103082, -3408),
	};

	public FleeingClanMembers(int questId, String name, String descr)
	{
		super(questId, name, descr);
		registerMobs(NPC);
		if (DEBUG)
			_log.info("Script " + name + " loaded.");
	}

	private final Location getFleeToLocation(Attackable character)
	{
		if (DEBUG)
			_log.info("getFleeToLocation()");
		if (character.getMoveAroundPos() == null)
		{
			double minSqDist = Double.MAX_VALUE;
			int minIndex = 0;
			for (int i = 0; i < CLAN_LOC.length; i++)
			{
				Location pos = CLAN_LOC[i];
				double sqDist = L2Math.calculateDistanceSq(pos.x, pos.y, character.getX(), character.getY());
				if (sqDist < minSqDist)
				{
					minSqDist = sqDist;
					minIndex = i;
				}
			}
			if (DEBUG)
				_log.info("getFleeToLocation(): min index is " + minIndex);
			character.setMoveAroundPos(CLAN_LOC[minIndex]);
		}
		return character.getMoveAroundPos();
	}

	private final boolean isAtClanLocation(int x, int y, int z)
	{
		if (DEBUG)
			_log.info("isAtClanLocation()");
		for (Location pos : CLAN_LOC)
		{
			if (Config.GEODATA == 0 && pos.x == x && pos.y == y)// && pos.z == z)
				return true;
			else if (L2Math.calculateDistanceSq(pos.x, pos.y, x, y) < MAX_GEO_PLAN_DIST)
				return true;
		}
		if (DEBUG)
			_log.info("isAtClanLocation(): false");
		return false;
	}

	@Override
	public String onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		if (!(npc instanceof Attackable))
			return null;

		if (DEBUG)
			_log.info("onAttack()");
		Attackable myself = (Attackable) npc;
		if (!myself.hasBeenAttacked())
		{
			myself.setBeenAttacked(true);
			if (DEBUG)
				_log.info("onAttack(): setBeenAttacked(true)");
		}
		else if (myself.getFleeingStatus() == Attackable.FLEEING_NOT_STARTED)
		{
			if (DEBUG)
				_log.info("onAttack(): analysis");
			double hp = myself.getCurrentHp();
			if (hp < myself.getMaxHp() / 2D && hp > myself.getMaxHp() / 3D
					&& attacker.getCurrentHp() > attacker.getMaxHp() / 4D
					&& Rnd.get(100) < RUNAWAY_CHANCE)
			{
				int index = (ATTACKED.length - 1);
				int i5 = Rnd.get(100);
				if (i5 < 77)
					index = i5 / 7;
				else if (i5 < 95)
					index = (i5 - 77) / 2 + 11;
				npc.broadcastPacket(new NpcSay(npc, ATTACKED[index]));
				if (DEBUG)
					_log.info("onAttack(): fleeing with message " + index);
				myself.setFleeingStatus(L2Attackable.FLEEING_STARTED);
				myself.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, getFleeToLocation(myself));
			}
		}
		return null;
	}

	@Override
	public void onArrived(Npc character)
	{
		if (!(character instanceof L2Attackable))
			return;

		if (DEBUG)
			_log.info("onArrived()");
		L2Attackable myself = (L2Attackable) character;
		switch (myself.getFleeingStatus())
		{
		case L2Attackable.FLEEING_STARTED:
			if (DEBUG)
				_log.info("onArrived(): current status - started");
			if (isAtClanLocation(myself.getX(), myself.getY(), myself.getZ()))
			{
				if (DEBUG)
					_log.info("onArrived(): changed status to waiting");
				startQuestTimer("Flee_" + myself.getObjectId(), 15000, myself, null, true);
				myself.setFleeingStatus(L2Attackable.FLEEING_DONE_WAITING);
				((L2AttackableAI) myself.getAI()).setGlobalAggro(-30);
				myself.setWalking();
				myself.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			}
			else
			{
				if (DEBUG)
					_log.info("onArrived(): lost all hated chars, continuing run.");
				//	_log.info("onArrived(): CRITICAL FAILURE " + myself.getName() + "; " + myself.getObjectId());
				myself.setRunning();
				myself.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, getFleeToLocation(myself));
			}
			break;
		case L2Attackable.FLEEING_DONE_RETURNING:
			if (DEBUG)
				_log.info("onArrived(): current status - returning");
			L2Spawn spawn = myself.getSpawn();
			// currently no movement restrictions when isReturningToSpawnPoint()
			if (myself.getX() == spawn.getLocx() && myself.getY() == spawn.getLocy())
					//&& myself.getZ() == spawn.getLocz())
			{
				if (DEBUG)
					_log.info("onArrived(): changed status to normal");
				myself.setFleeingStatus(L2Attackable.FLEEING_NOT_STARTED);
			}
			else
			{
				if (DEBUG)
					_log.info("onArrived(): CRITICAL FAILURE");
				myself.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(spawn));
			}
			break;
		}
	}

	@Override
	public String onKill(Npc npc, Player killer, boolean isPet)
	{
		if (!(npc instanceof L2Attackable))
			return null;

		if (DEBUG)
			_log.info("onKill()");
		L2Attackable myself = (L2Attackable) npc;
		myself.setBeenAttacked(false);
		myself.setFleeingStatus(L2Attackable.FLEEING_NOT_STARTED);
		myself.setMoveAroundPos(null);
		cancelQuestTimers("Flee_" + myself.getObjectId());
		return null;
	}

	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (!(npc instanceof L2Attackable))
			return null;

		if (DEBUG)
			_log.info("onAdvEvent() " + event);
		L2Attackable myself = (L2Attackable) npc;
		if (myself.getFleeingStatus() == L2Attackable.FLEEING_DONE_WAITING)
		{
			if (myself.getCurrentHp() / myself.getMaxHp() > 0.7 ||
					myself.getMostHated() == null)
			{
				cancelQuestTimers(event);
				// let the next rnd walk take care of "returning"
				//if (myself.getAI().getIntention() != CtrlIntention.AI_INTENTION_IDLE)
					myself.setFleeingStatus(L2Attackable.FLEEING_NOT_STARTED);
				//else
				//	myself.setFleeingStatus(L2Attackable.FLEEING_DONE_RETURNING);
				if (DEBUG)
					_log.info("onAdvEvent(): moving to spawn, status is " + myself.getFleeingStatus());
			}
			else
			{
				if (DEBUG)
					_log.info("onAdvEvent(): waiting to recover");
				if (!myself.isRunning() && !myself.isInCombat())
					((L2AttackableAI) myself.getAI()).setGlobalAggro(-30);
			}
		}
		else
		{
			if (DEBUG)
				_log.info("onAdvEvent(): CRITICAL FAILURE wrong runaway status!");
			cancelQuestTimers(event);
		}
		return null;
	}

	public static void main(String[] args)
	{
		new FleeingClanMembers(-1, "warrior_run_away_to_clan", "ai");
	}
}
