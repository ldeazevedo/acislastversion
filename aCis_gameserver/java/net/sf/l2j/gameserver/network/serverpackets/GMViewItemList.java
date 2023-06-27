package net.sf.l2j.gameserver.network.serverpackets;

import java.util.Set;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

public class GMViewItemList extends L2GameServerPacket
{
	private Set<ItemInstance> _items;
	private int _limit;
	private String _playerName;
	private boolean noGM = false;
	
	public GMViewItemList(Player player)
	{
		_items = player.getInventory().getItems();
		_playerName = player.getName();
		_limit = player.getStatus().getInventoryLimit();
	}
	
	public GMViewItemList(Player player, boolean noGM)
	{
		this.noGM = noGM;
		_items = player.getInventory().getItems();
		_playerName = player.getName();
		_limit = player.getStatus().getInventoryLimit();
	}
	
	public GMViewItemList(Pet pet)
	{
		_items = pet.getInventory().getItems();
		_playerName = pet.getName();
		_limit = pet.getInventoryLimit();
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x94);
		writeS(_playerName);
		writeD(_limit);
		writeH(0x01); // show window ??
		writeH(_items.size());
		
		for (ItemInstance temp : _items)
		{
			Item item = temp.getItem();
			if (noGM)
			if (!temp.isEquipped() || temp.getItemId() == 57)
				continue;
		//	_items.remove(temp);
			
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(item.getType2());
			writeH(temp.getCustomType1());
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(item.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			writeD((temp.isAugmented()) ? temp.getAugmentation().getId() : 0x00);
			writeD(temp.getMana());
		}
	}
}