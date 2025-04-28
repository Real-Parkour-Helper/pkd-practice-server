"""
Parkour Duels Practice Server Setup Script

This script does NOT set up anything gradle related.
It simply downloads necessary files and prepares
other files for use with the servers / proxy.

It does not build jar files from the projects'
sources. Production builds will ship with only
jar files and no source, and if you are setting
up for development, you should do it yourself.
"""

from rich.console import Console
from datetime import datetime
from pathlib import Path
import subprocess
import requests
import platform
import base64
import shutil
import json
import sys
import os


SCRIPT_DIR = Path(__file__).resolve().parent
VELOCITY = SCRIPT_DIR / "proxy"
SERVERS = SCRIPT_DIR / "servers"

console = Console()


#######################
## UTILS
#######################


def get_time_hms():
    return datetime.now().strftime("%H:%M:%S")


def download_file(url, destination_path):
    destination_path = Path(destination_path)
    destination_path.parent.mkdir(parents=True, exist_ok=True)

    response = requests.get(url)
    response.raise_for_status()

    with open(destination_path, "wb") as f:
        f.write(response.content)


#######################
## TASKS
#######################


class Task:
    def __init__(self, name: str):
        self.name = name

    def run(self):
        if self.checks():
            console.print(
                f"[cyan]{get_time_hms()}[/cyan][bold white] Running task [bold green]{self.name}[/bold green]...[/bold white]"
            )
            s = self.task()
            if not s:
                console.print(f"[bold red]Error: Task {self.name} failed.")
                sys.exit(1)
        else:
            console.print(
                f"[cyan]{get_time_hms()}[/cyan][bold magenta] Skipping task {self.name}...[/bold magenta]"
            )

    def checks(self) -> bool:
        """
        Checks for whether or not this task should be run.
        """
        return False

    def task(self) -> bool:
        """
        Run the task and return True / False depending on success.
        """
        return True


class DownloadTask(Task):
    def __init__(self, url: str, dirs: list[str] | str, filename: str = None):
        self.url = url
        if isinstance(dirs, str):
            self.dirs = [Path(dirs)]
        else:
            self.dirs = [Path(d) for d in dirs]

        self.filename = filename if filename else url.split("/")[-1]
        super().__init__(f"Download {self.filename}")

    def checks(self):
        # Only run if the file is missing in ANY of the target directories
        return any(not (d / self.filename).exists() for d in self.dirs)

    def task(self):
        try:
            # Download once into a temp file
            temp_path = self.dirs[0] / self.filename
            download_file(self.url, temp_path)

            # Copy to other dirs
            for d in self.dirs[1:]:
                d.mkdir(parents=True, exist_ok=True)  # make sure dir exists
                shutil.copy(temp_path, d / self.filename)

            return True

        except requests.RequestException as e:
            console.print(
                f"[bold red][ERROR] Failed to download [white][link={self.url}]{self.filename}[/link][/white]: {e}"
            )
        except Exception as e:
            console.print(
                f"[bold red][ERROR] Unexpected error: [/bold red][bold white]{e}[/bold white]"
            )
        return False


class DefaultFilesTask(Task):
    def __init__(self, dir: str | Path):
        self.dir = Path(dir)
        super().__init__(f"Rename {self.dir.name} defaults")

    def checks(self):
        # Needs to run if there are ANY .default files/folders without their final versions
        for path in self.dir.rglob("*"):
            if path.name.endswith(".default"):
                target = path.with_name(path.name.removesuffix(".default"))
                if not target.exists():
                    return True
        return False

    def task(self):
        try:
            for path in self.dir.rglob("*"):
                if not path.name.endswith(".default"):
                    continue

                target = path.with_name(path.name.removesuffix(".default"))

                if target.exists():
                    continue  # already applied

                if path.is_file():
                    # Copy file
                    target.write_bytes(path.read_bytes())
                elif path.is_dir():
                    # Copy entire directory
                    shutil.copytree(path, target)

            return True

        except Exception as e:
            console.print(
                f"[bold red][ERROR] Failed to apply .default templates:[/bold red] [bold white]{e}[/bold white]"
            )
            return False


