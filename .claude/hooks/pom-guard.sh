#!/usr/bin/env bash
# PreToolUse hook: refuse any Edit/Write/MultiEdit whose target is pom.xml.
#
# Claude Code pipes the tool input as JSON on stdin, for example:
#   {"tool_name":"Edit","tool_input":{"file_path":"/path/to/pom.xml", ...}}
#
# Exit 0 lets the tool run. Exit 2 blocks it and returns stderr to Claude.
#
# Enable: copy to .claude/hooks/pom-guard.sh, chmod +x it, and reference it from
# .claude/settings.json (see docs/examples/settings.hooks.json).
#
# Try it by hand:
#   echo '{"tool_input":{"file_path":"/x/pom.xml"}}' | ./pom-guard.sh; echo "exit=$?"

set -eu

input="$(cat)"

# Pull the file path out without depending on jq. Handles file_path and (for
# safety) notebook_path, which some tools use instead.
file_path="$(printf '%s' "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)"
if [ -z "$file_path" ]; then
  file_path="$(printf '%s' "$input" | sed -n 's/.*"notebook_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)"
fi

case "$file_path" in
  *pom.xml)
    echo "pom.xml dependencies are frozen. A change needs a CHG ticket." >&2
    exit 2
    ;;
esac

exit 0
