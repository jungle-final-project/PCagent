BuildGraph PC Agent download

The web app downloads the PC Agent executable from the GitHub Release asset.
The repository no longer needs to keep agent.exe under apps/web/public.

Expected release asset:

  https://github.com/jungle-final-project/PCagent/releases/latest/download/agent.exe

The support page also downloads a small activation config file named:

  buildgraph-agent-activation-*.json

Double-click agent.exe:

  Creates %LOCALAPPDATA%\BuildGraphAgent\agent-config.json when missing.
  Imports activationToken/apiBaseUrl/webBaseUrl from the latest activation JSON
  in the current folder, Downloads, or %LOCALAPPDATA%\BuildGraphAgent.
  Creates %LOCALAPPDATA%\BuildGraphAgent\logs.
  Registers a startup command in the current user's Startup folder.
  Starts demo metric collection in the background.
  Shows a BuildGraph PC Agent tray icon.

Tray menu:

  Open log viewer
  Open log folder
  Open AS page
  Stop

The log viewer lets you choose a date and 1-hour range.

Config fields:

  apiBaseUrl: Agent API server, for example http://localhost:8080
  webBaseUrl: Web support page base URL, for example http://localhost:5173

When register saves agentToken, the agent attempts to restrict the config file
ACL to the current Windows user, Administrators, and SYSTEM.

For Windows packaging, rebuild agent.exe from the repository:

  cd apps\pc-agent
  build-agent-exe.cmd

The build creates:

  apps\pc-agent\dist\agent.exe      silent tray/viewer executable
  apps\pc-agent\dist\agent-cli.exe  console executable for status/doctor

Run examples:

  agent.exe
  agent-cli.exe doctor --config agent-config.json
  agent-cli.exe collect --config agent-config.json --iterations 1
  agent-cli.exe upload --config agent-config.json --no-open

Release flow:

  1. Build agent.exe from apps\pc-agent.
  2. Upload agent.exe as a GitHub Release asset named agent.exe.
  3. Keep the web page pointed at the release download URL.

Not included yet:

  Windows Service
  installer
  auto-update
  signed release channel
