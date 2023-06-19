package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.actor.Player;

public class ExPCCafePointInfo extends L2GameServerPacket
{
	private final int _score;
	private final int _modify;
	private final int _remainingTime;
	private final int _pointType;
	private final int _periodType;

/*
	public ExPCCafePointInfo(int score, int modify, boolean addPoint, boolean pointType, int remainingTime)
	{
		_score = score;
		_modify = (addPoint) ? modify : modify * -1;
		_remainingTime = remainingTime;
		_pointType = (addPoint) ? (pointType ? 0 : 1) : 2;
		_periodType = 1;
	}
	*/
	public ExPCCafePointInfo(Player player, int modify, boolean addPoint, int hour, boolean _double)
    {
		_score = player.getPcBangScore();
		_modify = modify;
		if (addPoint)
		{
			_periodType = 1;
			_pointType = 1;
        }
        else
        {
            if (addPoint && _double)
            {
            	_periodType = 1;
            	_pointType = 0;
            }
            else
            {
            	_periodType = 2;
            	_pointType = 2;
            }
        }
        
		_remainingTime = hour;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x31);
		writeD(_score);
		writeD(_modify);
		writeC(_periodType);
		writeD(_remainingTime);
		writeC(_pointType);
	}
}