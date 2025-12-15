# javaparser-to-plantuml
Generate a UML diagram from Java code, using Java Parser and PlantUML


## Usage

### Get the latest version

Download the latest release at: 
https://github.com/masmangan/javaparser-to-plantuml/releases

### Place the jar and run ASSIS

ASSIS is the assistant that will scan your source folder for .java files and generate a class-diagram.puml.

For instance, for version v0.1.0:

Get the file at https://github.com/masmangan/javaparser-to-plantuml/releases/download/v0.1.0/assis.jar

Place the jar at the project root folder.

Run ASSIS at command line:

```bash
java -jar assis.jar
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
java -jar target/assis-0.1.0.jar
```

### Run with Maven
```
mvn -q -DskipTests exec:java -Dexec.mainClass=io.github.masmangan.assis.AssisApp
```

## ASSIS Classes Overview

![Assis](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/masmangan/javaparser-to-plantuml/refs/heads/main/docs/uml/class-diagram.puml)

## License

This project is licensed under the MIT License.

## References

Assis uses existing open-source tools as building blocks:

- **PlantUML** — UML tool 
  https://plantuml.com

- **JavaParser** — Java source code parser  
  https://javaparser.org
