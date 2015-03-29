package com.toraboka.Skyrim.ArmourMangler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import skyproc.ARMA;
import skyproc.ARMO;
import skyproc.FormID;
import skyproc.RACE;
import skyproc.SPGlobal;

class ArmourInformation {
	ARMO armour;
	HashSet<String> models = new HashSet<String>();
	TreeMap<String, HashSet<GenderPerspective>> modelsGp = new TreeMap<String,HashSet<GenderPerspective>>();
	Map<FormID, RACE> races = new TreeMap<FormID, RACE>();
	Map<FormID, ARMA> addOns = new TreeMap<FormID, ARMA>();
	Map<String, ARMA> modelAddOns = new TreeMap<String, ARMA>();
	Map<String, Map<GenderPerspective,ARMA>> fred = new TreeMap<String, Map<GenderPerspective, ARMA>>();

	public ArmourInformation(ARMO armour) {
		this.armour = armour;
	}

	public ARMA getAddOnForRaces(Collection<FormID> targetRaces, String raceName, HashSet<String> baseModels) {
		ARMA candidateAddOn = null;
		for (String model : baseModels) {
			if (modelAddOns.containsKey(model)) {
				candidateAddOn = modelAddOns.get(model);
				break;
			}
		}
		for (FormID id : targetRaces) {
			if (addOns.containsKey(id)) {
				candidateAddOn = addOns.get(id);
				break;
			}
		}
		if (candidateAddOn == null) {
			candidateAddOn = addOns.values().iterator().next();
		}
		for (FormID id : candidateAddOn.getAdditionalRaces()) {
			if (!targetRaces.contains(id)) {
				return splitAddOnForRaceName(candidateAddOn, targetRaces, raceName);
			}
		}
		SPGlobal.getGlobalPatch().addRecord(candidateAddOn);
		return candidateAddOn;
	}

	private ARMA splitAddOnForRaceName(ARMA originalAddOn, Collection<FormID> targetRaces, String raceName) {
		ARMA newAddOn = originalAddOn.copy(originalAddOn.getEDID() + "For" + raceName);
		
		SPGlobal.getGlobalPatch().addRecord(armour);
		//armour.getArmatures().add(0, newAddOn.getForm());
		armour.addArmature(newAddOn.getForm());
		
		newAddOn.clearAdditionalRaces();
		boolean changedOriginal = false;
		for (FormID id : targetRaces) {
			if (originalAddOn.getRace().equals(id)) {
				// There will always be at least one remaining race, if there were not, originalAddOn could have been used directly.
				originalAddOn.setRace(originalAddOn.getAdditionalRaces().remove(0));
				changedOriginal = true;
			} else {
				originalAddOn.removeAdditionalRace(id);
				changedOriginal = true;
			}
		}
		if (changedOriginal) {
			SPGlobal.getGlobalPatch().addRecord(originalAddOn);
		}
		
		ArrayList<FormID> tempRaces = new ArrayList<FormID>(targetRaces);
		newAddOn.setRace(tempRaces.remove(0));
		for (FormID id : tempRaces) {
			newAddOn.addAdditionalRace(id);
		}
		return newAddOn;
	}

}