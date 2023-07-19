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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.scripting.Quest;

public class EventEngine extends Quest
{
	protected static final Logger _log = Logger.getLogger(EventEngine.class.getName());
	
	private State state = State.INACTIVE;
	
	private final Location loc1 = new Location(179621, 54371, -3093);
	private final Location loc2 = new Location(178167, 54851, -3093);

	public enum State
	{
		INACTIVE,		// 0
		REGISTER,		// 1
		LOADING,		// 2
		PREPARING,		// 3
		FIGHT,			// 4
		ENDING			// 5
	}
	
	protected EventEngine()
	{
		super(-1, "events");
	}
	
	public static void getBuffs(Player killer)
	{
		killer.getSkill(1204, 2); // Wind Walk
		if (killer.isMageClass())
			killer.getSkill(1085, 3);// Acumen
		else
			killer.getSkill(1086, 2); // haste
	}
	
	private static class DataBaseQuery
	{
		public static final String QUERY_EVENT_INFO = "select * from rf where char_name=?";
		public static final String UPDATE_EVENT_INFO = "update rf set count=count+1 where char_name=?";
		public static final String INSERT_EVENT_INFO = "insert rf set count=1,char_name=?";
	}
	
	public boolean isInEvent(Player pc)
	{
		return state == State.FIGHT && TeamsPlayers.containsPlayer(pc);
	}
	
	public static void announce(String msg)
	{
		World.announceToOnlinePlayers(msg);
	}
	
	public boolean isInProgress()
	{
		return state != State.INACTIVE;
	}
	
	public void checkEvents(String text, Player player)
	{
		if (/*!isInProgress() || */
			player == null || 
			player.isInObserverMode() || 
			player.isInOlympiadMode() || 
			player.isFestivalParticipant() || 
			player.isInJail() || 
			player.isCursedWeaponEquipped() || 
			player.getKarma() > 0 || 
			/*player.isInSameActiveSiegeSide(player) || */
			TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(player.getObjectId()))
		{
			if (player != null)
			{
				if (player.isInObserverMode())
					player.sendMessage("isInObserverMode()");
				if (player.isInOlympiadMode())
					player.sendMessage("isInOlympiadMode()");
				if (player.isFestivalParticipant())
					player.sendMessage("isFestivalParticipant()");
				if (player.isInJail() )
					player.sendMessage("isInJail()");
				if (player.isCursedWeaponEquipped())
					player.sendMessage("isCursedWeaponEquipped()");
				if (player.getKarma() > 0)
					player.sendMessage("getKarma() > 0");
				player.sendMessage("You do not meet the conditions to participate.");
			}
			return;
		}
		if (isInProgress())
		{
			player.sendMessage("isInProgress()");
			if (OlympiadManager.getInstance().isRegistered(player))
			{
				player.sendMessage("No puedes participar ni ver el evento mientras estas registrado en oly.");
				return;
			}
			if (text.equalsIgnoreCase(".salir") && isInProgress())
			{
				if (!TeamsPlayers.containsPlayer(player) || state != State.FIGHT)
					return;
				if (player.isDead())
				{
					TeamsPlayers.removePlayer(player);
					revertPlayer(player);
				}
				return;
			}
			if (text.equalsIgnoreCase(".ver"))
			{
				if (TeamsPlayers.containsPlayer(player) || player.isInObserverMode())
					return;
				
				player.enterObserverMode(new Location(179747, 54696, -2805));
				return;
			}
			if (text.equalsIgnoreCase(".reg1"))
			{
				if (player.isDead())
					return;
				if (player.isInObserverMode())
				{
					player.sendMessage("No te podes anotar si estas mirando el evento.");
					return;
				}
				if (TeamsPlayers.containsPlayer(player))
				{
					player.sendMessage("Ya estas registrado en el evento.");
					return;
				}
				if (state != State.REGISTER)
				{
					player.sendMessage("El evento ya comenzo.");
					return;
				}
				TeamsPlayers.addPlayer(player);
				player.sendMessage("Te registraste al evento!");
				return;
			}
			if (text.equalsIgnoreCase(".unreg1"))
			{
				if (!TeamsPlayers.containsPlayer(player))
				{
					player.sendMessage("No te registraste al evento.");
					return;
				}
				if (state != State.REGISTER)
				{
					player.sendMessage("El evento ya comenzo.");
					return;
				}
				TeamsPlayers.removePlayer(player);
				player.sendMessage("Saliste del evento.");
		}
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
		player.getStatus().setMaxCpHpMp();
		player.broadcastUserInfo();
		
		if (player.getSavedLocation() != null)
			player.teleportTo(player.getSavedLocation(), 0);
		else
			player.teleportTo(82698, 148638, -3473, 0);
		
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
			if (!getPlayers().isEmpty())
				for (Player p : getPlayers())
				{
					if (p == null)
						continue;
					
					if (state == State.FIGHT || state == State.ENDING)
					{
						revertPlayer(p);
						for (Player player : getPlayers())
							setPlayerStats(player, null);
					}
				}
			clean();
		}
	}
	
