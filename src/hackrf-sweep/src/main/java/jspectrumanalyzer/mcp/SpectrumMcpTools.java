package jspectrumanalyzer.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP tool schemas and handlers. No Swing, no USB.
 */
public final class SpectrumMcpTools
{
	public static final String PROTOCOL = "2024-11-05";
	public static final String SERVER_NAME = "hackrf-spectrum-analyzer";
	public static final String SERVER_VERSION = "2.0";

	private final SpectrumSnapshotStore store;

	public SpectrumMcpTools(SpectrumSnapshotStore store)
	{
		if (store == null)
			throw new IllegalArgumentException("store");
		this.store = store;
	}

	public SpectrumSnapshotStore store()
	{
		return store;
	}

	public String initializeResult()
	{
		return "{\"protocolVersion\":\"" + PROTOCOL + "\",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\""
				+ SERVER_NAME + "\",\"version\":\"" + SERVER_VERSION + "\"}}";
	}

	public String toolsListResult()
	{
		return "{\"tools\":[" + tool("spectrum_snapshot",
				"Latest filled-bin sweep (hop holes omitted). Optional maxPoints and minDbm.",
				"{\"type\":\"object\",\"properties\":{\"maxPoints\":{\"type\":\"integer\",\"minimum\":1},"
						+ "\"minDbm\":{\"type\":\"number\"}}}")
				+ ","
				+ tool("spectrum_summary",
						"Noise floor, peak, span, pause/released, and sweep rate for the current window.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("radio_identity", "Attached radio board, short serial, firmware, and USB API.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("sweep_config",
						"Armed radio settings (range, FFT, gain, CLKOUT) plus display flags (peaks, auto-scale).",
						"{\"type\":\"object\",\"properties\":{}}")
				+ ","
				+ tool("fm_stations",
						"Live FM dial hits for an FM-scale view, or an empty list when zoomed out.",
						"{\"type\":\"object\",\"properties\":{}}")
				+ "]}";
	}

	public String call(String name, Map<String, Object> args)
	{
		if (name == null)
			throw new IllegalArgumentException("missing tool name");
		if (args == null)
			args = Map.of();
		if ("spectrum_snapshot".equals(name))
			return snapshotCall(args);
		if ("spectrum_summary".equals(name))
			return textResult(store.latest().toSummaryJson(store.context()), store.latest().isEmpty());
		if ("radio_identity".equals(name))
			return textResult(store.context().identityJson(), false);
		if ("sweep_config".equals(name))
			return textResult(store.context().sweepConfigJson(), false);
		if ("fm_stations".equals(name))
			return textResult(store.context().fmStationsJson(), false);
		throw new IllegalArgumentException("unknown tool: " + name);
	}

	public String handleRpc(String json)
	{
		Map<String, Object> req;
		try
		{
			req = McpJson.parseObject(json);
		}
		catch (RuntimeException e)
		{
			return McpJson.rpcError(null, -32700, "parse error");
		}
		Object id = req.get("id");
		String method = McpJson.getString(req, "method");
		if (method == null)
			return McpJson.rpcError(id, -32600, "missing method");
		if (method.startsWith("notifications/"))
			return null;
		if ("initialize".equals(method) || "ping".equals(method))
			return McpJson.rpcResult(id, "initialize".equals(method) ? initializeResult() : "{}");
		if ("tools/list".equals(method))
			return McpJson.rpcResult(id, toolsListResult());
		if ("tools/call".equals(method))
		{
			Map<String, Object> params = McpJson.getObject(req, "params");
			String name = McpJson.getString(params, "name");
			Map<String, Object> arguments = McpJson.getObject(params, "arguments");
			if (arguments == null)
				arguments = new LinkedHashMap<String, Object>();
			try
			{
				return McpJson.rpcResult(id, call(name, arguments));
			}
			catch (IllegalArgumentException e)
			{
				return McpJson.rpcError(id, -32601, e.getMessage());
			}
		}
		return McpJson.rpcError(id, -32601, "method not found: " + method);
	}

	private String snapshotCall(Map<String, Object> args)
	{
		SpectrumSnapshot latest = store.latest();
		Integer max = McpJson.getInt(args, "maxPoints");
		Double min = McpJson.getDouble(args, "minDbm");
		if (max == null && min == null)
			return textResult(latest.toJson(), latest.isEmpty());
		// Re-filter from the stored points (already hole-stripped).
		int cap = max == null ? latest.mhz.length : Math.max(1, max.intValue());
		return textResult(downsampleStored(latest, cap, min == null ? null : min.floatValue()).toJson(),
				latest.isEmpty());
	}

	static SpectrumSnapshot downsampleStored(SpectrumSnapshot src, int maxPoints, Float minDbm)
	{
		if (src == null || src.isEmpty())
			return src == null ? SpectrumSnapshot.empty(0L) : src;
		int n = src.mhz.length;
		float[] m = new float[Math.min(n, maxPoints)];
		float[] d = new float[m.length];
		int out = 0;
		if (n <= maxPoints)
		{
			for (int i = 0; i < n; i++)
			{
				if (minDbm != null && src.dbm[i] < minDbm.floatValue())
					continue;
				if (out < m.length)
				{
					m[out] = src.mhz[i];
					d[out] = src.dbm[i];
					out++;
				}
			}
		}
		else
		{
			for (int p = 0; p < maxPoints; p++)
			{
				int i0 = (int) ((long) p * n / maxPoints);
				int i1 = Math.max(i0 + 1, (int) ((long) (p + 1) * n / maxPoints));
				float peak = Float.NEGATIVE_INFINITY;
				float xAt = src.mhz[i0];
				boolean any = false;
				for (int i = i0; i < i1 && i < n; i++)
				{
					if (minDbm != null && src.dbm[i] < minDbm.floatValue())
						continue;
					any = true;
					if (src.dbm[i] > peak)
					{
						peak = src.dbm[i];
						xAt = src.mhz[i];
					}
				}
				if (!any)
					continue;
				m[out] = xAt;
				d[out] = peak;
				out++;
			}
		}
		float[] mo = new float[out];
		float[] do_ = new float[out];
		System.arraycopy(m, 0, mo, 0, out);
		System.arraycopy(d, 0, do_, 0, out);
		return new SpectrumSnapshot(src.timestampMs, src.startMHz, src.endMHz, src.fftBinHz, mo, do_, src.filledBins,
				src.omittedHoles, src.noiseDbm, src.peakDbm, src.peakMhz);
	}

	private static String textResult(String json, boolean isError)
	{
		String escaped = SpectrumSnapshot.Json.quote(json);
		return "{\"content\":[{\"type\":\"text\",\"text\":" + escaped + "}]" + (isError ? ",\"isError\":true" : "")
				+ "}";
	}

	private static String tool(String name, String description, String schema)
	{
		return "{\"name\":\"" + name + "\",\"description\":" + SpectrumSnapshot.Json.quote(description)
				+ ",\"inputSchema\":" + schema + "}";
	}
}
