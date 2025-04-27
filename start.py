"""
Parkour Duels Practice Server Start Script
Improved: Each server/tool runs in its own terminal window.
"""

from rich.console import Console
from pathlib import Path
import subprocess
import json
import sys
import os
import platform

console = Console()

#######################
## UTILS
#######################

java_versions = {}

with open("java_versions.json", "r") as f:
    d = f.read()
    try:
        java_versions = json.loads(d)
    except json.JSONDecodeError as e:
        console.print(f"[red]Error reading java_versions.json: {e}[/red]")
        sys.exit(1)

if "8" not in java_versions:
    console.print(f"[red]Java 8 not found! Make sure you have Java 8 installed.[/red]")
    sys.exit(1)

if "23" not in java_versions:
    console.print(f"[red]Java 23 not found! Make sure you have Java 23 installed.[/red]")
    sys.exit(1)

#######################
## JARS SETUP
#######################

PROJECT_ROOT = Path(__file__).resolve().parent

JARS = [
    {
        "name": "velocity",
        "jar": PROJECT_ROOT / "proxy" / "velocity-3.4.0-SNAPSHOT-496.jar",
        "cwd": PROJECT_ROOT / "proxy",
        "memory": "512M",
        "java_version": "23"
    },
    {
        "name": "lobby",
        "jar": PROJECT_ROOT / "servers" / "lobby" / "paper-1.8.8-445.jar",
        "cwd": PROJECT_ROOT / "servers" / "lobby",
        "memory": "512M",
        "java_version": "8"
    },
    {
        "name": "rooms",
        "jar": PROJECT_ROOT / "servers" / "rooms" / "paper-1.8.8-445.jar",
        "cwd": PROJECT_ROOT / "servers" / "rooms",
        "memory": "512M",
        "java_version": "8"
    },
    {
        "name": "world-generator",
        "jar": PROJECT_ROOT / "tools" / "world-generator.jar",
        "cwd": PROJECT_ROOT / "tools",
        "memory": "512M",
        "java_version": "23"
    },
]


#######################
## CORE
#######################

def launch_in_new_terminal(server):
    java_path = java_versions[server["java_version"]]
    jar_path = server["jar"]
    cwd = server["cwd"]
    memory = server["memory"]

    if platform.system() == "Windows":
        command = f"start \"\" cmd /c \"\"{java_path}\" -Xmx512M -jar \"{jar_path}\"\""
        subprocess.Popen(
            command,
            cwd=cwd,
            shell=True
        )
    elif platform.system() == "Linux":
        command = f'"{java_path}" -Xmx{memory} -jar "{jar_path}"; exec bash'
        subprocess.Popen(
            ["gnome-terminal", "--", "bash", "-c", command],
            cwd=cwd
        )
    elif platform.system() == "Darwin":  # macOS
        command = f'cd "{cwd}" && "{java_path}" -Xmx{memory} -jar "{jar_path}"'
        subprocess.Popen([
            "osascript", "-e",
            f'tell application "Terminal" to do script "{command}"'
        ])
    else:
        console.print(f"[red]Unsupported OS: {platform.system()}[/red]")
        sys.exit(1)


def main():
    console.print("[green]==== Starting servers and tools ====[/green]")

    for server in JARS:
        if not server["jar"].exists():
            console.print(f"[red]Jar not found for {server['name']}: {server['jar']}[/red]")
            sys.exit(1)

        console.print(f"[green]Starting {server['name']}...[/green]")
        launch_in_new_terminal(server)

    console.print("[bold green]All servers and tools launched into new terminals![/bold green]")
    console.print("[bold yellow]Press Ctrl+C here to exit the launcher script (servers keep running).[/bold yellow]")

    try:
        while True:
            pass  # Keep the script alive if you want
    except KeyboardInterrupt:
        console.print("\n[bold yellow]Launcher script exited. You need to close terminals manually![/bold yellow]")


if __name__ == "__main__":
    main()
