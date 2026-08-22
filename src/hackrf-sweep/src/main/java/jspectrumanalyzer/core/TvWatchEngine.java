package jspectrumanalyzer.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;

import hackrfsweep.HackrfSweepLibrary;

/**
 * ATSC 1.0 watch: int8 IQ → native 8VSB → MPEG-TS → ffmpeg frames/PCM.
 * The libusb callback must only {@link #offerIq}.
 */
public final class TvWatchEngine
{
	public static final int QUEUE_CAP = 128;

	private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CAP);
	private final AtomicLong bytes = new AtomicLong();
	private final AtomicLong dropped = new AtomicLong();
	private final AtomicInteger volume = new AtomicInteger(80);
	private final MpegTsPlayer player = new MpegTsPlayer();
	private final IqSpectrum iqSpectrum = new IqSpectrum();
	private final WatchPreview preview = new WatchPreview();
	private volatile AudioSpectrum.FrameListener spectrumListener;
	private volatile boolean run;
	private volatile Pointer rx;
	private volatile boolean locked;
	private volatile float snrDb;
	private volatile int packets;
	private Consumer<BufferedImage> onFrame;
	private AudioSink sink;
	private Thread worker;
	private Thread previewWorker;
	private final AtomicReference<byte[]> previewIq = new AtomicReference<>();
	private long lastLogMs;
	private long lastPreviewMs;
	private long startMs;
	private volatile boolean sawPat;
	private FileOutputStream tsDump;
	private int dumpLeft;

	public synchronized void start(Consumer<BufferedImage> onFrame, AudioSink sink)
	{
		stop();
		this.onFrame = onFrame;
		this.sink = sink == null ? new RecordingAudioSink() : sink;
		bytes.set(0);
		dropped.set(0);
		locked = false;
		snrDb = 0;
		packets = 0;
		sawPat = false;
		lastLogMs = 0;
		iqSpectrum.reset();
		preview.reset();
		lastPreviewMs = 0;
		startMs = System.currentTimeMillis();
		previewIq.set(null);
		queue.clear();
		openDump();
		run = true;
		try
		{
			HackrfSweepLibrary.class.getName();
			rx = HackrfSweepLibrary.atsc_rx_create(TvChannelPlan.IQ_RATE_HZ);
		}
		catch (UnsatisfiedLinkError e)
		{
			System.err.println("ATSC watch: native 8VSB missing (" + e.getMessage() + ")");
			rx = null;
		}
		worker = new Thread(this::loop, "atsc-8vsb");
		worker.setDaemon(true);
		worker.setPriority(Thread.MAX_PRIORITY);
		worker.start();
		previewWorker = new Thread(this::previewLoop, "atsc-iq-preview");
		previewWorker.setDaemon(true);
		previewWorker.start();
	}

	public synchronized void stop()
	{
		run = false;
		if (worker != null)
		{
			worker.interrupt();
			try
			{
				worker.join(800);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			worker = null;
		}
		if (previewWorker != null)
		{
			previewWorker.interrupt();
			try
			{
				previewWorker.join(400);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			previewWorker = null;
		}
		previewIq.set(null);
		queue.clear();
		player.stop();
		closeDump();
		Pointer p = rx;
		rx = null;
		if (p != null)
		{
			try
			{
				HackrfSweepLibrary.atsc_rx_destroy(p);
			}
			catch (UnsatisfiedLinkError ignored)
			{
			}
		}
		AudioSink s = sink;
		sink = null;
		if (s != null)
			s.close();
		locked = false;
	}

	public void setVolume(int volume0to100)
	{
		int v = volume0to100;
		if (v < 0)
			v = 0;
		if (v > 100)
			v = 100;
		volume.set(v);
	}

	public boolean isRunning()
	{
		return run;
	}

	public boolean locked()
	{
		return locked;
	}

	public float snrDb()
	{
		return snrDb;
	}

	public int packets()
	{
		return packets;
	}

	public int frames()
	{
		return player.frames();
	}

	public int previewFrames()
	{
		return preview.frames();
	}

	public boolean hasPat()
	{
		return sawPat;
	}

	public void setSpectrumListener(AudioSpectrum.FrameListener listener)
	{
		this.spectrumListener = listener;
	}

	public IqSpectrum iqSpectrum()
	{
		return iqSpectrum;
	}

	public boolean offerIq(byte[] iq)
	{
		if (!run || iq == null || iq.length == 0)
			return false;
		bytes.addAndGet(iq.length);
		previewIq.set(iq);
		if (queue.offer(iq))
			return true;
		dropped.incrementAndGet();
		return false;
	}

	public long bytesOffered()
	{
		return bytes.get();
	}

	public long droppedChunks()
	{
		return dropped.get();
	}

	private void pushPreview(float[] row)
	{
		if (player.frames() > 0)
			return;
		long now = System.currentTimeMillis();
		if (now - lastPreviewMs < 33)
			return;
		lastPreviewMs = now;
		BufferedImage img = preview.pushDb(row);
		Consumer<BufferedImage> cb = onFrame;
		if (preview.frames() == 1)
			System.err.println("ATSC watch: IQ video frame 1 " + img.getWidth() + "x" + img.getHeight());
		if (cb != null)
			cb.accept(img);
	}

	private void startPlayer()
	{
		player.start(img -> {
			Consumer<BufferedImage> cb = this.onFrame;
			if (cb != null)
				cb.accept(img);
		}, pcm -> {
			AudioSink s = this.sink;
			if (s == null || pcm == null || pcm.length == 0)
				return;
			int vol = volume.get();
			if (vol < 100)
			{
				for (int i = 0; i < pcm.length; i++)
					pcm[i] = (short) (pcm[i] * vol / 100);
			}
			s.write(pcm, 0, pcm.length);
		});
	}

	private void loop()
	{
		byte[] ts = new byte[188 * 64];
		FloatByReference snr = new FloatByReference();
		while (run)
		{
			byte[] chunk;
			try
			{
				chunk = queue.poll(50, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				break;
			}
			if (chunk == null)
				continue;
			Pointer p = rx;
			int n = 0;
			if (p != null)
				n = HackrfSweepLibrary.atsc_rx_process(p, chunk, chunk.length, ts, ts.length, snr);
			if (p == null)
				continue;
			snrDb = snr.getValue();
			try
			{
				locked = HackrfSweepLibrary.atsc_rx_locked(p) != 0;
				packets = HackrfSweepLibrary.atsc_rx_packets(p);
			}
			catch (UnsatisfiedLinkError ignored)
			{
			}
			if (n >= 188)
			{
				int nbytes = n - (n % 188);
				dumpTs(ts, nbytes);
				if (!sawPat)
					sawPat = containsPat(ts, nbytes);
				if (sawPat)
				{
					if (!player.running())
						startPlayer();
					player.writeTs(ts, nbytes);
				}
			}
			long now = System.currentTimeMillis();
			if (now - lastLogMs > 2000)
			{
				lastLogMs = now;
				System.err.println("ATSC watch: locked=" + locked + " packets=" + packets + " snr="
						+ String.format(java.util.Locale.US, "%.1f", snrDb) + " dB dropped="
						+ dropped.get() + " pat=" + sawPat + " frames=" + player.frames()
						+ " preview=" + preview.frames());
			}
		}
	}

	private void previewLoop()
	{
		while (run)
		{
			byte[] chunk = previewIq.getAndSet(null);
			if (chunk == null)
			{
				try
				{
					Thread.sleep(15);
				}
				catch (InterruptedException e)
				{
					break;
				}
				continue;
			}
			float[] row = iqSpectrum.accept(chunk, chunk.length);
			if (row == null)
				continue;
			AudioSpectrum.FrameListener spec = spectrumListener;
			if (spec != null)
				spec.onFrame(row);
			pushPreview(row);
		}
	}

	private void openDump()
	{
		closeDump();
		if (!Boolean.getBoolean("hackrf.atsc.dump"))
			return;
		dumpLeft = 2 * 1024 * 1024;
		try
		{
			File f = new File(System.getProperty("java.io.tmpdir"), "hackrf-atsc.ts");
			tsDump = new FileOutputStream(f);
			System.err.println("ATSC watch: dumping TS to " + f.getAbsolutePath());
		}
		catch (IOException e)
		{
			tsDump = null;
		}
	}

	private void dumpTs(byte[] ts, int n)
	{
		FileOutputStream out = tsDump;
		if (out == null || dumpLeft <= 0)
			return;
		int w = Math.min(n, dumpLeft);
		try
		{
			out.write(ts, 0, w);
			dumpLeft -= w;
			if (dumpLeft <= 0)
				closeDump();
		}
		catch (IOException e)
		{
			closeDump();
		}
	}

	private void closeDump()
	{
		FileOutputStream out = tsDump;
		tsDump = null;
		if (out != null)
		{
			try
			{
				out.close();
			}
			catch (IOException ignored)
			{
			}
		}
	}

	static boolean containsPat(byte[] ts, int n)
	{
		if (ts == null)
			return false;
		int lim = n - (n % 188);
		for (int i = 0; i + 188 <= lim; i += 188)
		{
			if (ts[i] != 0x47)
				continue;
			int pid = ((ts[i + 1] & 0x1f) << 8) | (ts[i + 2] & 0xff);
			if (pid == 0)
				return true;
		}
		return false;
	}
}
