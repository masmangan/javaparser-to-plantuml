# javaparser-to-plantuml
Generate a UML diagram from Java code, using Java Parser and PlantUML



## Build
```bash
mvn -DskipTests package
```

## Run

```bash
java -jar target/assis-1.0-SNAPSHOT.jar
```

## Run (Development)
```
mvn -q -DskipTests exec:java -Dexec.mainClass=io.github.masmangan.assis.AssisApp
```

## Classes Overview

![Assis](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/masmangan/javaparser-to-plantuml/refs/heads/main/docs/uml/class-diagram.puml)

## License

This project is licensed under the MIT License.

## References

Assis uses existing open-source tools as building blocks:

- **PlantUML** — UML tool 
  https://plantuml.com

- **JavaParser** — Java source code parser  
  https://javaparser.org
