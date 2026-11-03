#!/usr/bin/env python3
"""Validate a built plugin and write factual release evidence. Standard library only."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent.parent
VERSION_RE = re.compile(r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-(alpha|beta|rc)\.([1-9]\d*))?")
RUNTIME_SCENARIOS = (
    "installation", "completion", "navigation", "rename", "inspections",
    "settings", "restart", "indexing", "non_yii_project",
)


def validate_version(version):
    if not VERSION_RE.fullmatch(version):
        raise ValueError("Use MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-rc.N (alpha/beta also supported)")
    return version


def properties(root=ROOT):
    return dict(line.split("=", 1) for line in (root / "gradle.properties").read_text().splitlines()
                if "=" in line and not line.startswith("#"))


def changelog_section(version, text):
    match = re.search(r"^## " + re.escape(version) + r"\n(.*?)(?=^## |\Z)", text, re.M | re.S)
    if not match or not match[1].strip():
        raise ValueError("Missing exact, nonempty changelog section for " + version)
    return match[1].strip()


def inspect_zip(path, version, root=ROOT):
    descriptors = []
    with zipfile.ZipFile(path) as archive:
        if archive.testzip():
            raise ValueError("Corrupt ZIP")
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate ZIP entries")
        for name in names:
            if name.startswith("/") or ".." in Path(name).parts:
                raise ValueError("Unsafe archive path")
            if not name.endswith(".jar"):
                continue
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as jar:
                if jar.testzip():
                    raise ValueError("Corrupt plugin JAR")
                if any("RegressionTest" in entry for entry in jar.namelist()):
                    raise ValueError("Test classes in plugin ZIP")
                if "META-INF/plugin.xml" not in jar.namelist():
                    continue
                descriptor = ET.fromstring(jar.read("META-INF/plugin.xml"))
                if descriptor.findtext("version") != version:
                    raise ValueError("Descriptor/version mismatch")
                expected = ET.parse(root / "resources/META-INF/plugin.xml").getroot()
                for field in ("id", "name", "vendor"):
                    if descriptor.findtext(field) != expected.findtext(field):
                        raise ValueError("Plugin identity mismatch: " + field)
                for inspection in descriptor.iter("localInspection"):
                    description = "inspectionDescriptions/" + inspection.attrib["shortName"] + ".html"
                    if description not in jar.namelist():
                        raise ValueError("Missing inspection description: " + description)
                for notice in ("LICENSE.md", "NOTICE.md", "AUTHORS.md"):
                    if jar.read("META-INF/" + notice) != (root / notice).read_bytes():
                        raise ValueError("Missing or changed distribution notice: " + notice)
                compatibility = descriptor.find("idea-version")
                if compatibility is None or compatibility.attrib != {"since-build": "262.10315.130", "until-build": "262.*"}:
                    raise ValueError("Unexpected compatibility range; review the release validator")
                descriptors.append({"id": descriptor.findtext("id"), "compatibility": compatibility.attrib})
    if len(descriptors) != 1:
        raise ValueError("Expected exactly one plugin descriptor")
    return {**descriptors[0], "file": path.name, "bytes": path.stat().st_size,
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}


def junit_results(directory):
    files = sorted(directory.rglob("TEST-*.xml"))
    if not files:
        raise ValueError("No JUnit XML evidence found")
    totals = {"test_methods": 0, "failures": 0, "errors": 0, "skipped": 0}
    checks = {}
    for path in files:
        suite = ET.parse(path).getroot()
        if suite.tag != "testsuite":
            raise ValueError("Unsupported JUnit report")
        for key in totals:
            totals[key] += int(suite.attrib.get("tests" if key == "test_methods" else key, 0))
        output = "\n".join(element.text or "" for element in suite.findall("system-out"))
        for count, label in re.findall(r"PASS: (\d+) (core regression checks|real PHP PSI regression checks)", output):
            checks[label] = checks.get(label, 0) + int(count)
    if not totals["test_methods"] or any(totals[key] for key in ("failures", "errors", "skipped")):
        raise ValueError("Tests failed, were skipped or did not execute")
    if set(checks) != {"core regression checks", "real PHP PSI regression checks"}:
        raise ValueError("Both regression suite outputs are required")
    return {**totals, "checks": checks}


def runtime_evidence(path, version, digest):
    if not path.exists():
        return {"status": "not_run", "reason": "No runtime evidence for this ZIP"}
    evidence = json.loads(path.read_text())
    if evidence.get("version") != version or evidence.get("artifact_sha256") != digest:
        raise ValueError("Runtime evidence belongs to another version or ZIP")
    if evidence.get("status") != "passed":
        raise ValueError("Runtime evidence is not passed")
    for key in ("ide_build", "os", "architecture", "tested_at", "evidence_url"):
        if not isinstance(evidence.get(key), str) or not evidence[key].strip():
            raise ValueError("Missing runtime evidence field: " + key)
    if not evidence["evidence_url"].startswith("https://"):
        raise ValueError("Runtime evidence must link to a durable HTTPS report")
    if not re.fullmatch(r"262\.\d+\.\d+", evidence["ide_build"]):
        raise ValueError("Runtime evidence must name an exact supported IDE build")
    if any(evidence.get("scenarios", {}).get(key) != "passed" for key in RUNTIME_SCENARIOS):
        raise ValueError("Incomplete runtime scenarios")
    return evidence


def stable_gate(report):
    if report["dirty_worktree"]:
        raise ValueError("Release builds require a clean source checkout")
    if report["plugin_verifier"] != "passed":
        raise ValueError("Release builds require successful official Plugin Verifier")
    if "-" not in report["version"] and report["ide_runtime"]["status"] != "passed":
        raise ValueError("Stable release blocked: test this exact ZIP in isolated IDE and add runtime evidence")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--verifier", choices=("passed", "not_run"), default="not_run",
                        help="passed only after verifyPlugin exited successfully")
    parser.add_argument("--duration-seconds", type=int)
    parser.add_argument("--release", action="store_true", help="Enforce tag and release eligibility")
    args = parser.parse_args()
    props = properties()
    version = validate_version(props["pluginVersion"])
    changes = changelog_section(version, (ROOT / "CHANGELOG.md").read_text())
    archive = args.directory / f"yii2-insight-{version}.zip"
    artifact = inspect_zip(archive, version)
    tests = junit_results(args.directory / "reports/test-results")
    evidence = runtime_evidence(ROOT / f"docs/verification/runtime-{version}.json", version, artifact["sha256"])
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain", "--untracked-files=normal"], cwd=ROOT, text=True).strip())
    report = {"schema_version": 1, "version": version, "source_commit": sha,
              "dirty_worktree": dirty, "sdk_version": props["platformVersion"],
              "build_method": "Docker / Gradle", "runner_os": os.environ.get("RUNNER_OS", "local host"),
              "run_url": os.environ.get("BUILD_RUN_URL"), "artifact": artifact, "tests": tests,
              "plugin_verifier": args.verifier, "ide_runtime": evidence,
              "metrics": {"build_duration_seconds": args.duration_seconds,
                          "coverage_percent": None, "completion_latency_ms": None,
                          "indexing_duration_seconds": None, "ide_memory_bytes": None}}
    (args.directory / "build-report.json").write_text(json.dumps(report, indent=2) + "\n")
    note = f"""# {version}

