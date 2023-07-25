package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.model.actor.Player;

import java.util.ArrayList;
import java.util.List;

public class RegisterEngine {

	private final List<Player> registeredPlayers = new ArrayList<>();

	public void processCommand(String text, Player player) {
		if (text.equalsIgnoreCase(EventConstants.REGISTER) && !registeredPlayers.contains(player)) {
			registeredPlayers.add(player);
		} else if (text.equalsIgnoreCase(EventConstants.UNREGISTER)) {
			registeredPlayers.remove(player);
		}
	}
}