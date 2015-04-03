package com.toraboka.Skyrim.ArmourMangler;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
import skyproc.SPDatabase;
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

    public static String authorName = "Martin Rudat";

    public static String descriptionToShowInSUM = "For every armour record "
	    + "found, looks for a replacement mesh under meshes/races/<race>/... "
	    + "if no replacement mesh is found, uses default mesh.\n"
	    + "Because it seems that loading two .nif files with the same name "
	    + "doesn't work, or at least doesn't work for me, copies models used to "
	    + "meshes/races/.modesl/<hash of pathname>_[01].nif.";

    public static Color headerColor = new Color(66, 181, 184); // Teal

    public static String myPatchName = "Armour Mangler";

    public static SkyProcSave save = new YourSaveFile();

    public static Color settingsColor = new Color(72, 179, 58); // Green

    public static Font settingsFont = new Font("Serif", Font.BOLD, 15);

    public static String version = "0.0.2";

    public static String welcomeText = "Modifies armour to have a different mesh for each race";

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

    private GRUP<ARMA> armas;

    private HashMap<ARMO, ArmourInformation> armourInformation;

    GRUP_TYPE[] dangerousRecordTypes = new GRUP_TYPE[] { GRUP_TYPE.ARMO };

    private Path dataPath;

    private ArrayList<GenderPerspective> genderPerspectives;

    GRUP_TYPE[] importRequests = new GRUP_TYPE[] { GRUP_TYPE.ARMA,
	    GRUP_TYPE.ARMO, GRUP_TYPE.RACE };

    private Mod merger;

    Path meshesPath;

    private Path newMeshesPath;

    private Mod patch;

    private HashMap<String, HashSet<RACE>> raceNameToRACE;

    private GRUP<RACE> races;

    private Path mangledMeshesPath;

    private <K, V> void addToMapOfSets(Map<K, HashSet<V>> map, K key,
	    Collection<V> values) {
	if (!map.containsKey(key)) {
	    map.put(key, new HashSet<V>());
	}
	map.get(key).addAll(values);
    }

    private <K, V> void addToMapOfSets(Map<K, HashSet<V>> map, K key, V value) {
	if (!map.containsKey(key)) {
	    map.put(key, new HashSet<V>());
	}
	map.get(key).add(value);
    }

    @Override
    public GRUP_TYPE[] dangerousRecordReport() {
	return dangerousRecordTypes;
    }

    @Override
    public String description() {
	return descriptionToShowInSUM;
    }

    private void findCandidateModels() {
	SPGlobal.logMain("Catalogue Candidate Models", "Started");

	SUMGUI.progress.setMax(merger.getArmors().size(),
		"Find Candidate Models");

	int count = 0;
	for (ARMO armour : merger.getArmors()) {
	    if (!armour.getTemplate().isNull()) {
		/*
		 * Sanity check, only the parent armour has ARMA records that
		 * count.
		 */
		count++;
		SUMGUI.progress.setBar(count);
		continue;
	    }
	    ArmourInformation ai = new ArmourInformation(armour);
	    armourInformation.put(armour, ai);
	    for (FormID armaId : armour.getArmatures()) {
		ARMA armourAddOn = armas.get(armaId);
		for (GenderPerspective gp : genderPerspectives) {
		    String modelPath = armourAddOn.getModelPath(gp.gender,
			    gp.perspective);
		    if (modelPath.isEmpty()) {
			continue;
		    }
		    ai.models.add(modelPath);
		    addToMapOfSets(ai.modelsGp, modelPath, gp);
		    addToMapOfSets(ai.modelToAddOns, modelPath, armourAddOn);
		}
		for (FormID raceID : armourAddOn.getAdditionalRaces()) {
		    addToMapOfSets(ai.raceToAddOns2, raceID, armourAddOn);
		}
	    }
	    count++;
	    SUMGUI.progress.setBar(count);
	}

	SPGlobal.logMain("Catalogue Candidate Models", "Finished");
    }

    private void findCandidateRaces() {
	SPGlobal.logMain("Find Candidate Races", "Started");

	raceNameToRACE = new HashMap<String, HashSet<RACE>>();

	races = merger.getRaces();

	/*
	 * This goes so fast, it's probably not worth doing, and if your
	 * computer is sufficiently slow that this takes noticeable time, you
	 * probably can't run Skyrim on it.
	 */
	SUMGUI.progress.setMax(races.size(), "Find Candidate Races");
	int count = 0;
	for (RACE race : races) {
	    String raceName = race.getName();
	    try {
		Path racePath = newMeshesPath.resolve(raceName).resolve(
			"meshes");
		if (Files.isDirectory(racePath)) {
		    SPGlobal.log("Find Candidate Races",
			    "Found candidate race: ", raceName);
		    addToMapOfSets(raceNameToRACE, raceName, race);
		}
	    } catch (InvalidPathException e) {
		SPGlobal.logError("Find Candidate Races",
			"Invalid characters in path: ", raceName);
		SPGlobal.logException(e);
	    }
	    count++;
	    SUMGUI.progress.setBar(count);
	}

	SPGlobal.logMain("Find Candidate Races", "Found ",
		Integer.toString(raceNameToRACE.size()),
		" potential races with replacement meshes");

	SPGlobal.logMain("Find Candidate Races", "Finished");
    }

    private void findReplacementModelsAndApply() {
	SPGlobal.logMain("Lookup Replacement Models", "Started");

	SUMGUI.progress.setMax(
		armourInformation.size() * raceNameToRACE.size(),
		"Find Replacement Models And Apply");

	int count = 0;

	for (Entry<String, HashSet<RACE>> i : raceNameToRACE.entrySet()) {
	    String raceName = i.getKey();
	    HashSet<RACE> race = i.getValue();
	    Path racePath = newMeshesPath.resolve(raceName).resolve("meshes");
	    for (ArmourInformation ai : armourInformation.values()) {
		count++;
		HashSet<String> foundModels = new HashSet<String>();
		HashSet<String> baseModels = new HashSet<String>();
		for (String model : ai.models) {
		    try {
			Path modelPath = racePath.resolve(model);
			if (Files.exists(modelPath)) {
			    String foundModel = meshesPath
				    .relativize(modelPath).toString();
			    foundModels.add(foundModel);
			    baseModels.add(model);
			    addToMapOfSets(ai.modelsGp, foundModel,
				    ai.modelsGp.get(model));
			}
		    } catch (InvalidPathException e) {
			SPGlobal.logError("Lookup Replacement Models",
				"Invalid characters in path: ", model);
			SPGlobal.logException(e);
		    }
		}
		if (foundModels.isEmpty()) {
		    SUMGUI.progress.setBar(count);
		    continue;
		}
		ARMA addOn = ai.getAddOnForRaces(
			race.stream().map(j -> j.getForm())
				.collect(Collectors.toSet()), raceName,
			baseModels);
		for (String model : foundModels) {
		    for (GenderPerspective gp : ai.modelsGp.get(model)) {
			try {
			    addOn.setModelPath(getNewPath(model), gp.gender,
				    gp.perspective);
			} catch (SkipThisException e) {
			    // we already logged this.
			}
		    }
		}
		SUMGUI.progress.setBar(count);
	    }
	}

	SPGlobal.logMain("Lookup Replacement Models", "Done");
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

    @Override
    public ModListing getListing() {
	return new ModListing(getName(), false);
    }

    @Override
    public URL getLogo() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
	return myPatchName;
    }

    @Override
    public LSaveFile getSave() {
	return save;
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

    @Override
    public String getVersion() {
	return version;
    }

    // Usually false unless you want to make your own GUI
    @Override
    public boolean hasCustomMenu() {
	return false;
    }

    @Override
    public boolean hasLogo() {
	return false;
    }

    @Override
    public boolean hasSave() {
	return true;
    }

    @Override
    public boolean hasStandardMenu() {
	return true;
    }

    @Override
    public boolean importAtStart() {
	return false;
    }

    @Override
    public GRUP_TYPE[] importRequests() {
	return importRequests;
    }

    // Add any custom checks to determine if a patch is needed.
    // On Automatic Variants, this function would check if any new packages were
    // added or removed.
    @Override
    public boolean needsPatching() {
	save.setStrings(Settings.LAST_MOD_LIST, SPDatabase.getModListDates());
	try {
	    String temp = Files.getLastModifiedTime(
		    FileSystems.getDefault().getPath("ArmorMangler.jar"))
		    .toString();
	    String oldFileTime = save.getStr(Settings.JAR_LAST_MOD);
	    save.setStr(Settings.JAR_LAST_MOD, temp);
	    if (!temp.equals(oldFileTime)) {
		return true;
	    }
	} catch (IOException e) {
	    SPGlobal.logException(e);
	}
	if (save.getStrings(Settings.MODELS_LAST_MOD)
		.parallelStream()
		.filter(s -> {
		    int nl = s.indexOf('<');
		    if (nl == -1) {
			SPGlobal.log("needsPatching",
				"Something wrong with saved model list, better patch.");
			return true;
		    }
		    String filePath = s.substring(0, nl);
		    String fileTime = s.substring(nl + 1);
		    try {
			if (fileTime.equals(Files.getLastModifiedTime(
				FileSystems.getDefault().getPath(filePath))
				.toString())) {
			    return false;
			}
		    } catch (IOException e) {
			SPGlobal.logException(e);
		    }
		    SPGlobal.log("needsPatching", filePath,
			    " been modified at a different time than ",
			    fileTime, " or is missing, better patch.");
		    return true;

		}).findFirst().isPresent()) {
	    return true;
	}
	return false;
    }

    @Override
    public void onExit(boolean patchWasGenerated) throws Exception {
	((YourSaveFile) save).storeModelsLastMod();
    }

    @Override
    public void onStart() throws Exception {

    }

    @Override
    public JFrame openCustomMenu() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ArrayList<ModListing> requiredMods() {
	return new ArrayList<>(0);
    }

    @Override
    public void runChangesToPatch() throws IGiveUpException {

	patch = SPGlobal.getGlobalPatch();
	patch.setAuthor(authorName);

	merger = new Mod(getName() + "_TEMP", false);
	merger.addAsOverrides(SPGlobal.getDB());

	FileSystem fs = FileSystems.getDefault();

	dataPath = fs.getPath(SPGlobal.pathToData).toAbsolutePath().normalize();
	meshesPath = dataPath.resolve("meshes");
	newMeshesPath = meshesPath.resolve("races");
	mangledMeshesPath = newMeshesPath.resolve(".models");

	armas = merger.getArmatures();

	genderPerspectives = new ArrayList<GenderPerspective>(
		Gender.values().length * Perspective.values().length);
	for (Gender g : Gender.values()) {
	    for (Perspective p : Perspective.values()) {
		genderPerspectives.add(new GenderPerspective(g, p));
	    }
	}

	armourInformation = new HashMap<ARMO, ArmourInformation>();

	findCandidateRaces();

	findCandidateModels();

	findReplacementModelsAndApply();

    }

    HashMap<String, String> modelToDigestedModel = new HashMap<String, String>();

    private String getNewPath(String modelPath) throws SkipThisException {
	if (modelToDigestedModel.containsKey(modelPath)) {
	    return modelToDigestedModel.get(modelPath);
	}
	buildNewPath(modelPath);
	return modelToDigestedModel.get(modelPath);
    }

    MessageDigest md = setupMessageDigest();

    static MessageDigest setupMessageDigest() {
	try {
	    return MessageDigest.getInstance("SHA-1");
	} catch (NoSuchAlgorithmException e) {
	}
	try {
	    return MessageDigest.getInstance("MD5");
	} catch (NoSuchAlgorithmException e) {
	    throw new RuntimeException(e);
	}
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
	char[] hexChars = new char[bytes.length * 2];
	for (int j = 0; j < bytes.length; j++) {
	    int v = bytes[j] & 0xFF;
	    hexChars[j * 2] = hexArray[v >>> 4];
	    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	}
	return new String(hexChars);
    }

    private void buildNewPath(String modelPath) throws SkipThisException {
	Path meshPath = meshesPath.resolve(modelPath);
	Path fileName = meshPath.getFileName();

	String temp = fileName.toString();
	String file0 = temp.substring(0, temp.length() - 6) + "_0.nif";
	Path path0 = meshPath.getParent().resolve(file0);

	md.reset();
	md.update(temp.getBytes());

	String digest = bytesToHex(md.digest());

	Path new0Path = mangledMeshesPath.resolve(digest + "_0.nif");
	Path new1Path = mangledMeshesPath.resolve(digest + "_1.nif");

	if (Files.exists(path0)) {
	    copyFile(path0, new0Path);
	}
	copyFile(meshPath, new1Path);

	modelToDigestedModel.put(modelPath, meshesPath.relativize(new1Path)
		.toString());
    }

    private void copyFile(Path oldPath, Path newPath) throws SkipThisException {
	if (Files.exists(newPath)) {
	    try {
		FileTime lmt = Files.getLastModifiedTime(oldPath);
		if (Files.getLastModifiedTime(newPath).equals(lmt)) {
		    // File times are the same, assume content is identical.
		    recordFileDependency(oldPath, lmt);
		    return;
		}
		Files.delete(newPath);
	    } catch (IOException e) {
		return;
	    }
	}
	try {
	    Files.createSymbolicLink(newPath, oldPath);
	} catch (IOException e) {
	    try {
		Files.createLink(newPath, oldPath);
	    } catch (IOException e2) {
		try {
		    Files.copy(oldPath, newPath);
		    FileTime lmt = Files.getLastModifiedTime(oldPath);
		    Files.setLastModifiedTime(newPath, lmt);
		    recordFileDependency(oldPath, lmt);
		} catch (IOException e3) {
		    throw new SkipThisException(e3);
		}
	    }
	}
    }

    private void recordFileDependency(Path path, FileTime lmt) {
	((YourSaveFile) save).recordModelLastMod(path.toString(), lmt);
    }
}
