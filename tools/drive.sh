#!/bin/bash
# DOSBox-X driver helpers for WWCS capture
NEW=/private/tmp/claude-504/-Users-denisrosa-Documents-projects-whereintheworld/71108252-e4c9-4cdb-a513-4565302fd62c/scratchpad
CAPS="$NEW/caps"

focus(){ osascript -e 'tell application "System Events" to set frontmost of process "dosbox-x" to true' >/dev/null 2>&1; sleep 0.3; }

# type literal text
sx_text(){ focus; osascript -e "tell application \"System Events\" to keystroke \"$1\""; sleep 0.4; }
# press a key by key code (36=return,49=space,53=esc,123=left,124=right,125=down,126=up,124...)
sx_key(){ focus; osascript -e "tell application \"System Events\" to key code $1"; sleep 0.5; }
# press return
sx_enter(){ sx_key 36; }
sx_space(){ sx_key 49; }
sx_esc(){ sx_key 53; }

# park cursor over description panel (right side), outside the photo crop
park(){ focus; cliclick m:1150,480 >/dev/null 2>&1; sleep 0.3; }

# take a raw screenshot via menu; echo the newest caps file
shot(){
  focus
  osascript -e 'tell application "System Events" to tell process "dosbox-x" to click menu item "Take raw screenshot [F12+Ctrl+P]" of menu 1 of menu bar item "Capture" of menu bar 1' >/dev/null 2>&1
  sleep 0.8
  ls -t "$CAPS"/*.png 2>/dev/null | head -1
}

"$@"
