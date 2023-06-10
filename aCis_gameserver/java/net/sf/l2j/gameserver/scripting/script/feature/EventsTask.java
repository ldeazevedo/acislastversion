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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.events.EventManager;
import net.sf.l2j.gameserver.model.events.TvTEvent;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.scripting.Quest;

public class EventsTask extends Quest
{
	private static final int MESSENGER = Config.TVT_EVENT_PARTICIPATION_NPC_ID;
	private Timer time = new Timer();
	
	public EventsTask()
	{
		super(-1, "feature");
		addFirstTalkId(MESSENGER);
		addTalkId(MESSENGER);
		setTask();
	}
	
	@Override
	public String onTimer(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("clear"))
		{
			if (time != null)
				time.cancel();
			setTask();
			EventManager.getInstance().clean();
		}
		else if (event.equalsIgnoreCase("Survival"))
		{
			startQuestTimer("Survival", 10800000, null, null, true); // 10800000
			setSurvival(0);
			
			startQuestTimer("Survival01", 60000, null, null, false); // checkRegist
			startQuestTimer("Survival02", 75000, null, null, false);
			startQuestTimer("Survival03", 105000, null, null, false);
			startQuestTimer("Survival04", 405000, null, null, false);
		}
		else if (event.equalsIgnoreCase("doItJustOnceSurvival"))
		{
			setSurvival(0);
			startQuestTimer("Survival01", 60000, null, null, false);
			startQuestTimer("Survival02", 75000, null, null, false);
			startQuestTimer("Survival03", 105000, null, null, false);
			startQuestTimer("Survival04", 405000, null, null, false);
		}
		else if (event.equalsIgnoreCase("Survival01"))
			setSurvival(1);
		else if (event.equalsIgnoreCase("Survival02"))
			setSurvival(2);
		else if (event.equalsIgnoreCase("Survival03"))
			setSurvival(3);
		else if (event.equalsIgnoreCase("Survival04"))
			setSurvival(4);
		else if (event.equalsIgnoreCase("RF"))
		{
			startQuestTimer("RF", 3600000, null, null, true);
			setRandomFight(0);
			
			startQuestTimer("RF01", 60000, null, null, false); // checkRegist
			startQuestTimer("RF02", 70000, null, null, false);
			startQuestTimer("RF03", 85000, null, null, false);
			startQuestTimer("RF04", 115000, null, null, false);
			startQuestTimer("RF05", 295000, null, null, false);
		}
		else if (event.equalsIgnoreCase("doItJustOnceRF"))
		{
			setRandomFight(0);
			startQuestTimer("RF01", 60000, null, null, false);
			startQuestTimer("RF02", 70000, null, null, false);
			startQuestTimer("RF03", 85000, null, null, false);
			startQuestTimer("RF04", 115000, null, null, false);
			startQuestTimer("RF05", 295000, null, null, false);
		}
		else if (event.equalsIgnoreCase("RF01"))
			setRandomFight(1);
		else if (event.equalsIgnoreCase("RF02"))
			setRandomFight(2);
		else if (event.equalsIgnoreCase("RF03"))
			setRandomFight(3);
		else if (event.equalsIgnoreCase("RF04"))
			setRandomFight(4);
		else if (event.equalsIgnoreCase("RF05"))
			setRandomFight(5);
		else if (event.equalsIgnoreCase("cancelQuestTimers"))
		{
			cancelQuestTimers("Survival01");
			cancelQuestTimers("Survival02");
			cancelQuestTimers("Survival03");
			cancelQuestTimers("Survival04");
			
			cancelQuestTimers("DM01");
			cancelQuestTimers("DM02");
			cancelQuestTimers("DM03");
			cancelQuestTimers("DM04");
			
			cancelQuestTimers("RF01");
			cancelQuestTimers("RF02");
			cancelQuestTimers("RF03");
			cancelQuestTimers("RF04");
			cancelQuestTimers("RF05");
			LOGGER.info("EventsTask: cancelQuestTimers");
		}
		
