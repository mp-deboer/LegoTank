# LegoTank

This is a revival of a 2014 school project: a line-following robot built as a LEGO tank. The project runs on a Raspberry Pi for high-level control and a PIC microcontroller for low-level hardware interfacing. The tank supports manual control via either a PS3 or a PS5 controller, line following, sounds, and basic motor/sensor operations.

## Project Structure
- **lts/**: Design and modeling files for concurrency and state machines. See [lts/README.md](./lts/README.md) for details on LTS files and the LTSA tool.
- **src/Pibotica.X**: C firmware for the PIC microcontroller (PiBotica PCB). See [src/Pibotica.X/README.md](./src/Pibotica.X/README.md) for details on the firmware and microcontroller.
- **src/tankpack**: Java control software running on the Raspberry Pi. See [src/tankpack/README.md](./src/tankpack/README.md) for details on code structure, core classes, drivers, logic, input handling, and utilities.
- **sound/**: Audio resources (.wav files) for event-triggered sounds (e.g., tank shots, horns, connection chimes).
- **Root files**:
  - `Manifest.txt`: JAR manifest for packaging.
  - `Makefile`: Build script for compiling Java and creating Tank.jar.

## Dependencies
Developed and tested on:
- **Operating System**: Raspberry Pi OS (Legacy, 32-bit) Lite (Raspbian GNU/Linux 12 / Bookworm).
- **Hardware**: Raspberry Pi Model 1 B (revision 2 board).
- **Java Version**: OpenJDK 17.

Required libraries:
- **JInput**: 2.0.1 (for PS3/PS5 controller input; requires JUtils).
- **libgpiod2**: 1.6.3 (required during runtime for setting the state of LED2)

### SPI Configuration (required for HardwareDriver)
The PIC firmware expects a SPI clock speed of 500 kHz.
As the default SPI clock speed cannot be set directly via `/boot/firmware/config.txt` on Raspberry Pi OS Bookworm, a custom device tree overlay must be created.

#### Steps to create and install the overlay

1. Download the base overlay source:
<pre><code>wget https://raw.githubusercontent.com/raspberrypi/linux/rpi-6.6.y/arch/arm/boot/dts/overlays/spi0-1cs-overlay.dts</code></pre>

2. Edit the file and add the following fragment **before** the `__overrides__` section:
<pre><code>fragment@4 {
    target = <&spidev0>;
    __overlay__ {
        spi-max-frequency = <500000>;
    };
};</code></pre>

3. Save as `custom-spi0-1cs.dts`, then compile:
<pre><code>dtc -@ -Hepapr -I dts -O dtb -o custom-spi0-1cs.dtbo custom-spi0-1cs.dts</code></pre>

4. Copy the compiled overlay:
<pre><code>sudo cp custom-spi0-1cs.dtbo /boot/firmware/overlays/</code></pre>

5. Add to `/boot/firmware/config.txt` (at the end):
<pre><code>dtoverlay=custom-spi0-1cs</code></pre>

6. Reboot:
<pre><code>sudo reboot</code></pre>

After this step, the SPI bus will default to 500 kHz, matching the speed of the PIC Firmware.

## Building and Running
1. Follow the SPI Configuration above.
2. Ensure dependencies are installed.
3. Compile and package: `make all`.
4. Run: `make start`.

## Hardware Notes
- The Raspberry Pi communicates with the PIC16F1829 via SPI.
- Sensors: IR for line following (left, middle, right).
- Motors: Three DC motors (left/right tracks, turret) controlled via PWM.
- Controller: PS3 or PS5 via Bluetooth (using JInput).
- LEDs: For feedback (e.g., when receiving PS3/PS5 input or when following a line).