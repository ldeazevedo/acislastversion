 package net.sf.l2j.gameserver.scripting.script.ai.boss;
 import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.GrandBossManager;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.enums.EventHandler;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.enums.skills.FlyType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.GrandBoss;
import net.sf.l2j.gameserver.model.zone.type.BossZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceled;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;
 
 public class FrintezzaTest extends AttackableAIScript
 {
 	private static final BossZone ZONE = ZoneManager.getInstance().getZoneById(110012, BossZone.class);
 	
 	private static final int[][] SKILLS =
 	{
 		{ 5015, 1, 5000 },
 		{ 5015, 4, 5000 },
 		{ 5015, 2, 5000 },
 		{ 5015, 5, 5000 },
 		{ 5018, 1, 10000 },
 		{ 5016, 1, 5000 },
 		{ 5015, 3, 5000 },
 		{ 5015, 6, 5000 },
 		{ 5018, 2, 10000 },
 		{ 5019, 1, 10000 },
 		{ 5016, 1, 5000 }
 	};
 	
	private static final int[][] MOBS =
	{
		{ 18328,172894,-76019,-5107,243 },
		{ 18328,174095,-77279,-5107,16216 },
		{ 18328,174111,-74833,-5107,49043 },
		{ 18328,175344,-76042,-5107,32847 },
		{ 18330,173489,-76227,-5134,63565 },
		{ 18330,173498,-75724,-5107,58498 },
		{ 18330,174365,-76745,-5107,22424 },
		{ 18330,174570,-75584,-5107,31968 },
		{ 18330,174613,-76179,-5107,31471 },
		{ 18332,173620,-75981,-5107,4588 },
		{ 18332,173630,-76340,-5107,62454 },
		{ 18332,173755,-75613,-5107,57892 },
		{ 18332,173823,-76688,-5107,2411 },
		{ 18332,174000,-75411,-5107,54718 },
		{ 18332,174487,-75555,-5107,33861 },
		{ 18332,174517,-76471,-5107,21893 },
		{ 18332,174576,-76122,-5107,31176 },
		{ 18332,174600,-75841,-5134,35927 },
		{ 18329,173481,-76043,-5107,61312 },
		{ 18329,173539,-75678,-5107,59524 },
		{ 18329,173584,-76386,-5107,3041 },
		{ 18329,173773,-75420,-5107,51115 },
		{ 18329,173777,-76650,-5107,12588 },
		{ 18329,174585,-76510,-5107,21704 },
		{ 18329,174623,-75571,-5107,40141 },
		{ 18329,174744,-76240,-5107,29202 },
		{ 18329,174769,-75895,-5107,29572 },
		{ 18333,173861,-76011,-5107,383 },
		{ 18333,173872,-76461,-5107,8041 },
		{ 18333,173898,-75668,-5107,51856 },
		{ 18333,174422,-75689,-5107,42878 },
		{ 18333,174460,-76355,-5107,27311 },
		{ 18333,174483,-76041,-5107,30947 },
		{ 18331,173515,-76184,-5107,6971 },
		{ 18331,173516,-75790,-5134,3142 },
		{ 18331,173696,-76675,-5107,6757 },
		{ 18331,173766,-75502,-5134,60827 },
		{ 18331,174473,-75321,-5107,37147 },
		{ 18331,174493,-76505,-5107,34503 },
		{ 18331,174568,-75654,-5134,41661 },
		{ 18331,174584,-76263,-5107,31729 },
		{ 18339,173892,-81592,-5123,50849 },
		{ 18339,173958,-81820,-5123,7459 },
		{ 18339,174128,-81805,-5150,21495 },
		{ 18339,174245,-81566,-5123,41760 },
		{ 18334,173264,-81529,-5072,1646 },
		{ 18334,173265,-81656,-5072,441 },
		{ 18334,173267,-81889,-5072,0 },
		{ 18334,173271,-82015,-5072,65382 },
		{ 18334,174867,-81655,-5073,32537 },
		{ 18334,174868,-81890,-5073,32768 },
		{ 18334,174869,-81485,-5073,32315 },
		{ 18334,174871,-82017,-5073,33007 },
		{ 18335,173074,-80817,-5107,8353 },
		{ 18335,173128,-82702,-5107,5345 },
		{ 18335,173181,-82544,-5107,65135 },
		{ 18335,173191,-80981,-5107,6947 },
		{ 18335,174859,-80889,-5134,24103 },
		{ 18335,174924,-82666,-5107,38710 },
		{ 18335,174947,-80733,-5107,22449 },
		{ 18335,175096,-82724,-5107,42205 },
		{ 18336,173435,-80512,-5107,65215 },
		{ 18336,173440,-82948,-5107,417 },
		{ 18336,173443,-83120,-5107,1094 },
		{ 18336,173463,-83064,-5107,286 },
		{ 18336,173465,-80453,-5107,174 },
		{ 18336,173465,-83006,-5107,2604 },
		{ 18336,173468,-82889,-5107,316 },
		{ 18336,173469,-80570,-5107,65353 },
		{ 18336,173469,-80628,-5107,166 },
		{ 18336,173492,-83121,-5107,394 },
		{ 18336,173493,-80683,-5107,0 },
		{ 18336,173497,-80510,-5134,417 },
		{ 18336,173499,-82947,-5107,0 },
		{ 18336,173521,-83063,-5107,316 },
		{ 18336,173523,-82889,-5107,128 },
		{ 18336,173524,-80627,-5134,65027 },
		{ 18336,173524,-83007,-5107,0 },
		{ 18336,173526,-80452,-5107,64735 },
		{ 18336,173527,-80569,-5134,65062 },
		{ 18336,174602,-83122,-5107,33104 },
		{ 18336,174604,-82949,-5107,33184 },
		{ 18336,174609,-80514,-5107,33234 },
		{ 18336,174609,-80684,-5107,32851 },
		{ 18336,174629,-80627,-5107,33346 },
		{ 18336,174632,-80570,-5107,32896 },
		{ 18336,174632,-83066,-5107,32768 },
		{ 18336,174635,-82893,-5107,33594 },
		{ 18336,174636,-80456,-5107,32065 },
		{ 18336,174639,-83008,-5107,33057 },
		{ 18336,174660,-80512,-5107,33057 },
		{ 18336,174661,-83121,-5107,32768 },
		{ 18336,174663,-82948,-5107,32768 },
		{ 18336,174664,-80685,-5107,32676 },
		{ 18336,174687,-83008,-5107,32520 },
		{ 18336,174691,-83066,-5107,32961 },
		{ 18336,174692,-80455,-5107,33202 },
		{ 18336,174692,-80571,-5107,32768 },
		{ 18336,174693,-80630,-5107,32994 },
		{ 18336,174693,-82889,-5107,32622 },
		{ 18337,172837,-82382,-5107,58363 },
		{ 18337,172867,-81123,-5107,64055 },
		{ 18337,172883,-82495,-5107,64764 },
		{ 18337,172916,-81033,-5107,7099 },
		{ 18337,172940,-82325,-5107,58998 },
		{ 18337,172946,-82435,-5107,58038 },
		{ 18337,172971,-81198,-5107,14768 },
		{ 18337,172992,-81091,-5107,9438 },
		{ 18337,173032,-82365,-5107,59041 },
		{ 18337,173064,-81125,-5107,5827 },
		{ 18337,175014,-81173,-5107,26398 },
		{ 18337,175061,-82374,-5107,43290 },
		{ 18337,175096,-81080,-5107,24719 },
		{ 18337,175169,-82453,-5107,37672 },
		{ 18337,175172,-80972,-5107,32315 },
		{ 18337,175174,-82328,-5107,41760 },
		{ 18337,175197,-81157,-5107,27617 },
		{ 18337,175245,-82547,-5107,40275 },
		{ 18337,175249,-81075,-5107,28435 },
		{ 18337,175292,-82432,-5107,42225 },
		{ 18338,173014,-82628,-5107,11874 },
		{ 18338,173033,-80920,-5107,10425 },
		{ 18338,173095,-82520,-5107,49152 },
		{ 18338,173115,-80986,-5107,9611 },
		{ 18338,173144,-80894,-5107,5345 },
		{ 18338,173147,-82602,-5107,51316 },
		{ 18338,174912,-80825,-5107,24270 },
		{ 18338,174935,-80899,-5107,18061 },
		{ 18338,175016,-82697,-5107,39533 },
		{ 18338,175041,-80834,-5107,25420 },
		{ 18338,175071,-82549,-5107,39163 },
		{ 18338,175154,-82619,-5107,36345 }
	};
	
	private static final int[][] PORTRAITS = 
	{
		{ 29049,175876,-88713 },
		{ 29049,172608,-88702 },
		{ 29048,175833,-87165 },
		{ 29048,172634,-87165 }
	};
	
	private static final int[][] DEMONS = 
	{
		{ 29051,175876,-88713,-4972,28205 },
		{ 29051,172608,-88702,-4972,64817 },
		{ 29050,175833,-87165,-4972,35048 },
		{ 29050,172634,-87165,-4972,57730 }
	};
	
	public static final int SCARLET1 = 29046;
	public static final int SCARLET2 = 29047;
	public static final int FRINTEZZA = 29045;
	public static final int CUBE = 29061;
	public static final int EVIL_SPIRIT = 29048;
	public static final int EVIL_SPIRIT_2 = 29049;
	
	private static final int SOUL_BREAKING_ARROW = 8192;
	private static final int DEWDROP_OF_DESTRUCTION = 8556;
	
	// Frintezza Status Tracking
	public static final byte DORMANT = 0; // Frintezza is spawned and no one has entered yet. Entry is unlocked
	public static final byte WAITING = 1; // Frintezza is spawend and someone has entered, triggering a 30 minute window for additional people to enter before he unleashes his attack. Entry is unlocked
	//before he unleashes his attack. Entry is unlocked
	public static final byte FIGHTING = 2; // Frintezza is engaged in battle, annihilating his foes. Entry is locked
	public static final byte DEAD = 3; // Frintezza has been killed. Entry is locked
	
	private Set<Npc> _roomMobs = ConcurrentHashMap.newKeySet();
	private int _notice;

	private static final int[] NPCS =
	{
		SCARLET1, SCARLET2, FRINTEZZA, 18328, 18329, 18330, 18331, 18332, 18333,
		18334, 18335, 18336, 18337, 18338, 18339, 29048, 29049, 29050, 29051, 32011
	};
	
	// remover proximo update
	private static long _LastAction = 0;
	private static int _Angle = 0;
	private static int _Bomber = 0;
	private static int _CheckDie = 0;
	private static int _OnCheck = 0;
	private static int _OnSong = 0;
	private static int _Abnormal = 0;
	private static int _OnMorph = 0;
	private static int _Scarlet_x = 0;
	private static int _Scarlet_y = 0;
	private static int _Scarlet_z = 0;
	private static int _Scarlet_h = 0;
	private static int _SecondMorph = 0;
	private static int _ThirdMorph = 0;
	private static int _SoulBreakArrowUse = 0;
	private GrandBoss frintezza, weakScarlet, strongScarlet, activeScarlet;
	private Npc _frintezzaDummy, _overheadDummy, _portraitDummy1, _portraitDummy3, _scarletDummy;
	
	@Override
	protected void registerNpcs()
	{
		addEventIds(NPCS, EventHandler.ATTACKED, EventHandler.MY_DYING, EventHandler.SEE_SPELL);
	}
	
	public FrintezzaTest()
	{
		super("ai/individual");
		
		final int status = GrandBossManager.getInstance().getBossStatus(FRINTEZZA);
		if (status == DEAD)
		{
			final long temp = GrandBossManager.getInstance().getStatSet(FRINTEZZA).getLong("respawn_time") - System.currentTimeMillis();
			if (temp > 0)
				startQuestTimer("frintezza_unlock", temp, null, null, false);
			else
				GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
		}
		else if (status != DORMANT)
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("beginning"))
		{
			closeDoors();
			_notice = 35;
			for (int i = 0; i <= 17; i++)
				_roomMobs.add(addSpawn(MOBS[i][0], MOBS[i][1], MOBS[i][2], MOBS[i][3], MOBS[i][4], false, 0, false));
			
			ZONE.broadcastPacket(new CreatureSay(0, SayType.SHOUT, "Hall Alarm Device", "Intruders! Sound the alarm!"));
			
			startQuestTimer("notice", 60000, null, null, false);
			startQuestTimer("frintezza_despawn", 60000, null, null, false);
		}
		else if (event.equalsIgnoreCase("notice"))
		{
			if (_notice == 0)
			{
				ZONE.broadcastPacket(new CreatureSay(0, SayType.SHOUT, "Frintezza Gatekeeper", "Time limit exceeded, challenge failed!"));
				ZONE.oustAllPlayers();
				
				cancelQuestTimers("notice");
				cancelQuestTimers("frintezza_despawn");
				
				deleteAllMobs();
				closeDoors();
				
				GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
			}
			
			_notice--;
			ZONE.broadcastPacket(new ExShowScreenMessage(_notice + " minute(s) remaining.", 10000));
		}
		else if (event.equalsIgnoreCase("waiting"))
		{
			startQuestTimer("close", 27000, npc, null, false);
			startQuestTimer("camera_1", 30000, npc, null, false);
			for (Creature pl : ZONE.getKnownTypeInside(Player.class))
				ZONE.broadcastPacket(new Earthquake(pl, /*174232, -88020, -5116, */45, 27));
		}
		else if (event.equalsIgnoreCase("close"))
			closeDoors();
		else if (event.equalsIgnoreCase("loc_check"))
		{
			if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
			{
				if (!ZONE.isInsideZone(npc))
					npc.teleportTo(174232, -88020, -5116, 0);
				if (npc.getX() < 171932 || npc.getX() > 176532 || npc.getY() < -90320 || npc.getY() > -85720 || npc.getZ() < -5130)
					npc.teleportTo(174232, -88020, -5116, 0);
			}
		}
		else if (event.equalsIgnoreCase("camera_1"))
		{
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, FIGHTING);
			_frintezzaDummy = addSpawn(29052, 174240, -89805, -5022, 16048, false, 0, false);
			_frintezzaDummy.setInvul(true);
			_frintezzaDummy.setIsImmobilized(true);
			_overheadDummy = addSpawn(29052, 174232, -88020, -5110, 16384, false, 0, false);
			_overheadDummy.setInvul(true);
			_overheadDummy.setIsImmobilized(true);
			_overheadDummy.setCollisionHeight(600);
			ZONE.broadcastPacket(new NpcInfo(_overheadDummy, null));
			_portraitDummy1 = addSpawn(29052, 172450, -87890, -5100, 16048, false, 0, false);
			_portraitDummy1.setIsImmobilized(true);
			_portraitDummy1.setInvul(true);
			_portraitDummy3 = addSpawn(29052, 176012, -87890, -5100, 16048, false, 0, false);
			_portraitDummy3.setIsImmobilized(true);
			_portraitDummy3.setInvul(true);
			_scarletDummy = addSpawn(29053, 174232, -88020, -5110, 16384, false, 0, false);
			_scarletDummy.setInvul(true);
			_scarletDummy.setIsImmobilized(true);
			
			stopPcActions();
			startQuestTimer("camera_2", 1000, _overheadDummy, null, false);
		}
		else if (event.equalsIgnoreCase("camera_2"))
		{
			ZONE.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 0, 75, -89, 0, 100, 0, 0, 1, 0));
			startQuestTimer("camera_2b", 0, _overheadDummy, null, false);
		}
		else if (event.equalsIgnoreCase("camera_2b"))
		{
			ZONE.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 0, 75, -89, 0, 100, 0, 0, 1, 0));
			startQuestTimer("camera_3", 0, _overheadDummy, null, false);
		}
		else if (event.equalsIgnoreCase("camera_3"))
		{
			ZONE.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 300, 90, -10, 6500, 7000, 0, 0, 1, 0));
			frintezza = (GrandBoss) addSpawn(FRINTEZZA, 174240, -89805, -5022, 16048, false, 0, false);
			GrandBossManager.getInstance().addBoss(frintezza);
			frintezza.setIsImmobilized(true);
			frintezza.setInvul(true);
			frintezza.disableAllSkills();
			
			for (int[] demon : DEMONS)
			{
				final Npc demons = addSpawn(demon[0], demon[1], demon[2], demon[3], demon[4], false, 0, false);
				demons.setIsImmobilized(true);
				demons.disableAllSkills();
			}
			
			startQuestTimer("camera_4", 6500, _overheadDummy, null, false);
		}
		else if (event.equalsIgnoreCase("camera_4"))
		{
			ZONE.broadcastPacket(new SpecialCamera(_frintezzaDummy.getObjectId(), 1800, 90, 8, 6500, 7000, 0, 0, 1, 0));
			startQuestTimer("camera_5", 900, _frintezzaDummy, null, false);
		}
		else if (event.equalsIgnoreCase("camera_5"))
		{
			ZONE.broadcastPacket(new SpecialCamera(_frintezzaDummy.getObjectId(), 140, 90, 10, 2500, 4500, 0, 0, 1, 0));
			startQuestTimer("camera_5b", 4000, _frintezzaDummy, null, false);
		}
		else if (event.equalsIgnoreCase("camera_5b"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 40, 75, -10, 0, 1000, 0, 0, 1, 0));
			startQuestTimer("camera_6", 0, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_6"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 40, 75, -10, 0, 12000, 0, 0, 1, 0));
			startQuestTimer("camera_7", 1350, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_7"))
		{
			ZONE.broadcastPacket(new SocialAction(frintezza, 2));
			startQuestTimer("camera_8", 7000, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_8"))
		{
			startQuestTimer("camera_9", 1000, frintezza, null, false);
			_frintezzaDummy.deleteMe();
			_frintezzaDummy = null;
		}
		else if (event.equalsIgnoreCase("camera_9"))
		{
			for (Npc mob : ZONE.getKnownTypeInside(Npc.class))
			{
				if (mob.getNpcId() == 29051 || mob.getNpcId() == 29050)
					ZONE.broadcastPacket(new SocialAction(mob, 1));
			}
			
			startQuestTimer("camera_9b", 400, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_9b"))
		{
			for (Npc mob : ZONE.getKnownTypeInside(Npc.class))
			{
				if (mob.getNpcId() == 29051 || mob.getNpcId() == 29050)
					ZONE.broadcastPacket(new SocialAction(mob, 1));
			}
			
			for (Creature pc : ZONE.getKnownTypeInside(Player.class))
			{
				if (pc.getX() < 174232)
					pc.broadcastPacket(new SpecialCamera(_portraitDummy1.getObjectId(), 1000, 118, 0, 0, 1000, 0, 0, 1, 0));
				else
					pc.broadcastPacket(new SpecialCamera(_portraitDummy3.getObjectId(), 1000, 62, 0, 0, 1000, 0, 0, 1, 0));
			}
			
			for (Creature pc : ZONE.getKnownTypeInside(Player.class))
			{
				if (pc.getX() < 174232)
					pc.broadcastPacket(new SpecialCamera(_portraitDummy1.getObjectId(), 1000, 118, 0, 0, 10000, 0, 0, 1, 0));
				else
					pc.broadcastPacket(new SpecialCamera(_portraitDummy3.getObjectId(), 1000, 62, 0, 0, 10000, 0, 0, 1, 0));
			}
			
			startQuestTimer("camera_10", 2000, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_10"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 240, 90, 0, 0, 1000, 0, 0, 1, 0));
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 240, 90, 25, 5500, 10000, 0, 0, 1, 0));
			
			ZONE.broadcastPacket(new SocialAction(frintezza, 3));
			_portraitDummy1.deleteMe();
			_portraitDummy3.deleteMe();
			_portraitDummy1 = null;
			_portraitDummy3 = null;
			startQuestTimer("camera_11b", 4000, frintezza, null, false);
			startQuestTimer("camera_12", 4500, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_11b"))
			ZONE.broadcastPacket(new MagicSkillUse(frintezza, frintezza, 5006, 1, 34000, 0));
		else if (event.equalsIgnoreCase("camera_12"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 100, 195, 35, 0, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_13", 700, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_13"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 100, 195, 35, 0, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_14", 1300, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_14"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 120, 180, 45, 1500, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_16", 1500, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_16"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 520, 135, 45, 8000, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_17", 7500, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_17"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 1500, 110, 25, 10000, 13000, 0, 0, 1, 0));
			startQuestTimer("camera_18", 9500, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("camera_18"))
		{
			ZONE.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 930, 160, -20, 0, 1000, 0, 0, 1, 0));
			ZONE.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 600, 180, -25, 0, 10000, 0, 0, 1, 0));
			ZONE.broadcastPacket(new MagicSkillUse(_scarletDummy, _scarletDummy, 5004, 1, 5800, 0));
			
			weakScarlet = (GrandBoss) addSpawn(29046, 174232, -88020, -5110, 16384, false, 0, true);
			weakScarlet.setInvul(true);
			weakScarlet.setIsImmobilized(true);
			weakScarlet.disableAllSkills();
			activeScarlet = weakScarlet;
			startQuestTimer("camera_19", 2400, _scarletDummy, null, false);
			startQuestTimer("camera_19b", 5000, _scarletDummy, null, false);
			startQuestTimer("camera_19c", 6300, _scarletDummy, null, false);
		}
		else if (event.equalsIgnoreCase("camera_19"))
			weakScarlet.teleportTo(174232, -88020, -5110, 0);
		else if (event.equalsIgnoreCase("camera_19b"))
		{
			ZONE.broadcastPacket(new SpecialCamera(_scarletDummy.getObjectId(), 800, 180, 10, 1000, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_20", 2100, _scarletDummy, null, false);
		}
		else if (event == "camera_19c")
			throwUp(npc, 500, SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5004, 1));
		else if (event.equalsIgnoreCase("camera_20"))
		{
			ZONE.broadcastPacket(new SpecialCamera(weakScarlet.getObjectId(), 300, 60, 8, 0, 10000, 0, 0, 1, 0));
			startQuestTimer("camera_21", 2000, weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("camera_21"))
		{
			ZONE.broadcastPacket(new SpecialCamera(weakScarlet.getObjectId(), 500, 90, 10, 3000, 5000, 0, 0, 1, 0));
			startQuestTimer("camera_22", 3000, weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("camera_22"))
		{
			for (int[] portrait : PORTRAITS)
			{
				final Npc portaits = addSpawn(portrait[0], portrait[1], portrait[2], -5000, 0, false, 0, false);
				portaits.setIsImmobilized(true);
				portaits.disableAllSkills();
			}
			
			startNpcActions();
			
			startQuestTimer("camera_23", 2000, weakScarlet, null, false);
			startQuestTimer("loc_check", 60000, weakScarlet, null, true);
			startQuestTimer("songs_play", 10000 + Rnd.get(10000), frintezza, null, false);
			startQuestTimer("skill01", 10000 + Rnd.get(10000), weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("camera_23"))
		{
			for (Npc minion : ZONE.getKnownTypeInside(Npc.class))
			{
				if (minion.getNpcId() == FRINTEZZA)
					continue;
				
				minion.setIsImmobilized(false);
				minion.enableAllSkills();
				
				if (minion.getNpcId() == 29049 || minion.getNpcId() == 29048)
					startQuestTimer("spawn_minion", 20000, minion, null, false);
			}
			
			startPcActions();
			startNpcActions();
		}
		else if (event.equalsIgnoreCase("start_pc"))
			startPcActions();
		else if (event.equalsIgnoreCase("start_npc"))
			startNpcActions();
		else if (event.equalsIgnoreCase("morph_end"))
			_OnMorph = 0;
		else if (event.equalsIgnoreCase("morph_01"))
		{
			ZONE.broadcastPacket(new SpecialCamera(weakScarlet.getObjectId(), 250, _Angle, 12, 2000, 11000, 0, 0, 1, 0));
			startQuestTimer("morph_02", 3000, weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("morph_02"))
		{
			ZONE.broadcastPacket(new SocialAction(weakScarlet, 1));
			weakScarlet.setRightHandItemId(7903);
			startQuestTimer("morph_03", 1500, weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("morph_03"))
		{
			startQuestTimer("morph_04", 1500, weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("morph_04"))
		{
			ZONE.broadcastPacket(new SocialAction(weakScarlet, 4));
			L2Skill skill = SkillTable.getInstance().getInfo(5017, 1);
			if (skill != null)
				skill.getEffects(weakScarlet, weakScarlet);
			
			startQuestTimer("morph_end", 3000, weakScarlet, null, false);
			startQuestTimer("start_pc", 1000, weakScarlet, null, false);
			startQuestTimer("start_npc", 1000, weakScarlet, null, false);
			startQuestTimer("songs_play", 10000 + Rnd.get(10000), frintezza, null, false);
			startQuestTimer("skill02", 10000 + Rnd.get(10000), weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("morph_05a"))
			ZONE.broadcastPacket(new SocialAction(frintezza, 4));
		else if (event.equalsIgnoreCase("morph_05"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 250, 120, 15, 0, 1000, 0, 0, 1, 0));
			startQuestTimer("morph_06", 0, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("morph_06"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 250, 120, 15, 0, 10000, 0, 0, 1, 0));
			
			cancelQuestTimers("loc_check");
			
			_Scarlet_x = weakScarlet.getX();
			_Scarlet_y = weakScarlet.getY();
			_Scarlet_z = weakScarlet.getZ();
			_Scarlet_h = weakScarlet.getHeading();
			weakScarlet.deleteMe();
			weakScarlet = null;
			activeScarlet = null;
			weakScarlet = (GrandBoss) addSpawn(29046, _Scarlet_x, _Scarlet_y, _Scarlet_z, _Scarlet_h, false, 0, false);
			weakScarlet.setInvul(true);
			weakScarlet.setIsImmobilized(true);
			weakScarlet.disableAllSkills();
			weakScarlet.setRightHandItemId(7903);
			
			startQuestTimer("morph_07", 4000, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("morph_07"))
		{
			ZONE.broadcastPacket(new MagicSkillUse(frintezza, frintezza, 5006, 1, 34000, 0));
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 500, 70, 15, 3000, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_08", 3000, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("morph_08"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 2500, 90, 12, 6000, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_09", 3000, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("morph_09"))
		{
			ZONE.broadcastPacket(new SpecialCamera(weakScarlet.getObjectId(), 250, _Angle, 12, 0, 1000, 0, 0, 1, 0));
			ZONE.broadcastPacket(new SpecialCamera(weakScarlet.getObjectId(), 250, _Angle, 12, 0, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_11", 500, weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("morph_11"))
		{
			weakScarlet.doDie(weakScarlet);
			ZONE.broadcastPacket(new SpecialCamera(weakScarlet.getObjectId(), 450, _Angle, 14, 8000, 8000, 0, 0, 1, 0));
			
			startQuestTimer("morph_12", 6250, weakScarlet, null, false);
			startQuestTimer("morph_13", 7200, weakScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("morph_12"))
		{
			weakScarlet.deleteMe();
			weakScarlet = null;
		}
		else if (event.equalsIgnoreCase("morph_13"))
		{
			strongScarlet = (GrandBoss) addSpawn(SCARLET2, _Scarlet_x, _Scarlet_y, _Scarlet_z, _Scarlet_h, false, 0, false);
			strongScarlet.setInvul(true);
			strongScarlet.setIsImmobilized(true);
			strongScarlet.disableAllSkills();
			activeScarlet = strongScarlet;
			
			ZONE.broadcastPacket(new SpecialCamera(strongScarlet.getObjectId(), 450, _Angle, 12, 500, 20000, 0, 0, 1, 0));
			
			startQuestTimer("morph_14", 3000, strongScarlet, null, false);
			startQuestTimer("loc_check", 60000, strongScarlet, null, true);
		}
		else if (event.equalsIgnoreCase("morph_14"))
			startQuestTimer("morph_15", 11100, strongScarlet, null, false);
		else if (event.equalsIgnoreCase("morph_15"))
		{
			ZONE.broadcastPacket(new SocialAction(strongScarlet, 2));
			L2Skill skill = SkillTable.getInstance().getInfo(5017, 1);
			if (skill != null)
				skill.getEffects(strongScarlet, strongScarlet);
			
			startQuestTimer("morph_end", 9000, strongScarlet, null, false);
			startQuestTimer("start_pc", 6000, strongScarlet, null, false);
			startQuestTimer("start_npc", 6000, strongScarlet, null, false);
			startQuestTimer("songs_play", 10000 + Rnd.get(10000), frintezza, null, false);
			startQuestTimer("skill03", 10000 + Rnd.get(10000), strongScarlet, null, false);
		}
		else if (event.equalsIgnoreCase("morph_16"))
		{
			ZONE.broadcastPacket(new SpecialCamera(strongScarlet.getObjectId(), 300, _Angle - 180, 5, 0, 7000, 0, 0, 1, 0));
			ZONE.broadcastPacket(new SpecialCamera(strongScarlet.getObjectId(), 200, _Angle, 85, 4000, 10000, 0, 0, 1, 0));
			startQuestTimer("morph_17b", 7400, frintezza, null, false);
			startQuestTimer("morph_18", 7500, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("morph_17b"))
			frintezza.doDie(frintezza);
		else if (event.equalsIgnoreCase("morph_18"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 100, 120, 5, 0, 7000, 0, 0, 1, 0));
			startQuestTimer("morph_19", 0, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("morph_19"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 100, 90, 5, 5000, 15000, 0, 0, 1, 0));
			startQuestTimer("morph_20", 7000, frintezza, null, false);
			startQuestTimer("spawn_cubes", 7000, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("morph_20"))
		{
			ZONE.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 900, 90, 25, 7000, 10000, 0, 0, 1, 0));
			startQuestTimer("start_pc", 7000, frintezza, null, false);
		}
		else if (event.equalsIgnoreCase("songs_play"))
		{
			_SoulBreakArrowUse = 0;
			
			if (frintezza != null && !frintezza.isDead() && _OnMorph == 0)
			{
				_OnSong = Rnd.get(1, 5);
				
				String SongName = "";
				
				// Name of the songs are custom, named with client side description.
				switch (_OnSong)
				{
					case 1:
						SongName = "Frintezza's Healing Rhapsody";
						break;
					case 2:
						SongName = "Frintezza's Rampaging Opus";
						break;
					case 3:
						SongName = "Frintezza's Power Concerto";
						break;
					case 4:
						SongName = "Frintezza's Plagued Concerto";
						break;
					case 5:
						SongName = "Frintezza's Psycho Symphony";
						break;
					default:
						SongName = "Frintezza's Song";
						break;
				}
				
				// Like L2OFF the skill name is printed on screen
				ZONE.broadcastPacket(new ExShowScreenMessage(SongName, 6000));
				
				if (_OnSong == 1 && _ThirdMorph == 1 && strongScarlet.getStatus().getHp() < strongScarlet.getStatus().getMaxHp() * 0.6 && Rnd.get(100) < 80)
				{
					ZONE.broadcastPacket(new MagicSkillUse(frintezza, frintezza, 5007, 1, 32000, 0));
					startQuestTimer("songs_effect", 5000, frintezza, null, false);
					startQuestTimer("songs_play", 32000 + Rnd.get(10000), frintezza, null, false);
				}
				else if (_OnSong == 2 || _OnSong == 3)
				{
					ZONE.broadcastPacket(new MagicSkillUse(frintezza, frintezza, 5007, _OnSong, 32000, 0));
					startQuestTimer("songs_effect", 5000, frintezza, null, false);
					startQuestTimer("songs_play", 32000 + Rnd.get(10000), frintezza, null, false);
				}
				else if (_OnSong == 4 && _SecondMorph == 1)
				{
					ZONE.broadcastPacket(new MagicSkillUse(frintezza, frintezza, 5007, 4, 31000, 0));
					startQuestTimer("songs_effect", 5000, frintezza, null, false);
					startQuestTimer("songs_play", 31000 + Rnd.get(10000), frintezza, null, false);
				}
				else if (_OnSong == 5 && _ThirdMorph == 1 && _Abnormal == 0)
				{
					_Abnormal = 1;
					ZONE.broadcastPacket(new MagicSkillUse(frintezza, frintezza, 5007, 5, 35000, 0));
					startQuestTimer("songs_effect", 5000, frintezza, null, false);
					startQuestTimer("songs_play", 35000 + Rnd.get(10000), frintezza, null, false);
				}
				else
					startQuestTimer("songs_play", 5000 + Rnd.get(5000), frintezza, null, false);
			}
		}
		else if (event.equalsIgnoreCase("songs_effect"))
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5008, _OnSong);
			if (skill == null)
				return null;
			
			if (_OnSong == 1 || _OnSong == 2 || _OnSong == 3)
			{
				if (frintezza != null && !frintezza.isDead() && activeScarlet != null && !activeScarlet.isDead())
					skill.getEffects(frintezza, activeScarlet);
			}
			else if (_OnSong == 4)
			{
				for (Creature cha : ZONE.getKnownTypeInside(Player.class))
				{
					if (Rnd.get(100) < 80)
					{
						skill.getEffects(frintezza, cha);
						cha.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5008, 4));
					}
				}
			}
			else if (_OnSong == 5)
			{
				for (Creature cha : ZONE.getKnownTypeInside(Player.class))
				{
					if (Rnd.get(100) < 70)
					{
						cha.abortAll(true);
						//cha.abortAttack();
						//cha.abortCast();
						cha.disableAllSkills();
						//cha.stopMove(null);
						cha.setIsParalyzed(true);
						cha.setIsImmobilized(true);
						cha.getAI().tryToIdle();//setIntention(IntentionType.IDLE);
						skill.getEffects(frintezza, cha);
						cha.startAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
						cha.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5008, 5));
					}
				}
				startQuestTimer("stop_effect", 25000, frintezza, null, false);
			}
		}
		else if (event.equalsIgnoreCase("stop_effect"))
		{
			for (Creature cha : ZONE.getKnownTypeInside(Player.class))
			{
				cha.stopAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
				cha.stopAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
				cha.enableAllSkills();
				cha.setIsImmobilized(false);
				cha.setIsParalyzed(false);
			}
			_Abnormal = 0;
		}
		else if (event.equalsIgnoreCase("check_hp"))
		{
			if (npc.isDead())
			{
				_OnMorph = 1;
				ZONE.broadcastPacket(new PlaySound(1, "BS01_D"));
				
				stopAttacks();
				stopPcActions();
				stopNpcActions();
				startQuestTimer("morph_16", 0, npc, null, false);
			}
			else
			{
				_CheckDie = _CheckDie + 10;
				if (_CheckDie < 3000)
					startQuestTimer("check_hp", 10, npc, null, false);
				else
				{
					_OnCheck = 0;
					_CheckDie = 0;
				}
			}
		}
		else if (event.equalsIgnoreCase("skill01"))
		{
			if (weakScarlet != null && !weakScarlet.isDead() && _SecondMorph == 0 && _ThirdMorph == 0 && _OnMorph == 0)
			{
				int i = Rnd.get(0, 1);
				L2Skill skill = SkillTable.getInstance().getInfo(SKILLS[i][0], SKILLS[i][1]);
				if (skill != null)
				{
					weakScarlet.abortAll(true);
					weakScarlet.getCast().doCast(skill, getTarget(), null);
					//weakScarlet.stopMove(null);
				//	weakScarlet.setIsCastingNow(true);
					//weakScarlet.doCast(skill);
				}
				startQuestTimer("skill01", SKILLS[i][2] + 5000 + Rnd.get(10000), npc, null,false);
			}
		}
		else if (event.equalsIgnoreCase("skill02"))
		{
			if (weakScarlet != null && !weakScarlet.isDead() && _SecondMorph == 1 && _ThirdMorph == 0 && _OnMorph == 0)
			{
				int i = 0;
				if (_Abnormal == 0)
					i = Rnd.get(2, 5);
				else
					i = Rnd.get(2, 4);
				
				L2Skill skill = SkillTable.getInstance().getInfo(SKILLS[i][0], SKILLS[i][1]);
				if (skill != null)
				{
					strongScarlet.abortAll(true);
					//weakScarlet.stopMove(null);
					//weakScarlet.setIsCastingNow(true);
					//weakScarlet.doCast(skill);
					weakScarlet.getCast().doCast(skill, getTarget(), null);
				}
				startQuestTimer("skill02", SKILLS[i][2] + 5000 + Rnd.get(10000), npc, null,false);
				
				if (i == 5)
				{
					_Abnormal = 1;
					startQuestTimer("float_effect", 4000, weakScarlet, null, false);
				}
			}
		}
		else if (event.equalsIgnoreCase("skill03"))
		{
			if (strongScarlet != null && !strongScarlet.isDead() && _SecondMorph == 1 && _ThirdMorph == 1 && _OnMorph == 0)
			{
				int i = 0;
				if (_Abnormal == 0)
					i = Rnd.get(6, 10);
				else
					i = Rnd.get(6, 9);
				
				L2Skill skill = SkillTable.getInstance().getInfo(SKILLS[i][0], SKILLS[i][1]);
				if (skill != null)
				{
					strongScarlet.abortAll(true);
					//strongScarlet.stopMove(null);
					//strongScarlet.setIsCastingNow(true);
					strongScarlet.getCast().doCast(skill, getTarget(), null);
				}
				startQuestTimer("skill03", SKILLS[i][2] + 5000 + Rnd.get(10000), npc, null,false);
				
				if (i == 10)
				{
					_Abnormal = 1;
					startQuestTimer("float_effect", 3000, npc, null, false);
				}
			}
		}
		else if (event.equalsIgnoreCase("float_effect"))
		{
			if (npc.getCast().isCastingNow())
				startQuestTimer("float_effect", 500, npc, null, false);
			else
			{
				for (Creature cha : ZONE.getKnownTypeInside(Player.class))
				{
					if (cha.getFirstEffect(5016) != null)
					{
						cha.abortAll(true);
					//	cha.abortAttack();
					//	cha.abortCast();
						cha.disableAllSkills();
					//	cha.stopMove(null);
						cha.setIsParalyzed(true);
						cha.setIsImmobilized(true);
						cha.getAI().tryToIdle();//setIntention(IntentionType.IDLE);
						cha.startAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
					}
				}
				startQuestTimer("stop_effect", 25000, npc, null, false);
			}
		}
		else if (event.equalsIgnoreCase("action"))
			ZONE.broadcastPacket(new SocialAction(npc, 1));
		else if (event.equalsIgnoreCase("bomber"))
			_Bomber = 0;
		else if (event.equalsIgnoreCase("frintezza_despawn"))
		{
			if (System.currentTimeMillis() - _LastAction > 900000)
			{
				ZONE.oustAllPlayers();
				
				cancelQuestTimers("waiting");
				cancelQuestTimers("loc_check");
				cancelQuestTimers("spawn_minion");
				cancelQuestTimers("notice");
				cancelQuestTimers("frintezza_despawn");
				
				deleteAllMobs();
				closeDoors();
				stopAttacks();
				
				GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
			}
		}
		else if (event.equalsIgnoreCase("spawn_minion"))
		{
			if (npc != null && !npc.isDead() && frintezza != null && !frintezza.isDead())
			{
				Npc mob = addSpawn(npc.getNpcId() + 2, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, false);
				
				startQuestTimer("action", 200, mob, null, false);
				startQuestTimer("spawn_minion", 18000, npc, null, false);
			}
		}
		else if (event.equalsIgnoreCase("spawn_cubes"))
			addSpawn(CUBE, 174232, -88020, -5114, 16384, false, 900000, false);
		else if (event.equalsIgnoreCase("frintezza_unlock"))
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DORMANT);
		else if (event.equalsIgnoreCase("remove_players"))
			ZONE.oustAllPlayers();
		
		return super.onAdvEvent(event, npc, player);
	}
	
	private static Creature getTarget()
	{
		return Rnd.get(ZONE.getKnownTypeInside(Player.class));
	}

	//@Override
	public String onSkillSee(Npc npc, Player caster, L2Skill skill, WorldObject[] targets, boolean isPet)
	{
		if (targets.length > 0 && targets[0] == npc)
		{
			if (npc == frintezza)
				npc.getStatus().setHp(npc.getStatus().getMaxHp(), false);
			switch (skill.getId())
			{
				case 2234:
					if (frintezza != null && targets[0] == npc && npc.getNpcId() == FRINTEZZA && _SoulBreakArrowUse == 1)
						ZONE.broadcastPacket(new SocialAction(npc, 2));
					
					if (frintezza != null && targets[0] == npc && npc.getNpcId() == FRINTEZZA && _SoulBreakArrowUse == 0)
					{
						if (Rnd.get(100) < 100)
						{
							ZONE.broadcastPacket(new MagicSkillCanceled(frintezza.getObjectId()));
							cancelQuestTimers("songs_play");
							cancelQuestTimers("songs_effect");
							startQuestTimer("stop_effect", 0, frintezza, null, false);
							npc.getCast().stop();
						//	npc.abortCast();
							ZONE.broadcastPacket(new MagicSkillCanceled(frintezza.getObjectId()));
							
							for (Creature pc : ZONE.getKnownTypeInside(Player.class))
								pc.stopSkillEffects(5008);
							
							startQuestTimer("songs_play", 60000 + Rnd.get(60000), frintezza, null, false);
							npc.broadcastNpcSay("Musical performance as temporarily interrupted.");
							_SoulBreakArrowUse = 1;
						}
					}
					break;
				case 2276:
					if (frintezza != null && targets[0] == npc && npc.getNpcId() == EVIL_SPIRIT || frintezza != null && targets[0] == npc && npc.getNpcId() == EVIL_SPIRIT_2)
					{
						npc.doDie(caster);
						npc.broadcastNpcSay("I was destroyed by Dewdrop of Destruction.");
					}
					break;
			}
		}
		
		return null/*super.onSkillSee(npc, caster, skill, targets, isPet)*/;
	}
	
	//@Override
	public String onAttack(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		_LastAction = System.currentTimeMillis();
		if (npc.getNpcId() == FRINTEZZA)
		{
			npc.getStatus().setHpMp(npc.getStatus().getMaxHp(), 0);
			return null;
		}
		if (npc.getNpcId() == SCARLET1 && _SecondMorph == 0 && _ThirdMorph == 0 && _OnMorph == 0 && npc.getStatus().getHp() < npc.getStatus().getMaxHp() * 0.75 && GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
		{
			stopAttacks();
			stopPcActions();
			stopNpcActions();
			
			_SecondMorph = 1;
			_OnMorph = 1;
			
			startQuestTimer("morph_01", 1100, npc, null, false);
		}
		else if (npc.getNpcId() == SCARLET1 && _SecondMorph == 1 && _ThirdMorph == 0 && _OnMorph == 0 && npc.getStatus().getHp() < npc.getStatus().getMaxHp() * 0.5 && GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
		{
			_ThirdMorph = 1;
			_OnMorph = 1;
			
			stopAttacks();
			stopPcActions();
			stopNpcActions();
			
			startQuestTimer("morph_05a", 2000, npc, null, false);
			startQuestTimer("morph_05", 2100, npc, null, false);
		}
		else if (npc.getNpcId() == SCARLET2 && _SecondMorph == 1 && _ThirdMorph == 1 && _OnCheck == 0 && damage >= npc.getStatus().getHp() && GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
		{
			_OnCheck = 1;
			startQuestTimer("check_hp", 0, npc, null, false);
		}
		else if ((npc.getNpcId() == 29050 || npc.getNpcId() == 29051) && _Bomber == 0)
		{
			if (npc.getStatus().getHp() < npc.getStatus().getMaxHp() * 0.1)
			{
				if (Rnd.get(100) < 30)
				{
					_Bomber = 1;
					startQuestTimer("bomber", 3000, npc, null, false);
					
					L2Skill sk = SkillTable.getInstance().getInfo(5011, 1);
					if (sk != null)
						npc.getCast().doCast(sk, attacker, null);
				}
			}
		}
		
		return null;//super.onAttack(npc, attacker, damage, skill);
	}
	//@Override
	public String onKill(Npc npc, Creature killer)
	{
		final Player player = killer.getActingPlayer();
		if (player == null)
			return null;
		
		if (npc.getNpcId() == SCARLET2)
		{
			ZONE.broadcastPacket(new PlaySound(1, "BS01_D", npc));
			
			stopPcActions();
			stopNpcActions();
			
			int angle;
			if (npc.getHeading() < 32768)
				angle = Math.abs(180 - (int) (npc.getHeading() / 182.044444444));
			else
				angle = Math.abs(540 - (int) (npc.getHeading() / 182.044444444));
			
			ZONE.broadcastPacket(new SpecialCamera(npc.getObjectId(), 300, angle - 180, 5, 0, 7000, 0, 0, 0, 0));
			ZONE.broadcastPacket(new SpecialCamera(npc.getObjectId(), 200, angle, 85, 4000, 10000, 0, 0, 0, 0));
			startQuestTimer("morph_17b", 7400, npc, null, false);
			
			GrandBossManager.getInstance().setBossStatus(FRINTEZZA, DEAD);
			long respawnTime = (long) Config.SPAWN_INTERVAL_FRINTEZZA + Rnd.get(-Config.RANDOM_SPAWN_TIME_FRINTEZZA, Config.RANDOM_SPAWN_TIME_FRINTEZZA);
			respawnTime *= 3600000;
			
			cancelQuestTimers("spawn_minion");
			cancelQuestTimers("frintezza_despawn");
			startQuestTimer("remove_players", 900000, null, null, false);
			startQuestTimer("frintezza_unlock", respawnTime, null, null, false);
			
			StatSet info = GrandBossManager.getInstance().getStatSet(FRINTEZZA);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(FRINTEZZA, info);
		}
		else if (npc.getNpcId() == 18328)
		{
			if (Rnd.get(100) < 33)
				player.addItem("Quest", DEWDROP_OF_DESTRUCTION, 1, npc, true);
			
			int alarmsRemaining = getMobs(npc);
			if (alarmsRemaining == 1)
			{
				for (int i = 25150051; i <= 25150058; i++)
					DoorData.getInstance().getDoor(i).openMe();
			}
			else if (alarmsRemaining == 0)
			{
				ZONE.broadcastPacket(new CreatureSay(npc.getObjectId(), SayType.SHOUT, npc.getName(), "De-activate the alarm."));
				deleteAllMobs();
				room2Spawn(1);
			}
		}
		else if (npc.getNpcId() == 18333)
		{
			if (Rnd.get(100) < 10)
				player.addItem("Quest", DEWDROP_OF_DESTRUCTION, 1, npc, true);
		}
		else if (npc.getNpcId() == 18339)
		{
			if (getMobs(npc) == 0)
				room2Spawn(2);
		}
		else if (npc.getNpcId() == 18334)
		{
			if (Rnd.get(100) < 33)
				player.addItem("Quest", SOUL_BREAKING_ARROW, Rnd.get(5,15), npc, true);
			
			if (getMobs(npc) == 0)
			{
				deleteAllMobs();
				DoorData.getInstance().getDoor(25150045).openMe();
				DoorData.getInstance().getDoor(25150046).openMe();
				cancelQuestTimers("notice");
				startQuestTimer("waiting", Config.WAIT_TIME_FRINTEZZA, null, null, false);
			}
		}
		
		return null;//super.onKill(npc, killer);
	}
	
	private int getMobs(Npc npc)
	{
		_roomMobs.remove(npc);
		
		int count = 0;
		for (Npc mob : _roomMobs)
		{
			if (mob.getNpcId() == npc.getNpcId())
				count++;
		}
		
		return count;
	}
	
	private void deleteAllMobs()
	{
		for (Npc mob : _roomMobs)
			mob.deleteMe();
		
		for (Npc mob : ZONE.getKnownTypeInside(Npc.class))
			mob.deleteMe();
		
		_roomMobs.clear();
		
		if (frintezza != null)
		{
			frintezza.deleteMe();
			frintezza = null;
		}
	}
	
	private void stopAttacks()
	{
		cancelQuestTimers("skill01");
		cancelQuestTimers("skill02");
		cancelQuestTimers("skill03");
		cancelQuestTimers("songs_play");
		cancelQuestTimers("songs_effect");
		
		ZONE.broadcastPacket(new MagicSkillCanceled(frintezza.getObjectId()));
	}
	
	private void room2Spawn(int spawn)
	{
		if (spawn == 1)
		{
			for (int i = 41; i <= 44; i++)
				_roomMobs.add(addSpawn(MOBS[i][0], MOBS[i][1], MOBS[i][2], MOBS[i][3], MOBS[i][4], false, 0, false));
			
			for (int i = 25150051; i <= 25150058; i++)
				DoorData.getInstance().getDoor(i).openMe();
			
			DoorData.getInstance().getDoor(25150042).openMe();
			DoorData.getInstance().getDoor(25150043).openMe();
		}
		else
		{
			DoorData.getInstance().getDoor(25150042).closeMe();
			DoorData.getInstance().getDoor(25150043).closeMe();
			DoorData.getInstance().getDoor(25150045).closeMe();
			DoorData.getInstance().getDoor(25150046).closeMe();
			
			for (int i = 25150061; i <= 25150070; i++)
				DoorData.getInstance().getDoor(i).openMe();
			
			for (int i = 45; i <= 131; i++)
				_roomMobs.add(addSpawn(MOBS[i][0], MOBS[i][1], MOBS[i][2], MOBS[i][3], MOBS[i][4], false, 0, false));
		}
	}
	
	private static void stopNpcActions()
	{
		for (Npc mob : ZONE.getKnownTypeInside(Npc.class))
		{
			if (mob.getNpcId() != FRINTEZZA)
			{
				mob.disableAllSkills();
				mob.setInvul(true);
				mob.setIsImmobilized(true);
			}
		}
	}
	
	private static void startNpcActions()
	{
		for (Npc mob : ZONE.getKnownTypeInside(Npc.class))
		{
			if (mob.getNpcId() != FRINTEZZA)
			{
				mob.enableAllSkills();
				mob.setRunning(true);
				mob.setInvul(false);
				mob.setIsImmobilized(false);
			}
		}
	}
	
	private static void closeDoors()
	{
		for (int i = 25150051; i <= 25150058; i++)
			DoorData.getInstance().getDoor(i).closeMe();
		
		for (int i = 25150061; i <= 25150070; i++)
			DoorData.getInstance().getDoor(i).closeMe();
		
		DoorData.getInstance().getDoor(25150042).closeMe();
		DoorData.getInstance().getDoor(25150043).closeMe();
		DoorData.getInstance().getDoor(25150045).closeMe();
		DoorData.getInstance().getDoor(25150046).closeMe();
	}
	
	private static void stopPcActions()
	{
		for (Creature cha : ZONE.getKnownTypeInside(Player.class))
		{
			cha.abortAll(true);
		//	cha.abortAttack();
		//	cha.abortCast();
			cha.disableAllSkills();
			cha.setTarget(null);
		//	cha.stopMove(null);
			cha.setIsImmobilized(true);
			cha.getAI().tryToIdle();//setIntention(IntentionType.IDLE);
		}
	}
	
	private static void startPcActions()
	{
		for (Creature cha : ZONE.getKnownTypeInside(Player.class))
		{
			cha.enableAllSkills();
			cha.setIsImmobilized(false);
		}
	}
	
	private static void throwUp(Creature attacker, final double range, SystemMessage msg)
	{
		final int mx = attacker.getX(), my = attacker.getY();
		for (Creature target : ZONE.getKnownTypeInside(Player.class))
		{
			if (target == attacker)
				continue;
			
			if (target instanceof Npc && isFrintezzaFriend(((Npc)target).getNpcId()))
				continue;
			
			double dx = target.getX() - mx;
			double dy = target.getY() - my;
			if (dx == 0 && dy == 0) dx = dy = range / 2;
			double aa = range / Math.sqrt(dx * dx + dy * dy);
			if (aa > 1.0)
			{
				int x = mx + (int)(dx * aa);
				int y = my + (int)(dy * aa);
				int z = target.getZ();
				target.getAI().tryToIdle();//setIntention(IntentionType.IDLE);
				target.abortAll(true);
				//target.abortAttack();
				//target.abortCast();
				target.broadcastPacket(new FlyToLocation(target, x, y, z, FlyType.THROW_UP));
				target.setXYZ(x, y, z);
				target.broadcastPacket(new ValidateLocation(target));
				
				if (msg != null) 
					target.sendPacket(msg);
				
				if (target instanceof Player)
					((Player)target).standUp();
			}
		}
	}
	
	private static boolean isFrintezzaFriend(int npcId)
	{
		return npcId >= 29045 && npcId <= 29053;
	}
}