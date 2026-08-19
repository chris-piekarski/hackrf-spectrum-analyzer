package jspectrumanalyzer.mcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * MCP over stdio (Content-Length or one JSON object per line) or a
 * localhost TCP accept loop. I/O never runs on the Swing EDT.
 */
public final class SpectrumMcpServer
{
	public static final int DEFAULT_PORT = 8765;

	private final SpectrumMcpTools tools;
	private volatile boolean stop;
	private volatile ServerSocket server;

	public SpectrumMcpServer(SpectrumSnapshotStore store)
	{
		this.tools = new SpectrumMcpTools(store);
	}

	public SpectrumMcpTools tools()
	{
		return tools;
	}

	public String handle(String requestJson)
	{
		return tools.handleRpc(requestJson);
	}

	public void stop()
	{
		stop = true;
		ServerSocket s = server;
		if (s != null)
		{
			try
			{
				s.close();
			}
			catch (IOException ignored)
			{
			}
		}
	}

	public void runStdio() throws IOException
	{
		runStreams(System.in, System.out);
	}

	public void runStreams(InputStream in, OutputStream out) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
		while (!stop)
		{
			String msg = readMessage(reader);
			if (msg == null)
				return;
			if (msg.isEmpty())
				continue;
			String reply = handle(msg);
			if (reply != null)
				writeMessage(writer, reply);
		}
	}

	public Thread startLocalhost(int port) throws IOException
	{
		server = new ServerSocket(port, 8, InetAddress.getByName("127.0.0.1"));
		Thread t = new Thread(() -> {
			while (!stop)
			{
				try
				{
					Socket sock = server.accept();
					Thread client = new Thread(() -> {
						try
						{
							runStreams(sock.getInputStream(), sock.getOutputStream());
						}
						catch (IOException ignored)
						{
						}
						finally
						{
							try
							{
								sock.close();
							}
							catch (IOException ignored)
							{
							}
						}
					}, "spectrum-mcp-client");
					client.setDaemon(true);
					client.start();
				}
				catch (IOException e)
				{
					if (!stop)
						e.printStackTrace();
				}
			}
		}, "spectrum-mcp-listen");
		t.setDaemon(true);
		t.start();
		System.err.println("Spectrum MCP listening on 127.0.0.1:" + port);
		return t;
	}

	static String readMessage(BufferedReader reader) throws IOException
	{
		String first = reader.readLine();
		if (first == null)
			return null;
		if (first.isEmpty())
			return readMessage(reader);
		if (first.startsWith("{"))
			return first;
		int contentLength = -1;
		String line = first;
		while (line != null && !line.isEmpty())
		{
			int colon = line.indexOf(':');
			if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Length"))
			{
				try
				{
					contentLength = Integer.parseInt(line.substring(colon + 1).trim());
				}
				catch (NumberFormatException e)
				{
					contentLength = -1;
				}
			}
			line = reader.readLine();
		}
		if (contentLength < 0)
			return first.startsWith("{") ? first : "";
		char[] buf = new char[contentLength];
		int n = 0;
		while (n < contentLength)
		{
			int r = reader.read(buf, n, contentLength - n);
			if (r < 0)
				break;
			n += r;
		}
		return new String(buf, 0, n);
	}

	static void writeMessage(BufferedWriter writer, String json) throws IOException
	{
		byte[] raw = json.getBytes(StandardCharsets.UTF_8);
		writer.write("Content-Length: ");
		writer.write(Integer.toString(raw.length));
		writer.write("\r\n\r\n");
		writer.write(json);
		writer.flush();
	}
}
