"""
Parkour Duels Practice Server Dist Build Script

Creates a clean dist/ folder, copying needed servers and proxy,
building required plugins, and copying them into the right places.
"""

from pathlib import Path
import shutil
import os
import sys
import subprocess
import platform
import pathspec
from rich.console import Console
from rich.progress import Progress

console = Console()

#######################
## SETUP
#######################

PROJECT_ROOT = Path(__file__).resolve().parent
DIST_DIR = PROJECT_ROOT / "dist"

COPY_TARGETS = [
    ("servers", PROJECT_ROOT / "servers"),
    ("proxy", PROJECT_ROOT / "proxy"),
]

PLUGINS = [
    "shared-plugin",
    "rooms-plugin",
    "dynamic-plugin",
    "lobby-plugin",
]

PLUGIN_TARGETS = {
    "shared-plugin": ["servers/lobby/plugins", "servers/rooms/plugins", "servers/dynamic/plugins"],
    "rooms-plugin": ["servers/rooms/plugins"],
    "dynamic-plugin": ["servers/dynamic/plugins"],
    "lobby-plugin": ["servers/lobby/plugins"],
}

#######################
## FUNCTIONS
#######################

def load_gitignore_patterns(start_dir):
    """Recursively load .gitignore patterns from a directory."""
    patterns = []
    for root, dirs, files in os.walk(start_dir):
        if '.gitignore' in files:
            gitignore_path = Path(root) / '.gitignore'
            with gitignore_path.open() as f:
                lines = f.readlines()
                base = os.path.relpath(root, start_dir)
                for line in lines:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        if base != ".":
                            patterns.append(os.path.join(base, line))
                        else:
                            patterns.append(line)
    return pathspec.PathSpec.from_lines('gitwildmatch', patterns)

def copy_project_filtered(src_dir: Path, dst_dir: Path):
    """Copy a directory into dst_dir, ignoring files matched by .gitignore rules and manual exclusions."""
    spec = load_gitignore_patterns(src_dir)

    for root, dirs, files in os.walk(src_dir):
        rel_root = os.path.relpath(root, src_dir)
        if rel_root == '.':
            rel_root = ''

        # Filter directories
        dirs[:] = [
            d for d in dirs
            if not spec.match_file(os.path.join(rel_root, d))
        ]

        for file in files:
            rel_file = os.path.join(rel_root, file)

            # Manual exclusions: skip .gitignore and .md files
            if file == ".gitignore" or file.lower().endswith(".md"):
                continue

            if spec.match_file(rel_file):
                continue  # Skip files matching .gitignore patterns

            src_path = Path(root) / file
            dst_path = dst_dir / rel_file
            dst_path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src_path, dst_path)

def gradlew(*args):
    """Run a gradle command."""
    system = platform.system()
    if system == "Windows":
        gradlew_file = PROJECT_ROOT / "gradlew.bat"
    else:
        gradlew_file = PROJECT_ROOT / "gradlew"

    command = [str(gradlew_file)] + list(args)
    result = subprocess.run(command, cwd=PROJECT_ROOT, capture_output=True, text=True)

    if result.returncode != 0:
        console.print(f"[red]Gradle failed:[/red]\n{result.stdout}\n{result.stderr}")
        raise Exception("Gradle build failed")

def build_plugins():
    console.print("[bold cyan]==== Building Plugins ====[/bold cyan]")
    with Progress() as progress:
        task = progress.add_task("[green]Building plugins...", total=len(PLUGINS))

        for plugin in PLUGINS:
            console.print(f"[blue]Building [bold]{plugin}[/bold]...[/blue]")
            gradlew(f":plugins:{plugin}:build")
            progress.update(task, advance=1)

def copy_built_plugins():
    console.print("[bold cyan]==== Copying Built Plugins ====[/bold cyan]")
    for plugin in PLUGINS:
        built_jar_dir = PROJECT_ROOT / "plugins" / plugin / "build" / "libs"
        if not built_jar_dir.exists():
            console.print(f"[red]Built JAR folder not found for {plugin}![/red]")
            sys.exit(1)

        jars = list(built_jar_dir.glob("*.jar"))
        if not jars:
            console.print(f"[red]No built JAR found for {plugin}![/red]")
            sys.exit(1)

        jar_to_copy = jars[0]  # Take the first jar (warn if multiple?)

        targets = PLUGIN_TARGETS.get(plugin, [])
        for target_rel in targets:
            full_target_dir = DIST_DIR / target_rel
            full_target_dir.mkdir(parents=True, exist_ok=True)

            target_path = full_target_dir / jar_to_copy.name
            shutil.copy2(jar_to_copy, target_path)
            console.print(f"[green]✔ Copied {plugin} JAR to {full_target_dir}[/green]")

def build_world_generator():
    console.print("[bold cyan]==== Building World Generator ====[/bold cyan]")
    gradlew(":tools:world-generator:build")

def copy_world_generator():
    console.print("[bold cyan]==== Copying World Generator ====[/bold cyan]")
    built_jar_dir = PROJECT_ROOT / "tools" / "world-generator" / "build" / "libs"

    if not built_jar_dir.exists():
        console.print(f"[red]Built JAR folder not found for world-generator![/red]")
        sys.exit(1)

    jars = list(built_jar_dir.glob("*.jar"))
    if not jars:
        console.print(f"[red]No built JAR found for world-generator![/red]")
        sys.exit(1)

    jar_to_copy = jars[0]  # Take first found JAR (warn if multiple?)

    target_dir = DIST_DIR / "tools"
    target_dir.mkdir(parents=True, exist_ok=True)

    # Always rename to world-generator.jar
    target_path = target_dir / "world-generator.jar"
    shutil.copy2(jar_to_copy, target_path)

    console.print(f"[green]✔ Copied world-generator as {target_path}[/green]")

def copy_extra_scripts():
    console.print("[bold cyan]==== Copying Extra Scripts ====[/bold cyan]")
    scripts = ["setup.py", "start.py"]
    for script_name in scripts:
        src_path = PROJECT_ROOT / script_name
        dst_path = DIST_DIR / script_name

        if not src_path.exists():
            console.print(f"[yellow]⚠️ {script_name} not found, skipping.[/yellow]")
            continue

        shutil.copy2(src_path, dst_path)
        console.print(f"[green]✔ Copied {script_name} into dist/[/green]")


#######################
## MAIN
#######################

def main():
    console.print("[bold cyan]==== Creating Clean Dist Folder ====[/bold cyan]")

    # Clean dist/
    if DIST_DIR.exists():
        console.print("[yellow]Removing old dist/ folder...[/yellow]")
        shutil.rmtree(DIST_DIR)
    DIST_DIR.mkdir()

    with Progress() as progress:
        task = progress.add_task("[green]Copying servers and proxy...", total=len(COPY_TARGETS))

        for name, src_path in COPY_TARGETS:
            console.print(f"[blue]Copying [bold]{name}[/bold]...[/blue]")

            dst_path = DIST_DIR / name
            try:
                copy_project_filtered(src_path, dst_path)
                console.print(f"[green]✔ Copied {name} successfully[/green]")
            except Exception as e:
                console.print(f"[red]✖ Failed to copy {name}: {e}[/red]")
                sys.exit(1)

            progress.update(task, advance=1)

    build_plugins()
    copy_built_plugins()

    build_world_generator()
    copy_world_generator()

    copy_extra_scripts()

    console.print("\n[bold green]==== Dist folder created successfully with built plugins and world generator! ====[/bold green]")


if __name__ == "__main__":
    main()
