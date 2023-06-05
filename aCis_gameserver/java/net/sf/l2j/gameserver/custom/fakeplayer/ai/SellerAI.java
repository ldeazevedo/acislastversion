package net.sf.l2j.gameserver.custom.fakeplayer.ai;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.custom.fakeplayer.FakeHelper;
import net.sf.l2j.gameserver.custom.fakeplayer.FakePlayer;

import java.util.List;

public class SellerAI extends FakeShopAI {

	public SellerAI(FakePlayer character, FakeHelper.City city) {
		super(character, city);
		ThreadPool.scheduleAtFixedRate(this::checkSellListAndSell, 15000, Rnd.get(1000 * 60 * 5, 1000 * 60 * 20));
	}

	private void checkSellListAndSell() {
		if (_fakePlayer.getSellList().getAvailableItems(_fakePlayer.getInventory()).size() == 0) {
			final List<ShopItem> shopItemList = getItemsForShop(FakeHelper.FakePlayerType.SELLER);
			_fakePlayer.standUp();

			ThreadPool.schedule(() -> {
				addItemsToInventory(shopItemList);
				addItemsToStore(shopItemList, FakeHelper.FakePlayerType.SELLER);
				//_fakePlayer.setOnline(false);
				_fakePlayer.getSellList().setTitle(sellerShopNames[Rnd.get(sellerShopNames.length)]);
				_fakePlayer.getSellList().setPackaged(false);
				_fakePlayer.tryOpenPrivateSellStore(false);
				_fakePlayer.sitDown();
				_fakePlayer.broadcastUserInfo();
			}, Rnd.get(20000, 60000));
		}
	}
}