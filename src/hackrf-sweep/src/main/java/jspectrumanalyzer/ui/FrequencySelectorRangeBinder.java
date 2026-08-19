package jspectrumanalyzer.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import jspectrumanalyzer.core.FrequencyRange;

/**
 * Limits frequency selection of two selectors (start/end)
 */
public class FrequencySelectorRangeBinder
{
	public FrequencySelectorPanel selFreqStart, selFreqEnd;
	public QuickFrequencySelectorPanel selFreqQuick;
	private PropertyChangeListener rangeListener;
	private boolean applyingPreset;

	public FrequencySelectorRangeBinder(FrequencySelectorPanel selFreqStart, FrequencySelectorPanel selFreqEnd,
			QuickFrequencySelectorPanel selFreqQuick)
	{
		this.selFreqEnd	= selFreqEnd;
		this.selFreqStart = selFreqStart;
		this.selFreqQuick = selFreqQuick;
		VetoableChangeListener freqStartVetoable = evt -> {
			Integer newVal = (Integer) evt.getNewValue();
			if (newVal >= selFreqEnd.getValue())
			{
				//try to increase freq end by the same value
				if (!selFreqEnd.setValue(selFreqEnd.getValue() + (newVal - (Integer) evt.getOldValue())))
					throw new PropertyVetoException(">", evt);
			}
		};
		VetoableChangeListener freqEndVetoable = evt -> {
			Integer newVal = (Integer) evt.getNewValue();
			if (newVal <= selFreqStart.getValue())
			{
				if (!selFreqStart.setValue(selFreqStart.getValue() - ((Integer) evt.getOldValue() - newVal)))
					throw new PropertyVetoException(">", evt);
			}
		};
		VetoableChangeListener freqQuickVetoable = evt -> {
			QuickSelectPreset.findByLabel((String) evt.getNewValue()).ifPresent(preset ->
					applyPreset(preset.startMHz, preset.endMHz));
		};


		selFreqEnd.addVetoableChangeListener(freqEndVetoable);
		selFreqStart.addVetoableChangeListener(freqStartVetoable);
		selFreqQuick.addVetoableChangeListener(freqQuickVetoable);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
		rangeListener = propertyChangeListener;
		PropertyChangeListener wrap = evt -> {
			if (!applyingPreset)
				propertyChangeListener.propertyChange(evt);
		};
		selFreqStart.addPropertyChangeListener("value", wrap);
		selFreqEnd.addPropertyChangeListener("value", wrap);
	}

	/**
	 * Set start and end as one range so the sweep restarts once. Setting the
	 * digits separately used to queue two USB-resetting native restarts.
	 */
	void applyPreset(int startMHz, int endMHz) {
		applyingPreset = true;
		try {
			if (startMHz >= selFreqEnd.getValue())
				selFreqEnd.setValue(endMHz);
			selFreqStart.setValue(startMHz);
			selFreqEnd.setValue(endMHz);
		} finally {
			applyingPreset = false;
		}
		if (rangeListener != null)
			rangeListener.propertyChange(new PropertyChangeEvent(this, "value", null, getFrequencyRange()));
	}
	
	public FrequencyRange getFrequencyRange() {
		return new FrequencyRange(selFreqStart.getValue(), selFreqEnd.getValue());
	}
}
