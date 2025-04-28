# Parkour Duels Practice Server

A locally running server which aims to replicate some of the functionality
of Parkour Duels on Hypixel for improved deliberate practice.

## Features

- **Individual rooms**: Practice rooms individually
- **Full run practice**: Play a normal run, like on Hypixel. You can choose between:
    - Random 8 room runs
    - Random N room runs
    - Set seed 8 room runs
    - Set seed N room runs
    - Custom room runs
- **Hypixel Mechanics**: Replicates Hypixel mechanics exactly (boosts, checkpoint grab zones, etc.)
- **Ping simulator**: Customize what ping you want to practice for (for boosts)
- **Extensive version support**: Connect on 1.8 as well as modern versions (1.21.4 tested)

## Getting Started

### Prerequisites

- [Python](https://www.python.org/downloads/)
- Java 8
- Java 23

### Installation

Download the latest release and unzip it into its own folder. Then:

```commandline
python setup.py
```

This will download the necessary dependencies and set up the servers and the proxy for you.
If you encounter an error about a missing dependency, please install it manually using pip.

### Running the Server

To run the server, use the following command:

```commandline
python start.py
```

**Note**: This will open 4 new terminal windows:

1. Velocity Proxy
2. Lobby Server
3. Rooms Server
4. Backend World Generator API

These need to stay open for everything to work properly. By default, each program has 512M of memory
allocated to it. If you wish to change this, simply edit the `JARS` entries in `start.py`

### Connecting to the Server

Once everything is running, simply connect to `localhost` in Minecraft and start practicing!

If you aren't able to connect using a modern version of Minecraft on the first run,
try restarting everything by closing all terminal windows and running `start.py` again.
If you are still having issues, please open an issue on the issue tracker.

---

## Commands

### Lobby Server

| Command                          | Description                                                                                                      |
|----------------------------------|------------------------------------------------------------------------------------------------------------------|
| `/play`                          | Start a new, random seed, 8 room run                                                                             |
| `/playseed <seed>`               | Start a new, set seed, 8 room run                                                                                |
| `/playrooms <number>`            | Start a new, random seed, N room run                                                                             |
| `/playroomsseed <seed> <number>` | Start a new, set seed, N room run                                                                                |
| `/playcustom`                    | Start a new, custom room run. You will be prompted to enter the rooms you want to run after running this command |
| `/rooms` or `/r`                 | Switch to the individual rooms practice server                                                                   |

### Rooms Server

| Command        | Description                                                                                                     |
|----------------|-----------------------------------------------------------------------------------------------------------------|
| `/room <room>` | Switch to the specified room                                                                                    |
| `/prev`        | Go to the previous room                                                                                         |
| `/next`        | Go to the next room                                                                                             |
| `/ping`        | Set the ping simulator for boosts. **NOTE**: Ping is **not** shared between the rooms and the full runs server. |
| `/lobby`       | Go back to the lobby server                                                                                     |

### Runs Server

| Command  | Description                        |
|----------|------------------------------------|
| `/ping`  | Set the ping simulator for boosts. |
| `/lobby` | Go back to the lobby server        |

---

## Contributing
If you have any ideas for new features or improvements, please open an issue or a pull request.
If you encounter any bugs, please report them on the issue tracker.
