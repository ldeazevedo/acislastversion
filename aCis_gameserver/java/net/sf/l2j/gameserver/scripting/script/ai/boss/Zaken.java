package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.data.manager.SpawnManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;

public class Zaken extends AttackableAIScript
{
	private static final BossZone ZONE = ZoneManager.getInstance().getZoneById(110000, BossZone.class);
	private static final Set<Player> VICTIMS = ConcurrentHashMap.newKeySet();
	
	private static final Location[] LOCS =
	{
		new Location(53950, 219860, -3488),
		new Location(55980, 219820, -3488),
		new Location(54950, 218790, -3488),
		new Location(55970, 217770, -3488),
		new Location(53930, 217760, -3488),
		
		new Location(55970, 217770, -3216),
		new Location(55980, 219920, -3216),
		new Location(54960, 218790, -3216),
		new Location(53950, 219860, -3216),
		new Location(53930, 217760, -3216),
		
		new Location(55970, 217770, -2944),
		new Location(55980, 219920, -2944),
		new Location(54960, 218790, -2944),
		new Location(53950, 219860, -2944),
		new Location(53930, 217760, -2944)
	};
	
	// Grand boss
	private static final int ZAKEN = 29022;
	
	// Monsters
	private static final int DOLL_BLADER = 29023;
	private static final int VALE_MASTER = 29024;
	private static final int PIRATE_CAPTAIN = 29026;
	private static final int PIRATE_ZOMBIE = 29027;
	
	private static final byte DEAD = 0;
	private static final byte ALIVE = 1;
	
	private final Location _zakenLocation = new Location(0, 0, 0);
	
	private int _teleportCheck;
	private int _minionStatus;
	private int _hate;
	
	private boolean _hasTeleported;
	
	private AggroInfo _mostHated;
	
	public Zaken()
	{
		super("ai/boss");
	}
	
