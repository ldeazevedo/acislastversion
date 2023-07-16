package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.skills.L2Skill;

public class WildSummon extends Monster
{
	private Player _caster;
	private Future<?> _followTask;
	private L2Skill skill;
	private boolean isMage;
	
	public WildSummon(int objectId, NpcTemplate template, Player caster, L2Skill skill, boolean isMage)
	{
		super(objectId, template);
		
        setInstanceId(caster.getInstanceId());
		
		setShowSummonAnimation(true);
		
		_caster = caster;
		this.skill = skill;
		this.isMage = isMage;
	}
	
	@Override
	public void onSpawn()
	{
		setTitle(_caster.getName());
		setAI();
		super.onSpawn();
	}
	
	public boolean isMage()
	{
		return isMage;
	}
	
	@Override
	public void setAI()
	{
		if (_followTask == null)
			_followTask = ThreadPool.scheduleAtFixedRate(new AI(this), 500, 500);
		
		Creature caster = _caster;
		SpawnLocation loc = caster.getPosition();
		int rnd = Rnd.get(-20, +30);

		if (checkIfInRange(60, this, caster))
			doCast(skill);
		else if (!checkIfInRange(750, this, caster))
			getAI().tryToFollow(caster, false);
		
		if (caster.isMoving())
		{
			if (checkIfInRange(30, this, caster))
				getAI().tryToIdle();
			if (!checkIfInRange(75, this, caster))
				getAI().tryToMoveTo(new Location(loc.getX()+rnd, loc.getY()+rnd, loc.getZ()), null);
		}
	}
	
	private void doCast(L2Skill skill)
	{
		getCast().doCast(skill, _caster, null);
	}

	private static class AI implements Runnable
	{
		private final WildSummon mob;
		
		protected AI(WildSummon aga)
		{
			mob = aga;
		}
		
		@Override
		public void run()
		{
			mob.setAI();
		}	
	}
	
	public static boolean checkIfInRange(int range, Npc npc, Creature player)
	{
		return MathUtil.checkIfInRange((int) (range + npc.getCollisionRadius()), npc, player, true);
	}
}