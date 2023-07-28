package net.sf.l2j.gameserver.model.events;

import java.util.logging.Logger;

public abstract class AbstractEvent
{
	protected final Logger log = Logger.getLogger(getClassName());

	protected abstract String getClassName();
}