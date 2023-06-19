package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.skills.FlyType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.zone.type.ScriptZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceled;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

public class Frintezza extends AttackableAIScript
{
	private static final ScriptZone FRINTEZZA_LAIR = ZoneManager.getInstance().getZoneById(100004, ScriptZone.class);
	
	public static final int FRINTEZZA = 29045;
	private static final int SCARLET1 = 29046;
	private static final int SCARLET2 = 29047;
	
	// Frintezza Status Tracking :
	public static final byte DORMANT = 0; // No one has entered yet. Entry is unlocked
	public static final byte WAITING = 1; // Someone has entered, triggering a 35 minute window for additional people to enter before he unleashes his attack. Entry is unlocked
	public static final byte FIGHTING = 2; // Frintezza is engaged in battle, annihilating his foes. Entry is locked
	public static final byte DEAD = 3; // Frintezza has been killed. Entry is locked
	
	private Player _actualVictim; // Actual target of Scarlet.
	private static long _lastAttackTime = 0;

	private static GrandBoss scarlet, frintezza;

	private final List<Npc> _dummys = new CopyOnWriteArrayList<>();
	private final List<Monster> demons = new CopyOnWriteArrayList<>();
	private final Map<Monster, Integer> portraits = new ConcurrentHashMap<>();
	
	private final boolean unconfirme = true;

	// Skills
	private static final int DEWDROP_OF_DESTRUCTION_SKILL_ID = 2276;
	private static final int BREAKING_ARROW_SKILL_ID = 2234;
	
	private L2Skill songEffect = null;
	private static int[][] scarletSkills;
	
	private static final IntIntHolder Bomber_Ghost = new IntIntHolder(5011, 1);
	
	protected static final int[][] PORTRAIT_GHOST =
	{
		{29048, 175833, -87165, -5126, 35048, 175833, -87165, -5100, 35048},
		{29048, 172634, -87165, -5126, 57730, 172634, -87165, -5100, 57730},
		{29049, 175876, -88713, -5126, 28205, 175876, -88713, -5100, 28205},
		{29049, 172608, -88702, -5126, 64817, 172608, -88702, -5100, 64817}
	};
	
	protected static final int[][] DUMMY_SPAWNS =
	{
		{29052, 174240, -89805, -5022, 16048},
		{29052, 174232, -88020, -5110, 16384},
		{29052, 172450, -87890, -5100, 16048},
		{29052, 176012, -87890, -5100, 16048},
		{29053, 174232, -88020, -5110, 16384}
	};
	
