package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.idfactory.IdFactory;

public class Tuple<L, R> {

	private final L leftElement;
	private final R rightElement;
	private final Integer instanceId;

	public Tuple() {
		this(null, null, false);
	}

	public Tuple(L leftElement, R rightElement) {
		this(leftElement, rightElement, true);
	}

	public Tuple(L leftElement, R rightElement, boolean newInstanceId) {
		this.leftElement = leftElement;
		this.rightElement = rightElement;
		this.instanceId = newInstanceId ? IdFactory.getInstance().getNextId() : -1;
	}

	public L left() {
		return leftElement;
	}

	public R right() {
		return rightElement;
	}

	public Integer getInstanceId() {
		return instanceId;
	}
}