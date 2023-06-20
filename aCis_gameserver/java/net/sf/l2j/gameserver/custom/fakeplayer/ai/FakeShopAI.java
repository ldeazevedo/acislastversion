package net.sf.l2j.gameserver.custom.fakeplayer.ai;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.custom.fakeplayer.FakeHelper;
import net.sf.l2j.gameserver.custom.fakeplayer.FakePlayer;
import net.sf.l2j.gameserver.custom.fakeplayer.FakePlayerAI;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

import java.util.ArrayList;
import java.util.List;

public abstract class FakeShopAI extends FakePlayerAI {

	protected FakeHelper.City city;
	String[] sellerShopNames = new String[]{"te vendo todo", "baratitoooo", "ssd bssd", "compra raton", "ratonaso, veni", "SSD BSSD", "gratis", "otra tienda", "vendo de todo",
			"shots barato", "todo por 2", "COTO", "Carrefuour", "Super Dia", "24 horas", "comprame", "ayudaaaaa", "MAMAAAA", "SELLS", "buy here", "please money", "Ricasio", "hernandez",
			"compra rapido!!", "rapido amigo!!", "muere bart muere", "vicio", "vota", "mats", "tienda ramon", "tienda mia", "altas recipes", "me pica el mause", "tenedor libre",
			"5 estrellas", "3 estrellas", "tienda de apu", "kamehouse", "joyitas", "regalitos", "viernes de vicio", "sabado de vicio", "domingo de vicio", "feriado de vicio", "sacate las pulgas",
			"el drop de un pk", "top drop", "drop free", "te mata un elpy?", "asi no te matan", "compra chinwenwencha", "rare items!"};
	String[] buyerShopNames = new String[]{"te compro todo", "pago mas", "cositas ricas", "vende raton", "ratonaso, veni", "gratis", "otra tienda", "enano compra", "compro de todo",
			"cosas caras", "todo por 10", "COTO", "Carrefuour", "Super Dia", "24 horas", "ayudaaaaa", "MAMAAAA", "24/7", "compra y craftea!",
			"hacete groso", "hacete el pro", "craftea!", "mats al costo!", "vota x el server", "vendo al por mayor", "te vendo la casa", "equipate un poco", "craftea algo mejor", "polvo magico",
			"polvo blanco", "tienda online", "store free", "store online", "store craft", "store 23hs", "craft set", "craft weapons", "craft jewell"};

	FakeShopAI(FakePlayer creature, FakeHelper.City city) {
		super(creature);
		this.city = city;
	}

	void addItemsToStore(List<ShopItem> shopItemList, FakeHelper.FakePlayerType fakePlayerType) {
		shopItemList.forEach(sd -> {
			ItemInstance consumable = _fakePlayer.getInventory().getItemByItemId(sd.getItemId());
			if (consumable != null) {
				if (fakePlayerType == FakeHelper.FakePlayerType.BUYER)
					_fakePlayer.getBuyList().addItem(consumable.getObjectId(), consumable.getCount(), sd.getPrice());
				if (fakePlayerType == FakeHelper.FakePlayerType.SELLER)
					_fakePlayer.getSellList().addItem(consumable.getObjectId(), consumable.getCount(), sd.getPrice());
			}
		});
	}

	void addItemsToInventory(List<ShopItem> shopItemList) {
		shopItemList.forEach(sd -> {
			ItemInstance itemInstance = _fakePlayer.getInventory().getItemByItemId(sd.getItemId());
			if (itemInstance == null || itemInstance.getCount() < sd.getQuantity()) {
				ItemInstance item = _fakePlayer.getInventory().addItem("fakeshop", sd.getItemId(), sd.getQuantity(), _fakePlayer, null);
				if (item != null)
					World.getInstance().addObject(item);
			}
		});
	}

	public List<ShopItem> getItemsForShop(FakeHelper.FakePlayerType fakePlayerType) {
		List<ShopItem> sellerData = List.of();
		switch (fakePlayerType) {
			case SELLER:
				sellerData = populateSellerData();
				break;
			case BUYER:
				sellerData = populateBuyerData();
				break;
		}
		List<ShopItem> itemsToSell = new ArrayList<>();
		for (int i = 0; i < Rnd.get(1, 3); i++)
			itemsToSell.add(getRandomItem(sellerData, itemsToSell));
		return itemsToSell;
	}

	private ShopItem getRandomItem(List<ShopItem> items, List<ShopItem> itemsToSell) {
		ShopItem shopItem = items.get(Rnd.get(items.size()));
		if (itemsToSell.contains(shopItem))
			return getRandomItem(items, itemsToSell);
		return shopItem;
	}

