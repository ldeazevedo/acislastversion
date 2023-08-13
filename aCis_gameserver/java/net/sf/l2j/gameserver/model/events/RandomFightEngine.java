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

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.events.tvt.TvTEvent;
import net.sf.l2j.gameserver.model.events.util.EventConstants;
import net.sf.l2j.gameserver.model.events.util.EventUtil;
import net.sf.l2j.gameserver.model.events.util.State;
import net.sf.l2j.gameserver.model.events.util.Tuple;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ConfirmDlg;

import java.util.*;
import java.util.stream.Collectors;

public class RandomFightEngine extends AbstractEvent implements IEvent
{
	private final List<Tuple<Player, Player>> tupleList = Collections.synchronizedList(new ArrayList<>());

	//Arriba de gc - 179621, 54371, -3093 - 178167, 54851, -3093
	private final Location playerOneLocation = new Location(148862, 46716, -3408); //Coliseo
	private final Location playerTwoLocation = new Location(150053, 46730, -3408);

	private RandomFightEngine()
	{
	}

	@Override
	public void processCommand(String text, Player player)
	{
		if (isCommandInvalid(text, player))
			return;

		if (text.equalsIgnoreCase(EventConstants.WATCH))
		{
			if (registeredPlayers.contains(player) || player.isInObserverMode())
				return;

			String html = EventUtil.generateHtmlForInstances(tupleList);
			EventUtil.sendHtmlMessage(player, html, false);
			return;
		}

		validateRegister(text, player);
	}

	public void revertPlayers(Player... players)
	{
		Arrays.stream(players).forEach(EventUtil::revertPlayer);
	}

	private class RevertTask implements Runnable
	{
		private Player killer = null;

		RevertTask(Player killer)
		{
			this.killer = killer;
		}

		public RevertTask()
		{
		}

		@Override
		public void run()
		{
			if (killer == null)
			{
				log.info("Running RevertTask for every tuple because there's no winner.");
				tupleList.forEach(t -> {
					log.info("Doing " + t.getInstanceId() + " ...");
					revertPlayers(t.left(), t.right());
					setPlayersStats(null, t.left(), t.right());
				});
				clean();
			} else
			{
				var pt = tupleList.stream().filter(p -> p.getInstanceId() == killer.getInstanceId()).findFirst();
				log.info("RevertTask::PlayerTuple found? " + pt.isPresent());
				if (pt.isPresent() && (currentState == State.FIGHT || currentState == State.ENDING))
				{
					revertPlayers(pt.get().left(), pt.get().right());
					setPlayersStats(null, pt.get().right(), pt.get().left());
				}
			}
		}
	}

	@Override
	public void onLogout(Player pc)
	{
		var playerTuple = tupleList.stream().filter(t -> t.left().equals(pc) || t.right().equals(pc)).findFirst();
		if (playerTuple.isPresent())
		{
			pc.setXYZInvisible(pc.getSavedLocation() != null ? pc.getSavedLocation() : EventConstants.DEFAULT_LOCATION);
			registeredPlayers.remove(pc);
			var killer = playerTuple.get().left().equals(pc) ? playerTuple.get().right() : playerTuple.get().left();
			onKill(null, killer);
		}
	}

	@Override
	public void clean()
	{
		if (currentState == State.FIGHT)
			registeredPlayers.forEach(p -> p.setTeam(TeamType.NONE));

		tupleList.forEach(t -> {
			t.left().setIsInEvent(false);
			t.right().setIsInEvent(false);
		});

		tupleList.clear();
		super.clean("EventsEngineTask");
	}

