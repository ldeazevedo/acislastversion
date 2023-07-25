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

import java.util.List;
import java.util.stream.Collectors;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Manages monsters in locations such as Abandoned Camp or Orc Barracks.
 * @author savormix
 */
public final class FleeingClanMembers extends AttackableAIScript
{
	public static final int FLEEING_NOT_STARTED = 0;
	public static final int FLEEING_STARTED = 1;
	public static final int FLEEING_DONE_WAITING = 2;
	public static final int FLEEING_DONE_RETURNING = 3;
	
	private static final int RUNAWAY_CHANCE = 10;
	private static final int MAX_GEO_PLAN_DIST = 100 * 100;
	
	private static final String[] ATTACKED = 
	{
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
	
	private static final int[] NPC = 
	{
		// abandoned camp
		20053, 20058, 20061, 20063, 20066, 20076, 20436, 20437, 20438, 20439,
		// orc barracks
		20495, 20496, 20497, 20498, 20499, 20500, 20501, 20546
	};
	
	private static final Location[] CLAN_LOC = 
	{
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

	public FleeingClanMembers()
	{
		super("ai/area");
	}
	
	@Override
	protected void registerNpcs()
	{
		addEventIds(NPC, EventHandler.MY_DYING, EventHandler.ON_ARRIVED, EventHandler.ATTACKED);
	}
	
	@Override
	public String onArrived(Creature character)
	{
		Attackable myself = (Attackable)character;
		switch (myself.getScriptValue())//switch (myself.getFleeingStatus())
		{
			case FLEEING_STARTED:
				if (isAtClanLocation(myself.getX(), myself.getY(), myself.getZ()))
				{
					startQuestTimerAtFixedRate("fleeing", myself, null, 15000);
					myself.setScriptValue(FLEEING_DONE_WAITING);
					(myself.getAI()).setGlobalAggro(-30);
					myself.forceWalkStance();
					myself.getAI().tryToIdle();
				}
				else
				{
					myself.forceRunStance();//.setRunning();
					myself.getAI().tryToMoveTo(new Location(getFleeToLocation(myself)), null);
				}
				break;
			case FLEEING_DONE_RETURNING:
				Spawn spawn = (Spawn) myself.getSpawn();
				// currently no movement restrictions when isReturningToSpawnPoint()
				if (myself.getX() == spawn.getLocX() && myself.getY() == spawn.getLocY())
					myself.setScriptValue(FLEEING_NOT_STARTED);
				else
					myself.getAI().tryToMoveTo(new Location(spawn.getSpawnLocation()), null);
				break;
		}
		return null;
	}

	private final static Location getFleeToLocation(Npc npc)
	{
		if (npc.getMoveAroundPos() == null)	//if (npc.getScriptValue() == 0)
		{
			double minSqDist = Double.MAX_VALUE;
			int minIndex = 0;
			for (int i = 0; i < CLAN_LOC.length; i++)
			{
				Location pos = CLAN_LOC[i];
				double sqDist = calculateDistanceSq(pos.getX(), pos.getY(), npc.getX(), npc.getY());
				if (sqDist < minSqDist)
				{
					minSqDist = sqDist;
					minIndex = i;
				}
			}
			npc.setMoveAroundPos(CLAN_LOC[minIndex]);
			npc.getAI().tryToMoveTo(new Location(CLAN_LOC[minIndex]), null);
		}
		return npc.getMoveAroundPos();
	}

	private final static boolean isAtClanLocation(int x, int y, int z)
	{
		for (Location pos : CLAN_LOC)
			if ((pos.getX() == x && pos.getY() == y) || (calculateDistanceSq(pos.getX(), pos.getY(), x, y) < MAX_GEO_PLAN_DIST))// && pos.z == z)
				return true;
		return false;
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			if (npc.getScriptValue() == FLEEING_NOT_STARTED)
			{
				double hp = npc.getStatus().getHp();
				if (hp < npc.getStatus().getMaxHp() / 2D && hp > npc.getStatus().getMaxHp() / 3D && attacker.getStatus().getHp() > attacker.getStatus().getMaxHp() / 4D && Rnd.get(100) < RUNAWAY_CHANCE)
				{
					int index = (ATTACKED.length - 1);
					int i5 = Rnd.get(100);
					if (i5 < 77)
						index = i5 / 7;
					else if (i5 < 95)
						index = (i5 - 77) / 2 + 11;
					npc.broadcastPacket(new CreatureSay(npc, SayType.ALL, ATTACKED[index]));
					if (npc.getNpcId() != 20076 && npc.getNpcId() != 20495)
					{
						npc.setScriptValue(FLEEING_STARTED);
						npc.disableCoreAi(true);
						npc.getAttack().stop();
						npc.forceRunStance();
						npc.getAI().tryToMoveTo(new Location(getFleeToLocation(npc)), null);
						List<Attackable> targets = npc.getKnownTypeInRadius(Monster.class, 2000).stream().filter(x -> !x.isDead() && x.getNpcId() == 20076 || x.getNpcId() == 20495).collect(Collectors.toList());
						if (!targets.isEmpty())
						{
							Attackable target = targets.stream().min((a1, a2) -> (int) calculateDistanceSq(a1.getX(), a1.getY(), a2.getX(), a2.getY())).get();
							
							npc.setFactionHelp(target);
							npc.setFactionEnemy(attacker);
						}
						startQuestTimerAtFixedRate("help", npc, attacker.getActingPlayer(), 2000);
					}
				}
			}
			else if (npc.getMoveAroundPos() != null && !npc.isMoving())
				if (npc.isCoreAiDisabled())
				{
					for (Monster clan : npc.getKnownTypeInRadius(Monster.class, npc.getTemplate().getAggroRange()))
						if (clan != null && npc.isIn3DRadius(attacker, 300))
							clan.forceAttack(attacker, 1);
					npc.disableCoreAi(false);
				}
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer) //npc.setBeenAttacked(false);
	{
		setReset(npc);
		super.onMyDying(npc, killer);
	}

	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("fleeing"))
		{
			if (npc.getScriptValue() == FLEEING_DONE_WAITING)
			{
				if (npc.getStatus().getHp() / npc.getStatus().getMaxHp() > 0.7 || ((Attackable) npc).getAggroList().getMostHated() == null)
				{
					// let the next rnd walk take care of "returning"
					//if (myself.getAI().getIntention() != CtrlIntention.AI_INTENTION_IDLE)
					npc.setScriptValue(FLEEING_NOT_STARTED);
					npc.disableCoreAi(false);
					cancelQuestTimers("fleeing");
				}
				else if (!npc.isRunning() && !npc.isInCombat())
					((Attackable) npc).getAI().setGlobalAggro(-30);
			}
			else
			{
				cancelQuestTimers("fleeing");
			}
		}
		else if (name.equalsIgnoreCase("help"))
		{
			if (npc.isCoreAiDisabled())
			{
				if (npc.isMoving())
				{
					if ((MathUtil.checkIfInRange(npc.getTemplate().getAggroRange(), npc, npc.getFactionHelp(), false)))
					{
						npc.disableCoreAi(false);
						for (Monster clan : npc.getKnownTypeInRadius(Monster.class, npc.getTemplate().getAggroRange()))
							((Attackable) clan).forceAttack(npc.getFactionEnemy(), 1);
					}
				}
				else
					npc.disableCoreAi(false);
			}
			else if (npc.getScriptValue() == FLEEING_NOT_STARTED)
			{
				cancelQuestTimers("help");
				startQuestTimer("reset", npc, null, 15000);
			}
		}
		else if (name.equalsIgnoreCase("reset"))
		{
			if (!npc.isDead() && !npc.getAttack().isAttackingNow())
			{
				setReset(npc);
				npc.getAI().tryToMoveTo(new Location(npc.getSpawnLocation()), null);
			}
		}
		return super.onTimer(name, npc, player);
	}
	
	private void setReset(Npc npc)
	{
		npc.setScriptValue(FLEEING_NOT_STARTED);
		npc.setMoveAroundPos(null);
		npc.disableCoreAi(false);
		cancelQuestTimers("fleeing");
		cancelQuestTimers("reset");
		cancelQuestTimers("help");
	}
	
	public static long calculateDistanceSq(int x1, int y1, int x2, int y2)
	{
		final long diffX = x1 - x2;
		final long diffY = y1 - y2;
		
		return diffX * diffX + diffY * diffY;
	}
}