	public boolean onKill(Player killer)
	{
	//	boolean isInEvent = false;
		if (isInProgress() && state == State.FIGHT)
		{
			if (state != State.ENDING)
			{
				state = State.ENDING;
				if (killer != null)
				{
					killer.sendMessage("Sos el ganador!");
					announce("Resultado Random Fight: " + killer.getName() + " es el ganador.");
					announce("Evento finalizado");
					// pk.addItem("", Config.RANDOM_FIGHT_REWARD_ID, Config.RANDOM_FIGHT_REWARD_COUNT, null, true);
					
					// Guardar en la base de datos
					try (Connection con = ConnectionPool.getConnection();
						 PreparedStatement statement = con.prepareStatement(DataBaseQuery.QUERY_EVENT_INFO))
					{
						statement.setString(1, killer.getName());
						@SuppressWarnings("resource")
						boolean existsRow = statement.executeQuery().first();
						String sql = existsRow ? DataBaseQuery.UPDATE_EVENT_INFO : DataBaseQuery.INSERT_EVENT_INFO;
						try (PreparedStatement statement2 = con.prepareStatement(sql))
						{
							statement2.setString(1, killer.getName());
							statement2.execute();
						}
					}
					catch (Exception e)
					{
						_log.warning("Error en RF Ranking: " + e);
					}
				}
				ThreadPool.schedule(new RevertTask(), 15000);
				
				return true;
			}
		}
		return false;
	}
	
	
	public void onLogout(Player pc)
	{
		Player pk = null;
		int alive = 0;
		if (pc != null)
		{
			if (TeamsPlayers.containsPlayer(pc) || pc.atEvent || pc.isInSurvival)
			{
				Location loc = pc.getSavedLocation();
				if (loc != null)
					pc.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
				else
					pc.setXYZInvisible(82698, 148638, -3473);
			}
			if (TeamsPlayers.containsPlayer(pc))
				TeamsPlayers.removePlayer(pc);
			
			pc.atEvent = false;
			pc.isInSurvival = false;
		}
		synchronized (getPlayers()) // cuando un player se desconecta se redirecciona a onkill si solo queda un pj vivo en el evento
		{
			for (Player player : getPlayers())
				if (!player.isDead() || player.isInSurvival)
				{
					alive++;
					pk = player;
				}
			
			if (alive == 1)
				onKill(pk);
		}
	}
	
	public boolean reqPlayers()
	{
		return getPlayers().isEmpty() || getPlayers().size() < 2;
	}
	
	public void clean()
	{
		if (state == State.FIGHT)
			for (Player p : getPlayers())
				p.setTeam(TeamType.NONE);
			
		for (Player pc : World.getInstance().getPlayers())
		{
			pc.isInSurvival = false;
			pc.atEvent = false;
		}
		
		getPlayers().clear();
		state = State.INACTIVE;
		ScriptData.getInstance().getQuest("EventsEngineTask").startQuestTimer("cancelQuestTimers", null, null, 1000);
	}
	
	public void clear()
	{
		clean();
		ScriptData.getInstance().getQuest("EventsEngineTask").startQuestTimer("clear", null, null, 1000);
	}
	
	
	
	
	
