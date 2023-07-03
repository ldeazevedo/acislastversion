package net.sf.l2j.gameserver.scripting.script.ai.boss;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.npc.AggroInfo;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.model.zone.type.ScriptZone;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

@SuppressWarnings("unused")
public class Scarlet extends AttackableAIScript
{
	private static final ScriptZone FRINTEZZA_LAIR = ZoneManager.getInstance().getZoneById(100004, ScriptZone.class);
	
	private final L2Skill[] DaemonAttack = { SkillTable.getInstance().getInfo(5014, 1), SkillTable.getInstance().getInfo(5014, 2), SkillTable.getInstance().getInfo(5014, 3) };

	private final L2Skill[] DaemonCharge = {
			SkillTable.getInstance().getInfo(5015, 1),
			SkillTable.getInstance().getInfo(5015, 2),
			SkillTable.getInstance().getInfo(5015, 3),
			SkillTable.getInstance().getInfo(5015, 4),
			SkillTable.getInstance().getInfo(5015, 5),
			SkillTable.getInstance().getInfo(5015, 6) };

	private final L2Skill[] DaemonField = { SkillTable.getInstance().getInfo(5018, 1), SkillTable.getInstance().getInfo(5018, 2) };

	private final L2Skill YokeOfScarlet = SkillTable.getInstance().getInfo(5016, 1);

	private final L2Skill DaemonDrain = SkillTable.getInstance().getInfo(5019, 1);

	private static final int _strongScarletId = 29047;
	private static final int _frintezzasSwordId = 7903;

	public Scarlet(Creature actor)
	{
		super();
	}

	protected boolean createNewTask()
	{
		return false;
	/*	clearTasks();
		Creature target;
		if((target = prepareTarget()) == null)
			return false;

		Npc actor = getActor();
		if(actor == null || actor.isDead())
			return false;

		if(!FRINTEZZA_LAIR.getZone().checkIfInZone(actor))
		{
			teleportHome(true);
			return false;
		}

		int stage = 0;
		if(actor.getNpcId() == _strongScarletId)
			stage = 2;
		else if(actor.getRightHandItemId() == _frintezzasSwordId)
			stage = 1;

		double distance = actor.getDistance(target);
		int rnd_per = Rnd.get(100);

		// if(rnd_per < 10)
		// return chooseTaskAndTargets(null, target, distance);

		if(rnd_per < 50)
			return chooseTaskAndTargets(DaemonAttack[stage], target, distance);

		boolean PowerEncore = false;
		
		AbstractEffect effect = actor.getFirstEffect(SkillTable.getInstance().getInfo(5015, 1));//getEffectList().getFirstEffect(5008);
		if(effect != null && effect.getSkill().getLevel() == 3)
			PowerEncore = true;

		Map<L2Skill, Integer> d_skill = new HashMap<L2Skill, Integer>();

		switch (stage)
		{
			case 0:
				if(distance > 200)
					addDesiredSkill(d_skill, target, distance, DaemonCharge[PowerEncore ? 3 : 0]);
				break;
			case 1:
				if(distance > 200)
					addDesiredSkill(d_skill, target, distance, DaemonCharge[PowerEncore ? 4 : 1]);
				addDesiredSkill(d_skill, target, distance, DaemonField[0]);
				break;
			case 2:
				if(distance > 200)
					addDesiredSkill(d_skill, target, distance, DaemonCharge[PowerEncore ? 5 : 2]);
				addDesiredSkill(d_skill, target, distance, DaemonField[1]);
				addDesiredSkill(d_skill, target, distance, YokeOfScarlet);
				addDesiredSkill(d_skill, target, distance, DaemonDrain);
				break;
		}

		L2Skill r_skill = selectTopSkill(d_skill);
		if(r_skill != null && !r_skill.isOffensive())
			target = actor;

		return chooseTaskAndTargets(r_skill, target, distance);*/
	}

	protected boolean maybeMoveToHome()
	{
	//	Npc actor = getActor();
//		if(actor != null && !FrintezzaManager.getZone().checkIfInZone(actor))
	//		teleportHome(true);
		return false;
	}

	public boolean isGlobalAI()
	{
		return true;
	}
	
	public void teleportHome(final boolean clearAggro)
	{
/*		final Npc actor = getActor();
		if(actor == null)
			return;

		if(clearAggro)
			actor.clearAggroList(true);

		setIntention(AI_INTENTION_ACTIVE);
		clearTasks();

		final SpawnLocation sloc = actor.getSpawnLocation();
		if(sloc != null)
		{
			actor.broadcastPacket(new MagicSkillUse(actor, actor, 2036, 1, 500, 0));
			actor.teleportTo(sloc.getX(), sloc.getY(), GeoEngine.getInstance().getHeight(sloc), 0);
		}*/
	}
	
	private static Player getRandomPlayer()
	{
		List<Player> list = FRINTEZZA_LAIR.getKnownTypeInside(Player.class);//.getZone().getInsidePlayers();
		if(list.isEmpty())
			return null;

		return list.get(Rnd.get(list.size()));
	}
	
	
	

	private static class doSkill implements Runnable
	{
		private final Creature _caster;
		private final int _interval, _range;

		public doSkill(Creature caster, int interval, int range)
		{
			_caster = caster;
			_interval = interval;
			_range = range;
		}

		@Override
		public void run()
		{
			if(_caster == null || _caster.isDead())
				return;
			try
			{
				WorldObject tempTarget = _caster.getTarget();
				if(tempTarget == null || !(tempTarget instanceof Creature))
					tempTarget = _caster;

				int x = tempTarget.getX() + Rnd.get(_range) - _range / 2, y = tempTarget.getY() + Rnd.get(_range) - _range / 2, z = tempTarget.getZ();
			//	if(_caster.getDistance(x, y) > _range && getZone().checkIfInZone(tempTarget))
				{
					_caster.broadcastPacket(new MagicSkillUse(_caster, (Creature) tempTarget, 1086, 1, 0, 0));
					_caster.decayMe();
					_caster.setXYZ(x, y, z);
					_caster.spawnMe();
					_caster.setTarget(tempTarget);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			ThreadPool.schedule(new doSkill(_caster, _interval, _range), _interval + Rnd.get(500));
		}
	}
}
