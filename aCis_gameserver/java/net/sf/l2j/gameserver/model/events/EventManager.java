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
package net.sf.l2j.gameserver.model.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.SpawnType;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.zone.type.TownZone;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;

public class EventManager
{
	protected static final Logger _log = Logger.getLogger(EventManager.class.getName());
	
	public Vector<Player> players = new Vector<>();
	public State state = State.INACTIVE;
	public Events event = Events.NULL;
	
	int StateEvent = 0;
	protected int topKills = 0;
	public List<Player> topPlayers = new ArrayList<>();
	
	public Player _activeChar;
	
	enum State
	{
		INACTIVE,
		REGISTER,
		LOADING,
		FIGHT,
		ENDING
	}
	
	enum Events
	{
		NULL,
		SURVIVAL,
		RF,
		DM
	}
	
	protected EventManager()
	{
	}
	
	public void removePlayer(Player player)
	{
		synchronized (players)
		{
			players.remove(player);
		}
	}
	
	public boolean addPlayer(Player player)
	{
		if (player == null)
			return false;
		
		synchronized (players)
		{
			players.add(player);
		}
		return true;
	}
	
	public boolean containsPlayer(Player player)
	{
		synchronized (players)
		{
			return players.contains(player);
		}
	}
	
	public static void getBuffs(Player killer)
	{
		killer.getSkill(1204, 2); // Wind Walk
		if (killer.isMageClass())
			killer.getSkill(1085, 3);// Acumen
		else
			killer.getSkill(1086, 2); // haste
	}
	
	public class Msg implements Runnable
	{
		private String _Msg;
		private int _Time;
		
		public Msg(String msg, int time)
		{
			_Msg = msg;
			_Time = time;
		}
		
		@Override
		public void run()
		{
			sendMsg(_Msg, _Time);
		}
		
		void sendMsg(String msg, int time)
		{
			for (Player player : World.getInstance().getPlayers())
				player.sendPacket(new ExShowScreenMessage(msg, time, SMPOS.TOP_CENTER, false));
		}
	}
	
	public boolean isInEvent(Player pc)
	{
		return state == State.FIGHT && containsPlayer(pc);
	}
	
	public void announce(String msg)
	{
		World.announceToOnlinePlayers(msg);
	}
	
	public boolean isInProgress()
	{
		return state != State.INACTIVE;
	}
	
	public void checkTimeusEvents(String text, Player player)
	{
		if (!isInProgress() || player == null || player.isInObserverMode() || player.isInOlympiadMode() || player.isFestivalParticipant() || /*player.isInSiege() || */ player.isInJail() || player.isFestivalParticipant() || player.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(player.getObjectId()) || player.getKarma() > 0)
			return;
		
		if (OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("No puedes participar ni ver el evento mientras estas registrado en oly.");
			return;
		}
		
		if (text.equalsIgnoreCase(".salir") && isInProgress())
		{
			if (!containsPlayer(player) || state != State.FIGHT)
				return;
			if (player.isDead())
			{
				removePlayer(player);
				revertPlayer(player);
			}
			return;
		}
		
		if (text.equalsIgnoreCase(".ver"))
		{
			if (containsPlayer(player) || player.isInObserverMode())
				return;

			if (event == Events.RF)
				player.enterObserverMode(new Location(179747, 54696, -2805));
			else if (event == Events.SURVIVAL)
				player.enterObserverMode(new Location(85574, 256964, -11674));
			return;
		}
		
		if (text.equalsIgnoreCase(".register"))
		{
			if (player.isDead())
				return;
		/*	if (player._active_boxes >= 1)
			{
				List<String> boxes = player.active_boxes_characters;
				
				if (boxes != null && boxes.size() > 1)
					for (String name : boxes)
					{
						Player pc = World.getInstance().getPlayer(name);
						if (pc == null || pc.isGM())
							continue;
						
						for (String ignore : ignorePlayers)
							if (ignore.equalsIgnoreCase(ignore))
								if (player != pc && pc.getName().equalsIgnoreCase(ignore) && containsPlayer(World.getInstance().getPlayer(ignore)))
									continue;
						
						if (containsPlayer(pc))
						{
							player.sendMessage("Ya estas parcitipando con otro personaje!");
							return;
						}
					}
			}*/
			if (player.isInObserverMode())
			{
				player.sendMessage("No te podes anotar si estas mirando el evento.");
				return;
			}
			if (containsPlayer(player))
			{
				player.sendMessage("Ya estas registrado en el evento.");
				return;
			}
			if (state != State.REGISTER)
			{
				player.sendMessage("El evento ya comenzo.");
				return;
			}
			addPlayer(player);
			player.sendMessage("Te registraste al evento!");
			return;
		}
		if (text.equalsIgnoreCase(".unregister"))
		{
			if (!containsPlayer(player))
			{
				player.sendMessage("No te registraste al evento.");
				return;
			}
			if (state != State.REGISTER)
			{
				player.sendMessage("El evento ya comenzo.");
				return;
			}
			removePlayer(player);
			player.sendMessage("Saliste del evento.");
			return;
		}
	}
	
