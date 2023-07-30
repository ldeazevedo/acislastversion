package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.model.events.tvt.TvTManager;

import java.util.Arrays;

public enum EventEnum
{
	RANDOM_FIGHT(1, RandomFightEngine.getInstance()),
	TVT(2, TvTManager.getInstance()),
	SURVIVAL(3, SurvivalEvenEngine.getInstance());

	private final int id;
	private final AbstractEvent event;

	EventEnum(int id, AbstractEvent event)
	{
		this.id = id;
		this.event = event;
	}

	public static AbstractEvent getEventByEnum(EventEnum eventEnum)
	{
		var eventFound = Arrays.stream(values()).filter(v -> v.id == eventEnum.id).findFirst();
		return eventFound.map(anEnum -> anEnum.event).orElse(null);
	}
}
