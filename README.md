# LegoTank

This is a revival of a 2014 school project: a line-following robot built as a LEGO tank. The project runs on a Raspberry Pi for high-level control and a PIC microcontroller for low-level hardware interfacing. The tank supports manual control via a PS3 controller, line following, sounds, and basic motor/sensor operations.

The source code in this repo is the original, as-is version from the project's completion (v1.1). Future commits will show revival efforts, including bug fixes, modernizations (e.g., added PS5 controller compatibility, updated OS), and new features.

## Project Structure
- **lts/**: Design and modeling files for concurrency and state machines. See [README.md in that folder](./lts/README.md) for details.
- **src/Pibotica.X**: C firmware for the PIC microcontroller. See [README.md in that folder](./src/Pibotica.X/README.md) for details.
- **src/tankpack**: Java control software running on the Raspberry Pi, implementing concurrent state machines for behaviors. Breakdown:
  - Core: `Main.java` (entry point), `CommunicationDriver.java` (manages events and processes), `StateMachine.java` (abstract base for state machines).
  - Drivers: `HardwareDriver.java` (handles SPI/GPIO for hardware commands, e.g., motors, sensors, LEDs), `Sounds.java` (triggers audio on events like controller connect/disconnect).
  - Logic: `LineFollower.java` (implements line-following using IR sensors), and StateMachine implementations like `Led2.java` (LED control), `Mode.java` (operation modes), `Motors.java` (motor acceleration/deceleration).
  - Input: `PsController.java` (polls PS3 controller), `Buttons.java` (handles button events), `Tracks.java` (translates left stick to track movements), `Turret.java` (translates right stick to turret control).
  - Utilities: `PsComponent.java` and `SensorPosition.java` (enums for controller components and sensors), `StateMachineTemplate.java` (template for new state machines).
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
3. Build CommunicationDriver: `make bin/tankpack/CommunicationDriver.class`.
4. Compile and package: `make all`.
5. Undo changes from step 2.
6. Rebuild and run: `make start`.

## Hardware Notes
- The Raspberry Pi communicates with the PIC16F1829 via SPI.
- Sensors: IR for line following (left, middle, right).
- Motors: Three DC motors (left/right tracks, turret) controlled via PWM.
- Controller: PS3 via Bluetooth (using JInput).
- LEDs: For feedback (e.g., when receiving PS3 input or when following a line).