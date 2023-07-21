package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Player;

public class PlayerTuple {

	private final Player fighterOne;
	private final Player fighterTwo;
	private final Integer instanceId;

	public PlayerTuple(Player fighterOne, Player fighterTwo) {
		this.fighterOne = fighterOne;
		this.fighterTwo = fighterTwo;
		this.instanceId = IdFactory.getInstance().getNextId();
	}

	public Player getFighterOne() {
		return fighterOne;
	}

	public Player getFighterTwo() {
		return fighterTwo;
	}

	public Integer getInstanceId() {
		return instanceId;
	}
}