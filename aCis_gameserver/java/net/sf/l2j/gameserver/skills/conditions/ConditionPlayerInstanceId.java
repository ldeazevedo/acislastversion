package net.sf.l2j.gameserver.skills.conditions;

import java.util.ArrayList;

import net.sf.l2j.gameserver.data.manager.InstanceManager;
import net.sf.l2j.gameserver.data.manager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.skills.L2Skill;

public class ConditionPlayerInstanceId extends Condition
{
	private final ArrayList<Integer> _instanceIds;
	
	public ConditionPlayerInstanceId(ArrayList<Integer> instanceIds)
	{
		_instanceIds = instanceIds;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, L2Skill skill, Item item)
	{
		if (!(effector instanceof Player))
			return false;

		final int instanceId =  effector.getInstanceId();
		if (instanceId <= 0)
			return false; // player not in instance

		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld((Player) effector);
		if (world == null || world.getInstanceId() != instanceId)
			return false; // player in the different instance

		return _instanceIds.contains(world.getTemplateId());         
	}
}