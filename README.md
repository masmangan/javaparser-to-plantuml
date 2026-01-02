[![SonarCloud analysis](https://github.com/masmangan/assis/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/masmangan/assis/actions/workflows/sonarcloud.yml)
[![CodeQL Advanced](https://github.com/masmangan/assis/actions/workflows/codeql.yml/badge.svg)](https://github.com/masmangan/assis/actions/workflows/codeql.yml)

# ASSIS
Generate a UML diagram from Java code, using Java Parser and PlantUML


## Usage

### Get the latest version

Download the latest release at: 
https://github.com/masmangan/assis/releases

### Place the jar and run ASSIS

ASSIS is the assistant that will scan your source folder for .java files and generate a class-diagram.puml.

For instance, for version v0.7.0:

Get the file at https://github.com/masmangan/assis/releases/download/v0.7.0/assis-0.7.0.jar

Place the jar at the project root folder.

Run ASSIS at command line:

```bash
java -jar assis-0.7.0.jar
```

Get the class-diagram.puml.

The diagram can be edit and rendered at Plantuml Online Server (https://www.plantuml.com/plantuml/uml/) or the .puml can be embeded on a Markdown file. See the diagram bellow for an example.

## Development

### Build

```bash
mvn -DskipTests package
```

### Run with Jar

```bash
java -jar target/assis-0.7.0.jar
```

### Run with Maven
```
mvn -q -DskipTests exec:java -Dexec.mainClass=io.github.masmangan.assis.AssisApp
```

## ASSIS Classes Overview

![Assis](http://www.plantuml.com/plantuml/proxy?cache=no&fmt=svg&src=https://raw.githubusercontent.com/masmangan/assis/refs/heads/main/assis-core/docs/diagrams/src/class-diagram.puml)


## License

This project is licensed under the MIT License.

## Use of Artificial Intelligence

ASSIS was developed with the assistance of Artificial Intelligence tools used in a
conversational and reflective role (e.g., architectural discussion, diagram modeling,
and documentation), rather than as inline code generation or autonomous programming agents.

All final decisions and implementations remain the responsibility of the author.


## References

Assis uses existing open-source tools as building blocks:

- **PlantUML** — UML tool 
  https://plantuml.com

- **JavaParser** — Java source code parser  
  https://javaparser.org
