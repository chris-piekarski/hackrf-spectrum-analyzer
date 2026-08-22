package jspectrumanalyzer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class TvWatchEngineTest {

	@Test
	void startStopWithoutNativeDoesNotThrow() {
		TvWatchEngine engine = new TvWatchEngine();
		AtomicInteger frames = new AtomicInteger();
		assertDoesNotThrow(() -> engine.start(img -> frames.incrementAndGet(), new RecordingAudioSink()));
		assertTrue(engine.isRunning());
		assertFalse(engine.locked());
		engine.offerIq(new byte[16]);
		engine.offerIq(null);
		engine.setVolume(40);
		engine.setVolume(-1);
		engine.setVolume(200);
		engine.stop();
		assertFalse(engine.isRunning());
		assertEquals(0, frames.get());
	}

	@Test
	void containsPatDetectsPid0() {
		byte[] ts = new byte[188];
		ts[0] = 0x47;
		ts[2] = 0x10; // PID 16, not PAT
		assertFalse(TvWatchEngine.containsPat(ts, 188));
		ts[1] = 0x40;
		ts[2] = 0x00;
		assertTrue(TvWatchEngine.containsPat(ts, 188));
		assertFalse(TvWatchEngine.containsPat(ts, 0));
	}

	@Test
	void spectrumListenerGetsARowFromIq() throws Exception {
		TvWatchEngine engine = new TvWatchEngine();
		java.util.concurrent.CountDownLatch row = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch frame = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.atomic.AtomicReference<java.awt.image.BufferedImage> img = new java.util.concurrent.atomic.AtomicReference<>();
		engine.setSpectrumListener(db -> {
			if (db != null && db.length == IqSpectrum.FFT_N)
				row.countDown();
		});
		engine.start(got -> {
			if (got != null && got.getWidth() == WatchPreview.WIDTH)
			{
				img.set(got);
				frame.countDown();
			}
		}, new RecordingAudioSink());
		byte[] iq = new byte[IqSpectrum.FFT_N * 2];
		for (int i = 0; i < IqSpectrum.FFT_N; i++)
			iq[2 * i] = 40;
		assertTrue(engine.offerIq(iq));
		assertTrue(row.await(2, java.util.concurrent.TimeUnit.SECONDS), "IQ FFT row");
		assertTrue(frame.await(2, java.util.concurrent.TimeUnit.SECONDS), "IQ video frame");
		assertEquals(WatchPreview.HEIGHT, img.get().getHeight());
		assertTrue(engine.previewFrames() >= 1);
		engine.stop();
	}

	@Test
	void previewKeepsUpdatingWhenDemodQueueIsFull() throws Exception {
		TvWatchEngine engine = new TvWatchEngine();
		java.util.concurrent.CountDownLatch frame = new java.util.concurrent.CountDownLatch(1);
		engine.start(got -> {
			if (got != null && got.getWidth() == WatchPreview.WIDTH)
				frame.countDown();
		}, new RecordingAudioSink());
		byte[] iq = new byte[IqSpectrum.FFT_N * 2];
		for (int i = 0; i < IqSpectrum.FFT_N; i++)
			iq[2 * i] = 40;
		for (int i = 0; i < TvWatchEngine.QUEUE_CAP + 8; i++)
			engine.offerIq(iq);
		assertTrue(frame.await(2, java.util.concurrent.TimeUnit.SECONDS), "IQ video while demod is backed up");
		engine.stop();
	}

	@Test
	void offerIqIgnoredWhenStopped() {
		TvWatchEngine engine = new TvWatchEngine();
		assertFalse(engine.offerIq(new byte[4]));
		engine.start(img -> {
		}, new RecordingAudioSink());
		assertTrue(engine.offerIq(new byte[8]));
		engine.stop();
		assertFalse(engine.offerIq(new byte[4]));
	}
}