	public void setNewState(State newState)
	{
		switch (currentState)
		{
			case INACTIVE:
				currentState = newState;
				World.announceToOnlinePlayers("Open registration to participate in the Arena Fight!");
				break;
			case REGISTER:
				if (!areRequiredPlayersRegistered())
				{
					ThreadPool.schedule(new RevertTask(), 1000);
					return;
				}

				announceNpc("Cantidad de registrados: " + registeredPlayers.size());
				announceNpc("Los participantes seran seleccionados en 10 segundos!");
				currentState = newState;
				break;
			case LOADING:
				if (!areRequiredPlayersRegistered())
				{
					ThreadPool.schedule(new RevertTask(), 1000);
					return;
				}

				checkRequirements();

				var sortedPlayers = registeredPlayers.stream().sorted(Comparator.comparingInt(p -> p.getStatus().getLevel())).collect(Collectors.toList());

				if (sortedPlayers.size() % 2 == 1)
				{
					var removedPlayer = sortedPlayers.remove(sortedPlayers.size() - 1);
					removedPlayer.sendMessage("Lo sentimos, el evento no puede comenzar cuando el numero de participantes es impar. Fuiste quitado del evento al azar.");
				}

				for (int i = 0; i < sortedPlayers.size(); i += 2)
					tupleList.add(new Tuple<>(sortedPlayers.get(i), sortedPlayers.get(i + 1)));

				announceNpc("Los personajes seran teleportados en 15 segundos.");
				currentState = newState;
				break;
			case PREPARING:
				if (!areRequiredPlayersRegistered())
				{
					ThreadPool.schedule(new RevertTask(), 1000);
					return;
				}

				if (newState == State.PREPARING)
				{
					tupleList.forEach(t -> {
						preparePlayer(t.getInstanceId(), t.left(), t.right());

						t.left().teleportTo(playerOneLocation, 0);
						t.right().teleportTo(playerTwoLocation, 0);
						t.left().setTeam(TeamType.BLUE);
						t.right().setTeam(TeamType.RED);
					});

				} else if (newState == State.FIGHT)
				{
					tupleList.forEach(t -> setPlayersStats("Pelea!", t.right(), t.left()));
					currentState = newState;
				}
				break;
			case FIGHT:
				if (newState != State.ENDING)
					return;

				currentState = newState;
				tupleList.forEach(t -> {
					if (!t.left().isDead() && !t.right().isDead())
						announceToPlayer("Finalizado en empate!", false, t.right(), t.left());
				});
				ThreadPool.schedule(new RevertTask(), 15000);
		}
	}

	private void checkRequirements()
	{
		registeredPlayers.stream()
				.filter(p -> p.isInOlympiadMode() || p.isInObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || net.sf.l2j.gameserver.model.events.tvt.TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
				.forEach(p -> {
					registeredPlayers.remove(p);
					p.sendMessage("No cumples los requisitos para participar en el evento.");
				});
	}

	public static RandomFightEngine getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final RandomFightEngine _instance = new RandomFightEngine();
	}

	private void preparePlayer(Integer instanceId, Player... players)
	{
		Arrays.stream(players).forEach(player -> {
			player.setInstanceId(instanceId);
			preparePlayer(player);
		});
	}

	private void setPlayersStats(String message, Player... players)
	{
		Arrays.stream(players).forEach(player -> {
			announceToPlayer(message, false, player);
			player.stopAbnormalEffect(0x0200);
			player.setIsImmobilized(false);
			player.setInvul(false);
			player.getStatus().setMaxCpHpMp();
		});
	}

	public void askJoinTeam(Player leader, Player target)
	{
		ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1.getId());
		confirm.addString("Do you wish to join " + leader.getName() + "'s Tournament Team?");
		confirm.addTime(30000);
		//	target.setTournamentTeamRequesterId(leader.getObjectId());
		//target.setTournamentTeamBeingInvited(true);
		target.sendPacket(confirm);
		leader.sendMessage(target.getName() + " was invited to your team.");

	}

	public void askTeleport(Player player)
	{
		ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1.getId());
		confirm.addString("Do you wish to teleport to Tournament Zone?");
		confirm.addTime(30000);
		//	setTournamentTeleporting(true);
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				//setTournamentTeleporting(false);
			}
		}, 30000);
		player.sendPacket(confirm);
	}

	@Override
	public boolean onKill(Player player, Player killer)
	{
		// boolean isInEvent = false;
		if (isInProgress() && currentState == State.FIGHT && killer != null)
		{
			var optionalTuple = tupleList.stream().filter(tp -> tp.left().equals(killer) || tp.right().equals(killer)).findFirst();
			optionalTuple.ifPresent(playerPlayerTuple -> log.info("RandomFight " + playerPlayerTuple.getInstanceId() + " finished! " + killer.getName() + " is the winner!"));
			killer.sendMessage("Sos el ganador!");
			announceToPlayer("Resultado Arena Fight: " + killer.getName() + " es el ganador.", false, killer);
			announceToPlayer("Pelea finalizada", true, killer); //TODO: trendia que ser para los 2, si estan online
			// pk.addItem("", Config.RANDOM_FIGHT_REWARD_ID, Config.RANDOM_FIGHT_REWARD_COUNT, null, true);

			EventUtil.storeEventResults(killer);

			ThreadPool.schedule(new RevertTask(killer), 15000);
			return true;
		}
		return false;
	}

	@Override
	protected String getClassName()
	{
		return getClass().getName();
	}
}