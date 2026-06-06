package jspectrumanalyzer.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.io.*;

public class QuickFrequencySelectorPanel extends JPanel
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -4830755053319335365L;
	private String			value			= "WiFi 2G";

	/**
	 * Create the panel.
	 */
	public QuickFrequencySelectorPanel()
	{
		setBackground(Color.BLACK);

		setLayout(new GridLayout(4, 3, 0, 0));

		JButton button_wifi2 = new JButton("WiFi 2");
		button_wifi2.setBackground(Color.BLACK);
		add(button_wifi2);

		JButton button_wifi5 = new JButton("WiFi 5");
		button_wifi5.setBackground(Color.BLACK);
		add(button_wifi5);

		JButton button_lte = new JButton("LTE-1");
		button_lte.setBackground(Color.BLACK);
		add(button_lte);

		JButton button_lte2 = new JButton("LTE-2");
		button_lte2.setBackground(Color.BLACK);
		add(button_lte2);


		JButton button_nfc = new JButton("NFC");
		button_nfc.setBackground(Color.BLACK);
		add(button_nfc);

		JButton button_fm = new JButton("FM");
		button_fm.setBackground(Color.BLACK);
		add(button_fm);

		JButton button_hf = new JButton("HF");
		button_hf.setBackground(Color.BLACK);
		add(button_hf);

		JButton button_vhf = new JButton("VHF");
		button_vhf.setBackground(Color.BLACK);
		add(button_vhf);

		JButton button_uhf = new JButton("UHF");
		button_uhf.setBackground(Color.BLACK);
		add(button_uhf);

		JButton button_vtv = new JButton("V-TV");
		button_vtv.setBackground(Color.BLACK);
		add(button_vtv);

		JButton button_utv = new JButton("U-TV");
		button_utv.setBackground(Color.BLACK);
		add(button_utv);


		button_wifi2.addActionListener(addListener("WiFi 2"));
		button_wifi5.addActionListener(addListener("WiFi 5"));
		button_lte.addActionListener(addListener("LTE-1"));
		button_lte2.addActionListener(addListener("LTE-2"));
		button_nfc.addActionListener(addListener("NFC"));
		button_fm.addActionListener(addListener("FM"));
		button_hf.addActionListener(addListener("HF"));
		button_vhf.addActionListener(addListener("VHF"));
		button_uhf.addActionListener(addListener("UHF"));
		button_vtv.addActionListener(addListener("V-TV"));
		button_utv.addActionListener(addListener("U-TV"));

		Dimension d = new Dimension(300, 100);
		setPreferredSize(d);
		setMaximumSize(d);
		setMinimumSize(d);
	}

	public String getValue()
	{
		return value;
	}

	private ActionListener addListener(String type)
	{
		ActionListener listener = e -> {
			System.out.println("quick link click: "+type);
			try {
				if (type != value) {
					fireValueChange(value, type);
				}
			}
			catch (PropertyVetoException ee)
			{
				System.out.println("Failed to set quick selection");
			}		
		};
		return listener;
	}

	private void fireValueChange(String oldValue, String newValue) throws PropertyVetoException
	{
		fireVetoableChange("value", oldValue, newValue);
		QuickFrequencySelectorPanel.this.value = newValue;
		firePropertyChange("value", oldValue, newValue);
	}

}
