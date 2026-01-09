# Getting Started: View diagrams as SVG (local rendering)



Scalable Vector Graphics (SVG) is an XML-based vector graphics format for defining two-dimensional graphics

This guide shows how to render ASSIS-generated PlantUML diagrams
as SVG images using local tools.

No IDE is required.

> Verified with ASSIS v0.9.3 (beta)

---

## What you will need

- A **terminal**
- **JDK 17 or newer**
- A terminal
- PlantUML
- Graphviz (`dot`)
- A web browser

---

## Step 1: Make sure you have a diagram file

This guide assumes you already ran **Getting Started (bash)** and have:

docs/diagrams/src/class-diagram.puml

If not, generate it first using the main guide.

---

## Step 2: Install PlantUML and Graphviz

### Linux (Debian / Ubuntu / CodeSpaces)

```bash
sudo apt update
sudo apt install -y plantuml graphviz
```

Verify installation:

```bash
plantuml -version
dot -V
```

---

### macOS (Homebrew)

Homebrew is a free and open-source software package management system that simplifies the installation of software on Apple computers.

If you don’t have Homebrew, see: https://brew.sh

```bash
brew install plantuml graphviz
```

Verify:

```bash
plantuml -version
dot -V
```

---

### Windows

Option A: WSL (recommended)

Windows Subsystem for Linux (WSL) is a component of Microsoft Windows that allows the use of a Linux environment from within Windows

If you are using WSL (Ubuntu), follow the Linux instructions above.

Option B: Native Windows

- Install Graphviz: https://graphviz.org/download/
- Install PlantUML:
  - via Chocolatey:
    ```powershell
    choco install plantuml
    ```
  - or download the PlantUML JAR from https://plantuml.com

Make sure `dot` is available on your PATH.

---

## Step 3: Generate SVG diagrams

From your project root:

```bash
plantuml -tsvg -o ../svg docs/diagrams/src/*.puml
```

This generates SVG files next to the `.puml` files.

Example:

docs/diagrams/src/
- class-diagram.puml
- class-diagram.svg

---

## Step 4: Open the SVG

Linux:
```bash
xdg-open docs/diagrams/src/svg/class-diagram.svg
```

macOS:
```bash
open docs/diagrams/src/dvg/class-diagram.svg
```

Windows:
```powershell
start docs\diagrams\src\svg\class-diagram.svg
```

You may also open the SVG in a web browser, VS Code, or vector tools such as Inkscape.

---


