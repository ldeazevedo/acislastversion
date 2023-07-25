package net.sf.l2j.gameserver.custom.fakeplayer.ai;

import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.custom.fakeplayer.FakeHelper;
import net.sf.l2j.gameserver.custom.fakeplayer.FakePlayer;
import net.sf.l2j.gameserver.enums.actors.OperateType;
import net.sf.l2j.gameserver.network.serverpackets.PrivateStoreMsgSell;

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
				_fakePlayer.getSellList().setTitle(sellerShopNames[Rnd.get(sellerShopNames.length)]);
				_fakePlayer.getSellList().setPackaged(false);
				_fakePlayer.getMove().stop();
				_fakePlayer.sitDown();
				_fakePlayer.setOperateType(OperateType.SELL);
				_fakePlayer.broadcastUserInfo();
				_fakePlayer.broadcastPacket(new PrivateStoreMsgSell(_fakePlayer));
			}, Rnd.get(20000, 60000));
		}
	}
}