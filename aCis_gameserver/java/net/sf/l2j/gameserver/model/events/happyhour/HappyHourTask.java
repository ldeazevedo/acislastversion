package net.sf.l2j.gameserver.model.events.happyhour;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.events.EventManager;

public class HappyHourTask
{
	protected static final Logger _log = Logger.getLogger("HappyHourTask");
	protected ScheduledFuture<?> _scheduledTask;
	List<HappyHour> happyHourList = Config.HAPPY_HOUR_LIST;
	private static boolean isInEvent = false;
	
	public boolean isInEvent()
	{
		return isInEvent;
	}
	
	protected HappyHourTask()
	{
		setTask();
	}
	
	protected class TimeTaskForExpTask implements Runnable
	{
		@Override
		public void run()
		{
			int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			int minutes = Calendar.getInstance().get(Calendar.MINUTE);
			synchronized (this)
			{
				for (HappyHour happyHour : happyHourList)
					if (happyHour.getDay() == Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
					{
						broadcastMessage(hourOfDay, happyHour.getStartHour(), happyHour.getEndHour(), minutes);
						return;
					}
				return;
			}
		}
	}
	
	public static void broadcastMessage(int hourOfDay, int happyHourStart, int happyHourEnd, int minutes)
	{
		if (hourOfDay == happyHourStart && minutes == 0)
		{
			isInEvent = !isInEvent;
			EventManager.announce(isInEvent ? "Evento HAPPY HOUR iniciado. EXP aumentado a x" + Config.HAPPY_HOUR_EXP + "!." : "Evento HAPPY HOUR finalizado. EXP vuelve a la normalidad: x" + Config.RATE_XP + "!.");
			_log.info(isInEvent ? "Comenzo HappyHour." : "Finalizo HappyHour.");
			// HappyHourTask.getInstance().setTask();
			return;
		}
	}
	
	private void setTask()
	{
		if (_scheduledTask != null)
		{
			_scheduledTask.cancel(true);
			_scheduledTask = null;
		}
		_scheduledTask = ThreadPool.scheduleAtFixedRate(new TimeTaskForExpTask(), 60000, 60000);
	}
	
	public static HappyHourTask getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final HappyHourTask _instance = new HappyHourTask();
	}
}