#!/usr/bin/env dotnet-script
#r "nuget: StateSmith, 0.19.0"

using StateSmith.Common;
using StateSmith.Input.Expansions;
using StateSmith.Output.UserConfig;
using StateSmith.Runner;
using System.IO;
using System.Text.RegularExpressions;

bool debug = false;
bool forceWrite = false;

string currentDirectory = Directory.GetCurrentDirectory();

// Get relative path ../src/tankpack (from csx file directory)
string targetPath = Path.GetFullPath(Path.Combine(currentDirectory, "..", "src", "tankpack"));

// Generate code for all state machines automatically
var drawioFiles = Directory.GetFiles(currentDirectory, "*.drawio.svg");

foreach (var fullPath in drawioFiles)
{
    // Strip .svg, then .drawio
    string baseName = Path.GetFileNameWithoutExtension(Path.GetFileNameWithoutExtension(fullPath));
    GenerateStateMachineCode(baseName);
}

void GenerateStateMachineCode(string ssName)
{
    string drawioFileName = ssName + ".drawio.svg";
    string javaFileName = ssName + ".java";
    
    // Get full paths (adjust if Draw.io path differs from current directory)
    string generatedFilePath = Path.Combine(targetPath, javaFileName);
    string drawioFullPath = Path.GetFullPath(drawioFileName);
    
    // Check if Java file needs regeneration
    if (forceWrite || !File.Exists(generatedFilePath) ||
        File.GetLastWriteTime(generatedFilePath) < File.GetLastWriteTime(drawioFullPath))
    {
		SmRunner runner = new(diagramPath: drawioFileName);
		runner.Settings.outputDirectory = targetPath;
        runner.Run();
		InsertGetAllowedEventsFunction(generatedFilePath);
    }
}

void InsertGetAllowedEventsFunction(string filePath)
{
	// Extract allowed events per state from generated function dispatchEvent
	string sourceDispatchEvent = ExtractDispatchEvent(File.ReadAllText(filePath));
	
	StringBuilder sb = new StringBuilder();
	sb.AppendLine("////////////////////////////////////////////////////////////////////////////////");
	sb.AppendLine("// Inserted code by csx script");
	sb.AppendLine("////////////////////////////////////////////////////////////////////////////////");
	sb.AppendLine();
	
	sb.AppendLine("protected String[] getAllowedEvents(StateId id) {");
	sb.AppendLine("    return switch (id) {");
	
	List<string> states = ExtractStates(sourceDispatchEvent);
	foreach (string state in states)
	{
		sb.Append("        case " + state + " -> new String[");
		
		// Skip if ROOT
		if (!state.Equals("ROOT"))
		{
			string stateSwitchCase = ExtractStateSwitchCase(sourceDispatchEvent, state) ?? "case DO:"; // if there is no switch case, artificially add DO, as this event is always allowed
		
			if (debug) Console.WriteLine("stateSwitchCase:\n" + stateSwitchCase + "\n-----");
			
			List<string> events = ExtractEvents(stateSwitchCase);
			if (events.Count > 0)
			{
				sb.Append("]{\"");
				sb.Append(events[0] + "\"");
				
				// todo if length is >2 add comma
				if (events.Count > 1)
				{
					for (int i = 1; i < events.Count; i++)
					{
						sb.Append(", \"" + events[i] + "\"");
					}
				}
				sb.AppendLine("};");
			}
			else
			{
				sb.AppendLine("0];  // No events");
			}
		}
		else
		{
			sb.AppendLine("0];  // No events");
		}
	}
	sb.AppendLine("        default -> new String[0];");
	sb.AppendLine("    };");
	sb.AppendLine("}");
	sb.AppendLine();
	
	// Add functions required in order to extend StateMachine
	sb.AppendLine("@Override");
	sb.AppendLine("protected String[] getAllEventNames()");
	sb.AppendLine("{");
	sb.AppendLine("    EventId[] values = EventId.values();");
	sb.AppendLine("    String[] names = new String[values.length];");
	sb.AppendLine("    ");
	sb.AppendLine("    for (int i = 0; i < values.length; i++)");
	sb.AppendLine("        names[i] = translateEventName(values[i].name());");
	sb.AppendLine("    ");
	sb.AppendLine("    return names;");
	sb.AppendLine("}");
	sb.AppendLine();
	
	sb.AppendLine("@Override");
	sb.AppendLine("protected String[] getAllStateNames()");
	sb.AppendLine("{");
	sb.AppendLine("    StateId[] values = StateId.values();");
	sb.AppendLine("    String[] names = new String[values.length];");
	sb.AppendLine("    ");
	sb.AppendLine("    for (int i = 0; i < values.length; i++)");
	sb.AppendLine("        names[i] = values[i].name();");
	sb.AppendLine("    ");
	sb.AppendLine("    return names;");
	sb.AppendLine("}");
	sb.AppendLine();
	
	sb.AppendLine("@Override");
	sb.AppendLine("protected String getCurrentStateString()");
	sb.AppendLine("{");
	sb.AppendLine("    return stateIdToString(this.stateId);");
	sb.AppendLine("}");
	sb.AppendLine();
	
	sb.AppendLine("@Override");
	sb.AppendLine("protected String[] getBaseAllowedEventsForCurrentState()");
	sb.AppendLine("{");
	sb.AppendLine("    return getAllowedEvents(this.stateId);");
	sb.AppendLine("}");
	sb.AppendLine();
	
	sb.AppendLine("// Allow sending strings to dispatchEvent(), with translate functionality");
	sb.AppendLine("@Override");
	sb.AppendLine("protected void dispatchEvent(String eventName)");
	sb.AppendLine("{");
	sb.AppendLine("    String baseEventName = translateToBaseEventName(eventName);");
	sb.AppendLine("    dispatchEvent(EventId.valueOf(baseEventName));");
	sb.AppendLine("}");
	sb.AppendLine();
	
	sb.AppendLine("@Override");
	sb.AppendLine("protected void startSm()");
	sb.AppendLine("{");
	sb.AppendLine("    start();");
	sb.AppendLine("}");
	sb.AppendLine();

	// Finally, add dummy function to fix unused function warning
	sb.AppendLine("// See function name");
	sb.AppendLine("@SuppressWarnings(\"unused\")");
	sb.AppendLine("private void suppressUnusedExitUpToStateHandler()");
	sb.AppendLine("{");
	sb.AppendLine("    exitUpToStateHandler(StateId.ROOT);");
	sb.AppendLine("}");
	sb.AppendLine();
	
	sb.AppendLine("////////////////////////////////////////////////////////////////////////////////");
	sb.AppendLine("// End of csx-inserted code");
	sb.AppendLine("////////////////////////////////////////////////////////////////////////////////");

	// Build the method code
	string methodCode = sb.ToString().Trim();
	
	// Indent properly (4 spaces per line)
	string indentedMethod = "    " + methodCode.Replace("\n", "\n    ");
	
	// Read generated source
	string source = File.ReadAllText(filePath);
	
	// Insert just before the final closing brace of the class
	int insertPos = source.LastIndexOf("\n}");  // Find line with final }
	if (insertPos != -1)
	{
	    string newSource = source.Insert(insertPos, "\n\n" + indentedMethod);
	    File.WriteAllText(filePath, newSource);
	    Console.WriteLine($"getAllowedEvents() and core functions inserted successfully into {Path.GetFileName(filePath)}");
	}
	else
	{
	    Console.WriteLine("Final closing brace not found - insertion skipped");
	}
}

