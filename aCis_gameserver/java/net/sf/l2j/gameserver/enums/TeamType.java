package net.sf.l2j.gameserver.enums;

public enum TeamType {
	NONE(0),
	BLUE(1),
	RED(2);

	private int _id;

	private TeamType(int id) {
		_id = id;
	}

	public int getId() {
		return _id;
	}

	public static TeamType getById(int id) {
		switch (id) {
			case 1:
				return BLUE;
			case 2:
				return RED;
		}
		return NONE;
	}
}