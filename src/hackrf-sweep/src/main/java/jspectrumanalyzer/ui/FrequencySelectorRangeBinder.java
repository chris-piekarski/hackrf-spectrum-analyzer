package jspectrumanalyzer.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.HackRFSettings;

/**
 * Limits frequency selection of two selectors (start/end)
 */
public class FrequencySelectorRangeBinder
{
	public FrequencySelectorPanel selFreqStart, selFreqEnd;
	public QuickFrequencySelectorPanel selFreqQuick;

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
			String newVal = (String) evt.getNewValue();
			switch(newVal) {
				case "WiFi 2":
					selFreqStart.setValue(2401);
					selFreqEnd.setValue(2495);
					break;
				case "WiFi 5":
					selFreqStart.setValue(5030);
					selFreqEnd.setValue(5875);
					break;
				case "LTE-1":
					//covers bands B1-4,9-11,21,24,25,65,66,70
					selFreqStart.setValue(1890);
					selFreqEnd.setValue(2200);
					break;
				case "LTE-2":
					//covers bands B5,6,8,12,13,14,17,18,19,20
					//26,27,28,71
					selFreqStart.setValue(663);
					selFreqEnd.setValue(915);
					break;
				case "FM":
         				selFreqStart.setValue(88);
					selFreqEnd.setValue(108);
					break;
				//HackRF operating range starts at 1MHz
				//case "AM":
				//	selFreqStart.setValue(0.55);
				//	selFreqEnd.setValue(1.6);
				//	break;
				case "NFC":
					selFreqStart.setValue(13);
					selFreqEnd.setValue(14);
					break;
				case "HF":
					selFreqStart.setValue(3);
					selFreqEnd.setValue(30);
					break;
				case "VHF":
					selFreqStart.setValue(30);
					selFreqEnd.setValue(300);
					break;
				case "UHF":
					selFreqStart.setValue(300);
					selFreqEnd.setValue(3000);
					break;
				case "V-TV":
					selFreqStart.setValue(54);
					selFreqEnd.setValue(216);
					break;
				case "U-TV":
					selFreqStart.setValue(470);
					selFreqEnd.setValue(890);
					break;

			}
		};


		selFreqEnd.addVetoableChangeListener(freqEndVetoable);
		selFreqStart.addVetoableChangeListener(freqStartVetoable);
		selFreqQuick.addVetoableChangeListener(freqQuickVetoable);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
		selFreqStart.addPropertyChangeListener("value", propertyChangeListener);
		selFreqEnd.addPropertyChangeListener("value", propertyChangeListener);
	}
	
	public FrequencyRange getFrequencyRange() {
		return new FrequencyRange(selFreqStart.getValue(), selFreqEnd.getValue());
	}
}
