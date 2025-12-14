package io.github.masmangan.assis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.transform.Source;

/**
 * 
 */
public class GenerateClassDiagram {

    /**
     * 
     */
    static class TypeInfo {
        String pkg;
        String name;
        boolean isInterface;
        Set<String> extendsTypes = new HashSet<>();
        Set<String> implementsTypes = new HashSet<>();
        Set<String> fieldsToTypes = new HashSet<>();
    }

    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void generate(String[] args) throws Exception {
        Path src = Paths.get("src/main/java");
        Path out = Paths.get("docs/uml/class-diagram.puml");
        Files.createDirectories(out.getParent());

        Map<String, TypeInfo> types = new HashMap<>();
        List<Path> files = new ArrayList<>();
        if (Files.exists(src)) {
            try (var s = Files.walk(src)) {
                s.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            }
        }

        for (Path p : files) {
            String code = Files.readString(p);
            CompilationUnit cu;
            try {
                cu = StaticJavaParser.parse(code);
            } catch (Exception e) {
                System.err.println("Parser fail: " + p + " (" + e.getMessage() + ")");
                continue;
            }

            String pkg = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");

            for (TypeDeclaration<?> td : cu.getTypes()) {
                if (!(td instanceof ClassOrInterfaceDeclaration))
                    continue;

                ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) td;
                TypeInfo info = new TypeInfo();
                info.pkg = pkg;
                info.name = cid.getNameAsString();
                info.isInterface = cid.isInterface();

                // extends / implements
                for (ClassOrInterfaceType ext : cid.getExtendedTypes()) {
                    info.extendsTypes.add(simpleName(ext.getNameAsString()));
                }
                for (ClassOrInterfaceType impl : cid.getImplementedTypes()) {
                    info.implementsTypes.add(simpleName(impl.getNameAsString()));
                }

                // fields -> association candidates
                for (FieldDeclaration fd : cid.getFields()) {
                    String t = fd.getElementType().asString();
                    // tira generics e arrays simples
                    t = t.replaceAll("<.*>", "").replace("[]", "");
                    info.fieldsToTypes.add(simpleName(t));
                }

                types.put(info.name, info);
            }
        }

        // Generate PlantUML
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
            pw.println("@startuml");
            // pw.println("skinparam classAttributeIconSize 0");
            pw.println("hide empty members");
            pw.println("!theme blueprint");

            // packages
            Map<String, List<TypeInfo>> byPkg = types.values().stream()
                    .collect(Collectors.groupingBy(t -> t.pkg == null ? "" : t.pkg));

            for (var entry : byPkg.entrySet()) {
                String pkg = entry.getKey();
                if (!pkg.isEmpty())
                    pw.println("package \"" + pkg + "\" {");
                for (TypeInfo t : entry.getValue()) {
                    if (t.isInterface) {
                        pw.println("interface " + t.name);
                    } else {
                        pw.println("class " + t.name);
                    }
                }
                if (!pkg.isEmpty())
                    pw.println("}");
            }

            // Extends and implements
            for (TypeInfo t : types.values()) {
                for (String e : t.extendsTypes) {
                    if (types.containsKey(e)) {
                        pw.println(e + " <|-- " + t.name);
                    }
                }
                for (String i : t.implementsTypes) {
                    if (types.containsKey(i)) {
                        pw.println(i + " <|.. " + t.name);
                    }
                }
            }

            // Associations
            for (TypeInfo t : types.values()) {
                for (String f : t.fieldsToTypes) {
                    if (types.containsKey(f) && !f.equals(t.name)) {
                        pw.println(t.name + " --> " + f);
                    }
                }
            }

            pw.println();
            pw.println("left to right direction");

            addAssisNote(pw);
            
            pw.println("@enduml");
        }

        System.out.println("Diagram at: " + out.toAbsolutePath());
    }

    private static void addAssisNote(PrintWriter pw) {
            pw.println("note bottom");
            pw.println("Generated with ASSIS (Java → UML)");

            pw.println("Source repository:");
            pw.println("https://github.com/masmangan/javaparser-to-plantuml");
            pw.println("end note");
    }

    /**
     * 
     * @param qname
     * @return
     */
    private static String simpleName(String qname) {
        int lt = qname.lastIndexOf('.');
        return (lt >= 0) ? qname.substring(lt + 1) : qname;
    }
}