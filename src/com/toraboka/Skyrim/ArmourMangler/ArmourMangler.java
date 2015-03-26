package com.toraboka.Skyrim.ArmourMangler;

import java.awt.Color;
import java.awt.Font;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import lev.gui.LSaveFile;
import skyproc.ARMA;
import skyproc.ARMO;
import skyproc.FormID;
import skyproc.GRUP;
import skyproc.GRUP_TYPE;
import skyproc.Mod;
import skyproc.ModListing;
import skyproc.RACE;
import skyproc.SPGlobal;
import skyproc.SkyProcSave;
import skyproc.genenums.Gender;
import skyproc.genenums.Perspective;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SUM;
import skyproc.gui.SUMGUI;

import com.toraboka.Skyrim.ArmourMangler.YourSaveFile.Settings;

/**
 *
 * @author Your Name Here
 */
public class ArmourMangler implements SUM {

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

	class Mangler extends GenderPerspective {
		final Path origPath;

		Mangler(GenderPerspective gp, Path origPath) {
			super(gp);
			this.origPath = origPath;
		}

		Mangler(Mangler m) {
			super(m);
			this.origPath = m.origPath;
		}
	}

	class Mangler2 extends Mangler {
		final Path newPath;

		Mangler2(Mangler m, Path newPath) {
			super(m);
			this.newPath = newPath;
		}
	}

	class RACERecord {
		RACE race;
		Path racePath;
		String raceName;

		RACERecord(RACE race, Path racePath) {
			this.race = race;
			this.racePath = racePath;
			this.raceName = race.getName();
		}
	}

	/*
	 * The types of records you want your patcher to import. Change this to
	 * customise the import to what you need.
	 */
	GRUP_TYPE[] importRequests = new GRUP_TYPE[] { GRUP_TYPE.ARMA,
			GRUP_TYPE.ARMO, GRUP_TYPE.RACE };

	GRUP_TYPE[] dangerousRecordTypes = new GRUP_TYPE[] { GRUP_TYPE.ARMO };

	public static String myPatchName = "Armour Mangler";

	public static String authorName = "Martin Rudat";

	public static String version = "0.0.1";

	public static String welcomeText = "Modifies armour to have a different mesh for each race";

	public static String descriptionToShowInSUM = "For every armour record found, looks for a replacement mesh under meshes/<race>/... if no replacement mesh is found, uses default mesh.";

	public static Color headerColor = new Color(66, 181, 184); // Teal

	public static Color settingsColor = new Color(72, 179, 58); // Green

	public static Font settingsFont = new Font("Serif", Font.BOLD, 15);

	public static SkyProcSave save = new YourSaveFile();

	public static void main(String[] args) {
		try {
			SPGlobal.createGlobalLog();
			SUMGUI.open(new ArmourMangler(), args);
		} catch (Exception e) {
			// If a major error happens, print it everywhere and display a
			// message box.
			System.err.println(e.toString());
			SPGlobal.logException(e);
			JOptionPane.showMessageDialog(null,
					"There was an exception thrown during program execution: '"
							+ e
							+ "'  Check the debug logs or contact the author.");
			SPGlobal.closeDebug();
		}
	}

	@Override
	public String getName() {
		return myPatchName;
	}

	@Override
	public GRUP_TYPE[] dangerousRecordReport() {
		return dangerousRecordTypes;
	}

	@Override
	public GRUP_TYPE[] importRequests() {
		return importRequests;
	}

	@Override
	public boolean importAtStart() {
		return false;
	}

	@Override
	public boolean hasStandardMenu() {
		return true;
	}

	// This is where you add panels to the main menu.
	// First create custom panel classes (as shown by YourFirstSettingsPanel),
	// Then add them here.
	@Override
	public SPMainMenuPanel getStandardMenu() {
		SPMainMenuPanel settingsMenu = new SPMainMenuPanel(getHeaderColor());

		settingsMenu.setWelcomePanel(new WelcomePanel(settingsMenu));
		settingsMenu.addMenu(new OtherSettingsPanel(settingsMenu), false, save,
				Settings.OTHER_SETTINGS);

		return settingsMenu;
	}

	// Usually false unless you want to make your own GUI
	@Override
	public boolean hasCustomMenu() {
		return false;
	}

