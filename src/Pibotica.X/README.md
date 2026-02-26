# PiBotica.X: C Firmware for PIC Microcontroller

This folder contains the original C source code (`PiBotica_Marco.c`) for the PIC16F1829 microcontroller used in the LEGO tank robot project (deployed as firmware). PiBotica is the name of the Printed Circuit Board (PCB) created during the school project, which hosts the PIC and interfaces with the Raspberry Pi via SPI.

The firmware acts as an SPI slave, translating bytes received from the Raspberry Pi (via SPI) to:
- Control three motors: Turret, left track, and right track (using PWM for speed/direction).
- Toggle an LED (e.g., for status).
- Read infrared sensors (AN0, AN1, AN3, AN6, AN10) on command and return values.

## Microcontroller Details
- **Chip**: PIC16F1829 (8-bit microcontroller from Microchip).
- **Clock**: Internal oscillator at 16 MHz (configured via `#define _XTAL_FREQ 16000000`).
- **Config Bits**: Set for internal oscillator, watchdog off, etc. (see `#pragma config` directives).
- **Key Features Used**:
  - ADC for IR sensor inputs (e.g., line detection).
  - SPI slave mode for commands from Raspberry Pi (e.g., motor speed, direction).
  - Hardware PWM (via CCP modules) and software PWM for motor control.
  - Defines for SPI data parsing (e.g., motor multipliers, boosts).

## Development Tool and Compilation
- **IDE**: This code was written and compiled in MPLAB X, v1.90.
- **Compiler**: XC8, version 1.20.
