package jspectrumanalyzer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class MpegTsPlayerTest {

	@Test
	void decodesMpegTsToAFrameWhenFfmpegExists() throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(ffmpegOk(), "ffmpeg on PATH");
		Path ts = Files.createTempFile("atsc-test", ".ts");
		try
		{
			Process gen = new ProcessBuilder("ffmpeg", "-y", "-f", "lavfi", "-i",
					"testsrc=size=640x360:rate=15", "-f", "lavfi", "-i",
					"sine=frequency=1000:sample_rate=48000", "-t", "2", "-c:v", "mpeg2video", "-b:v",
					"800k", "-g", "15", "-c:a", "mp2", "-f", "mpegts", ts.toString())
					.redirectErrorStream(true).start();
			gen.getInputStream().readAllBytes();
			org.junit.jupiter.api.Assumptions.assumeTrue(gen.waitFor() == 0, "ffmpeg muxed a TS");
			byte[] data = Files.readAllBytes(ts);
			assertTrue(data.length > 188);

			CountDownLatch frame = new CountDownLatch(1);
			MpegTsPlayer player = new MpegTsPlayer();
			player.start(img -> {
				if (img != null && img.getWidth() == MpegTsPlayer.WIDTH)
					frame.countDown();
			}, pcm -> {
			});
			assertTrue(player.running());
			Thread.sleep(150);
			int off = 0;
			while (off < data.length)
			{
				int n = Math.min(188 * 8, data.length - off);
				byte[] slice = new byte[n];
				System.arraycopy(data, off, slice, 0, n);
				player.writeTs(slice, n);
				off += n;
			}
			boolean got = frame.await(8, TimeUnit.SECONDS);
			player.stop();
			assertFalse(player.running());
			assertTrue(got, "expected a 640x360 frame from ffmpeg");
		}
		finally
		{
			Files.deleteIfExists(ts);
		}
	}

	private static boolean ffmpegOk() {
		try
		{
			Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
			p.getInputStream().readAllBytes();
			return p.waitFor() == 0;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
