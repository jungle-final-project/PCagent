BuildGraph PC Agent download

This local demo build does not ship a signed installer or Windows Service yet.

Double-click agent.exe:

  Creates %LOCALAPPDATA%\BuildGraphAgent\agent-config.json when missing.
  Creates %LOCALAPPDATA%\BuildGraphAgent\logs.
  Registers a startup command in the current user's Startup folder.
  Starts demo metric collection in the background.
  Shows a BuildGraph PC Agent tray icon.

Tray menu:

  Open log folder
  Open AS page
  Stop

For Windows CLI packaging, rebuild agent.exe from the repository:

  cd apps\pc-agent
  build-agent-exe.cmd

The build creates:

  apps\pc-agent\dist\agent.exe

Run examples:

  agent.exe
  agent.exe doctor --config agent-config.json
  agent.exe collect --config agent-config.json --iterations 1
  agent.exe upload --config agent-config.json --no-open

Not included yet:

  Windows Service
  tray app
  installer
  auto-update
  signed release channel
