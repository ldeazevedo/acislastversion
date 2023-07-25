package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.idfactory.IdFactory;

public class Tuple<R, L> {

	private final R leftElement;
	private final L rightElement;
	private final Integer instanceId;

	public Tuple(){
		this(null, null, false);
	}

	public Tuple(R leftElement, L rightElement) {
		this(leftElement, rightElement, true);
	}

	public Tuple(R leftElement, L rightElement, boolean newInstanceId){
		this.leftElement = leftElement;
		this.rightElement = rightElement;
		this.instanceId = newInstanceId ? IdFactory.getInstance().getNextId() : -1;
	}

	public R left() {
		return leftElement;
	}

	public L right() {
		return rightElement;
	}

	public Integer getInstanceId() {
		return instanceId;
	}
}