	public void revertPlayer(Player player)
	{
		if (player.atEvent)
			player.atEvent = false;
		if (player.isInSurvival)
			player.isInSurvival = false;
		if (player.isDead())
			player.doRevive();
	//	player.setCurrentHp(player.getMaxHp());
	//	player.setCurrentCp(player.getMaxCp()); //	player.setCurrentMp(player.getMaxMp());
			player.getStatus().setMaxCpHpMp();
			player.broadcastUserInfo();
		
			if (player.getLastLocation() != null)
				player.instantTeleportTo(player.getLastLocation(), 0);
			else
				player.instantTeleportTo(82698, 148638, -3473, 0);
		
			if (player.getKarma() > 0)
				player.setKarma(0);
		
			player.setPvpFlag(0);
			player.setTeam(TeamType.NONE);
	}
	
	private class RevertTask implements Runnable
	{
		RevertTask()
		{
		}
		
		@Override
		public void run()
		{
			if (!players.isEmpty())
				for (Player p : players)
				{
					if (p == null)
						continue;

					if (state == State.FIGHT || state == State.ENDING)
					{
						revertPlayer(p);
						if (StateEvent == 3)
							for (Player player : players)
							{
								player.stopAbnormalEffect(0x0200);
								player.setIsImmobilized(false);
								player.setInvul(false);
								player.getStatus().setMaxCpHpMp();
							}
					}
				}
			clean();
		}
	}
	
	public boolean onKill(Player pc, Player pk)
	{
		boolean isInEvent = false;
		if (isInProgress() && state == State.FIGHT)
		{
			if (event == Events.SURVIVAL && StateEvent == 4)
			{
				if (pc != null)
				{
					if (pc != pk)
						pk.addAncientAdena("Survival", 25000, pk, true);
					pc.sendPacket(new ExShowScreenMessage("Para regresar escribir .salir o esperar a que termine el evento", 5000, SMPOS.MIDDLE_RIGHT, false));
					pc.isInSurvival = false;
				}
				boolean allDead = true;
				synchronized (players)
				{
					for (Player player : players)
					{
						if (pk.equals(player))
							continue;
						if (player.isInSurvival)
							allDead = false;
					}
				}
				
				if (allDead && state != State.ENDING)
				{
					state = State.ENDING;
					if (pk != null)
					{
						pk.sendMessage("Sos el ganador!");
						announce("Resultado Survival: " + pk.getName() + " es el ganador.");
						announce("Evento finalizado");
		//				pk.addItem("", Config.RANDOM_FIGHT_REWARD_ID, 2, null, true);
						pk.addAncientAdena("Survival", 100000, pk, true);
					}
					ThreadPool.schedule(new RevertTask(), 15000);
				}
				isInEvent = true;
			}
			if (event == Events.RF && state != State.ENDING && StateEvent == 5)
			{
				for (Player player : players)
				{
					if (player.isDead())
						pc = player;
					if (!player.isDead())
						pk = player;
				}
				state = State.ENDING;
				if (pk != null)
				{
					pk.sendMessage("Sos el ganador!");
					announce("Resultado Random Fight: " + pk.getName() + " es el ganador.");
					announce("Evento finalizado");
	//				pk.addItem("", Config.RANDOM_FIGHT_REWARD_ID, Config.RANDOM_FIGHT_REWARD_COUNT, null, true);
					
					// Guardar en la base de datos
					try (Connection con = ConnectionPool.getConnection();
						PreparedStatement statement = con.prepareStatement("select * from rf where char_name=?");
						ResultSet rs = statement.executeQuery();
						PreparedStatement statement2 = con.prepareStatement(rs.first() ? "update rf set count=count+1 where char_name=?" : "insert rf set count=1,char_name=?"))
					{
						statement.setString(1, pk.getName());
						statement2.setString(1, pk.getName());
						statement2.execute();
						statement2.close();
						rs.close();
						statement.close();
					}
					catch (Exception e)
					{
						_log.warning("Error en RF Ranking: " + e);
					}
				}
				ThreadPool.schedule(new RevertTask(), 15000);
				
				isInEvent = true;
			}
			
			if (event == Events.DM && StateEvent == 4)
			{
				if (pc != null && pk != null)
				{
					if (pc.atEvent && pk.atEvent)
						if (pc != pk) //	pk.addAncientAdena("DM", 25000, pk, true);
							pk.countDMkills++;
				}
				isInEvent = true;
			}
		}
		return isInEvent;
	}
	
