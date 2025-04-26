"""
Parkour Duels Practice Server Start Script

This script simply launches the three servers
and the velocity proxy. You will need the correct
java versions installed for this to work (8 and 23).
"""

from rich.console import Console
from pathlib import Path
import subprocess
import threading
import platform
import shutil
import signal
import sys
import os

console = Console()


#######################
## UTILS
#######################

def detect_java_versions():
    java_paths = []

    system = platform.system()
    if system == "Windows":
        program_files = [os.environ.get('ProgramFiles'), os.environ.get('ProgramFiles(x86)')]
        for pf in program_files:
            if pf:
                java_paths += list(Path(pf).rglob("java.exe"))
    else:
        java_paths += list(Path("/usr/bin").glob("java*"))
        java_paths += list(Path("/usr/local/bin").glob("java*"))

    java_in_path = shutil.which("java")
    if java_in_path:
        java_paths.append(Path(java_in_path))

    detected = {}

    for java_path in java_paths:
        try:
            result = subprocess.run(
                [str(java_path), "-version"],
                capture_output=True,
                text=True
            )
            version_output = result.stderr if result.stderr else result.stdout

            if "version" in version_output:
                if '"1.8' in version_output:
                    detected[8] = java_path
                elif '"23' in version_output:
                    detected[23] = java_path

        except Exception as e:
            continue

    return detected


java_versions = detect_java_versions()

if 8 not in java_versions:
    console.print(f"[red]Java 8 not found! Make sure you have Java 8 installed.[/red]")
    
if 23 not in java_versions:
    console.print(f"[red]Java 23 not found! Make sure you have Java 23 installed.[/red]")


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
        "java_version": 23
    },
    {
        "name": "lobby",
        "jar": PROJECT_ROOT / "servers" / "lobby" / "paper-1.8.8-445.jar",
        "cwd": PROJECT_ROOT / "servers" / "lobby",
        "memory": "512M",
        "java_version": 8
    },
    {
        "name": "rooms",
        "jar": PROJECT_ROOT / "servers" / "rooms" / "paper-1.8.8-445.jar",
        "cwd": PROJECT_ROOT / "servers" / "rooms",
        "memory": "512M",
        "java_version": 8
    },
    {
        "name": "dynamic",
        "jar": PROJECT_ROOT / "servers" / "dynamic" / "paper-1.8.8-445.jar",
        "cwd": PROJECT_ROOT / "servers" / "dynamic",
        "memory": "512M",
        "java_version": 8
    },
    {
        "name": "world-generator",
        "jar": PROJECT_ROOT / "tools" / "world-generator.jar",
        "cwd": PROJECT_ROOT / "tools",
        "memory": "512M",
        "java_version": 23
    },
]

def run_jar(server):
    command = [
        str(java_versions[server["java_version"]]),
        f"-Xmx{server['memory']}",
        "-jar",
        str(server["jar"]),
        "nogui"
    ]
    process = subprocess.Popen(
        command,
        cwd=server["cwd"],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        bufsize=1,
        universal_newlines=True,
        preexec_fn=os.setsid if os.name != "nt" else None,
        creationflags=subprocess.CREATE_NEW_PROCESS_GROUP if os.name == "nt" else 0
    )
    return process

def stream_output(process, name):
    try:
        for line in process.stdout:
            console.print(f"[bold cyan][{name}][/bold cyan] {line.rstrip()}")
    except Exception as e:
        console.print(f"[red][{name}] Output error:[/red] {e}")
        

def kill_process(process, name):
    try:
        if os.name == "nt":
            process.send_signal(signal.CTRL_BREAK_EVENT) 
        else:
            os.killpg(os.getpgid(process.pid), signal.SIGINT)
        process.wait(timeout=10)
    except Exception as e:
        console.print(f"[yellow]Force killing {name}... ({e})[/yellow]")
        process.kill()

def main():
    console.print("[green]==== Starting servers and tools ====[/green]")

    processes = []
    threads = []

    for server in JARS:
        if not server["jar"].exists():
            console.print(f"[red]Jar not found for {server['name']}: {server['jar']}[/red]")
            sys.exit(1)

        console.print(f"[green]Starting {server['name']}...[/green]")
        process = run_jar(server)
        processes.append((server["name"], process))

        t = threading.Thread(target=stream_output, args=(process, server["name"]), daemon=True)
        t.start()
        threads.append(t)

    console.print("[bold green]All servers and tools are running! Press Ctrl+C to stop.[/bold green]")

    try:
        while True:
            for name, process in processes:
                if process.poll() is not None:
                    console.print(f"[red]{name} has stopped unexpectedly![/red]")
                    sys.exit(1)
    except KeyboardInterrupt:
        console.print("\n[bold yellow]Shutting down all servers gracefully...[/bold yellow]")
        for name, process in processes:
            kill_process(process, name)
        console.print("[bold green]All processes terminated cleanly.[/bold green]")

if __name__ == "__main__":
    main()