		else if (event.equalsIgnoreCase("DM"))
		{
			// startQuestTimer("Survival", 10800000, null, null, true); //10800000
			setDM(0);
			
			startQuestTimer("DM01", 60000, null, null, false); // checkRegist
			startQuestTimer("DM02", 75000, null, null, false);
			startQuestTimer("DM03", 105000, null, null, false);
			startQuestTimer("DM04", 405000, null, null, false);
		}
		else if (event.equalsIgnoreCase("doItJustOnceDM"))
		{
			setDM(0);
			startQuestTimer("DM01", 60000, null, null, false);
			startQuestTimer("DM02", 75000, null, null, false);
			startQuestTimer("DM03", 105000, null, null, false);
			startQuestTimer("DM04", 405000, null, null, false);
		}
		else if (event.equalsIgnoreCase("DM01"))
			setDM(1);
		else if (event.equalsIgnoreCase("DM02"))
			setDM(2);
		else if (event.equalsIgnoreCase("DM03"))
			setDM(3);
		else if (event.equalsIgnoreCase("DM04"))
			setDM(4);
		return null;
	}
	
	private static void setDM(int stage)
	{
		EventManager.getInstance().setDM(stage);
	}
	
	private static void setSurvival(int stage)
	{
		EventManager.getInstance().setSurvival(stage);
	}
	
	private static void setRandomFight(int stage)
	{
		EventManager.getInstance().setRandomFight(stage);
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
		else if (hour > 21 && hour <= 0)
			calendar.set(Calendar.HOUR_OF_DAY, 0);
		
		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		time.schedule(new setTimerTask(1000), calendar.getTime());
		time.schedule(new setTimerTask(1800000), calendar.getTime());
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		LOGGER.info("EventsTask: " + String.valueOf(format.format(calendar.getTime())));
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
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String html = null;
		if (TvTEvent.isParticipating())
			html = "TvTEventParticipation.htm";
		else if (TvTEvent.isStarting() || TvTEvent.isStarted())
			html = "Status.htm";
		
		final boolean isParticipant = TvTEvent.isPlayerParticipant(player.getObjectId());
		int[] teamsPlayerCounts = TvTEvent.getTeamsPlayerCounts();
		int[] teamsPointsCounts = TvTEvent.getTeamsPoints();
		
		if (TvTEvent.isParticipating())
		{
			html = getHtmlText(html).replace("%fee%", TvTEvent.getParticipationFee());
			html = (!isParticipant ? getHtmlText("TvTEventParticipation.htm").replace("%playercount%", String.valueOf(teamsPlayerCounts[0] + teamsPlayerCounts[1])) : "RemoveParticipation.htm");
		}
		else if (TvTEvent.isStarting() || TvTEvent.isStarted())
			html = getHtmlText("Status.htm").replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME).replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME).replace("%playercount%", String.valueOf(teamsPlayerCounts[0] + teamsPlayerCounts[1]).replace("%team1points%", String.valueOf(teamsPointsCounts[0])).replace("%team2points%", String.valueOf(teamsPointsCounts[1])).replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]).replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]))));
		if (!isParticipant)
			html = getHtmlText(html).replace("%fee%", TvTEvent.getParticipationFee());

		return html;
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String html = event;
		if (player == null || !TvTEvent.isParticipating())
			return html;
		
		if (event.equalsIgnoreCase("tvt_event_participation"))
		{
			int playerLevel = player.getStatus().getLevel();
			
			if (player.isCursedWeaponEquipped())
				html = "CursedWeaponEquipped.htm";
			else if (EventManager.getInstance().containsPlayer(player))
				html = "already_another_event.htm";
			else if (OlympiadManager.getInstance().isRegistered(player))
				html = "Olympiad.htm";
			else if (player.getKarma() > 0)
				html = "Karma.htm";
			else if ((playerLevel < Config.TVT_EVENT_MIN_LVL) || (playerLevel > Config.TVT_EVENT_MAX_LVL))
				html = "Karma.htm";
			else if ((TvTEvent.getTeams()[0].getParticipatedPlayerCount() == Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS) && (TvTEvent.getTeams()[1].getParticipatedPlayerCount() == Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS))
				html = "TeamsFull.htm";
			else if (TvTEvent.needParticipationFee() && !TvTEvent.hasParticipationFee(player))
				html = "ParticipationFee.htm";
			else if (TvTEvent.addParticipant(player))
				html = "Registered.htm";

			html = getHtmlText(html).replace("%min%", String.valueOf(Config.TVT_EVENT_MIN_LVL)).replace("%max%", String.valueOf(Config.TVT_EVENT_MAX_LVL)).replace("%max%", String.valueOf(Config.TVT_EVENT_MAX_PLAYERS_IN_TEAMS)).replace("%fee%", TvTEvent.getParticipationFee());
		}
		else if (event.equalsIgnoreCase("tvt_event_remove_participation"))
		{
			TvTEvent.removeParticipant(player.getObjectId());
			html = "Unregistered.htm";
		}
		return html;
	}
}