	public void onLogout(Player pc)
	{
		Player pk = null;
		int alive = 0;
		if (pc != null)
		{
			if (containsPlayer(pc) || pc.atEvent || pc.isInSurvival)
			{
				Location loc = pc.getLastLocation();
				if (loc != null)
					pc.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
				else
					pc.setXYZInvisible(82698, 148638, -3473);
			}
			if (containsPlayer(pc))
				removePlayer(pc);
			
			pc.atEvent = false;
			pc.isInSurvival = false;
		}
		synchronized (players) // cuando un player se desconecta se redirecciona a onkill si solo queda un pj vivo en el evento
		{
			for (Player player : players)
				if (!player.isDead() || player.isInSurvival)
				{
					alive++;
					pk = player;
				}
			
			if (alive == 1)
				onKill(null, pk);
			else
				alive = 0;
		}
	}
	
	public boolean reqPlayers()
	{
		return players.isEmpty() || players.size() < 2;
	}
	
	public void clean()
	{
		if (state == State.FIGHT)
			for (Player p : players)
				p.setTeam(TeamType.NONE);
		
		for (Player pc : World.getInstance().getPlayers())
		{
			pc.isInSurvival = false;
			pc.atEvent = false;
		}
		
		players.clear();
		state = State.INACTIVE;
		event = Events.NULL;

		StateEvent = 0;
	//	ScriptData.getInstance().getQuest("EventsTask").startQuestTimer("cancelQuestTimers", 1000, null, null, false);
		ScriptData.getInstance().getQuest("EventsTask").startQuestTimer("cancelQuestTimers", null, null, 1000);
	}
	
	public void clear()
	{
		clean();
	//	ScriptData.getInstance().getQuest("EventsTask").startQuestTimer("clear", 1000, null, null, false);
		ScriptData.getInstance().getQuest("EventsTask").startQuestTimer("clear", null, null, 1000);
		//ScriptData.getInstance().getQuest("EventsTask").onTimer("clear", null, null);
		//.startQuestTimer("clear", 1000, null, null, false);
	}

