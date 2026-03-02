package tankpack.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BashCmd
{
	private final boolean verbose;
	
	public BashCmd(boolean verbose)
	{
		this.verbose = verbose;
	}
	
	// Record returned by executeBashCommand. stdOut and exitCode are null if waitForCompletion = false
	public record BashResult(Process process, List<String> stdout, Integer exitCode)
	{}
	
	// Reusable function to execute a bash command in verbose mode
	public BashResult executeBashCommand(String command, boolean waitForCompletion)
			throws IOException, InterruptedException
	{
		// Announce command to be executed
		if (verbose)
			System.out.println("+ " + command);
		
		// Run command, add ', "-x"' to run in verbose mode
		String[] cmdArray = { "bash", "-c", command };
		Process process = Runtime.getRuntime().exec(cmdArray);
		
		if (waitForCompletion)
		{
			List<String> stdoutLines = new ArrayList<>();
			
			// Read stdOut and stdErr during process execution to prevent buffer overflow
			BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			
			// Read stdOut on the go, readLine() is blocking and will terminate when process finishes
			String line;
			while ((line = stdout.readLine()) != null)
			{
				stdoutLines.add(line);
				if (verbose)
					System.out.println(line);
			}
			
			// Print stdErr to Java's stdErr
			while ((line = stderr.readLine()) != null)
				System.err.println("Error: " + line);
			
			// Wait for the process to complete
			int exitCode = process.waitFor();
			if (exitCode != 0)
				System.err.println("Command '" + command + "' exited with code: " + exitCode);
			
			// Close streams
			stdout.close();
			stderr.close();
			
			return new BashResult(process, stdoutLines, exitCode);
		}
		else
		{
			// Do not wait for completion, instead create a thread to consume stdOut and stdErr, preventing a buffer
			// overflow/deadlock. Threads exits and streams close when process completes / is killed
			
			// stdOut: print if verbose, otherwise discard
			new Thread(() -> {
				// By using "try with resources" (BufferedReader), we do not need to close after
				try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream())))
				{
					String line;
					while ((line = stdout.readLine()) != null)
					{
						if (verbose)
						{
							System.out.println(line);
						}
					}
				}
				catch (IOException ignored)
				{
				}
			}).start();
			
			// stdErr: always print
			new Thread(() -> {
				try (BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream())))
				{
					String line;
					while ((line = stderr.readLine()) != null)
					{
						System.err.println("+ Error: " + line);
					}
				}
				catch (IOException ignored)
				{
				}
			}).start();
			
			return new BashResult(process, null, null);
		}
	}
}
