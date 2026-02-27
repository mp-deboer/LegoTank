# Java Source

This folder contains the Java source code for the Raspberry Pi-based control software of the LEGO tank robot project. It implements concurrent state machines for handling PS3/PS5 controller input, hardware interactions, line following, and other behaviors. The code uses libraries like Pi4J for GPIO/SPI, JInput for PS3 & PS5 controller, and relies on the PIC firmware (in `../Pibotica.X`) for low-level motor/sensor control.

## Code Structure
- **Core**:
  - `Main.java`: Entry point, initializes drivers, state machines, and threads (e.g., for mode, controller, hardware, sounds).
  - `CommunicationDriver.java`: Manages event dispatching and process registration for concurrent state machines.
  - `StateMachine.java`: Abstract base class for state machines, handling events, sensitivities, and execution.

- **Drivers**:
  - `HardwareDriver.java`: Maps high-level commands to SPI bytes for PIC communication (e.g., motor speeds, sensor reads, LEDs). Uses Pi4J for GPIO and WiringPi for SPI.
  - `Sounds.java`: Triggers .wav audio files on events (e.g., controller connect/disconnect, button presses like circle for shots).

- **Logic**:
  - `LineFollower.java`: Implements line-following behavior using IR sensors, adjusting motor directions/speeds based on sensor values.
  - Specific StateMachine implementations:
    - `Led2.java`: Controls an LED based on controller events.
    - `Mode.java`: Manages operation modes (e.g., off, control, follow).
    - `Motors.java`: Handles motor acceleration/deceleration for tracks and turret.

- **Input**:
  - `PsController.java`: Polls PS3/PS5 controller components (buttons, sticks) using JInput, fires events when connection status changes.
  - `Buttons.java`: Threaded handling for PS3/PS5 buttons, translating presses/releases to events.
  - `Tracks.java`: Processes left stick input for track control (direction, speed).
  - `Turret.java`: Processes right stick input for turret rotation.

- **Utilities**:
  - `PsComponent.java`: Enum for PS3 & PS5 controller components (e.g., buttons, axes).
  - `SensorPosition.java`: Enum for IR sensor positions (left, middle, right).
  - `../../StateMachineTemplate.java`: Template for creating new state machines.

## Dependencies and Build
See root README.md for full dependencies (e.g., Java 1.7, Pi4J 0.0.5, JInput 2.0.1). Use the root Makefile to compile and run.

This code is tightly coupled with LTS designs (in `../../lts/`) for concurrency validation.