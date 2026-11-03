#!/usr/bin/env python3
"""Repository policy checks for local work and unprivileged pull_request CI."""
import json
import os
from pathlib import Path
import re
import subprocess
from release_tools import ROOT, changelog_section, properties, validate_version


def main():
    if os.environ.get("GITHUB_ACTIONS") == "true":
        status = subprocess.check_output(["git", "status", "--porcelain", "--untracked-files=normal"], cwd=ROOT, text=True).strip()
        if status:
            raise ValueError("CI checkout must be clean:\n" + status)
    version = validate_version(properties()["pluginVersion"])
    changelog_section(version, (ROOT / "CHANGELOG.md").read_text())
    paths = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0")
    for name in filter(None, paths):
        if any(part in {".DS_Store", ".serena", ".claude", ".intellijPlatform"} for part in Path(name).parts):
            raise ValueError("Local state must not be tracked: " + name)
    for path in (ROOT / ".github/workflows").glob("*.yml"):
        text = path.read_text()
        if "pull_request_target:" in text:
            raise ValueError("Use unprivileged pull_request CI")
        for action in re.findall(r"\buses:\s+([^\s#]+)", text):
            if not action.startswith("./") and not re.fullmatch(r"[\w./-]+@[0-9a-f]{40}", action):
                raise ValueError("Action must be pinned to a full commit SHA: " + action)
    if os.environ.get("GITHUB_EVENT_NAME") == "pull_request":
        event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
        pr = event["pull_request"]
        if not re.fullmatch(r"(?:feat|fix|docs|build|ci|test|refactor|perf|chore|revert)(?:\([a-z0-9-]+\))?!?: .+", pr["title"]):
            raise ValueError("PR title must follow Conventional Commits")
        if not re.fullmatch(r"(?:feature|fix|hotfix|docs|build|ci|test|refactor|perf|chore|release|renovate|dependabot)/[a-z0-9][a-z0-9._/-]*", pr["head"]["ref"]):
            raise ValueError("Use a descriptive feature/fix/build/etc. branch")
    print("Repository policy passed; version " + version)


if __name__ == "__main__":
    main()
