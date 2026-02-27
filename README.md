# LegoTank

This is a revival of a 2014 school project: a line-following robot built as a LEGO tank. The project runs on a Raspberry Pi for high-level control and a PIC microcontroller for low-level hardware interfacing. The tank supports manual control via a PS3 controller, line following, sounds, and basic motor/sensor operations.

The source code in this repo is the original, as-is version from the project's completion (v1.1). Future commits will show revival efforts, including bug fixes, modernizations (e.g., added PS5 controller compatibility, updated OS), and new features.

## Project Structure
- **lts/**: Design and modeling files for concurrency and state machines. See [lts/README.md](./lts/README.md) for details on LTS files and the LTSA tool.
- **src/Pibotica.X**: C firmware for the PIC microcontroller (PiBotica PCB). See [src/Pibotica.X/README.md](./src/Pibotica.X/README.md) for details on the firmware and microcontroller.
- **src/tankpack**: Java control software running on the Raspberry Pi. See [src/tankpack/README.md](./src/tankpack/README.md) for details on code structure, core classes, drivers, logic, input handling, and utilities.
- **sound/**: Audio resources (.wav files) for event-triggered sounds (e.g., tank shots, horns, connection chimes).
- **Root files**:
  - `changelog.txt`: Project history up to v1.1.
  - `Manifest.txt`: JAR manifest for packaging.
  - `Makefile`: Build script for compiling Java and creating Tank.jar.

## Dependencies
Developed and tested on:
- **Operating System**: Raspbian GNU/Linux 7 (wheezy).
- **Hardware**: Raspberry Pi Model 1 B (revision 2 board).
- **Java Version**: 1.7.0_40.

Required libraries:
- **JInput**: 2.0.1 (for PS3 controller input; requires JUtils).
- **Pi4J**: 0.0.5 (for GPIO; requires WiringPi).
- **WiringPi**: 2.13 (runtime dependency for Pi4J).

## Building and Running
1. Ensure dependencies are installed.
2. Comment out / replace with "Object" any StateMachine references at CommunicationDriver.java.
3. Compile and package: `make all`.
4. Undo changes from step 2.
5. Rebuild and run: `make start`.

## Hardware Notes
- The Raspberry Pi communicates with the PIC16F1829 via SPI.
- Sensors: IR for line following (left, middle, right).
- Motors: Three DC motors (left/right tracks, turret) controlled via PWM.
- Controller: PS3 via Bluetooth (using JInput).
- LEDs: For feedback (e.g., when receiving PS3 input or when following a line).