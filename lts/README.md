# LTS Folder: Concurrency and State Machine Design Files

This folder contains the design and modeling files for the project's concurrency logic, developed during the 2014 school project. These are Labelled Transition System (LTS) files created using the LTSA tool, representing state machines for components like motor control, event handling, and behaviors (e.g., line following, PS3 input).

Key contents:
- Individual LTS files: Model specific state machines for concurrent processes (e.g., buttons, tracks, turret).
- Combined LTS files: Integrated models used for deadlock detection, debugging, and verifying system properties (e.g., no deadlocks in event flows).

## LTSA Tool Overview
LTSA (Labelled Transition System Analyser) is a verification tool for concurrent systems, developed at Imperial College London (Department of Computing). It mechanically checks that a system's specification satisfies required properties.

- **Purpose**: Models systems as interacting finite state machines. Verifies behaviors like safety (e.g., no invalid states) and liveness (e.g., no deadlocks). Supports graphical views of LTS and property checking.
- **Notation**: Uses Finite State Processes (FSP), a process algebra for concise descriptions of component behaviors.
- **Usage in This Project**: LTS files were used to design and validate the Java state machines (e.g., in CommunicationDriver and subclasses) before implementation. Combined files helped detect deadlocks in the overall system.
- **Features**: Graphical LTS visualization, composition of multiple LTS, deadlock/property analysis.
- **Download/Info**: Available from [Imperial College's LTSA page](https://www.doc.ic.ac.uk/ltsa) (as of 2026, including Java app and Eclipse plugin versions). Examples and help are included in the tool.

For revival, these files can be loaded into LTSA to re-verify or extend the designs.