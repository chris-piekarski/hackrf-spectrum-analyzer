package jspectrumanalyzer.core;

import java.util.Locale;

/**
 * Exclusive USB use: one HackRF is either sweeping, parked as a WFM
 * receiver, or released.
 */
public enum RadioMode
{
	SWEEP, LISTEN, STOPPED;

	public String jsonName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	public static RadioMode from(boolean released, boolean listening)
	{
		if (released)
			return STOPPED;
		if (listening)
			return LISTEN;
		return SWEEP;
	}
}