	public void setRandomFight(int st)
	{
		if (TvTEvent.isInProgress()/* || event == Events.SURVIVAL || event == Events.DM*/)
		{
			ThreadPool.schedule(new RevertTask(), 15000);
			return;
		}
		
		switch (st)
		{
			case 0:
				announce("REGISTER");
				state = State.REGISTER;
				announce("set REGISTER.");
				break;
			case 1:
				if (state == State.REGISTER)
				{
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					
					announce("Cantidad de registrados: " + getPlayers().size());
					announce("2 personajes al azar seran elegidos en 10 segundos!");
					announce("set LOADING.");
					state = State.LOADING;
				}
				break;
			case 2:
				if (state == State.LOADING)
				{
					try
					{
						if (reqPlayers())
						{
							announce("Random Fight no comenzara por que faltan participantes.");
							ThreadPool.schedule(new RevertTask(), 1000);
							return;
						}
					
						checkRequirements();
					
						int rnd1 = Rnd.get(getPlayers().size());
						int rnd2 = Rnd.get(getPlayers().size());
					
						while (rnd2 == rnd1)
							rnd2 = Rnd.get(getPlayers().size());
					
						int finalRnd = rnd2;
						getPlayers().stream().filter(p -> p.getName().equalsIgnoreCase(getPlayers().get(rnd1).getName()) || p.getName().equalsIgnoreCase(getPlayers().get(finalRnd).getName())).collect(Collectors.toList());
					
						announce("Personajes elegidos: " + getPlayers().get(0).getName() + " || " + getPlayers().get(getPlayers().size() - 1).getName());
						announce("Los personajes seran teleportados en 15 segundos.");
						announce("set PREPARING.");
						state = State.PREPARING;
					}
					catch (Exception ex)
					{
						_log.log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
				break;
			case 3:
				if (state == State.PREPARING)
				{
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}
					
					Player player1 = getPlayers().get(0);
					Player player2 = getPlayers().get(getPlayers().size() - 1);
					
					setPcPrepare(player1);
					setPcPrepare(player2);
					
					// Arriba de GC
					player1.teleportTo(loc1, 0);
					player2.teleportTo(loc2, 0);
					player1.setTeam(TeamType.BLUE);
					player2.setTeam(TeamType.RED);
					announce("get PREPARING.");
				}
				break;
			case 4:
				if (state == State.PREPARING)
				{
					if (reqPlayers())
					{
						announce("Uno de los personajes no esta Online, se cancela el evento.");
						ThreadPool.schedule(new RevertTask(), 15000);
						return;
					}
					for (Player player : getPlayers())
						setPlayerStats(player, "Pelea!");
					state = State.FIGHT;
					announce("State.PREPARING.");
				}
				break;
			case 5:
				if (state == State.FIGHT)
				{
					if (reqPlayers())
					{
						ThreadPool.schedule(new RevertTask(), 15000);
						return;
					}
					int alive = 0;
					for (Player player : getPlayers())
						if (!player.isDead())
							alive++;

					state = State.ENDING;
					if (alive == 2)
					{
						_log.info("ENDING");
						announce("[RandomFight] Termino en empate!");
						ThreadPool.schedule(new RevertTask(), 15000);
					}
					announce("set ENDING.");
				}
				break;
		}
	}
	
	private static void checkRequirements()
	{
		for (Player p : getPlayers())
			if (p.isInOlympiadMode() || p.isInObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
			{
				getPlayers().remove(p);
				p.sendMessage("No cumples los requisitos para participar en el evento.");
			}
	}
	
	public static EventEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventEngine _instance = new EventEngine();
	}
	
	private static void setPcPrepare(Player player)
	{
		player.setLastLocation(new Location(player.getX(), player.getY(), player.getZ()));
		player.atEvent = true;
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
	
	private static void setPlayerStats(Player player, String message)
	{
		if (message != null)
			player.sendMessage(message);
		player.stopAbnormalEffect(0x0200);
		player.setIsImmobilized(false);
		player.setInvul(false);
		player.getStatus().setMaxCpHpMp();
	}
	
	private static List<Player> getPlayers()
	{
		return TeamsPlayers.registersPlayers();
	}
}