	private List<ShopItem> populateSellerData() {
		List<ShopItem> sellerData = new ArrayList<>();
		if (city == FakeHelper.City.GIRAN || city == FakeHelper.City.OREN || city == FakeHelper.City.ADEN) {
			sellerData.add(new ShopItem(1464, Rnd.get(4000, 9000), Rnd.get(35, 45))); //SSC
			sellerData.add(new ShopItem(3949, Rnd.get(2000, 6000), Rnd.get(100, 120))); //BSSC
			//sellerData.add(new ShopItem(4043, Rnd.get(10, 50), Rnd.get(3800, 4000))); //Asofe
			//sellerData.add(new ShopItem(5550, Rnd.get(3, 15), Rnd.get(9000, 10000))); //Durable Metal plate
		}
		sellerData.add(new ShopItem(1463, Rnd.get(4000, 9000), Rnd.get(25, 30))); //SSD
		sellerData.add(new ShopItem(3948, Rnd.get(2000, 5000), Rnd.get(70, 90))); //BSSD
		sellerData.add(new ShopItem(1458, Rnd.get(100, 450), Rnd.get(750, 1000)));//D-Cry
		sellerData.add(new ShopItem(3953, Rnd.get(1, 3), Rnd.get(70000, 90000)));//Rec BSSD

		sellerData.add(new ShopItem(1867, Rnd.get(20, 80), Rnd.get(100, 140))); //Animal Skin
		sellerData.add(new ShopItem(1872, Rnd.get(10, 200), Rnd.get(100, 140))); //Animal Bone
		sellerData.add(new ShopItem(1871, Rnd.get(20, 40), Rnd.get(140, 170))); //Charcoal
		sellerData.add(new ShopItem(1881, Rnd.get(5, 25), Rnd.get(1200, 1300))); //CBP
		sellerData.add(new ShopItem(1880, Rnd.get(5, 20), Rnd.get(1600, 1700))); //Steel
		sellerData.add(new ShopItem(1895, Rnd.get(20, 80), Rnd.get(500, 550))); //Charcoal
		sellerData.add(new ShopItem(1865, Rnd.get(7, 75), Rnd.get(190, 220))); //Varnish
		sellerData.add(new ShopItem(1866, Rnd.get(7, 75), Rnd.get(190, 220))); //Suede
		sellerData.add(new ShopItem(1864, Rnd.get(50, 200), Rnd.get(55, 75))); //Stem
		sellerData.add(new ShopItem(1870, Rnd.get(50, 120), Rnd.get(55, 75))); //Coal

		sellerData.add(new ShopItem(2143, Rnd.get(2, 4), Rnd.get(10000, 15000))); //Synthetic Cokes
		sellerData.add(new ShopItem(2135, Rnd.get(2, 4), Rnd.get(10000, 15000))); //Braided Hemp
		sellerData.add(new ShopItem(2142, Rnd.get(2, 4), Rnd.get(10000, 15000))); //Varnish of Purity
		sellerData.add(new ShopItem(2141, Rnd.get(2, 4), Rnd.get(10000, 20000))); //Silver Mold
		return sellerData;
	}

	private List<ShopItem> populateBuyerData() {
		List<ShopItem> buyerData = new ArrayList<>();
		if (city == FakeHelper.City.GIRAN || city == FakeHelper.City.OREN || city == FakeHelper.City.ADEN) {
			buyerData.add(new ShopItem(4043, Rnd.get(2, 10), Rnd.get(3100, 3200))); //Asofe
			buyerData.add(new ShopItem(1881, Rnd.get(3, 15), Rnd.get(770, 800))); //CBP
			buyerData.add(new ShopItem(5550, Rnd.get(1, 5), Rnd.get(8000, 9000))); //DurableMetalPlate
			buyerData.add(new ShopItem(1875, Rnd.get(5, 13), Rnd.get(1600, 1800))); //SOP
		}
		buyerData.add(new ShopItem(1867, Rnd.get(20, 80), Rnd.get(77, 80))); //Animal Skin
		buyerData.add(new ShopItem(1872, Rnd.get(10, 200), Rnd.get(77, 80))); //Animal Bone
		buyerData.add(new ShopItem(1871, Rnd.get(10, 200), Rnd.get(102, 110))); //Charcoal
		buyerData.add(new ShopItem(1880, Rnd.get(1, 10), Rnd.get(1100, 1200))); //Steel
		buyerData.add(new ShopItem(1895, Rnd.get(10, 25), Rnd.get(360, 380))); //Metalic Fiber
		buyerData.add(new ShopItem(1884, Rnd.get(10, 25), Rnd.get(170, 185))); //Cord
		buyerData.add(new ShopItem(1885, Rnd.get(25, 75), Rnd.get(110, 120))); //Varnish
		buyerData.add(new ShopItem(1866, Rnd.get(25, 75), Rnd.get(160, 170))); //Suede
		buyerData.add(new ShopItem(1864, Rnd.get(40, 100), Rnd.get(60, 70))); //Stem
		buyerData.add(new ShopItem(1870, Rnd.get(15, 40), Rnd.get(120, 130))); //Coal
		buyerData.add(new ShopItem(1804, Rnd.get(1, 2), Rnd.get(5000, 10000))); //bssd rec
		buyerData.add(new ShopItem(3953, Rnd.get(2, 3), Rnd.get(4000, 7500))); //ssd rec
		return buyerData;
	}


	public static class ShopItem {
		private int itemId;
		private int quantity;
		private int price;

		ShopItem(int itemId, int quantity, int price) {
			this.itemId = itemId;
			this.quantity = quantity;
			this.price = price;
		}

		public int getItemId() {
			return itemId;
		}

		public void setItemId(int itemId) {
			this.itemId = itemId;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		public int getPrice() {
			return price;
		}

		public void setPrice(int price) {
			this.price = price;
		}
	}
}