package com.toraboka.Skyrim.ArmourMangler;

import lev.gui.LTextPane;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;

/**
 *
 * @author Martin Rudat
 */
public class WelcomePanel extends SPSettingPanel {

	private static final long serialVersionUID = 1L;

	LTextPane introText;

	public WelcomePanel(SPMainMenuPanel parent_) {
		super(parent_, ArmourMangler.myPatchName, ArmourMangler.headerColor);
	}

	@Override
	protected void initialize() {
		super.initialize();

		introText = new LTextPane(settingsPanel.getWidth() - 40, 400,
				ArmourMangler.settingsColor);
		introText.setText(ArmourMangler.welcomeText);
		introText.setEditable(false);
		introText.setFont(ArmourMangler.settingsFont);
		introText.setCentered();
		setPlacement(introText);
		Add(introText);

		alignRight();
	}
}
