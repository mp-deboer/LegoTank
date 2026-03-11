package tankpack.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class BashCmd
{
	private Logger logger;
	
	public BashCmd(Logger log)
	{
		// Initialise BashCmd logger with unique name and same log-level as calling object
		logger = LogManager.getLogger(log.getName() + this.getClass().getSimpleName());
		Configurator.setLevel(logger.getName(), log.getLevel());
	}
	
	// Record returned by executeBashCommand. stdOut and exitCode are null if waitForCompletion = false
	public record BashResult(Process process, List<String> stdout, Integer exitCode)
	{}
	
	// Reusable function to execute a bash command in verbose mode
	public BashResult executeBashCommand(String command, boolean waitForCompletion)
			throws IOException, InterruptedException
	{
		// Announce command to be executed
		logger.debug("+ " + command);
		
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
				logger.debug(line);
			}
			
			// Print stdErr to Java's stdErr
			while ((line = stderr.readLine()) != null)
				logger.error(line);
			
			// Wait for the process to complete
			int exitCode = process.waitFor();
			if (exitCode != 0)
				logger.error("Command '" + command + "' exited with code: " + exitCode);
			
			// Close streams
			stdout.close();
			stderr.close();
			
			return new BashResult(process, stdoutLines, exitCode);
		}
		else
		{
			// Do not wait for completion, instead create threads to consume stdOut and stdErr, preventing a buffer
			// overflow/deadlock. Threads exit and streams close when process completes / is killed
			
			// Create thread reading process output
			new Thread(() -> readProcessOutput(process), "OutputReader").start();
			
			// Create thread reading process error messages
			new Thread(() -> readProcessErrors(process), "ErrorReader").start();
			
			return new BashResult(process, null, null);
		}
	}
	
	private void readProcessOutput(Process process)
	{
		// stdOut: log with debug level
		try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream())))
		{
			String line;
			while ((line = stdout.readLine()) != null)
				logger.debug(line);
		}
		catch (IOException ignored)
		{
		}
	}
	
	private void readProcessErrors(Process process)
	{
		// stdErr: log with error level (always prints)
		try (BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream())))
		{
			String line;
			while ((line = stderr.readLine()) != null)
				logger.error(line);
		}
		catch (IOException ignored)
		{
		}
	}
}