	protected static final CLogger LOGGER = new CLogger(Quest.class.getName());
	public void setSurvival(int stage)
	{
		if (TvTEvent.isInProgress() || event == Events.RF || event == Events.DM)
		{
			ThreadPool.schedule(new RevertTask(), 15000);
			return;
		}
		switch (stage)
		{
			case 0:
				if (state == State.INACTIVE && event == Events.NULL)
				{
					event = Events.SURVIVAL;
					state = State.REGISTER;
					StateEvent = 1;
					for (Player player : World.getInstance().getPlayers())
						player.sendPacket(new ExShowScreenMessage("Evento Survival empezara en 1 minuto", 5000, SMPOS.TOP_CENTER, false));
					ThreadPool.schedule(new Msg("Para registrarte escribi .register", 5000), 5000);
					ThreadPool.schedule(new Msg("Para ver la pela escribi .ver", 5000), 10000);
					announce("Evento Survival empezara en 1 minuto");
					announce("Para registrarte, escribi .register");
					announce("Para mirar la pelea, escribi .ver");
					LOGGER.warn("stage 0 Reg time");
				}
				break;
			case 1:
				if (state == State.REGISTER && event == Events.SURVIVAL && StateEvent == 1)
				{
					state = State.LOADING;
					Vector<Player> newPlayers = new Vector<>();
					for (Player p : players)
						newPlayers.add(p);
					for (Player p : players)
						if (p.isInOlympiadMode() || p.isInObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
						{
							newPlayers.remove(p);
							p.sendMessage("no cumples los requisitos para participar en el evento.");
						}
					players = newPlayers;
					if (reqPlayers())
					{
						announce("Survival no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					
					announce("Cantidad de registrados: " + players.size());
					announce("Los personajes seran teleportados en 15 segundos.");
					StateEvent = 2;
				}
				break;
			case 2:
				if (state == State.LOADING && event == Events.SURVIVAL && StateEvent == 2)
				{
					if (reqPlayers())
					{
						announce("Survival no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					
					for (Player player : players)
						setPcPrepare(player);
					StateEvent = 3;
				}
				break;
			case 3:
				if (state == State.LOADING && event == Events.SURVIVAL && StateEvent == 3)
				{
					if (reqPlayers())
					{
						announce("Uno de los personajes no esta Online, se cancela el evento.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					state = State.FIGHT;
					for (Player player : players)
					{
						player.sendMessage("Pelea! Tenes 5 minutos para ganar!");
						player.stopAbnormalEffect(0x0200);
						player.setIsImmobilized(false);
						player.setInvul(false);
						player.isInSurvival = true;
						player.getStatus().setMaxCpHpMp();
					}
					StateEvent = 4;
				}
				break;
			case 4:
				if (state == State.FIGHT && event == Events.SURVIVAL && StateEvent == 4)
				{
					if (reqPlayers())
					{
						ThreadPool.schedule(new RevertTask(), 15000);
						return;
					}
					
					int alive = 0;
					for (Player player : players)
						if (player.isInSurvival)
							alive++;
					
					if (alive >= 2)
					{
						state = State.ENDING;
						announce("[Survival] No hubo ganador, no hay premio!");
						ThreadPool.schedule(new RevertTask(), 15000);
					}
				}
				break;
		}
	}
	
	public void setRandomFight(int _status)
	{
		if (TvTEvent.isInProgress() || event == Events.SURVIVAL || event == Events.DM)
		{
			ThreadPool.schedule(new RevertTask(), 15000);
			return;
		}
		switch (_status)
		{
			case 0:
				if (state == State.INACTIVE && event == Events.NULL && StateEvent == 0)
				{
					StateEvent = 1;
					event = Events.RF;
					state = State.REGISTER;
					for (Player player : World.getInstance().getPlayers())
						player.sendPacket(new ExShowScreenMessage("Evento Random Fight empezara en 1 minuto", 5000, SMPOS.TOP_CENTER, false));
					ThreadPool.schedule(new Msg("Para registrarte escribi .register", 5000), 5000);
					ThreadPool.schedule(new Msg("Para ver la pela escribi .ver", 5000), 10000);
					announce("Evento Random Fight empezara en 1 minuto");
					announce("Para registrarte, escribi .register");
					announce("Para mirar la pelea, escribi .ver");
				}
				break;
			case 1:
				if (state == State.REGISTER && event == Events.RF && StateEvent == 1)
				{
					state = State.LOADING;
					
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					
					announce("Cantidad de registrados: " + players.size());
					announce("2 personajes al azar seran elegidos en 10 segundos!");
					StateEvent = 2;
				}
				break;
			case 2:
				if (state == State.LOADING && event == Events.RF && StateEvent == 2)
				{
					try
					{
						if (reqPlayers())
						{
							announce("Random Fight no comenzara por que faltan participantes.");
							ThreadPool.schedule(new RevertTask(), 1000);
							return;
						}
						
						Vector<Player> newPlayers = new Vector<>();
						for (Player p : players)
							newPlayers.add(p);
						for (Player p : players)
							if (p.isInOlympiadMode() || p.isInObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
							{
								newPlayers.remove(p);
								p.sendMessage("no cumples los requisitos para participar en el evento.");
							}
						players = newPlayers;
						
						int rnd1 = Rnd.get(players.size());
						int rnd2 = Rnd.get(players.size());
						
						while (rnd2 == rnd1)
							rnd2 = Rnd.get(players.size());
						
						Vector<Player> players2 = new Vector<>();
						for (Player player : players)
							if (player != players.get(rnd1) && player != players.get(rnd2))
								players2.add(player);
						for (Player player : players2)
							if (players.contains(player))
								players.remove(player);
						
						announce("Personajes elegidos: " + players.firstElement().getName() + " || " + players.lastElement().getName());
						announce("Los personajes seran teleportados en 15 segundos.");
						StateEvent = 3;
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				break;
			case 3:
				if (state == State.LOADING && event == Events.RF && StateEvent == 3)
				{
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					
					Player player1 = players.firstElement();
					Player player2 = players.lastElement();
					
					setPcPrepare(player1);
					setPcPrepare(player2);
					
					// Arriba de GC
					player1.instantTeleportTo(179621, 54371, -3093, 0);
					player2.instantTeleportTo(178167, 54851, -3093, 0);
					player1.setTeam(TeamType.BLUE);
					player2.setTeam(TeamType.RED);
					
					state = State.FIGHT;
					StateEvent = 4;
				}
				break;
			case 4:
				if (state == State.FIGHT && event == Events.RF && StateEvent == 4)
				{
					if (reqPlayers())
					{
						announce("Uno de los personajes no esta Online, se cancela el evento.");
						ThreadPool.schedule(new RevertTask(), 15000);
						return;
					}

					for (Player player : players)
					{
						player.sendMessage("Pelea!");
						player.stopAbnormalEffect(0x0200);
						player.setIsImmobilized(false);
						player.setInvul(false);
						player.getStatus().setMaxCpHpMp();
					}
					StateEvent = 5;
				}
				break;
			case 5:
				if (state == State.FIGHT && event == Events.RF && StateEvent == 5)
				{
					if (reqPlayers())
					{
						ThreadPool.schedule(new RevertTask(), 15000);
						return;
					}
					
					int alive = 0;
					for (Player player : players)
						if (!player.isDead())
							alive++;
					
					if (alive == 2)
					{
						state = State.ENDING;
						_log.info("ENDING");
						announce("[RandomFight] Termino en empate!");
						ThreadPool.schedule(new RevertTask(), 15000);
					}
				}
				break;
		}
	}
	
	public static EventManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventManager _instance = new EventManager();
	}
	
	private void setPcPrepare(Player player)
	{
		player.setLastLocation(new Location(player.getX(), player.getY(), player.getZ()));
		player.atEvent = true;
		if (state == State.LOADING && event == Events.SURVIVAL && StateEvent == 2)
		{
			player.setInvul(true);
			player.instantTeleportTo(85574, 256964, -11674, Rnd.get(200, 1800));
		}
		player.stopAllEffectsExceptThoseThatLastThroughDeath();
		if (player.getSummon() != null)
			player.getSummon().stopAllEffectsExceptThoseThatLastThroughDeath();
		player.startAbnormalEffect(0x0200);
		player.setIsImmobilized(true);
		player.broadcastPacket(new StopMove(player));
		getBuffs(player);
		player.getStatus().setMaxCpHpMp();
		String message = "La pelea comenzara en 30 segundos!";
		player.sendMessage(message);
		player.sendPacket(new ExShowScreenMessage(message, 3500, SMPOS.MIDDLE_RIGHT, false));
	}
	
	public void setDM(int stage)
	{
		if (TvTEvent.isInProgress() || event == Events.RF || event == Events.SURVIVAL)
		{
			ThreadPool.schedule(new RevertTask(), 15000);
			return;
		}
		switch (stage)
		{
			case 0:
				if (state == State.INACTIVE && event == Events.NULL)
				{
					event = Events.DM;
					state = State.REGISTER;
					StateEvent = 1;
					for (Player player : World.getInstance().getPlayers())
						player.sendPacket(new ExShowScreenMessage("Evento Death Match empezara en 1 minuto", 5000, SMPOS.TOP_CENTER, false));
					ThreadPool.schedule(new Msg("Para registrarte escribi .register", 5000), 5000);
					ThreadPool.schedule(new Msg("Para ver la pela escribi .ver", 5000), 10000);
					announce("Evento Death Match empezara en 1 minuto");
					announce("Para registrarte, escribi .register");
					announce("Para mirar la pelea, escribi .ver");
				}
				break;
			case 1:
				if (state == State.REGISTER && event == Events.DM && StateEvent == 1)
				{
					state = State.LOADING;
					Vector<Player> newPlayers = new Vector<>();
					for (Player p : players)
					{
						newPlayers.add(p);
						if (p.isInOlympiadMode() || p.isInObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
						{
							newPlayers.remove(p);
							p.sendMessage("no cumples los requisitos para participar en el evento.");
						}
					}
					players = newPlayers;
					if (reqPlayers())
					{
						announce("Death Match no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					
					announce("Cantidad de registrados: " + players.size());
					announce("Los personajes seran teleportados en 15 segundos.");
					StateEvent = 2;
				}
				break;
			case 2:
				if (state == State.LOADING && event == Events.DM && StateEvent == 2)
				{
					if (reqPlayers())
					{
						announce("Death Match no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					
					for (Player player : players)
						setPcPrepare(player);
					StateEvent = 3;
				}
				break;
			case 3:
				if (state == State.LOADING && event == Events.DM && StateEvent == 3)
				{
					if (reqPlayers())
					{
						announce("Death Match no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					state = State.FIGHT;
					for (Player player : players)
					{
						player.sendMessage("Pelea! Tenes 5 minutos para matar!");
						player.stopAbnormalEffect(0x0200);
						player.setIsImmobilized(false);
						player.setInvul(false);
						player.isInSurvival = true;
						player.getStatus().setMaxCpHpMp();
					}
					StateEvent = 4;
				}
				break;
			case 4:
				if (state == State.FIGHT && event == Events.DM && StateEvent == 4)
				{
					if (reqPlayers())
					{
						ThreadPool.schedule(new RevertTask(), 15000);
						return;
					}
					if (topKills != 0)
					{
						String winners = "";
						for (Player winner : topPlayers)
							winners = winners + " " + winner.getName();
						
						state = State.ENDING;
						if (topPlayers.size() > 0)
						{
							for (Player topPlayer : topPlayers)
							{
						//		topPlayer.addItem("DM Event: " + _eventName, _rewardId, _rewardAmount, topPlayer, true);

								topPlayer.addAncientAdena("Survival", 100000, topPlayer, true);
								StatusUpdate su = new StatusUpdate(topPlayer);
								su.addAttribute(StatusType.CUR_LOAD, topPlayer.getCurrentWeight());
								topPlayer.sendPacket(su);
								topPlayer.sendPacket(ActionFailed.STATIC_PACKET);
							}
						}
						ThreadPool.schedule(new RevertTask(), 15000);
					}
					else 
					{
						state = State.ENDING;
						announce("Death Match No hubo ganador, no hay premio!");
						ThreadPool.schedule(new RevertTask(), 15000);
					}
				}
				break;
		}
	}
	
	@SuppressWarnings("unused")
	private void removeParty()
	{
		synchronized (players)
		{
			for (final Player player : players)
			{
				if (player.getParty() != null)
				{
					Party party = player.getParty();
					party.removePartyMember(player, null);
				}
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void winner()
	{
		synchronized (players)
		{
			for (Player player : players)
			{
				if (player.countDMkills > topKills)
				{
					topPlayers.clear();
					topPlayers.add(player);
					topKills = player.countDMkills;
				}
				else if (player.countDMkills == topKills)
					if (!topPlayers.contains(player))
						topPlayers.add(player);
			}
		}
	}
	
	/**
	 * Teleport the {@link Player} back to his initial {@link Location}.
	 * @param player : The Player to teleport back.
	 */
	protected static final void portPlayerBack(Player player)
	{
		if (player == null)
			return;
		
		// Retrieve the initial Location.
		Location loc = player.getSavedLocation();
		if (loc.equals(Location.DUMMY_LOC))
			return;
		
		final TownZone town = MapRegionData.getTown(loc.getX(), loc.getY(), loc.getZ());
		if (town != null)
			loc = town.getRndSpawn(SpawnType.NORMAL);
		
		player.teleportTo(loc, 0);
		player.getSavedLocation().clean();
	}
}