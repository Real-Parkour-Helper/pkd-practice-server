"""
Parkour Duels Practice Server Plugin Build Script

This script builds the plugins into their JARs
and copies them into the necessary servers' plugin
folders.
"""

from rich.console import Console
from pathlib import Path
import subprocess
import platform
import shutil

console = Console()

#######################
## SETUP
#######################

PROJECT_ROOT = Path(__file__).resolve().parent

PLUGINS = [
    "shared-plugin",
    "rooms-plugin",
    "dynamic-plugin"
]

PLUGIN_TARGETS = {
    "shared-plugin": ["servers/lobby/plugins", "servers/rooms/plugins", "servers/dynamic/plugins"],
    "rooms-plugin": ["servers/rooms/plugins"],
    "dynamic-plugin": ["servers/dynamic/plugins"],
}

#######################
## MAIN
#######################


def gradlew(*args):
    system = platform.system()
    if system == "Windows":
        gradlew_file = PROJECT_ROOT / "gradlew.bat"
    else:
        gradlew_file = PROJECT_ROOT / "gradlew"
        
    command = [str(PROJECT_ROOT / gradlew_file)] + list(args)
    result = subprocess.run(command, cwd=PROJECT_ROOT, capture_output=True, text=True)
    if result.returncode != 0:
        console.print(
            f"[bold red]Gradle failed:[/bold red]\n{result.stdout}\n{result.stderr}"
        )
        raise Exception("Gradle build failed")
    return result


def find_built_jar(plugin_name):
    libs_folder = PROJECT_ROOT / "plugins" / plugin_name / "build" / "libs"
    jars = list(libs_folder.glob("*.jar"))

    if not jars:
        raise FileNotFoundError(
            f"No built JAR found for plugin {plugin_name} in {libs_folder}"
        )

    if len(jars) > 1:
        console.print(
            f"[yellow]Warning: multiple jars found for {plugin_name}, picking {jars[0].name}[/yellow]"
        )

    return jars[0]


def main():
    console.print("[green]==== Building and Copying Plugins ==== [/green]")

    # 1. Build all plugins
    for plugin_name in PLUGINS:
        console.print(f"[cyan]Building {plugin_name}...[/cyan]")
        gradlew(f":plugins:{plugin_name}:build")

    # 2. Copy plugins
    for plugin_name in PLUGINS:
        built_jar = find_built_jar(plugin_name)

        targets = PLUGIN_TARGETS.get(plugin_name, [])
        if not targets:
            console.print(
                f"[yellow]No targets defined for {plugin_name}, skipping copy.[/yellow]"
            )
            continue

        for target_dir in targets:
            full_target_dir = PROJECT_ROOT / target_dir
            full_target_dir.mkdir(parents=True, exist_ok=True)

            target_path = full_target_dir / built_jar.name
            shutil.copy(built_jar, target_path)
            console.print(
                f"[green]Copied {built_jar.name} -> {full_target_dir}[/green]"
            )

    console.print("[bold green]All plugins built and copied successfully![/bold green]")


if __name__ == "__main__":
    main()
