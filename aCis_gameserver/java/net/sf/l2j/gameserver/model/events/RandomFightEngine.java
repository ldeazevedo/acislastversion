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

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RandomFightEngine
{
	protected static final Logger log = Logger.getLogger(RandomFightEngine.class.getName());
	private final List<Player> registeredPlayers = new ArrayList<>();

	private State currentState = State.INACTIVE;

	private final List<Tuple<Player, Player>> tuple = Collections.synchronizedList(new ArrayList<>());

	private final Location loc1 = new Location(179621, 54371, -3093);
	private final Location loc2 = new Location(178167, 54851, -3093);

	protected RandomFightEngine()
	{
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

	public boolean isInEvent(Player player)
	{
		return currentState == State.FIGHT && registeredPlayers.contains(player);
	}

	public static void announce(String msg)
	{
		World.announceToOnlinePlayers(msg);
	}

	public boolean isInProgress()
	{
		return currentState != State.INACTIVE;
	}

	public void processCommand(String text, Player player)
	{
		log.info("inside processCommand(): text = " + text);
		if (isInProgress())
		{
			log.info("Event is in progress");
			if (player.isInObserverMode() || player.isInOlympiadMode() || player.isFestivalParticipant() || player.isInJail() || player.isCursedWeaponEquipped() || player.getKarma() > 0 || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(player.getObjectId()))
			{
				player.sendMessage("You do not meet the conditions to participate.");
				return;
			}
			if (OlympiadManager.getInstance().isRegistered(player))
			{
				player.sendMessage("No puedes participar ni ver el evento mientras estas registrado en oly.");
				return;
			}
			var isPlayerRegistered = registeredPlayers.contains(player);

			if (text.equalsIgnoreCase(EventConstants.EXIT))
			{
				if (!isPlayerRegistered || currentState != State.FIGHT)
					return;
				if (player.isDead())
				{
					registeredPlayers.remove(player);
					revertPlayer(player);
				}
				return;
			}
			if (text.equalsIgnoreCase(EventConstants.WATCH))
			{
				//TODO: mostrar un html para elegir que pelea ver
				if (isPlayerRegistered || player.isInObserverMode())
					return;

				player.enterObserverMode(new Location(179747, 54696, -2805));
				return;
			}
			if (text.equalsIgnoreCase(EventConstants.REGISTER))
			{
				if (player.isDead())
					return;
				if (player.isInObserverMode())
				{
					player.sendMessage("No te podes anotar si estas mirando el evento.");
					return;
				}
				if (registeredPlayers.contains(player))
				{
					player.sendMessage("Ya estas registrado en el evento.");
					return;
				}
				if (currentState != State.REGISTER)
				{
					player.sendMessage("El evento ya comenzo.");
					return;
				}
				registeredPlayers.add(player);
				player.sendMessage("Te registraste al evento!");
				return;
			}
			if (text.equalsIgnoreCase(EventConstants.UNREGISTER))
			{
				if (!isPlayerRegistered)
				{
					player.sendMessage("No te registraste al evento.");
					return;
				}
				if (currentState != State.REGISTER)
				{
					player.sendMessage("El evento ya comenzo.");
					return;
				}
				registeredPlayers.remove(player);
				player.sendMessage("Saliste del evento.");
			}
		} else
			log.info("The event is inactive");
	}

	public void revertPlayers(Player... players)
	{
		Arrays.stream(players).forEach(this::revertPlayer);
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
		private Player killer = null;

		RevertTask(Player killer) {
			this.killer = killer;
		}

		public RevertTask() {
		}

		@Override
		public void run()
		{
			if (killer == null) {
				tuple.forEach(t -> {
					revertPlayers(t.left(), t.right());
					setPlayersStats(null, t.left(), t.right());
				});
			} else {
				Optional<Tuple<Player, Player>> pt = tuple.stream().filter(p -> p.getInstanceId() == killer.getInstanceId()).findFirst();
				log.info("RevertTask::PlayerTuple found? " + pt.isPresent());
				if (pt.isPresent() && (currentState == State.FIGHT || currentState == State.ENDING)) {
					revertPlayers(pt.get().left(), pt.get().right());
					setPlayersStats(null, pt.get().right(), pt.get().left());
				}
			}
			clean();

			/*
			 * if (!getPlayers().isEmpty()) for (Player p : getPlayers()) { if (p == null) continue; if (currentState == State.FIGHT || currentState == State.ENDING) { revertPlayer(p); for (Player player : getPlayers()) setPlayerStats(player, null); } }
			 */
		}
	}

	public boolean onKill(Player killer)
	{
		// boolean isInEvent = false;
		if (isInProgress() && currentState == State.FIGHT)
		{
			currentState = State.ENDING;
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
					log.warning("Error en RF Ranking: " + e);
				}
			}
			ThreadPool.schedule(new RevertTask(killer), 15000);
			return true;
		}
		return false;
	}

	public void onLogout(Player pc)
	{
		Player pk = null;
		int alive = 0;
		if (pc != null)
		{
			var isPlayerRegistered = registeredPlayers.contains(pc);
			if (isPlayerRegistered || pc.atEvent || pc.isInSurvival)
			{
				Location loc = pc.getSavedLocation();
				if (loc != null)
					pc.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
				else
					pc.setXYZInvisible(82698, 148638, -3473);
			}
			if (isPlayerRegistered)
				registeredPlayers.remove(pc);

			pc.atEvent = false;
			pc.isInSurvival = false;
		}
		for (Player player : registeredPlayers)
			if (!player.isDead() || player.isInSurvival)
			{
				alive++;
				pk = player;
			}

		if (alive == 1)
			onKill(pk);
	}

	public boolean reqPlayers()
	{
		return registeredPlayers.isEmpty() || registeredPlayers.size() < 2;
	}

	public void clean()
	{
		if (currentState == State.FIGHT)
			registeredPlayers.forEach(p -> p.setTeam(TeamType.NONE));

		//TODO: por que a todos los personajes que estan en el servidor?
		for (Player pc : World.getInstance().getPlayers())
		{
			pc.isInSurvival = false;
			pc.atEvent = false;
		}

		registeredPlayers.clear();
		tuple.clear();
		currentState = State.INACTIVE;
		ScriptData.getInstance().getQuest("EventsEngineTask").startQuestTimer("cancelQuestTimers", null, null, 1000);
	}

	public void setRandomFight(State newState)
	{
		if (TvTEvent.isInProgress()/* || event == Events.SURVIVAL || event == Events.DM */)
		{
			announce("TvTEvent.isInProgress()");
			ThreadPool.schedule(new RevertTask(), 15000);
			return;
		}

		log.info("State: " + currentState);
		log.info("New state: " + newState);

		switch (currentState)
		{
			case INACTIVE:
				this.currentState = newState;
				announce("State." + currentState);
				break;
			case REGISTER:
				if (this.currentState == State.REGISTER)
				{
					log.info("New state: " + currentState);
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}

					announce("Cantidad de registrados: " + registeredPlayers.size());
					announce("2 personajes al azar seran elegidos en 10 segundos!");
					this.currentState = newState;
					announce("State.LOADING");
				}
				break;
			case LOADING:
				try
				{
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}

					checkRequirements();

					var sortedPlayers = registeredPlayers.stream().sorted(Comparator.comparingInt(p -> p.getStatus().getLevel())).collect(Collectors.toList());

					if (sortedPlayers.size() % 2 == 1)
						sortedPlayers.remove(sortedPlayers.size() - 1);

					for (int i = 0; i < sortedPlayers.size(); i += 2)
						tuple.add(new Tuple<>(sortedPlayers.get(i), sortedPlayers.get(i + 1)));

					/*
					 * int rnd1 = Rnd.get(getPlayers().size()); int rnd2 = Rnd.get(getPlayers().size()); while (rnd2 == rnd1) rnd2 = Rnd.get(getPlayers().size()); announce("Personajes elegidos: " + getPlayers().get(0).getName() + " || " + getPlayers().get(getPlayers().size() - 1).getName());
					 */
					announce("Los personajes seran teleportados en 15 segundos.");
					this.currentState = newState;
					announce("State.PREPARING;");
				}
				catch (Exception ex)
				{
					log.log(Level.SEVERE, ex.getMessage(), ex);
				}
				break;
			case PREPARING:
				if (newState == State.PREPARING)
				{
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}

					tuple.forEach(tuple ->
					{
						preparePlayer(tuple.getInstanceId(), tuple.left(), tuple.right());

						tuple.left().teleportTo(loc1, 0);
						tuple.right().teleportTo(loc2, 0);
						tuple.left().setTeam(TeamType.BLUE);
						tuple.right().setTeam(TeamType.RED);
					});
					announce("State.PREPARING stage2");

					/*
					 * Player player1 = getPlayers().get(0); Player player2 = getPlayers().get(getPlayers().size() - 1); preparePlayer(player1); preparePlayer(player2); // Arriba de GC player1.teleportTo(loc1, 0); player2.teleportTo(loc2, 0); player1.setTeam(TeamType.BLUE);
					 * player2.setTeam(TeamType.RED); announce("get PREPARING.");
					 */
				}
				else if (newState == State.FIGHT)
				{
					if (reqPlayers())
					{
						announce("Uno de los personajes no esta Online, se cancela el evento.");
						ThreadPool.schedule(new RevertTask(), 15000);
						return;
					}

					tuple.forEach(tuple ->
					{
						setPlayersStats("Pelea!", tuple.right(), tuple.left());
					});

					this.currentState = newState;
					announce("State.FIGHT");
				}
				break;
			case FIGHT:
				if (newState == State.ENDING)
				{
					tuple.forEach(tuple ->
					{
						this.currentState = newState;
						announce("State.ENDING");
						if (!tuple.left().isDead() && !tuple.right().isDead())
						{
							log.info("ENDING RF[" + tuple.getInstanceId() + "]");
							announce("[RandomFight] Termino en empate!");
							ThreadPool.schedule(new RevertTask(), 15000);
						}
					});
				}
				break;
		}
	}

	private void checkRequirements()
	{
		for (Player p : registeredPlayers)
			if (p.isInOlympiadMode() || p.isInObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
			{
				registeredPlayers.remove(p);
				p.sendMessage("No cumples los requisitos para participar en el evento.");
			}
	}

	public static RandomFightEngine getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final RandomFightEngine _instance = new RandomFightEngine();
	}

	private static void preparePlayer(Player player)
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

	private static void preparePlayer(Integer instanceId, Player... players)
	{
		Arrays.stream(players).forEach(player -> {
			player.setInstanceId(instanceId);
			preparePlayer(player);
		});
	}

	private static void setPlayersStats(String message, Player... players)
	{
		Arrays.stream(players).forEach(player -> {
			if (message != null)
				player.sendMessage(message);
			player.stopAbnormalEffect(0x0200);
			player.setIsImmobilized(false);
			player.setInvul(false);
			player.getStatus().setMaxCpHpMp();
		});
	}
}