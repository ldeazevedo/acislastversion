package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.idfactory.IdFactory;

public class Tuple<T, Y> {

	private final T leftElement;
	private final Y rightElement;
	private final Integer instanceId;

	public Tuple(){
		this(null, null, false);
	}

	public Tuple(T leftElement, Y rightElement) {
		this(leftElement, rightElement, true);
	}

	public Tuple(T leftElement, Y rightElement, boolean newInstanceId){
		this.leftElement = leftElement;
		this.rightElement = rightElement;
		this.instanceId = newInstanceId ? IdFactory.getInstance().getNextId() : -1;
	}

	public T left() {
		return leftElement;
	}

	public Y right() {
		return rightElement;
	}

	public Integer getInstanceId() {
		return instanceId;
	}
}