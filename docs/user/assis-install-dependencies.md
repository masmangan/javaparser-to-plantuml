# Setup and Installation Guide

This guide explains how to install the external tools commonly used with ASSIS.

You do NOT need all of these tools to use ASSIS.
ASSIS itself only requires Java.

Use this guide only if another guide points you here.

---

## Required

### Java (JDK 17 or newer)

Check your Java version:

```bash
java -version
```

If Java is missing or too old, install a JDK 17+ distribution.

Recommended:
- Eclipse Temurin (Adoptium)
- OpenJDK packages from your OS

---

## Optional: Tools to view diagrams

ASSIS generates PlantUML (`.puml`) files.
To view diagrams as images, you may install:

- PlantUML
- Graphviz (`dot`)

These tools are optional.

---

## Linux (Debian / Ubuntu / CodeSpaces)

```bash
sudo apt update
sudo apt install -y plantuml graphviz
```

Verify:

```bash
plantuml -version
dot -V
```

---

## macOS (Homebrew)

Install Homebrew if needed:
https://brew.sh

Then:

```bash
brew install plantuml graphviz
```

If Homebrew asks to install additional command-line tools,
accept the prompt. A full Xcode installation is NOT required.

Verify:

```bash
plantuml -version
dot -V
```

---

## Windows

### Option A: WSL (recommended)

If you are using WSL (Ubuntu), follow the Linux instructions above.

### Option B: Native Windows

1. Install Graphviz:
   https://graphviz.org/download/

2. Install PlantUML:
   - via Chocolatey:
     ```powershell
     choco install plantuml
     ```
   - or download the PlantUML JAR from:
     https://plantuml.com

Make sure `dot` is available on your PATH.

Verify:

```powershell
plantuml -version
dot -V
```

---

## Notes

- ASSIS does not bundle PlantUML or Graphviz
- Rendering diagrams is intentionally external
- Keeping tools separate makes ASSIS easier to script and automate

---

## Where this guide is referenced

This guide is referenced from:
- Getting Started (bash)
- Getting Started: View diagrams as SVG

You do not need to read this guide unless one of those links points you here.