	@Override
	protected void registerNpcs()
	{
		addAttacked(ZAKEN);
		addClanAttacked(DOLL_BLADER, VALE_MASTER, PIRATE_CAPTAIN, PIRATE_ZOMBIE);
		addCreated(ZAKEN);
		addSeeCreature(ZAKEN, DOLL_BLADER, VALE_MASTER, PIRATE_CAPTAIN, PIRATE_ZOMBIE);
		addMyDying(ZAKEN, DOLL_BLADER, VALE_MASTER, PIRATE_CAPTAIN, PIRATE_ZOMBIE);
		addSeeSpell(ZAKEN);
		addUseSkillFinished(ZAKEN);
		
		addGameTime();
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (SpawnManager.getInstance().getSpawn(ZAKEN).getSpawnData().getStatus() == DEAD)
			return super.onTimer(name, npc, player);
		
		if (name.equalsIgnoreCase("1001"))
		{
			if (GameTimeTaskManager.getInstance().isNight())
			{
				L2Skill skill = FrequentSkill.ZAKEN_DAY_TO_NIGHT.getSkill();
				if (npc.getFirstEffect(skill) == null)
				{
					// Add effect "Day to Night" if not found.
					skill.getEffects(npc, npc);
					
					// Refresh stored Zaken location.
					_zakenLocation.set(npc.getPosition());
				}
				
				// Add Night regen if not found.
				skill = FrequentSkill.ZAKEN_REGEN_NIGHT.getSkill();
				if (npc.getFirstEffect(skill) == null)
					skill.getEffects(npc, npc);
				
				final AggroInfo ai = ((Attackable) npc).getAggroList().getMostHated();
				
				// Under attack stance, but didn't yet teleported. Check most hated and current victims distance.
				if (npc.getAI().getCurrentIntention().getType() == IntentionType.ATTACK && !_hasTeleported)
				{
					boolean willTeleport = true;
					
					// Check most hated distance. If distance is low, Zaken doesn't teleport.
					if (ai != null && ai.getAttacker().isIn3DRadius(_zakenLocation, 1500))
						willTeleport = false;
					
					// We're still under willTeleport possibility. Now we check each victim distance. If at least one is near Zaken, we cancel the teleport possibility.
					if (willTeleport && VICTIMS.stream().anyMatch(p -> p.isIn3DRadius(_zakenLocation, 1500)))
						willTeleport = false;
					
					// All targets are far, clear victims list and Zaken teleport.
					if (willTeleport)
					{
						VICTIMS.clear();
						npc.getAI().tryToCast(npc, FrequentSkill.ZAKEN_SELF_TELE.getSkill());
					}
				}
				
				// Potentially refresh the stored location.
				if (Rnd.get(20) < 1 && !_hasTeleported)
					_zakenLocation.set(npc.getPosition());
				
				// Process to cleanup hate from most hated upon 5 straight AI loops.
				if (npc.getAI().getCurrentIntention().getType() == IntentionType.ATTACK && ai != null)
				{
					if (_hate == 0)
					{
						_mostHated = ai;
						_hate = 1;
					}
					else
					{
						if (_mostHated == ai)
							_hate++;
						else
						{
							_hate = 1;
							_mostHated = ai;
						}
					}
				}
				
				// Cleanup build hate towards Intention IDLE.
				if (npc.getAI().getCurrentIntention().getType() == IntentionType.IDLE)
					_hate = 0;
				
				// We built enough hate ; release the current most hated target, reset the hate counter.
				if (_hate > 5)
				{
					_mostHated.stopHate();
					_hate = 0;
				}
			}
			else
			{
				L2Skill skill = FrequentSkill.ZAKEN_NIGHT_TO_DAY.getSkill();
				if (npc.getFirstEffect(skill) == null)
				{
					// Add effect "Night to Day" if not found.
					skill.getEffects(npc, npc);
					
					_teleportCheck = 3;
				}
				
				// Add Day regen if not found.
				skill = FrequentSkill.ZAKEN_REGEN_DAY.getSkill();
				if (npc.getFirstEffect(skill) == null)
					skill.getEffects(npc, npc);
			}
			
			if (Rnd.get(40) < 1)
				npc.getAI().tryToCast(npc, FrequentSkill.ZAKEN_SELF_TELE.getSkill());
		}
		else if (name.equalsIgnoreCase("1002"))
		{
			// Clear victims list.
			VICTIMS.clear();
			
			// Teleport Zaken.
			npc.getAI().tryToCast(npc, FrequentSkill.ZAKEN_SELF_TELE.getSkill());
			
			// Flag the teleport as false.
			_hasTeleported = false;
		}
		else if (name.equalsIgnoreCase("1003"))
		{
			if (_minionStatus == 1)
			{
				spawnMinionOnEveryLocation(PIRATE_CAPTAIN, 1);
				
				// Pass to the next spawn cycle.
				_minionStatus = 2;
			}
			else if (_minionStatus == 2)
			{
				spawnMinionOnEveryLocation(DOLL_BLADER, 1);
				
				// Pass to the next spawn cycle.
				_minionStatus = 3;
			}
			else if (_minionStatus == 3)
			{
				spawnMinionOnEveryLocation(VALE_MASTER, 2);
				
				// Pass to the next spawn cycle.
				_minionStatus = 4;
			}
			else if (_minionStatus == 4)
			{
				spawnMinionOnEveryLocation(PIRATE_ZOMBIE, 5);
				
				// Pass to the next spawn cycle.
				_minionStatus = 5;
			}
			else if (_minionStatus == 5)
			{
				addSpawn(DOLL_BLADER, 52675, 219371, -3290, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 52687, 219596, -3368, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 52672, 219740, -3418, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 52857, 219992, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 52959, 219997, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 53381, 220151, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 54236, 220948, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54885, 220144, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55264, 219860, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 55399, 220263, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55679, 220129, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 56276, 220783, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 57173, 220234, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 56267, 218826, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 56294, 219482, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 56094, 219113, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 56364, 218967, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 57113, 218079, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 56186, 217153, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55440, 218081, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 55202, 217940, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55225, 218236, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54973, 218075, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 53412, 218077, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 54226, 218797, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 54394, 219067, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54139, 219253, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 54262, 219480, -3488, Rnd.get(65536), false, 0, true);
				
				// Pass to the next spawn cycle.
				_minionStatus = 6;
			}
			else if (_minionStatus == 6)
			{
				addSpawn(PIRATE_ZOMBIE, 53412, 218077, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 54413, 217132, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 54841, 217132, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 55372, 217128, -3343, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 55893, 217122, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 56282, 217237, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 56963, 218080, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 56267, 218826, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 56294, 219482, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 56094, 219113, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 56364, 218967, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 56276, 220783, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 57173, 220234, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54885, 220144, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55264, 219860, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 55399, 220263, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55679, 220129, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 54236, 220948, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 54464, 219095, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 54226, 218797, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 54394, 219067, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54139, 219253, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 54262, 219480, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 53412, 218077, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55440, 218081, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 55202, 217940, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55225, 218236, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54973, 218075, -3216, Rnd.get(65536), false, 0, true);
				
				// Pass to the next spawn cycle.
				_minionStatus = 7;
			}
			else if (_minionStatus == 7)
			{
				addSpawn(PIRATE_ZOMBIE, 54228, 217504, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 54181, 217168, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 54714, 217123, -3168, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 55298, 217127, -3073, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 55787, 217130, -2993, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 56284, 217216, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 56963, 218080, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 56267, 218826, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 56294, 219482, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 56094, 219113, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 56364, 218967, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 56276, 220783, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 57173, 220234, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54885, 220144, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55264, 219860, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 55399, 220263, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55679, 220129, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 54236, 220948, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 54464, 219095, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 54226, 218797, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALE_MASTER, 54394, 219067, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54139, 219253, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(DOLL_BLADER, 54262, 219480, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 53412, 218077, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 54280, 217200, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55440, 218081, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_CAPTAIN, 55202, 217940, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 55225, 218236, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATE_ZOMBIE, 54973, 218075, -2944, Rnd.get(65536), false, 0, true);
				
				cancelQuestTimers("1003");
			}
		}
		else if (name.equalsIgnoreCase("CreateOnePrivateEx"))
			addSpawn(npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ(), Rnd.get(65535), false, 0, true);
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		// Curses
		if (attacker instanceof Playable && attacker.testCursesOnAttack(npc))
			return;
		
		if (Rnd.get(10) < 1)
			callSkills(npc, attacker);
		
		if (!GameTimeTaskManager.getInstance().isNight() && (npc.getStatus().getHp() < (npc.getStatus().getMaxHp() * _teleportCheck) / 4))
		{
			_teleportCheck -= 1;
			npc.getAI().tryToCast(npc, FrequentSkill.ZAKEN_SELF_TELE.getSkill());
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public void onClanAttacked(Attackable caller, Attackable called, Creature attacker, int damage)
	{
		if (caller.getNpcId() == ZAKEN && GameTimeTaskManager.getInstance().isNight())
		{
			if (called.getAI().getCurrentIntention().getType() == IntentionType.IDLE && !_hasTeleported && caller.getStatus().getHpRatio() < 0.9 && Rnd.get(450) < 1)
			{
				// Set the teleport flag as true.
				_hasTeleported = true;
				
				// Edit Zaken stored location.
				_zakenLocation.set(called.getPosition());
				
				// Run the 1002 timer.
				startQuestTimer("1002", caller, null, 300);
			}
		}
		
		super.onClanAttacked(caller, called, attacker, damage);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		// Reset variables.
		_teleportCheck = 3;
		_hate = 0;
		_hasTeleported = false;
		_mostHated = null;
		
		// Store current Zaken position.
		_zakenLocation.set(npc.getPosition());
		
		// Clear victims list.
		VICTIMS.clear();
		
		// If Zaken is on its lair, begin the minions spawn cycle.
		if (ZONE.isInsideZone(npc))
		{
			_minionStatus = 1;
			startQuestTimerAtFixedRate("1003", null, null, 1700);
		}
		
		// Generic task is running from now.
		startQuestTimerAtFixedRate("1001", npc, null, 1000, 30000);
		
		npc.broadcastPacket(new PlaySound(1, "BS01_A", npc));
		
		super.onCreated(npc);
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (npc.getNpcId() == ZAKEN)
		{
			// Broadcast death sound.
			npc.broadcastPacket(new PlaySound(1, "BS02_D", npc));
			
			// Cancel tasks.
			cancelQuestTimers("1001");
			cancelQuestTimers("1003");
		}
		else if (SpawnManager.getInstance().getSpawn(ZAKEN).getSpawnData().getStatus() == ALIVE)
			startQuestTimer("CreateOnePrivateEx", npc, null, ((30 + Rnd.get(60)) * 1000));
		
		super.onMyDying(npc, killer);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (creature instanceof Playable)
		{
			final Player player = creature.getActingPlayer();
			
			if (ZONE.isInsideZone(npc))
				((Attackable) npc).getAggroList().addDamageHate(creature, 1, 200);
			
			if (npc.getNpcId() == ZAKEN)
			{
				// Feed victims list, but only if not already full.
				if (Rnd.get(3) < 1 && VICTIMS.size() < 5)
					VICTIMS.add(player);
				
				// Cast a skill.
				if (Rnd.get(15) < 1)
					callSkills(npc, creature);
			}
			else if (creature.testCursesOnAggro(npc))
				return;
		}
		
		super.onSeeCreature(npc, creature);
	}
	
	@Override
	public void onSeeSpell(Npc npc, Player caster, L2Skill skill, Creature[] targets, boolean isPet)
	{
		if (Rnd.get(12) < 1)
			callSkills(npc, caster);
		
		super.onSeeSpell(npc, caster, skill, targets, isPet);
	}
	
	@Override
	public void onUseSkillFinished(Npc npc, Player player, L2Skill skill)
	{
		switch (skill.getId())
		{
			case 4222: // Instant Move; teleport Zaken to a defined, random Location.
				_zakenLocation.set(Rnd.get(LOCS));
				_zakenLocation.addPositiveOffset(650);
				
				((Attackable) npc).getAggroList().cleanAllHate();
				npc.teleportTo(_zakenLocation, 0);
				break;
			
			case 4216: // Scatter Enemy ; teleport the targeted Player to a defined, random Location.
				Location loc = Rnd.get(LOCS).clone();
				loc.addPositiveOffset(650);
				
				((Attackable) npc).getAggroList().stopHate(player);
				player.teleportTo(loc, 0);
				break;
			
			case 4217: // Mass Teleport ; teleport victims and targeted Player, each on a defined, random Location.
				for (Player victim : VICTIMS)
				{
					if (victim.isIn3DRadius(player, 250))
					{
						loc = Rnd.get(LOCS).clone();
						loc.addPositiveOffset(650);
						
						((Attackable) npc).getAggroList().stopHate(victim);
						victim.teleportTo(loc, 0);
					}
				}
				
				loc = Rnd.get(LOCS).clone();
				loc.addPositiveOffset(650);
				
				((Attackable) npc).getAggroList().stopHate(player);
				player.teleportTo(loc, 0);
				break;
		}
		super.onUseSkillFinished(npc, player, skill);
	}
	
	@Override
	public void onGameTime(int gameTime)
	{
		if (gameTime == 0)
		{
			final Door door = DoorData.getInstance().getDoor(21240006);
			if (door != null)
				door.openMe();
		}
	}
	
	/**
	 * Call skills depending of luck and specific events.
	 * @param npc : The npc who casts the spell (Zaken).
	 * @param target : The target Zaken currently aims at.
	 */
	private static void callSkills(Npc npc, Creature target)
	{
		final int chance = Rnd.get(225);
		if (chance < 1)
			npc.getAI().tryToCast(target, FrequentSkill.ZAKEN_TELE.getSkill());
		else if (chance < 2)
			npc.getAI().tryToCast(target, FrequentSkill.ZAKEN_MASS_TELE.getSkill());
		else if (chance < 4)
			npc.getAI().tryToCast(target, FrequentSkill.ZAKEN_HOLD.getSkill());
		else if (chance < 8)
			npc.getAI().tryToCast(target, FrequentSkill.ZAKEN_DRAIN.getSkill());
		else if (chance < 15)
		{
			if (target != ((Attackable) npc).getAggroList().getMostHatedCreature() && npc.isIn3DRadius(target, 100))
				npc.getAI().tryToCast(target, FrequentSkill.ZAKEN_MASS_DUAL_ATTACK.getSkill());
		}
		
		if (target == ((Attackable) npc).getAggroList().getMostHatedCreature() && Rnd.nextBoolean())
			npc.getAI().tryToCast(target, FrequentSkill.ZAKEN_DUAL_ATTACK.getSkill());
	}
	
	/**
	 * Spawn one {@link Npc} on every {@link Location} from the LOCS array. Process it for the roundsNumber amount.
	 * @param npcId : The npcId to spawn.
	 * @param roundsNumber : The rounds number to process.
	 */
	private void spawnMinionOnEveryLocation(int npcId, int roundsNumber)
	{
		for (Location loc : LOCS)
		{
			for (int i = 0; i < roundsNumber; i++)
			{
				final int x = loc.getX() + Rnd.get(650);
				final int y = loc.getY() + Rnd.get(650);
				
				addSpawn(npcId, x, y, loc.getZ(), Rnd.get(65536), false, 0, true);
			}
		}
	}
}