{changes}

## Verification for this artifact

- Source commit: `{sha}`; dirty checkout: `{dirty}`.
- SDK: PhpStorm {props['platformVersion']}; Java 25; Docker / Gradle.
- JUnit: {tests['test_methods']} test methods, no failures/errors/skips.
- Regression checks: {json.dumps(tests['checks'])}.
- Official Plugin Verifier: **{args.verifier}**.
- Full IDE runtime scenarios: **{evidence['status']}**.
- ZIP: `{archive.name}` ({artifact['bytes']} bytes).
- SHA-256: `{artifact['sha256']}`.
- Build run: {report['run_url'] or 'local run; not a GitHub Actions result'}.

The JUnit methods wrap regression suites; check counts are not coverage percentages.
Unmeasured metrics are null in build-report.json. Static/PSI checks do not establish
full IDE behavior. Read any runtime report for its exact tested IDE/OS scope.

## Attribution

Maintained by by_scrimm with AI assistance (Claude Code); based on Vxdy's fork
of NVlad's Yii2 Support. Original BSD license and author credits are included.
AI-assisted implementation/self-review is not independent human review.
"""
    (args.directory / "RELEASE_NOTES.md").write_text(note)
    checksum_files = sorted(path for path in args.directory.iterdir()
                            if path.is_file() and path.name not in {"SHA256SUMS"} and not path.name.endswith(".sha256"))
    (args.directory / "SHA256SUMS").write_text("".join(hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n" for path in checksum_files))
    if args.release:
        tag = os.environ.get("GITHUB_REF_NAME", "")
        if tag != "v" + version or os.environ.get("GITHUB_REF_TYPE") != "tag":
            raise ValueError("Release requires the existing tag v" + version)
        stable_gate(report)
        if "-" not in version:
            subprocess.run(["git", "merge-base", "--is-ancestor", "HEAD", "origin/master"], cwd=ROOT, check=True)
    print(json.dumps({"artifact": artifact, "tests": tests, "plugin_verifier": args.verifier, "ide_runtime": evidence["status"]}, indent=2))


if __name__ == "__main__":
    main()
