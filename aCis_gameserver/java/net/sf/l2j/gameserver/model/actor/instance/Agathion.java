package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import net.sf.l2j.commons.math.MathUtil;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.serverpackets.PetDelete;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

/**
 * @author maxi5
 *
 */
public class Agathion extends Npc
{
	private Player owner;
	private Future<?> _followTask;
	
	private ItemInstance item;
	
	public Agathion(int objectId, NpcTemplate template, Player owner)
	{
		super(objectId, template);
		
		// Set the magical circle animation.
		setShowSummonAnimation(true);
		
		// Set the Player owner.
		this.owner = owner;
		
        setInstanceId(owner.getInstanceId()); // set instance to same as owner
	}
	
	@Override
	public void onSpawn()
	{
		followToOwner();
		super.onSpawn();
	}
	
	public void unSummon(Player owner)
	{
		// Abort attack, cast and move.
		abortAll(true);
		
		if (item != null)
			item.setAgathion(owner, false);
		
		owner.sendPacket(new PetDelete(2, getObjectId()));
		owner.setAgathion(null);
		
		if (owner.getSummon() != null)
			owner.getSummon().sendInfo(owner);
		owner.broadcastUserInfo();
		
		if (_followTask != null)
			_followTask.cancel(true);
		
		decayMe();
		deleteMe();
		
		super.deleteMe();
	}
	
	public void followToOwner()
	{
		if (_followTask == null)
			_followTask = ThreadPool.scheduleAtFixedRate(new Follow(this), 1000, 1000);
		
		SpawnLocation loc = owner.getPosition();
		int rnd = Rnd.get(-20, +30);
		boolean action = Rnd.get(100) < 15;

		if (!checkIfInRange(2000, this, owner))
			teleportTo(loc, 30);
		else if (!checkIfInRange(1000, this, owner))
			instantTeleportTo(loc, 30);
		
		if (owner.isMoving())
			getAI().tryToFollow(owner, false);
		else
		{
			if (checkIfInRange(30, this, owner))
			{
				getAI().tryToIdle();
				if (action)
					broadcastPacket(new SocialAction(this, 1));
			}
			if (!checkIfInRange(75, this, owner) || action)
				getAI().tryToMoveTo(new Location(loc.getX()+rnd, loc.getY()+rnd, loc.getZ()), null);
		}
	}

	private static class Follow implements Runnable
	{
		private final Agathion agathion;
		
		protected Follow(Agathion aga)
		{
			agathion = aga;
		}
		
		@Override
		public void run()
		{
			agathion.followToOwner();
		}	
	}
	
	public static boolean checkIfInRange(int range, Npc npc, Player player)
	{
		return MathUtil.checkIfInRange((int) (range + npc.getCollisionRadius()), npc, player, true);
	}
	
	public void setAgathionItem(ItemInstance item)
	{
		this.item = item;
	}
}