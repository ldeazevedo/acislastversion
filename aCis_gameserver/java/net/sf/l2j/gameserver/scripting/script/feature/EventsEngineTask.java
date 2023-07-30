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
package net.sf.l2j.gameserver.scripting.script.feature;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.events.util.EventConstants;
import net.sf.l2j.gameserver.model.events.RandomFightEngine;
import net.sf.l2j.gameserver.model.events.util.State;
import net.sf.l2j.gameserver.scripting.Quest;

public class EventsEngineTask extends Quest
{
	private final Timer timer = new Timer();
	
	public EventsEngineTask()
	{
		super(-1, "feature");
	//	setTask();
	}
	
	@Override
	public String onTimer(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("clear"))
		{
			timer.cancel();
			setTask();
			RandomFightEngine.getInstance().clean();
		}
		else if (event.equalsIgnoreCase("RF"))
		{
			startQuestTimer("RF", 3600000, null, null, true);
			startRandomFight();
		}
		else if (event.equalsIgnoreCase("doItJustOnceRF"))
		{
			startRandomFight();
		}
		else if (event.equalsIgnoreCase(EventConstants.STAGE_1))
			RandomFightEngine.getInstance().setNewState(State.LOADING);
		else if (event.equalsIgnoreCase(EventConstants.STAGE_2) || event.equalsIgnoreCase(EventConstants.STAGE_3))
			RandomFightEngine.getInstance().setNewState(State.PREPARING);
		else if (event.equalsIgnoreCase(EventConstants.STAGE_4))
			RandomFightEngine.getInstance().setNewState(State.FIGHT);
		else if (event.equalsIgnoreCase(EventConstants.STAGE_5))
			RandomFightEngine.getInstance().setNewState(State.ENDING);
		else if (event.equalsIgnoreCase("cancelQuestTimers"))
		{
			EventConstants.RANDOM_FIGHT_TIMER_CONFIG.forEach(tuple -> cancelQuestTimers(tuple.left()));
			log.info("EventsEngineTask: cancelQuestTimers");
		}
		return null;
	}

	private void startRandomFight()
	{
		RandomFightEngine.getInstance().setNewState(State.REGISTER);
		EventConstants.RANDOM_FIGHT_TIMER_CONFIG.forEach(tuple -> startQuestTimer(tuple.left(), tuple.right(), null, null, false));
	}

	private void setTask()
	{
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		if (hour > 0 && hour <= 3)
			calendar.set(Calendar.HOUR_OF_DAY, 3);
		else if (hour > 3 && hour <= 6)
			calendar.set(Calendar.HOUR_OF_DAY, 6);
		else if (hour > 6 && hour <= 9)
			calendar.set(Calendar.HOUR_OF_DAY, 9);
		else if (hour > 9 && hour <= 12)
			calendar.set(Calendar.HOUR_OF_DAY, 12);
		else if (hour > 12 && hour <= 15)
			calendar.set(Calendar.HOUR_OF_DAY, 15);
		else if (hour > 15 && hour <= 18)
			calendar.set(Calendar.HOUR_OF_DAY, 18);
		else if (hour > 18 && hour <= 21)
			calendar.set(Calendar.HOUR_OF_DAY, 21);

		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		timer.schedule(new setTimerTask(1000), calendar.getTime());
		timer.schedule(new setTimerTask(1800000), calendar.getTime());
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		log.info("EventsEngineTask: " + format.format(calendar.getTime()));
	}
	
	private class setTimerTask extends TimerTask
	{
		final int _time;
		
		public setTimerTask(int time)
		{
			_time = time;
		}
		
		@Override
		public void run()
		{
			startQuestTimer(_time <= 1000 ? "Survival" : "RF", _time, null, null, false);
		}
	}
}