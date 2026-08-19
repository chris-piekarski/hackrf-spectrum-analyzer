package jspectrumanalyzer.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import jspectrumanalyzer.core.AnalyzerSettings;
import jspectrumanalyzer.core.DatasetSpectrum;
import jspectrumanalyzer.core.FrequencyRange;

class SpectrumMcpToolsTest {

	private static SpectrumMcpTools toolsWithSweep() {
		SpectrumSnapshotStore store = new SpectrumSnapshotStore();
		DatasetSpectrum ds = new DatasetSpectrum(100_000f, 2402, 2472, -150f);
		for (int i = 0; i < ds.spectrumLength(); i++)
			ds.getSpectrumArray()[i] = -70f;
		ds.getSpectrumArray()[3] = -25f;
		store.publishSweep(SpectrumSnapshot.fromDataset(ds, 9L, 500, null), 9L);
		AnalyzerSettings settings = new AnalyzerSettings();
		settings.getFrequency().setValue(new FrequencyRange(2402, 2472));
		settings.getFFTBinHz().setValue(100000);
		settings.isPowerAutoScale().setValue(true);
		store.publishContext(settings, java.util.List.of(), 10.0);
		return new SpectrumMcpTools(store);
	}

	@Test
	void emptyStoreSummaryIsAnErrorPayload() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		String rpc = tools.handleRpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_summary\"}}");
		assertTrue(rpc.contains("no sweep yet"));
		assertTrue(rpc.contains("\"isError\":true"));
	}

	@Test
	void toolsListAndInitializeAreValidJsonRpc() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		String init = tools.handleRpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
		assertTrue(init.contains("hackrf-spectrum-analyzer"));
		assertTrue(init.contains("2024-11-05"));
		String list = tools.handleRpc("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
		assertTrue(list.contains("spectrum_snapshot"));
		assertTrue(list.contains("spectrum_summary"));
		assertTrue(list.contains("radio_identity"));
		assertTrue(list.contains("sweep_config"));
		assertTrue(list.contains("fm_stations"));
		assertNull(tools.handleRpc("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
	}

	@Test
	void snapshotAndSummaryMatchKnownBins() {
		SpectrumMcpTools tools = toolsWithSweep();
		String snap = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_snapshot\"}}");
		assertTrue(snap.contains("peakDbm"));
		assertTrue(snap.contains("-25"));
		assertFalse(snap.contains("-150"));
		String sum = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"spectrum_summary\"}}");
		assertTrue(sum.contains("2402"));
		assertTrue(sum.contains("2472"));
	}

	@Test
	void sweepConfigSplitsRadioAndDisplay() {
		SpectrumMcpTools tools = toolsWithSweep();
		String cfg = tools.call("sweep_config", Map.of());
		assertTrue(cfg.contains("radio"));
		assertTrue(cfg.contains("display"));
		assertTrue(cfg.contains("autoScale"));
		assertTrue(cfg.contains("fftBinHz"));
		assertTrue(cfg.contains("100000"));
		assertFalse(cfg.contains("persistent"));
	}

	@Test
	void unknownToolIsJsonRpcError() {
		SpectrumMcpTools tools = new SpectrumMcpTools(new SpectrumSnapshotStore());
		String rpc = tools.handleRpc(
				"{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\"explode\"}}");
		assertTrue(rpc.contains("\"error\""));
		assertTrue(rpc.contains("unknown tool"));
	}

	@Test
	void contentLengthRoundTrip() throws Exception {
		java.io.StringReader in = new java.io.StringReader(
				"Content-Length: 55\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}");
		java.io.BufferedReader reader = new java.io.BufferedReader(in);
		String msg = SpectrumMcpServer.readMessage(reader);
		assertTrue(msg.contains("tools/list"));
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.BufferedWriter w = new java.io.BufferedWriter(sw);
		SpectrumMcpServer.writeMessage(w, "{\"ok\":true}");
		assertTrue(sw.toString().startsWith("Content-Length:"));
		assertTrue(sw.toString().contains("{\"ok\":true}"));
	}
}
