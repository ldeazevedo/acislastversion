/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.l2j.gameserver.scripting.script.ai.boss;

import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.scripting.script.ai.AttackableAIScript;
import net.sf.l2j.gameserver.skills.L2Skill;

/** Master Anays AI - Retail Like:
 ** When you open the Gate of Splendor to the raid boss room in the Monastery of Silence, the left and right doors have been changed to open at the same time. 
 ** The doors closes 60seconds after they are opened, and neither of them will open again for 30 minutes once closed. The Gate of Splendor cannot be opened using the Unlock skill.
 ** If raid boss Master Anays is outside when it receives a player�s attack, it will teleport back into the room. 
 ** If the raid boss is outside and its subordinate receives an attack while it is also outside, the raid boss will teleport back into the room. 
 ** When the raid boss is inside the room and its subordinates are outside, if a player attacks a subordinate, the raid boss will remain in the room, 
 ** and the player will only battle with the subordinates.
 **	@author ZaKaX
 **/
public class MasterAnays extends AttackableAIScript
{
	private static final int ANAIS = 29096;
	private static final int ROOMKEY = 8056;
	
	private static final int iCenterX = 112804; // Anais Room, X.
	private static final int iCenterY = -76503; // Anais Room, Y.
	private static DoorData _doorTable = DoorData.getInstance();
	
	public boolean isEntryLocked = false;
	
	public MasterAnays(int id, String name, String descr)
	{
		super("ai/boss");
		
		addAttacked(ANAIS);
	}

	@Override
	public String onTimer(String event, Npc npc, Player activeChar)
	{
		if (event.equalsIgnoreCase("TryOpenDoor"))
		{
			if (isEntryLocked)
				activeChar.sendMessage("It is impossible to open the door at this time.");
			else
				startQuestTimer("OpenDoor", null, activeChar, 0);
		}
		else if (event.equalsIgnoreCase("OpenDoor"))
		{
			ItemInstance iSplendorKey = activeChar.getInventory().getItemByItemId(ROOMKEY);
			
			if (iSplendorKey == null)
				return "";
			
			if (activeChar.destroyItemByItemId("QUEST", ROOMKEY, 1, activeChar, true))
			{
				_doorTable.getDoor(23150003).openMe();
				_doorTable.getDoor(23150004).openMe();
				this.startQuestTimer("CloseDoor", null, null, 60000);
			}
		}
		else if (event.equalsIgnoreCase("CloseDoor"))
		{
			isEntryLocked = true;
			_doorTable.getDoor(23150003).closeMe();
			_doorTable.getDoor(23150004).closeMe();
			this.startQuestTimer("UnlockEntry", null, null, 1800000);
		}
		else if (event.equalsIgnoreCase("UnlockEntry"))
		{
			isEntryLocked = false;
		}

		return super.onTimer(event, npc, activeChar);
	}

	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (npc.getNpcId() == ANAIS)
		{
			if (!npc.isIn2DRadius(iCenterX, iCenterY, 1700))
			{
			//	npc.teleportTo(npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ(), false);
				
				RaidBoss boss = (RaidBoss) npc;
				boss.getAggroList().cleanAllHate();
				boss.getAttackByList().clear();
				
				attacker.getAttack().stop();
				attacker.getCast().stop();
			}
		}

		super.onAttacked(npc, attacker, damage, skill);
	}
}