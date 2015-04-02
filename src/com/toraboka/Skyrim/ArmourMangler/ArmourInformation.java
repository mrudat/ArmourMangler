package com.toraboka.Skyrim.ArmourMangler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import skyproc.ARMA;
import skyproc.ARMO;
import skyproc.FormID;
import skyproc.SPGlobal;

class ArmourInformation {
	ARMO armour;
	HashSet<String> models = new HashSet<String>();
	TreeMap<String, HashSet<GenderPerspective>> modelsGp = new TreeMap<String, HashSet<GenderPerspective>>();
	//Map<FormID, RACE> races = new TreeMap<FormID, RACE>();
	Map<String, HashSet<ARMA>> modelToAddOns = new TreeMap<String, HashSet<ARMA>>();
	//Map<String, Map<GenderPerspective, ARMA>> fred = new TreeMap<String, Map<GenderPerspective, ARMA>>();
	public Map<FormID, HashSet<ARMA>> raceToAddOns2 = new TreeMap<FormID, HashSet<ARMA>>();

	public ArmourInformation(ARMO armour) {
		this.armour = armour;
	}

	public ARMA getAddOnForRaces(HashSet<FormID> targetRaces, String raceName, HashSet<String> baseModels) {
		SPGlobal.log("getAddOnForRaces", armour.toString(), " ", raceName, " models ", baseModels.toString());
		HashSet<ARMA> candidatesByModel = new HashSet<ARMA>();
		for (String model : baseModels) {
			if (modelToAddOns.containsKey(model)) {
				candidatesByModel.addAll(modelToAddOns.get(model));
			}
		}
		SPGlobal.log("getAddOnForRaces", armour.toString(), " ", raceName, " candidates by model ", candidatesByModel.toString());
		HashSet<ARMA> candidatesByRace = new HashSet<ARMA>();
		for (FormID id : targetRaces) {
			if (raceToAddOns2.containsKey(id)) {
				candidatesByRace.addAll(raceToAddOns2.get(id));
			}
		}
		SPGlobal.log("getAddOnForRaces", armour.toString(), " ", raceName, " candidates by race ", candidatesByRace.toString());
		HashSet<ARMA> candidates = new HashSet<ARMA>(candidatesByModel);
		candidates.retainAll(candidatesByRace);
		if (candidates.isEmpty()) {
			candidates = candidatesByModel;
		}
		SPGlobal.log("getAddOnForRaces", armour.toString(), " ", raceName, " candidates ", candidates.toString());
		ARMA candidateAddOn = null;
		if (candidates.isEmpty()) {
			candidateAddOn = raceToAddOns2.values().iterator().next().iterator().next();
			SPGlobal.log("getAddOnForRaces", armour.toString(), " ", raceName, " candidate by default ", candidateAddOn.toString());
		} else {
			candidateAddOn = candidates.iterator().next();
			SPGlobal.log("getAddOnForRaces", armour.toString(), " ", raceName, " candidate ", candidateAddOn.toString());
		}
		if (targetRaces.equals(new HashSet<FormID>(candidateAddOn.getAdditionalRaces()))) {
			SPGlobal.getGlobalPatch().addRecord(candidateAddOn);
			SPGlobal.log("getAddOnForRaces", armour.toString(), " ", raceName, " editing ", candidateAddOn.toString());
			return candidateAddOn;
		}
		SPGlobal.log("getAddOnForRaces", armour.toString(), " ", raceName, " duplicating ", candidateAddOn.toString());
		return splitAddOnForRaceName(candidateAddOn, targetRaces, raceName);
	}

	private ARMA splitAddOnForRaceName(ARMA originalAddOn, HashSet<FormID> targetRaces, String raceName) {
		ARMA newAddOn = originalAddOn.copy(originalAddOn.getEDID() + "For" + raceName);
		SPGlobal.log("getAddOnForRaceName", "splitting ", newAddOn.toString(), " from ", originalAddOn.toString());
		SPGlobal.log("getAddOnForRaceName", originalAddOn.toString(), " original races ", originalAddOn.getAdditionalRaces().toString());

		SPGlobal.getGlobalPatch().addRecord(armour);
		armour.getArmatures().add(0, newAddOn.getForm());
		//armour.addArmature(newAddOn.getForm());

		newAddOn.clearAdditionalRaces();
		boolean changedOriginal = false;
		for (FormID id : targetRaces) {
			originalAddOn.removeAdditionalRace(id);
			changedOriginal = true;
		}
		if (changedOriginal) {
			SPGlobal.getGlobalPatch().addRecord(originalAddOn);
		}

		ArrayList<FormID> tempRaces = new ArrayList<FormID>(targetRaces);
		for (FormID id : tempRaces) {
			newAddOn.addAdditionalRace(id);
		}
		SPGlobal.log("getAddOnForRaceName", originalAddOn.toString(), " new races ", originalAddOn.getAdditionalRaces().toString());
		SPGlobal.log("getAddOnForRaceName", newAddOn.toString(), " new races ", newAddOn.getAdditionalRaces().toString());
		return newAddOn;
	}

}