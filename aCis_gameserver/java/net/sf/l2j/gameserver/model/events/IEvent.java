package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.model.actor.Player;

public interface IEvent
{
	void onLogout(Player player);

	void clean();

	boolean onKill(Player player, Player killer);
}