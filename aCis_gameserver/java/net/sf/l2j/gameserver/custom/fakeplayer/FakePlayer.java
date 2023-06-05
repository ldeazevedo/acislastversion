package net.sf.l2j.gameserver.custom.fakeplayer;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.player.Appearance;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;

public class FakePlayer extends Player {

	private FakePlayerAI _fakeAi;

	protected FakePlayer(PlayerTemplate template, String name, Appearance app) {
		super(IdFactory.getInstance().getNextId(), template, "acc-" + Rnd.get(5000), app);
		setName(name);
	}

	public FakePlayerAI getFakeAi() {
		return _fakeAi;
	}

	public void setFakeAi(FakePlayerAI _fakeAi) {
		this._fakeAi = _fakeAi;
	}

	@Override
	public void updateAbnormalEffect() {
	}

	@Override
	public ItemInstance getActiveWeaponInstance() {
		return null;
	}

	@Override
	public Weapon getActiveWeaponItem() {
		return null;
	}

	@Override
	public ItemInstance getSecondaryWeaponInstance() {
		return null;
	}

	@Override
	public Item getSecondaryWeaponItem() {
		return null;
	}

	@Override
	public int getWeightLimit() {
		return 0;
	}

	@Override
	public int getKarma() {
		return 0;
	}

	@Override
	public byte getPvpFlag() {
		return 0;
	}
}