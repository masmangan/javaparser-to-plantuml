# Getting Started (bash)

This guide shows how to run **ASSIS CLI** from the command line and generate your first UML diagram from a minimal Java source file.

No IDE is required.

> Verified with ASSIS v0.9.3 (beta)

---

## What you will need

- A **terminal**
- **JDK 17 or newer**
- A **text editor** (any: `vi`, `nano`, VS Code, etc.)

---

## Step 1: Open a terminal

Open your system terminal.

> **Tip**  
> If you are using PowerShell, don’t worry — ASSIS works there too.  
> Examples below assume a Unix-like shell.

---

## Step 2: Create a working directory

```bash
mkdir assis-gs
cd assis-gs
```

This directory will hold:
- a Java source file (`A.java`)
- a PlantUML file (`docs/diagrams/src/class-diagram.puml`)
- the ASSIS CLI JAR file (`assis.jar`)

---

## Step 3: Download ASSIS

Download the ASSIS CLI JAR file from GitHub Releases.

For **v0.9.3**:

```bash
wget https://github.com/masmangan/assis/releases/download/v0.9.3/assis-cli-0.9.3-beta.jar -O assis.jar
```

If `wget` is not available, you may use:

```bash
curl -L https://github.com/masmangan/assis/releases/download/v0.9.3/assis-cli-0.9.3-beta.jar -o assis.jar
```

Or download the file manually from:

https://github.com/masmangan/assis/releases

and save it as `assis.jar`.

---

## Step 4: Verify ASSIS is working

Run:

```bash
java -jar assis.jar --version
```

You should see the ASSIS version printed on the terminal.

✔ **Quick check:** if this works, ASSIS is correctly installed.

---

## Step 5: Create a minimal Java file

Create a file named `A.java` with a single class.

Using the command line:

```bash
cat <<EOF > A.java
class A {
}
EOF
```

This creates a simple Java type to analyze.

---

## Step 6: Run ASSIS

Run ASSIS in the current directory:

```bash
java -jar assis.jar
```

ASSIS will:
- Scan Java source files in the current folder
- Generate UML diagrams in PlantUML format under `docs/diagrams/src/`

> **Tip**  
> Make your life easier by creating an alias:
> ```bash
> alias assis='java -jar assis.jar'
> ```
> Then simply run:
> ```bash
> assis
> ```
> You may add this alias to your `~/.bashrc` or `~/.zshrc`.

---

## Step 7: Open the generated diagram

Look for the generated `class-diagram.puml` file.

A minimal example looks like:

```text
@startuml
class A {
}
@enduml
```

✔ You are done.

You have successfully generated a PlantUML class diagram from Java source code.

> **Tip**  
> To see available options:
> ```bash
> java -jar assis.jar --help
> ```
>
> In this guide, ASSIS effectively runs:
> ```bash
> java -jar assis.jar -sourcepath . -d docs/diagrams/src/
> ```
> These defaults work because the guide uses a flat directory structure.


At this point, you have a UML diagram in PlantUML format:

docs/diagrams/src/class-diagram.puml

This text file fully describes the diagram.

From here, you may choose how to view it:

- View it locally as SVG (recommended)
- Use the PlantUML Online Server (no installation)

See:
- Getting Started: View diagrams as SVG
- Getting Started: View diagrams via PlantUML Online

---

## Found a problem?

If something did not work as expected, please report it:

👉 https://github.com/masmangan/assis/issues

When reporting an issue, include:
- your operating system
- Java version (`java -version`)
- ASSIS version (`--version`)
- the command you ran
- any error message or screenshot

---

## Final note

ASSIS is a **static analysis tool**.

It does not:
- execute Java code
- require a build
- depend on frameworks
- infer runtime behavior

It simply reads Java source files and exposes their structure.

---
