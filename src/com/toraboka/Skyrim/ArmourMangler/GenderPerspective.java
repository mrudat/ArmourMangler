package com.toraboka.Skyrim.ArmourMangler;

import skyproc.genenums.Gender;
import skyproc.genenums.Perspective;

class GenderPerspective {
	final Gender gender;
	final Perspective perspective;

	GenderPerspective(Gender gender, Perspective perspective) {
		this.gender = gender;
		this.perspective = perspective;
	}

	GenderPerspective(GenderPerspective gp) {
		this.gender = gp.gender;
		this.perspective = gp.perspective;
	}
}