	public Frintezza()
	{
		super("ai/boss");
		
		StatSet info = GrandBossManager.getInstance().getStatSet(FRINTEZZA);
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == DEAD)
		{
			long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
			if (temp > 0)
				startQuestTimer("frintezza_unlock", null, null, temp);
			else
				GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
		}
		else if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) != DORMANT)
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
	}
	
	@Override
	protected void registerNpcs()
	{
		addAttacked(SCARLET1,  SCARLET2, FRINTEZZA, 29048, 29049);
		addCreated(FRINTEZZA, SCARLET1, SCARLET2);
		addMyDying(SCARLET2, 29048, 29049, 29050, 29051);
		addSeeSpell(29050, 29051);
		addUseSkillFinished(FRINTEZZA, 29050, 29051);
		addZoneExit(100004);
	}

	@Override
	public void onCreated(Npc npc)
	{
		npc.disableCoreAi(true);
		super.onCreated(npc);
	}

	@Override
	public String onTimer(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("start"))
		{
			for (Creature cha : FRINTEZZA_LAIR.getKnownTypeInside(Player.class))
				if (cha instanceof Player)
					cha.broadcastPacket(new Earthquake(cha, 45, 27, false));
			clean(WAITING);
			dropTimers();
			startQuestTimer("beginning", null, null, 4000);
		}
		else if (event.equalsIgnoreCase("beginning")) //TODO: beginning
		{
			startQuestTimer("camera_00", null, null, 29000);
			startQuestTimer("camera_01", null, null, 30000);
			startQuestTimer("camera_02", null, null, 31000); //100
			startQuestTimer("camera_03", null, null, 37500); //6500
			startQuestTimer("camera_04", null, null, 38400); //900
			startQuestTimer("camera_05", null, null, 42400); //4000 ex - cam5b 
			startQuestTimer("camera_06", null, null, 43750); //1350
			startQuestTimer("camera_07", null, null, 51750); //7000
			startQuestTimer("camera_08", null, null, 52150); //1000
			startQuestTimer("camera_09", null, null, 54150); //2000
			startQuestTimer("camera_10", null, null, 58650); //4500
			startQuestTimer("camera_11", null, null, 59350); //700
			startQuestTimer("camera_12", null, null, 60650); //1300 -15
			startQuestTimer("camera_13", null, null, 62150); //1500 -16
			startQuestTimer("camera_14", null, null, 69650); //7500 -17
			startQuestTimer("camera_15", null, null, 79150); //9500 -16 //79350//200
			startQuestTimer("camera_16", null, null, 81750); //2400 //81550 //81150
			startQuestTimer("camera_17", null, null, 84150); //5000
			startQuestTimer("camera_18", null, null, 84850); //6300 -85450 -throw_up
			startQuestTimer("camera_19", null, null, 87650); //3500
			startQuestTimer("camera_20", null, null, 89650); //2000
			startQuestTimer("camera_21", null, null, 92650); //3000
			startQuestTimer("camera_22", null, null, 94650); //2000
		}
		else if (event.equalsIgnoreCase("despawn"))
		{
			if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING && (System.currentTimeMillis() - _lastAttackTime) >= 900000)
			{
				clean(DORMANT);
				dropTimers();
			}
		}
		else if (event.equalsIgnoreCase("camera_00")) //TODO: camera 00
		{
			for (int[] loc : DUMMY_SPAWNS)
			{
				Npc dummy = addSpawn(loc[0], loc[1], loc[2], loc[3], loc[4], false, 62650, false);
				dummy.setIsImmobilized(true);
				dummy.setIsParalyzed(true);
				dummy.disableCoreAi(true);
				_dummys.add(dummy);
			}
			_dummys.get(2).setIsFlying(true);
			_dummys.get(3).setIsFlying(true);
			_dummys.get(1).setCollisionHeight(600);
			setPrepareRoom(true);
		}
		else if (event.equalsIgnoreCase("camera_01"))
		{
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, FIGHTING);
			frintezza = (GrandBoss) addSpawn(FRINTEZZA, 174240, -89805, -5022, 16048, false, 0, false);
			frintezza.setInvul(true);
			frintezza.decayMe();
		}
		else if (event.equalsIgnoreCase("camera_02"))
		{
			for (int[] ghosts : PORTRAIT_GHOST)
			{
				Monster demon = (Monster) addSpawn(ghosts[0] + 2, ghosts[5], ghosts[6], ghosts[7], ghosts[8], false, 0, false);
				demon.setIsImmobilized(true);
				demon.disableAllSkills();
				demons.add(demon);
			}
			frintezza.spawnMe();
			_dummys.get(1).decayMe();
			broadcastPacket(new NpcInfo(_dummys.get(1), null));
			GrandBossManager.getInstance().addBoss(frintezza);
			broadcastPacket(new SpecialCamera(_dummys.get(1).getObjectId(), 0, 75, -89, 0, 100, 0, 0, 1, 0), new SpecialCamera(_dummys.get(1).getObjectId(), 0, 75, -89, 0, 100, 0, 0, 1, 0), new ValidateLocation(frintezza), new SpecialCamera(_dummys.get(1).getObjectId(), 300, 90, -10, 6500, 7000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("camera_03"))
			broadcastPacket(new SpecialCamera(_dummys.get(0).getObjectId(), 1800, 90, 8, 6500, 7000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_04"))
			broadcastPacket(new SpecialCamera(_dummys.get(0).getObjectId(), 140, 90, 10, 2500, 4500, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_05"))
			broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 40, 75, -10, 0, 1000, 0, 0, 1, 0), new SpecialCamera(frintezza.getObjectId(), 40, 75, -10, 0, 12000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_06"))
			broadcastPacket(new SocialAction(frintezza, 2));
		else if (event.equalsIgnoreCase("camera_07"))
			broadcastPacket(new SocialAction(demons.get(1), 1), new SocialAction(demons.get(3), 1)); // TODO: 1 y 3
		else if (event.equalsIgnoreCase("camera_08"))
		{
			broadcastPacket(new SocialAction(demons.get(0), 1), new SocialAction(demons.get(2), 1));
			_dummys.get(2).instantTeleportTo(172450, -87890, -5100, 0);
			_dummys.get(3).instantTeleportTo(176012, -87890, -5100, 0);
			for (Creature pc : FRINTEZZA_LAIR.getKnownTypeInside(Creature.class))
				if (pc instanceof Player)
					if (pc.getX() < 174232)
						broadcastPacket(new SpecialCamera(_dummys.get(2).getObjectId(), 1000, 118, 0, 0, 1000, 0, 0, 1, 0), new SpecialCamera(_dummys.get(2).getObjectId(), 1000, 118, 0, 0, 10000, 0, 0, 1, 0));
					else
						broadcastPacket(new SpecialCamera(_dummys.get(3).getObjectId(), 1000, 62, 0, 0, 1000, 0, 0, 1, 0), new SpecialCamera(_dummys.get(3).getObjectId(), 1000, 62, 0, 0, 10000, 0, 0, 1, 0));
	/*		for (Creature pc : cc)
				if (pc instanceof Player)
				{
					boolean loc = pc.getX() < 174232;
					broadcastPacket(new SpecialCamera(_dummys.get(loc ? 2 : 3).getObjectId(), 1000, loc ? 118 : 62, 0, 0, 1000, 0, 0, 1, 0), new SpecialCamera(_dummys.get(loc ? 2 : 3).getObjectId(), 1000, loc ? 118 : 62, 0, 0, 10000, 0, 0, 1, 0));
				}*/
		}
		else if (event.equalsIgnoreCase("camera_09"))
			broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 240, 90, 0, 0, 1000, 0, 0, 1, 0), new SpecialCamera(frintezza.getObjectId(), 240, 90, 25, 5500, 10000, 0, 0, 1, 0), new SocialAction(frintezza, 3));
		else if (event.equalsIgnoreCase("camera_10"))
			broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 100, 195, 35, 0, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_11"))
		{
			broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 100, 195, 35, 0, 10000, 0, 0, 1, 0));
			if (unconfirme)
				playFrintezzaMelody();
		}
		else if (event.equalsIgnoreCase("camera_12"))
		{
			scarlet = (GrandBoss) addSpawn(SCARLET1, 174232, -88020, -5110, 16384, false, 0, true);
			scarlet.setRightHandItemId(8204);
			scarlet.decayMe();
			broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 120, 180, 45, 1500, 10000, 0, 0, 1, 0));
			if (!unconfirme)
				playFrintezzaMelody();
		}
		else if (event.equalsIgnoreCase("camera_13"))
			broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 520, 135, 45, 8000, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_14"))
			broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 1500, 110, 25, 10000, 13000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_15"))
			broadcastPacket(new SpecialCamera(_dummys.get(1).getObjectId(), 930, 160, -20, 0, 1000, 0, 0, 1, 0), new MagicSkillUse(_dummys.get(4), _dummys.get(4), 5004, 1, 5800, 0), new SpecialCamera(_dummys.get(1).getObjectId(), 600, 180, -25, 0, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_16"))
			scarlet.spawnMe();
		else if (event.equalsIgnoreCase("camera_17"))
			broadcastPacket(new SocialAction(_dummys.get(4), 3), new SpecialCamera(_dummys.get(4).getObjectId(), 800, 180, 10, 1000, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_18")) // TODO: throw_up
			throwUp(_dummys.get(4), 500, SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5004, 1));
		else if (event.equalsIgnoreCase("camera_19"))
			broadcastPacket(new SpecialCamera(_dummys.get(4).getObjectId(), 300, 60, 8, 0, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_20"))
			broadcastPacket(new SpecialCamera(scarlet.getObjectId(), 500, 90, 10, 1000, 5000, 0, 0, 1, 0), new SpecialCamera(scarlet.getObjectId(), 500, 90, 10, 3000, 5000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_21"))
		{
			for (int i = 0; i < PORTRAIT_GHOST.length; i++)
			{
				Monster portrait = (Monster) addSpawn(PORTRAIT_GHOST[i][0], PORTRAIT_GHOST[i][1], PORTRAIT_GHOST[i][2], PORTRAIT_GHOST[i][3], PORTRAIT_GHOST[i][4], false, 0, false);
				portrait.setIsImmobilized(true);
				portrait.disableCoreAi(true);
				portraits.put(portrait, i);
			}
			if (!_dummys.isEmpty())
			{
				for (Npc n : _dummys)
					deleteNpc(n);
				_dummys.clear();
			}
		}
		else if (event.equalsIgnoreCase("camera_22")) //TODO: camera_22
		{
			roomTasks();
			setPrepareRoom(false);
			frintezza.setInvul(false);
			startQuestTimerAtFixedRate("spawn_minion", null, null, 20000);
		}
		else if (event.equalsIgnoreCase("spawn_minion")) //TODO: spawn_minion
		{
			if (frintezza != null && !frintezza.isDead() && !portraits.isEmpty())
			{
				if (!frintezza.isInvul())
					for (int i : portraits.values())
					{
						if (demons.size() > 24)
							break;
						Monster demon = (Monster) addSpawn(PORTRAIT_GHOST[i][0] + 2, PORTRAIT_GHOST[i][5], PORTRAIT_GHOST[i][6], PORTRAIT_GHOST[i][7], PORTRAIT_GHOST[i][8], false, 0, false);
						demon.setRaidRelated();
						demon.forceRunStance();
						demons.add(demon);
						demon.disableSkill(Bomber_Ghost.getSkill(), -1); // Bomb
						startQuestTimer("action", demon, null, 200);
					}
			}
			else 
				cancelQuestTimers("spawn_minion");
		}
		else if (event.equalsIgnoreCase("end_camera"))
			npc.setInvul(false);
		else if (event.equalsIgnoreCase("first_morph_01")) //TODO: first_morph
		{
			npc.stopSkillEffects(5008);
			setPrepareRoom(true);
			broadcastPacket(new SpecialCamera(npc.getObjectId(), 250, calcCameraAngle(npc), 12, 2000, 15000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("first_morph_02"))
		{
			broadcastPacket(new SocialAction(npc, 1));
			npc.setRightHandItemId(7903);
		}
		else if (event.equalsIgnoreCase("first_morph_03"))
			daemonMorph(npc, 1);
		else if (event.equalsIgnoreCase("second_morph_01"))
		{
			setPrepareRoom(true);
			broadcastPacket(new SocialAction(frintezza, 4));
		}
		else if (event.equalsIgnoreCase("second_morph_02"))
		{
			broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 250, 120, 15, 0, 1000, 0, 0, 1, 0), new SpecialCamera(frintezza.getObjectId(), 250, 120, 15, 0, 10000, 0, 0, 1, 0));
			npc.setIsImmobilized(true);
		}
		else if (event.equalsIgnoreCase("second_morph_03"))
		{
			playFrintezzaMelody();
			broadcastPacket(new SpecialCamera(npc.getObjectId(), 500, 70, 15, 3000, 10000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("second_morph_04"))
			broadcastPacket(new SpecialCamera(npc.getObjectId(), 2500, 90, 12, 6000, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("second_morph_05"))
			broadcastPacket(new SpecialCamera(npc.getObjectId(), 250, calcCameraAngle(npc), 12, 0, 1000, 0, 0, 1, 0), new SpecialCamera(npc.getObjectId(), 250, calcCameraAngle(npc), 12, 0, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("second_morph_06"))
		{
			npc.doDie(npc);
			broadcastPacket(new SpecialCamera(npc.getObjectId(), 450, calcCameraAngle(npc), 14, 8000, 8000, 0, 0, 1, 0));
			scarlet = null;
		}
		else if (event.equalsIgnoreCase("second_morph_07"))
			deleteNpc(npc);
		else if (event.equalsIgnoreCase("second_morph_08"))
		{
			scarlet = (GrandBoss) addSpawn(SCARLET2, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, false);
			broadcastPacket(new SpecialCamera(scarlet.getObjectId(), 450, calcCameraAngle(npc), 12, 500, 14000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("second_morph_09"))
			daemonMorph(scarlet, 2);
		else if (event.equalsIgnoreCase("start_room"))
			setPrepareRoom(false);
		else if (event.equalsIgnoreCase("die_01")) //TODO: die_01
			npc.doDie(npc);
		else if (event.equalsIgnoreCase("die_02"))
			broadcastPacket(new SpecialCamera(npc.getObjectId(), 100, 120, 5, 0, 7000, 0, 0, 1, 0), new SpecialCamera(npc.getObjectId(), 100, 90, 5, 5000, 15000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("die_03"))
		{
			broadcastPacket(new SpecialCamera(npc.getObjectId(), 900, 90, 25, 7000, 10000, 0, 0, 1, 0));
			addSpawn(29061, 174232, -88020, -5114, 16384, false, 900000, false);
			ScriptData.getInstance().getQuest("LastImperialTomb").startQuestTimer("remove_players", null, null, 900000);
		}
		else if (event.equalsIgnoreCase("die_04"))
		{
			cancelQuestTimers("spawn_minion");
			clean(DEAD);
			long respawnTime = (long) Config.SPAWN_INTERVAL_FRINTEZZA + Rnd.get(-Config.RANDOM_SPAWN_TIME_FRINTEZZA, Config.RANDOM_SPAWN_TIME_FRINTEZZA);
			respawnTime *= 3600000;
			startQuestTimer("frintezza_unlock", npc, null, respawnTime);
			// also save the respawn time so that the info is maintained past reboots
			StatSet info = GrandBossManager.getInstance().getStatSet(FRINTEZZA);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(FRINTEZZA, info);
		}
		else if (event.equalsIgnoreCase("songs_play")) //TODO: play song
			callSongAI();
		else if (event.equalsIgnoreCase("skill_task")) //TODO: skill_task
		{
			if (npc.isInvul() || npc.getCast().isCastingNow() || npc.isDead() || frintezza.isInvul())
				return super.onTimer(event, npc, player);
			
			final L2Skill skill = getRandomSkill();
			int rnd = Rnd.get(10);
			
			if (_actualVictim == null || _actualVictim.isDead() || (!npc.isMoving() || npc.isMoving()) && rnd == 0)
				_actualVictim = getRandomPlayer(npc);
			if (_actualVictim == null)
				return super.onTimer(event, npc, player);
			
			int range = skill.getCastRange();
			if (_actualVictim.isMoving())
	        	range = range * 80 / 100;
			if (MathUtil.checkIfInRange((int) (range + npc.getCollisionRadius()), npc, _actualVictim, true))
			{
				npc.getAI().tryToIdle();
				npc.getAI().tryToCast(_actualVictim, skill);
				if (rnd < 0)
					_actualVictim = null;
			}
			else
			{
				npc.forceRunStance();
				npc.getAI().tryToMoveTo(new Location(_actualVictim.getX(), _actualVictim.getY(), _actualVictim.getZ()), null);//npc.getAI().setIntention(CtrlIntention.FOLLOW, _actualVictim, null);
			}
		}
		else if (event.equalsIgnoreCase("songs_effect"))
		{
			if (scarlet == null || frintezza == null || scarlet.isDead() || scarlet.isInvul() || frintezza.isDead() || frintezza.isInvul() || songEffect == null)
				return super.onTimer(event, npc, player);
			
			if (frintezza.getScriptValue() == 1)
			{
				for (Creature pc : FRINTEZZA_LAIR.getKnownTypeInside(Creature.class))
					if (pc instanceof Player)
						scarlet.getCast().doCast(songEffect, songEffect.getLevel() < 4 ? scarlet : pc, null);
			}
			else
				cancelQuestTimers("songs_effect");
		}
		else if (event.equalsIgnoreCase("end_song")) // Stop effect song
			npc.setScriptValue(3);
		else if (event.equalsIgnoreCase("timeNextSong")) //BreakingArrow - time next song
			npc.setScriptValue(0);
		else if (event.equalsIgnoreCase("action"))
			broadcastPacket(new SocialAction(npc, 1));
		else if (event.equalsIgnoreCase("frintezza_unlock"))
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
		return super.onTimer(event, npc, player);
	}
	
	/**
	 * 
	 */
	private void callSongAI()
	{
		if (frintezza == null || frintezza.isDead() || frintezza.isInvul() || frintezza.getScriptValue() == 1)
			return;
		else if (frintezza.getScriptValue() == 0)
		{
			boolean isRequiem = getState() == 3 && scarlet != null && scarlet.getStatus().getHp() < scarlet.getStatus().getMaxHp() * 0.6 && 100 < Rnd.get(80);
			// new song play
			int rnd = Rnd.get(100);
			for (FrintezzaSong song : FRINTEZZA_SONG_LIST)
			{
				int songLvl = song.skill.getSkill().getLevel();
				if (rnd < song.chance || isRequiem && songLvl == 1)
				{
					L2Skill sk = song.skill.getSkill();
					songEffect = song.effectSkill.getSkill();
					int hitTime = sk.getHitTime();
					broadcastPacket(new ExShowScreenMessage(1, -1, SMPOS.TOP_CENTER, false, 0, 0, 0, true, 4000, false, song.songName));
					broadcastPacket(new MagicSkillUse(frintezza, frintezza, sk.getId(), songLvl, hitTime, sk.getReuseDelay()));
					startQuestTimer("songs_effect", 10000, scarlet, null, true);
					startQuestTimer("endSong", frintezza, null, hitTime);
					startQuestTimer("timeNextSong", frintezza, null, hitTime + Rnd.get(10000, 15000)/*Rnd.get(30000, 120000)*/);
					frintezza.setScriptValue(1);
					return;
					/*	for (Creature pc : FRINTEZZA_LAIR.getKnownTypeInside(Creature.class))
					if (pc instanceof Player)
						frintezza.getCast().doCast(songEffect, songEffect.getId() == 5008 ? pc : frintezza, null);*/
				}
			}
		}
		else if (frintezza.getScriptValue() == 2)
		{
			frintezza.setScriptValue(3);
			startQuestTimer("timeNextSong", frintezza, null, Rnd.get(30000, 120000));
		}
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) != FIGHTING || npc.isInvul())
			return;
		
		if (!FRINTEZZA_LAIR.isInsideZone(attacker))
		{
			attacker.teleportTo(150037 + Rnd.get(500), -57720 + Rnd.get(500), -2976, 0);
			return;
		}
		if (!FRINTEZZA_LAIR.isInsideZone(npc) || npc.getX() < 171932 || npc.getX() > 176532 || npc.getY() < -90320 || npc.getY() > -85720 || npc.getZ() < -5130)
		{
			npc.enableAllSkills();
			npc.setIsImmobilized(false);
			npc.teleportTo(174232, -88020, -5116, 0);
			return;
		}

		switch (npc.getNpcId())
		{
			// When Dewdrop of Destruction is used on Portraits they suicide.
			case 29048:
			case 29049:
			if (skill != null && (skill.getId() == DEWDROP_OF_DESTRUCTION_SKILL_ID))
				if (attacker.getTarget() == npc)
					npc.doDie(attacker);
			break;
			case 29050:
			case 29051:
				double hp = npc.getStatus().getHp();
				double mm = npc.getStatus().getMaxHp() * 0.10;
				if (hp >= mm && hp - damage > 0 && hp - damage < mm)
					npc.enableSkill(Bomber_Ghost.getSkill());
				break;
			case FRINTEZZA:
				if (skill != null && skill.getId() == BREAKING_ARROW_SKILL_ID)
				{
					npc.setScriptValue(2);
					npc.abortAll(true);
					npc.getAI().tryToIdle();
						for (Creature ch : FRINTEZZA_LAIR.getKnownTypeInside(Creature.class))
							if (ch instanceof Player)
								ch.stopSkillEffects(5008);
						if (scarlet != null)
							scarlet.stopSkillEffects(5008);
						broadcastPacket(new MagicSkillCanceled(frintezza.getObjectId()));
				}
				npc.getStatus().setHp(npc.getStatus().getMaxHp(), true);
				break;
			case SCARLET1:
				synchronized (this)
				{
					taskCameraMorph(getState(), npc);
					break;
				}
		}
		_lastAttackTime = System.currentTimeMillis();
		super.onAttacked(npc, attacker, damage, skill);
	}

	@Override
	public void onUseSkillFinished(Npc npc, Player player, L2Skill skill)
	{
		if (skill.isSuicideAttack() && npc.getNpcId() != FRINTEZZA)
			this.onMyDying(npc, null);
		super.onUseSkillFinished(npc, player, skill);
	}
	
	@Override
	public void onMyDying(Npc npc, Creature killer)
	{
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
		{
			if (npc.getNpcId() == SCARLET2)
				taskCameraMorph(getState(), npc);
			if (portraits.containsKey(npc))
				portraits.remove(npc);
			if (demons.contains(npc))
				demons.remove(npc);
		}
		super.onMyDying(npc, killer);
	}
	
	private static L2Skill getRandomSkill() //TODO: getRandomSkill()
	{
		int[][] FIRST_SCARLET_SKILLS = 
		{
			{ 5014, 1, 100}, // Frintezza's Daemon Attack 1
			{ 5015, 1, 5  }, // Frintezza's Daemon Charge 1
			{ 5015, 4, 5  }, // Frintezza's Daemon Charge 4
		};
		int[][] SECOND_SCARLET_SKILLS = 
		{
			{ 5014, 2, 100}, // Frintezza's Daemon Attack
			{ 5015, 2, 5  }, // Frintezza's Daemon Charge 2
			{ 5015, 5, 5  }, // Frintezza's Daemon Charge 5
			{ 5018, 1, 10 }, // Frintezza's Daemon Field 1
			{ 5016, 1, 10 }, // Yoke of Scarlet
		};
		int[][] THIRD_SCARLET_SKILLS = 
		{
			{ 5014, 3, 100}, // Frintezza's Daemon Attack
			{ 5015, 3, 5  }, // Frintezza's Daemon Charge 3
			{ 5015, 6, 5  }, // Frintezza's Daemon Charge 6
			{ 5018, 2, 10 }, // Frintezza's Daemon Field 2
			{ 5019, 1, 10 }, // Frintezza's Daemon Drain
			{ 5016, 1, 10 }, // Yoke of Scarlet
		};
		int skillLvl = getState();
		if (skillLvl == 1)
			scarletSkills = FIRST_SCARLET_SKILLS;
		else if (skillLvl == 2)
			scarletSkills = SECOND_SCARLET_SKILLS;
		else if (skillLvl == 3)
			scarletSkills = THIRD_SCARLET_SKILLS;
		
		int rnd = Rnd.get(100);
		for (int i = scarletSkills.length; --i >= 0;)
		{
			int[] sk = scarletSkills[i];
			int chance = sk[2];
			if ((rnd -= chance) <= 0)
				return new IntIntHolder(sk[0], sk[1]).getSkill();
		}
		return null;
	}
	
	private static int calcCameraAngle(Creature npc)
	{
		final int heading = npc.getHeading();
		return Math.abs((heading < 32768 ? 180 : 540) - (int)(heading / 182.044444444));
	}
	
	private static void throwUp(Creature attacker, final double range, SystemMessage msg)
	{
		final int mx = attacker.getX(), my = attacker.getY();
		for (Creature target : FRINTEZZA_LAIR.getKnownTypeInside(Creature.class))//.getCharactersInside())
		{
			if (target == attacker)
				continue;
			if (target instanceof Npc && ((Npc) target).getNpcId() >= 29045 && ((Npc) target).getNpcId() <= 29053)
				continue;
			double dx = target.getX() - mx;
			double dy = target.getY() - my;
			if (dx == 0 && dy == 0)
				dx = dy = range / 2;
			double aa = range / Math.sqrt(dx * dx + dy * dy);
			if (aa > 1.0)
			{
				int x = mx + (int) (dx * aa);
				int y = my + (int) (dy * aa);
				int z = target.getZ();

				target.abortAll(true); //cha.abortAttack(); //cha.abortCast();
				target.getAI().tryToIdle(); //target.getAI().setIntention(CtrlIntention.IDLE);
				
				target.broadcastPacket(new FlyToLocation(target, x, y, z, FlyType.THROW_UP));
				target.setXYZ(x, y, z);
				target.getPosition().setHeading(MathUtil.calculateHeadingFrom(x, y, mx, my));
				target.broadcastPacket(new ValidateLocation(target));
				if (msg != null)
					target.sendPacket(msg);
				if (target instanceof Player)
					((Player) target).standUp();
			}
		}
	}
	
	private static class FrintezzaSong
	{
		public IntIntHolder skill;
		public IntIntHolder effectSkill;
		public String songName;
		public int chance;
		
		public FrintezzaSong(IntIntHolder sk, IntIntHolder esk, String sn, int ch)
		{
			skill = sk;
			effectSkill = esk;
			songName = sn;
			chance = ch;
		}
	}
	
	private final FrintezzaSong[] FRINTEZZA_SONG_LIST =
	{
		new FrintezzaSong(new IntIntHolder(5007, 1), new IntIntHolder(5008, 1), "Requiem of Hatred", 5),
		new FrintezzaSong(new IntIntHolder(5007, 2), new IntIntHolder(5008, 2), "Rondo of Solitude", 50),
		new FrintezzaSong(new IntIntHolder(5007, 3), new IntIntHolder(5008, 3), "Frenetic Toccata", 70),
		new FrintezzaSong(new IntIntHolder(5007, 4), new IntIntHolder(5008, 4), "Fugue of Jubilation", 90),
		new FrintezzaSong(new IntIntHolder(5007, 5), new IntIntHolder(5008, 5), "Hypnotic Mazurka", 100),
	};
	
	private static void broadcastPacket(L2GameServerPacket... packets)
	{
		for (L2GameServerPacket packet : packets)
			FRINTEZZA_LAIR.broadcastPacket(packet);
	}
	
	private void daemonMorph(Npc npc, int stage)
	{
		npc.setCollisionHeight(stage == 1 ? 110 : 130);
		broadcastPacket(new SocialAction(npc, stage == 1 ? 4 : 2));
		final IntIntHolder Daemon_Morph = new IntIntHolder(5017, 1);
		npc.getCast().doCast(Daemon_Morph.getSkill(), npc, null);
		broadcastPacket(new NpcInfo(npc, null));
		roomTasks();
	}
	
	private void setPrepareRoom(boolean move)
	{
		for (Creature cha : FRINTEZZA_LAIR.getKnownTypeInside(Creature.class))
		{
			if (cha != null)
			{
				if (frintezza != cha || frintezza == null)
					cha.setInvul(move);
				if (move)
				{
					cha.getAI().tryToIdle();
					cha.abortAll(true);
					if (frintezza != null)
					{
						if (move)
							cha.setInvul(true);
						dropTimers();
						frintezza.setScriptValue(1);
						broadcastPacket(new MagicSkillCanceled(frintezza.getObjectId()));
					}
				}
				if (!(cha instanceof GrandBoss))
				{
					if (move)
					{
						cha.disableAllSkills();
						if (cha instanceof Monster)
							cha.setRunning(true);
					}
					else
						cha.enableAllSkills();
					cha.setIsImmobilized(move);
					cha.setIsParalyzed(move);
				}
			}
		}
	}
	
	private void roomTasks()
	{
		_lastAttackTime = System.currentTimeMillis();
		startQuestTimerAtFixedRate("songs_play", frintezza, null, 5000);
		startQuestTimerAtFixedRate("skill_task", scarlet, null, 2500);
		startQuestTimerAtFixedRate("spawn_minion", null, null, 20000);
		startQuestTimer("despawn", scarlet, null, 60000);
		frintezza.setScriptValue(0);
	}
	
	private static void deleteNpc(Npc... npcs)
	{
		for (Npc npc : npcs)
		{
			if (npc != null)
				npc.deleteMe();
			npc = null;
		}
	}
	
	private void clean(int status)
	{
		if (!demons.isEmpty())
			for (Npc mobs : demons)
				deleteNpc(mobs);
		if (!portraits.isEmpty())
			for (Monster portrait : portraits.keySet())
				deleteNpc(portrait);
		deleteNpc(scarlet, frintezza);
		demons.clear();
		portraits.clear();
		GrandBossManager.getInstance().setBossStatus(FRINTEZZA, status);
		
		_lastAttackTime = 0;
		songEffect = null;
	}
	
	private static int getState()
	{
		final IntIntHolder Daemon_Morph = new IntIntHolder(5017, 1);
		if (scarlet != null)
		{
			if (scarlet.getNpcId() == SCARLET2)
				return 3;
			AbstractEffect[] effects = scarlet.getAllEffects();
			if (effects.length != 0)
				for (AbstractEffect e : effects)
					if (e.getSkill() == Daemon_Morph.getSkill())
						return 2;
			return 1;
		}
		return 0;
	}
	
	private static void playFrintezzaMelody()
	{
		// frintezza.getCast().doCast(Frintezza_Melody.getSkill(), frintezza, null); // frintezza.getAI().tryToCast(frintezza, Frintezza_Melody.getSkill());
		L2Skill skill = new IntIntHolder(5006, 1).getSkill();  // If tryToCast or doCast is used in the last acis, it looks like a speen run, Frintezza's casting stats are wrong?
		broadcastPacket(new MagicSkillUse(frintezza, frintezza, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
	}
	
	private void dropTimers()
	{
		cancelQuestTimers("despawn");
		cancelQuestTimers("spawn_minion");
		cancelQuestTimers("skill_task");
		cancelQuestTimers("songs_play");
		cancelQuestTimers("songs_effect");
		cancelQuestTimers("end_song");
		cancelQuestTimers("timeNextSong");
	}
	
	private void taskCameraMorph(int state, Npc npc)
	{
		if (frintezza.isInvul())
			return;
		final double hpRatio = npc.getStatus().getHp() / npc.getStatus().getMaxHp();
		if (state == 1 && hpRatio < 0.75)
		{
			setPrepareRoom(true);
			startQuestTimer("first_morph_01", npc, null, 1100);
			startQuestTimer("first_morph_02", npc, null, 4100);
			startQuestTimer("first_morph_03", npc, null, 8100);
			startQuestTimer("start_room", npc, null, 11100);
			startQuestTimer("end_camera", frintezza, null, 14100);
		}
		else if (state == 2 && hpRatio < 0.5)
		{
			setPrepareRoom(true);
			startQuestTimer("second_morph_01", npc, null, 2000);
			startQuestTimer("second_morph_02", npc, null, 2100);
			startQuestTimer("second_morph_03", frintezza, null, 6300);
			startQuestTimer("second_morph_04", frintezza, null, 9300);
			startQuestTimer("second_morph_05", npc, null, 12300);
			startQuestTimer("second_morph_06", npc, null, 12800);
			startQuestTimer("second_morph_07", npc, null, 19050);
			startQuestTimer("second_morph_08", npc, null, 20000);
			startQuestTimer("second_morph_09", npc, null, 28100);
			startQuestTimer("start_room", npc, null, 34100);
			startQuestTimer("end_camera", frintezza, null, 37100);
		}
		if (state == 3 && npc.getNpcId() == SCARLET2)
		{
			setPrepareRoom(true);
			broadcastPacket(new SpecialCamera(npc.getObjectId(), 300, calcCameraAngle(npc) - 180, 5, 0, 7000, 0, 0, 1, 0), new SpecialCamera(npc.getObjectId(), 200, calcCameraAngle(npc), 85, 4000, 10000, 0, 0, 1, 0));
			startQuestTimer("die_01", frintezza, null, 7400);
			startQuestTimer("die_02", frintezza, null, 7500);
			startQuestTimer("die_03", frintezza, null, 14500);
			startQuestTimer("die_04", frintezza, null, 22500);
			startQuestTimer("start_room", frintezza, null, 21500);
		}
	}
}