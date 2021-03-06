package simulizer.settings.types;

import simulizer.settings.SettingType;
import simulizer.settings.SettingValue;

/**
 * Represents an integer setting
 * 
 * @author Michael
 *
 */
public class IntegerSetting extends SettingValue<Integer> {
	private int lowBound, highBound;
	private boolean boundsSet = false;

	public IntegerSetting(String jsonName, String humanName, String description) {
		super(jsonName, humanName, description, 0);
	}

	public IntegerSetting(String jsonName, String humanName, String description, int defaultValue) {
		super(jsonName, humanName, description, defaultValue);
	}

	public IntegerSetting(String jsonName, String humanName, String description, int defaultValue, int lowBound, int highBound) {
		super(jsonName, humanName, description, defaultValue);
		this.lowBound = lowBound;
		this.highBound = highBound;
		boundsSet = true;
	}

	@Override
	public boolean isValid(Integer value) {
		if (!boundsSet)
			return true;
		else
			return getLowBound() <= value && value <= getHighBound();
	}

	@Override
	public SettingType getSettingType() {
		return SettingType.INTEGER;
	}

	public int getLowBound() {
		return lowBound;
	}

	public int getHighBound() {
		return highBound;
	}

}
