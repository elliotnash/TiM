# TiM

TiM (Typst for iMessage) is an iMessage bot that renders Typst code.

Inspired by [typst-bot](https://github.com/mattfbacon/typst-bot) for discord!

## Getting started

### Requirements

- A [BlueBubbles server](https://github.com/BlueBubblesApp/bluebubbles-server) instance (TiM uses the BlueBubbles API to read and send iMessages)

- [Java](https://www.oracle.com/java/technologies/downloads/) 17 or above

- [Rust](https://rustup.rs/) toolchain

### Compiling
To get started, clone the git repo
```shell
git clone https://github.com/elliotnash/TiM.git
cd TiM
```

You must copy the `config.example.toml` file to `config.toml`, and fill out the required parameters.
If running on the same machine as BlueBubbles, `BB_URL` should be set to `http://localhost:{BB_PORT}`

To compile TiM, run
```shell
./gradlew build
```
This will output 4 files in `build/libs` — the compiled jar, the rust worker binary, a launcher script, and a copy of the .env file in the root. These output files need to remain together.

### Running

To run TiM, run `./TiM`

A very basic systemd service file is available [here](scripts/TiM.service)
