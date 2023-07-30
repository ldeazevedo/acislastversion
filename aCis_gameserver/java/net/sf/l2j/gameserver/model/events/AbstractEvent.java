package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.events.util.State;
import net.sf.l2j.gameserver.model.location.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractEvent implements IEvent
{
	protected final Logger log = Logger.getLogger(getClassName());
	protected final List<Player> registeredPlayers = new ArrayList<>();
	protected final Location defaultLocation = new Location(82698, 148638, -3473);
	protected State currentState = State.INACTIVE;

	protected boolean isInProgress()
	{
		return currentState != State.INACTIVE;
	}

	protected void getBuffs(Player killer)
	{
		killer.getSkill(1204, 2); // Wind Walk
		if (killer.isMageClass())
			killer.getSkill(1085, 3);// Acumen
		else
			killer.getSkill(1086, 2); // haste
	}

	public boolean isInEvent(Player player)
	{
		return currentState == State.FIGHT && registeredPlayers.contains(player);
	}

	public boolean reqPlayers()
	{
		return registeredPlayers.isEmpty() || registeredPlayers.size() < 2;
	}

	public void clean(String questName)
	{
		registeredPlayers.clear();
		currentState = State.INACTIVE;
		ScriptData.getInstance().getQuest(questName).startQuestTimer("cancelQuestTimers", null, null, 1000);
	}

	protected abstract String getClassName();
}