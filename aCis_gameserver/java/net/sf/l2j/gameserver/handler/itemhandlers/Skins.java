package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.data.xml.DressMeData;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.DressMe;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

public class Skins implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final DressMe dress = DressMeData.getInstance().getItemId(item.getItemId());
		if (dress == null)
			return;
		
		final Player player = (Player) playable;
		player.setDress(dress);
	//	player.broadcastPacket(new MagicSkillUse(player, player, 1036, 1, 4000, 0));
		player.broadcastUserInfo();
	}
}