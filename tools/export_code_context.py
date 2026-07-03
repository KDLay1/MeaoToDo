#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build" / "code-context" / "code_context.md"
INCLUDE_DIRS = [
    "app/src/main/java",
    "app/src/main/res",
    "app/src/test",
    "app/src/androidTest",
    ".github/workflows",
]
INCLUDE_FILES = [
    "settings.gradle.kts",
    "build.gradle.kts",
    "gradle.properties",
    "app/build.gradle.kts",
    "README.md",
]
SUFFIXES = {".kt", ".kts", ".xml", ".properties", ".toml", ".md", ".yml", ".yaml"}
SKIP = {".git", ".gradle", ".idea", ".kotlin", "build", "captures", ".externalNativeBuild", ".cxx"}
MAX_CHARS = 40000


def skip(path: Path) -> bool:
    return any(part in SKIP for part in path.parts)


def collect_files() -> list[Path]:
    result = set()
    for name in INCLUDE_FILES:
        path = ROOT / name
        if path.is_file() and path.suffix in SUFFIXES:
            result.add(path)
    for dirname in INCLUDE_DIRS:
        base = ROOT / dirname
        if not base.exists():
            continue
        for path in base.rglob("*"):
            if path.is_file() and path.suffix in SUFFIXES and not skip(path):
                result.add(path)
    return sorted(result, key=lambda p: p.relative_to(ROOT).as_posix())


def lang(path: Path) -> str:
    return {
        ".kt": "kotlin",
        ".kts": "kotlin",
        ".xml": "xml",
        ".properties": "properties",
        ".toml": "toml",
        ".md": "markdown",
        ".yml": "yaml",
        ".yaml": "yaml",
    }.get(path.suffix, "text")


def numbered(text: str) -> str:
    return "\n".join(f"{i:04d}: {line}" for i, line in enumerate(text.splitlines(), 1))


def main() -> None:
    files = collect_files()
    lines = ["# MeaoToDo code context", "", "## Source tree", ""]
    for path in files:
        lines.append(f"- `{path.relative_to(ROOT).as_posix()}`")
    lines += ["", "## File contents", ""]
    for path in files:
        rel = path.relative_to(ROOT).as_posix()
        text = path.read_text(encoding="utf-8", errors="replace")
        if len(text) > MAX_CHARS:
            text = text[:MAX_CHARS] + "\n\n/* truncated */\n"
        lines += ["---", "", f"### `{rel}`", "", f"```{lang(path)}", numbered(text), "```", ""]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {OUT.relative_to(ROOT)} with {len(files)} files")


if __name__ == "__main__":
    main()
