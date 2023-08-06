package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.model.actor.Player;

import java.util.logging.Logger;

public class NewEventManager implements IEvent
{
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	private static NewEventManager instance;
	private AbstractEvent runningEvent = null;

	public static NewEventManager getInstance()
	{
		if (instance == null)
			instance = new NewEventManager();
		return instance;
	}

	private NewEventManager()
	{
	}

	public void startEvent(EventEnum eventEnum)
	{
		log.info("About to start " + eventEnum + " ...");
		if (runningEvent != null)
		{
			log.info("There's already an event going on at this moment. Can't start a new one at the same time.");
			return;
		}
		runningEvent = EventEnum.getEventByEnum(eventEnum);
	}

	@Override
	public void onLogout(Player player)
	{
		runningEvent.onLogout(player);
	}

	@Override
	public void clean()
	{
		runningEvent.clean();
		runningEvent = null;
	}

	@Override
	public boolean onKill(Player player, Player killer)
	{
		return runningEvent.onKill(player, killer);
	}

	@Override
	public void processCommand(String text, Player player)
	{
		runningEvent.processCommand(text, player);
	}

	public boolean isInEvent(Player player)
	{
		return runningEvent.isInEvent(player);
	}
}