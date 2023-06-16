package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Copy from EffectDanceStun.java
 */
public class EffectFloatStun extends AbstractEffect
{
	public EffectFloatStun(EffectTemplate template, L2Skill skill, Creature effected, Creature effector)
	{
		super(template, skill, effected, effector);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.STUN;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected().isStunned() || getEffected().isImmobilized())
			return false;
		int e = getEffected().getAbnormalEffect();
		if ((e & AbnormalEffect.FLOATING_ROOT.getMask()) != 0 || (e & AbnormalEffect.DANCE_STUNNED.getMask()) != 0)
			return false;

		// Abort attack, cast and move.
		getEffected().abortAll(false);
		
		getEffected().getAI().tryToIdle();
		
		getEffected().startAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
		getEffected().setIsImmobilized(true);
		getEffected().disableAllSkills();
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
		getEffected().setIsImmobilized(false);
		getEffected().enableAllSkills();
	}
	
	@Override
	public boolean onActionTime()
	{
		// just stop this effect
		return false;
	}
}