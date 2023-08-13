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
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.events.tvt.TvTEvent;
import net.sf.l2j.gameserver.model.events.util.EventConstants;
import net.sf.l2j.gameserver.model.events.util.EventUtil;
import net.sf.l2j.gameserver.model.events.util.State;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;

import java.util.Arrays;

public class SurvivalEvenEngine extends AbstractEvent implements IEvent
{
	private final Location defaultLocation = new Location(82698, 148638, -3473);
	private static Npc npc;

	//Arriba de gc - 179621, 54371, -3093 - 178167, 54851, -3093
	private final Location coliseumLocation = new Location(148862, 46716, -3408); //Coliseo
	private final Location gardenOfEvaLocation = new Location(150053, 46730, -3408);

	private SurvivalEvenEngine()
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

			player.enterObserverMode(new Location(85574, 256964, -11674));
			return;
		}

		validateRegister(text, player);
	}

	private class RevertTask implements Runnable
	{

		public RevertTask()
		{
		}

		@Override
		public void run()
		{
			if (!registeredPlayers.isEmpty())
			{
				registeredPlayers.forEach(player -> {
					if (currentState == State.FIGHT || currentState == State.ENDING)
					{
						EventUtil.revertPlayer(player);
					}
					if (currentState == State.ENDING)
					{
						setPlayerReadyToFight(player, null);
					}
				});
			}
			clean();
		}
	}

	@Override
	public boolean onKill(Player player, Player killer)
	{
		if (isInProgress() && currentState == State.FIGHT && killer != null)
		{
			if (player != killer)
				setReward(killer, "Survival", 25000);

			player.sendPacket(new ExShowScreenMessage("Para regresar escribir .salir o esperar a que termine el evento", 5000, SMPOS.MIDDLE_RIGHT, false));
			player.setIsInEvent(false);

			var allDead = registeredPlayers.stream().filter(p -> p != player).anyMatch(p -> !p.isInEvent());
			if (allDead)
			{
				currentState = State.ENDING;
				killer.sendMessage("Sos el ganador!");
				World.announceToOnlinePlayers("Resultado Survival: " + killer.getName() + " es el ganador.");
				World.announceToOnlinePlayers("Evento finalizado");
				setReward(killer, "Survival", EventConstants.SURVIVAL_REWARD_AMOUNT);
				EventUtil.storeEventResults(killer);
				ThreadPool.schedule(new RevertTask(), 15000);
			}
			return true;
		}
		return false;
	}

	@Override
	public void onLogout(Player player)
	{
		if (player != null)
		{
			if (registeredPlayers.contains(player) || player.isInEvent())
				player.setXYZInvisible(player.getSavedLocation() != null ? player.getSavedLocation() : defaultLocation);

			registeredPlayers.remove(player);
			player.setIsInEvent(false);
		}
	}

	@Override
	public void clean()
	{
		if (currentState == State.FIGHT)
			registeredPlayers.forEach(p -> {
				p.setTeam(TeamType.NONE);
				p.setIsInEvent(false);
			});

		super.clean("EventsTask");
	}

	public void setNewState(State newState)
	{
		switch (currentState)
		{
			case INACTIVE:
				for (Player player : World.getInstance().getPlayers())
					player.sendPacket(new ExShowScreenMessage("Evento Survival empezara en 1 minuto", 5000, SMPOS.TOP_CENTER, false));
				ThreadPool.schedule(new EventManager.Msg("Para registrarte escribi .register", 5000), 5000);
				ThreadPool.schedule(new EventManager.Msg("Para ver la pela escribi .ver", 5000), 10000);
				World.announceToOnlinePlayers("Evento Survival empezara en 1 minuto");
				World.announceToOnlinePlayers("Para registrarte, escribi .register");
				World.announceToOnlinePlayers("Para mirar la pelea, escribi .ver");
				currentState = newState;
				break;
			case REGISTER:
				checkRequirements();
				if (areRequiredPlayersRegistered())
				{
					World.announceToOnlinePlayers("Survival no comenzara por que faltan participantes.");
					ThreadPool.schedule(new RevertTask(), 1000);
					return;
				}

				World.announceToOnlinePlayers("Cantidad de registrados: " + registeredPlayers.size());
				World.announceToOnlinePlayers("Los personajes seran teleportados en 15 segundos.");
				currentState = newState;
				break;
			case LOADING:
				if (areRequiredPlayersRegistered())
				{
					World.announceToOnlinePlayers("Survival no comenzara por que faltan participantes.");
					ThreadPool.schedule(new RevertTask(), 1000);
					return;
				}

				currentState = newState;
				break;
			case PREPARING:
				if (newState == State.PREPARING)
				{
					if (areRequiredPlayersRegistered())
					{
						World.announceToOnlinePlayers("Uno de los personajes no esta Online, se cancela el evento.");
						ThreadPool.schedule(new RevertTask(), 1000);
						return;
					}

					registeredPlayers.forEach(player -> {
						preparePlayer(player);
						player.teleportTo(gardenOfEvaLocation, Rnd.get(200, 1800));
					});
				} else if (newState == State.FIGHT)
				{
					registeredPlayers.forEach(player -> setPlayerReadyToFight(player, "Pelea!"));
					currentState = newState;
				}
				break;
			case FIGHT:
				var alive = registeredPlayers.stream().filter(p -> !p.isDead() && p.isInEvent()).count();

				if (alive >= 2)
				{
					currentState = newState;
					World.announceToOnlinePlayers("[Survival] No hubo ganador, no hay premio!");
					ThreadPool.schedule(new RevertTask(), 15000);
				}
				break;
		}
	}

	private void checkRequirements()
	{
		for (var p : registeredPlayers)
			if (p.isInOlympiadMode() || p.isInObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
			{
				registeredPlayers.remove(p);
				p.sendMessage("No cumples los requisitos para participar en el evento.");
			}
	}

	public static SurvivalEvenEngine getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final SurvivalEvenEngine _instance = new SurvivalEvenEngine();
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
	protected String getClassName()
	{
		return getClass().getName();
	}

	private void setReward(Player player, String eventName, int amount)
	{
		player.addItem(eventName, EventConstants.SURVIVAL_REWARD_ID, amount, player, true);
		player.addAncientAdena(eventName, 100000, player, true);
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusType.CUR_LOAD, player.getCurrentWeight());
		player.sendPacket(su);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void setPlayerReadyToFight(Player player, String message)
	{
		if (message != null)
			player.sendMessage(message);
		player.stopAbnormalEffect(0x0200);
		player.setIsImmobilized(false);
		player.setInvul(false);
		player.getStatus().setMaxCpHpMp();
	}
}