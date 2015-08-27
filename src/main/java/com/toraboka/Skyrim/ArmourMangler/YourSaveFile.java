/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toraboka.Skyrim.ArmourMangler;

import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import skyproc.SkyProcSave;

/**
 *
 * @author Martin Rudat
 */
public class YourSaveFile extends SkyProcSave {

    private Map<String, String> lastMod = new HashMap<String,String>();

    @Override
    protected void initSettings() {
	// The Setting, The default value, Whether or not it changing means a
	// new patch should be made
	Add(Settings.IMPORT_AT_START, true, false);
	Add(Settings.LAST_MOD_LIST, new ArrayList<String>(), false);
	Add(Settings.JAR_LAST_MOD, "", true);
	Add(Settings.MODELS_LAST_MOD, new ArrayList<String>(), true);
    }

    public void recordModelLastMod(String path, FileTime time) {
	lastMod.put(path, time.toString());
    }

    public void storeModelsLastMod() {
	ArrayList<String> n = new ArrayList<String>();
	for(Entry<String,String> e : lastMod.entrySet()) {
	    n.add(e.getKey() + "<" + e.getValue());
	}
	this.setStrings(Settings.MODELS_LAST_MOD, n);
    }

    @Override
    protected void initHelp() {
	helpInfo.put(
		Settings.IMPORT_AT_START,
		"If enabled, the program will begin importing your mods when the program starts.\n\n"
			+ "If turned off, the program will wait until it is necessary before importing.\n\n"
			+ "NOTE: This setting will not take effect until the next time the program is run.\n\n"
			+ "Benefits:\n"
			+ "- Faster patching when you close the program.\n"
			+ "- More information displayed in GUI, as it will have access to the records from your mods."
			+ "\n\n"
			+ "Downsides:\n"
			+ "- Having this on might make the GUI respond sluggishly while it processes in the "
			+ "background.");

	helpInfo.put(Settings.OTHER_SETTINGS,
		"These are other settings related to this patcher program.");
    }

    // Note that some settings just have help info, and no actual values in
    // initSettings().
    public enum Settings {
	IMPORT_AT_START, OTHER_SETTINGS, LAST_MOD_LIST, JAR_LAST_MOD, MODELS_LAST_MOD;
    }
}
