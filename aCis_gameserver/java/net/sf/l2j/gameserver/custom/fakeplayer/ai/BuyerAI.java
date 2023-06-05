package net.sf.l2j.gameserver.custom.fakeplayer.ai;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.custom.fakeplayer.FakeHelper;
import net.sf.l2j.gameserver.custom.fakeplayer.FakePlayer;

import java.util.List;

public class BuyerAI extends FakeShopAI {

	public BuyerAI(FakePlayer fakePlayer, FakeHelper.City city) {
		super(fakePlayer, city);
		ThreadPool.scheduleAtFixedRate(this::checkSellListAndBuy, 15000, Rnd.get(1000 * 60 * 5, 1000 * 60 * 20));
	}

	private synchronized void checkSellListAndBuy() {
		if (_fakePlayer.getSellList().getAvailableItems(_fakePlayer.getInventory()).size() == 0) {
			if (_fakePlayer.getAdena() < 20000) {
				_fakePlayer.addItem("adenaForBuy", 57, 20000000, null, false);
			}
			_fakePlayer.standUp();

			ThreadPool.schedule(() -> {
				List<ShopItem> shopItemList = getItemsForShop(FakeHelper.FakePlayerType.BUYER);
				addItemsToInventory(shopItemList);
				addItemsToStore(shopItemList, FakeHelper.FakePlayerType.BUYER);
				//_fakePlayer.setOnline(false);
				_fakePlayer.getBuyList().setTitle(buyerShopNames[Rnd.get(buyerShopNames.length)]);
				_fakePlayer.getBuyList().setPackaged(false);
				_fakePlayer.tryOpenPrivateBuyStore();
				_fakePlayer.sitDown();
				_fakePlayer.broadcastUserInfo();
			}, Rnd.get(20000, 60000));
		}
	}
}