class VelocitySecretTask(Task):
    def __init__(self, dir: str | Path):
        self.dir = Path(dir)
        self.secret_file = self.dir / "forwarding.secret"
        super().__init__(f"Generate Velocity forwarding.secret in {self.dir.name}")

    def checks(self):
        # Needs to run if the forwarding.secret file does not exist
        return not self.secret_file.exists()

    def task(self):
        try:
            self.secret_file.parent.mkdir(parents=True, exist_ok=True)
            secret = base64.b64encode(os.urandom(32)).decode("utf-8")
            self.secret_file.write_text(secret)
            return True

        except Exception as e:
            console.print(
                f"[bold red][ERROR] Failed to generate forwarding.secret:[/bold red] [bold white]{e}[/bold white]"
            )
            return False


class DiscoverJavaVersions(Task):
    def __init__(self):
        super().__init__("Discover Java versions")
        self.lobbyConfig = SERVERS / "lobby" / "plugins" / "LobbyPlugin" / "config.yml"

    def checks(self):
        # This task does not need to be run
        if os.path.exists("java_versions.json"):
            with open("java_versions.json", "r") as f:
                try:
                    data = json.load(f)
                    if 8 in data and 23 in data:
                        return False
                except json.JSONDecodeError:
                    console.print(
                        f"[red]Error: Failed to decode java_versions.json. Will run task...[/red]"
                    )
                    return True
        else:
            return True

        if os.path.exists(self.lobbyConfig):
            with open(self.lobbyConfig, "r") as f:
                data = f.read()
                if "java8_version" in data:
                    return False
        return True

    def task(self):
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

        string_paths = {}
        for version, path in detected.items():
            string_paths[version] = str(path)

        with open("java_versions.json", "w") as f:
            f.write(json.dumps(string_paths, indent=2))

        if 8 not in detected:
            console.print(f"[red]Java 8 not found! Make sure you have Java 8 installed.[/red]")
            return False

        if not os.path.exists(self.lobbyConfig):
            os.makedirs(str(self.lobbyConfig).replace("config.yml", ""), exist_ok=True)

        with open(self.lobbyConfig, "w") as f:
            f.write(f"java8_path: \"{string_paths[8].replace("\\", "\\\\")}\"\n")

        return True

#######################
## MAIN
#######################

velocityTask = DownloadTask(
    "https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/496/downloads/velocity-3.4.0-SNAPSHOT-496.jar",
    str(VELOCITY),
)

viaVersionTask = DownloadTask(
    "https://github.com/ViaVersion/ViaVersion/releases/download/5.3.2/ViaVersion-5.3.2.jar",
    str(VELOCITY / "plugins"),
)

viaBackwardsTask = DownloadTask(
    "https://github.com/ViaVersion/ViaBackwards/releases/download/5.3.2/ViaBackwards-5.3.2.jar",
    str(VELOCITY / "plugins"),
)

paperTask = DownloadTask(
    "https://api.papermc.io/v2/projects/paper/versions/1.8.8/builds/445/downloads/paper-1.8.8-445.jar",
    [str(SERVERS / "dynamic"), str(SERVERS / "lobby"), str(SERVERS / "rooms")],
)

protocolLibTask = DownloadTask(
    "https://dev.bukkit.org/projects/protocollib/files/2405871/download",
    [str(SERVERS / "dynamic" / "plugins"), str(SERVERS / "rooms" / "plugins")],
    "ProtocolLib.jar",
)

defaultLobbyTask = DefaultFilesTask(SERVERS / "lobby")
defaultDynamicTask = DefaultFilesTask(SERVERS / "dynamic")
defaultRoomsTask = DefaultFilesTask(SERVERS / "rooms")

velocitySecretTask = VelocitySecretTask(VELOCITY)

discoverJavaTask = DiscoverJavaVersions()

tasks = [
    velocityTask,
    viaVersionTask,
    viaBackwardsTask,
    paperTask,
    protocolLibTask,
    defaultLobbyTask,
    defaultDynamicTask,
    defaultRoomsTask,
    velocitySecretTask,
    discoverJavaTask
]

# Run tasks
console.print("[green]==== PKD Practice Setup ==== [/green]")

with console.status("[bold green]Getting things ready...") as status:
    while tasks:
        task = tasks.pop(0)
        task.run()

console.print(
    f"[cyan]{get_time_hms()}[/cyan] [bold green]Setup finished successfully![/bold green]"
)
