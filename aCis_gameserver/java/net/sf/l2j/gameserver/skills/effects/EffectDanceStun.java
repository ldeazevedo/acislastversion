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
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * Copy from http://www.l2jserver.com/forum/viewtopic.php?f=69&t=13999
 * @author KKnD
 */
public class EffectDanceStun extends AbstractEffect
{
	public EffectDanceStun(EffectTemplate template, L2Skill skill, Creature effected, Creature effector)
	{
		super(template, skill, effected, effector);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.STUN;
	}
	
	/** Notify started */
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
		
		getEffected().startAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
		getEffected().setIsImmobilized(true);
		getEffected().disableAllSkills();
		return true;
	}
	
	/** Notify exited */
	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
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