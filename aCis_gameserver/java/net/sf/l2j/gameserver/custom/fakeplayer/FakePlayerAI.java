package net.sf.l2j.gameserver.custom.fakeplayer;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.network.serverpackets.TeleportToLocation;

public abstract class FakePlayerAI extends CreatureAI<FakePlayer> {

	protected FakePlayer _fakePlayer;

	public FakePlayerAI(FakePlayer creature) {
		super(creature);
		_fakePlayer = creature;
		setup();
	}

	public void setup() {
		_fakePlayer.setRunning(true);
	}

	protected void teleportToLocation(int x, int y, int z, int randomOffset) {
		_fakePlayer.setTeleporting(true);
		_fakePlayer.getAI().tryToActive();
		if (randomOffset > 0) {
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		z += 5;
		_fakePlayer.broadcastPacket(new TeleportToLocation(_fakePlayer, x, y, z, false));
		_fakePlayer.decayMe();
		_fakePlayer.setXYZ(x, y, z);
		_fakePlayer.onTeleported();
		_fakePlayer.revalidateZone(true);
	}
}