// Function to extract the full dispatchEvent method (signature + body + closing brace)
string ExtractDispatchEvent(string sourceCode)
{
    // Robust regex: matches the method signature, captures everything inside {} with proper nesting handling,
    // and allows optional comments/whitespace. Works for all StateSmith-generated dispatchEvent variants.
    string pattern = @"public void dispatchEvent\(EventId eventId\)\s*\{((?>[^{}]+|\{(?<depth>)|\}(?<-depth>))*(?(depth)(?!)))\}";

	// Extract and return the function dispatchEvent
	return ExtractMatch(sourceCode, pattern);
}

List<string> ExtractStates(string dispatchEvent)
{
    // Pattern to match each 'case STATE:'
	string pattern = @"\/\/ STATE:[\w\s\:\/\(\)\.\`\{\}\;]*?case (\w*?):";

	// Extract all state names
	return ExtractGroup1Values(dispatchEvent, pattern);
}

string ExtractStateSwitchCase(string sourceDispatchEvent, string stateName)
{
	// First get trimmmed text after "case STATE:"
    string stateMarker = $"case {stateName}:";
    int startIndex = sourceDispatchEvent.IndexOf(stateMarker);
    if (startIndex == -1) return null;
    startIndex += stateMarker.Length;
    string textAfterStateCase = sourceDispatchEvent.Substring(startIndex).Trim();
    
	if (debug) Console.WriteLine("stateName: " + stateName);
	if (debug) Console.WriteLine("textAfterStateCase:\n" + textAfterStateCase + "\n-----");
	
    // Then use regex to extract the first switch case found
	string pattern = @"switch \(eventId\)\s*?\{[\w\s\(\)_:;\/]*?\}";
	
	// Trimmed text must start with "switch", otherwise we return null
	if (textAfterStateCase?.Trim().StartsWith("switch") ?? false)
    	return ExtractMatch(textAfterStateCase, pattern);
    else
    	return null; // not found
}

List<string> ExtractEvents(string eventSwitchCase)
{
    // Pattern to match each 'case EVENT:'
	string pattern = @"case (\w*?):";

	// Extract all event names
	return ExtractGroup1Values(eventSwitchCase, pattern);
}

string ExtractMatch (string text, string pattern)
{
	var match = Regex.Match(text, pattern, RegexOptions.Singleline);

    if (match.Success)
        return match.Value.Trim();  // Returns the entire method (signature + body)
	else
		return null; // not found
}

List<string> ExtractGroup1Values (string text, string patternWithGroup)
{
	MatchCollection matches = Regex.Matches(text, patternWithGroup);
	
	List<string> items = new List<string>();
	foreach (Match match in matches)
	{
	    if (match.Groups.Count > 1)
	    {
	        items.Add(match.Groups[1].Value);
	    }
	}
	
	return items;
}