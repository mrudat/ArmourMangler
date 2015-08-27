package com.toraboka.Skyrim.ArmourMangler;

import skyproc.SPGlobal;

@SuppressWarnings("serial")
public class MangledException extends Exception {

	public MangledException(Throwable e) {
		super(e);
		SPGlobal.logException(e);
	}

}