	@Override
	public JFrame openCustomMenu() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasLogo() {
		return false;
	}

	@Override
	public URL getLogo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasSave() {
		return true;
	}

	@Override
	public LSaveFile getSave() {
		return save;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public ModListing getListing() {
		return new ModListing(getName(), false);
	}

	@Override
	public Mod getExportPatch() {
		Mod out = new Mod(getListing());
		out.setAuthor(authorName);
		return out;
	}

	@Override
	public Color getHeaderColor() {
		return headerColor;
	}

	// Add any custom checks to determine if a patch is needed.
	// On Automatic Variants, this function would check if any new packages were
	// added or removed.
	@Override
	public boolean needsPatching() {
		return false;
	}

	// This function runs when the program opens to "set things up"
	// It runs right after the save file is loaded, and before the GUI is
	// displayed
	@Override
	public void onStart() throws Exception {
	}

	// This function runs right as the program is about to close.
	@Override
	public void onExit(boolean patchWasGenerated) throws Exception {
	}

	// Add any mods that you REQUIRE to be present in order to patch.
	@Override
	public ArrayList<ModListing> requiredMods() {
		return new ArrayList<>(0);
	}

	@Override
	public String description() {
		return descriptionToShowInSUM;
	}

	// This is where you should write the bulk of your code.
	// Write the changes you would like to make to the patch,
	// but DO NOT export it. Exporting is handled internally.
	@Override
	public void runChangesToPatch() throws Exception {

		Mod patch = SPGlobal.getGlobalPatch();

		Mod merger = new Mod(getName() + "Merger", false);
		merger.addAsOverrides(SPGlobal.getDB());

		FileSystem fs = FileSystems.getDefault();

		Map<FormID, RACERecord> races = new HashMap<FormID, RACERecord>(merger
				.getRaces().size());
		for (RACE race : merger.getRaces()) {
			String raceName = race.getName();
			Path racePath = fs.getPath(raceName);
			if (racePath.toFile().isDirectory()) {
				races.put(race.getForm(), new RACERecord(race, racePath));
			}
		}
		GRUP<ARMA> armas = merger.getArmatures();

		ArrayList<GenderPerspective> genderPerspectives = new ArrayList<GenderPerspective>(
				Gender.values().length * Perspective.values().length);
		for (Gender g : Gender.values()) {
			for (Perspective p : Perspective.values()) {
				genderPerspectives.add(new GenderPerspective(g, p));
			}
		}

		for (ARMO armor : merger.getArmors()) {
			for (FormID armaId : armor.getArmatures()) {
				ARMA arma = armas.get(armaId);

				ArrayList<Mangler> origPaths = new ArrayList<Mangler>(
						genderPerspectives.size());
				for (GenderPerspective gp : genderPerspectives) {
					String modelPath = arma.getModelPath(gp.gender,
							gp.perspective);
					if (modelPath.isEmpty()) {
						continue;
					}
					origPaths.add(new Mangler(gp, fs.getPath(modelPath)));
				}

				for (FormID raceId : arma.getAdditionalRaces()) {
					RACERecord race = races.get(raceId);
					if (race == null) {
						continue;
					}

					String raceName = race.raceName;

					ArrayList<Mangler2> mangledPaths = new ArrayList<Mangler2>(
							origPaths.size());
					for (Mangler p : origPaths) {
						Path mp = manglePath(p.origPath, race);
						if (mp == null) {
							continue;
						}
						mangledPaths.add(new Mangler2(p, mp));
					}

					if (mangledPaths.isEmpty()) {
						continue;
					}

					ARMA newArma = patch.makeCopy(arma, arma.getEDID()
							+ " for " + raceName);
					patch.addRecord(arma);
					arma.removeAdditionalRace(raceId);
					newArma.clearAdditionalRaces();
					newArma.addAdditionalRace(raceId);

					for (Mangler2 m : mangledPaths) {
						if (m.newPath != null) {
							newArma.setModelPath(m.newPath.toString(),
									m.gender, m.perspective);
						}
					}

				}
			}
		}
	}

	private Path manglePath(Path origPath, RACERecord race) {
		Path mangledPath = race.racePath.resolve(origPath);
		if (mangledPath.toFile().exists()) {
			return mangledPath;
		}
		return null;
	}

}
