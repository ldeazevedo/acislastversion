package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.gameserver.model.actor.Player;

public class NewEventManager implements IEvent
{
	protected final CLogger log = new CLogger(this.getClass().getName());
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
		if (runningEvent != null)
			runningEvent.onLogout(player);
	}

	@Override
	public void clean()
	{
		if (runningEvent == null)
			return;
		runningEvent.clean();
		runningEvent = null;
	}

	@Override
	public boolean onKill(Player player, Player killer)
	{
		return runningEvent != null && runningEvent.onKill(player, killer);
	}

	@Override
	public void processCommand(String text, Player player)
	{
		if (runningEvent == null)
		{
			player.sendMessage("No hay ningun evento activo.");
			return;
		}
		runningEvent.processCommand(text, player);
	}

	public boolean isInEvent(Player player)
	{
		return runningEvent.isInEvent(player);
	}
}