/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.io.PrintWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

/**
 * The {@code GenerateClassDiagram} class is a PlantUML class diagram generator.
 */
public class GenerateClassDiagram {

    /**
     * Simplified symbol table entry
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
     * Generates code for the current project.
     * 
     * @throws Exception
     */
    public static void generate() throws Exception {
        Path src = Paths.get("src/main/java");
        Path out = Paths.get("docs/uml/class-diagram.puml");
        Files.createDirectories(out.getParent());
        generate(src, out);
    }

    /**
     * Generates code for a given source code and output path.
     * 
     * @param src source code path
     * @param out output path
     * @throws Exception
     */
    public static void generate(Path src, Path out) throws Exception {

        System.out.println("Scanning " + src);

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

        System.out.println("Writing " + out);

        // Generate PlantUML
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
            pw.println("@startuml class-diagram");
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

            addFooter(pw);

            pw.println("@enduml");
        }

       // System.out.println("Diagram at: " + out.toAbsolutePath());
    }

    /**
     * Generates a footer with Assis watermark.
     * 
     * @param pw this translation open printwriter
     */
    private static void addFooter(PrintWriter pw) {
        String timestamp = OffsetDateTime
                .now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        pw.println();
        pw.println("center footer");
        pw.println("Generated with ASSIS (Java -> UML) at: " + timestamp);
        pw.println("https://github.com/masmangan/javaparser-to-plantuml");
        pw.println("end footer");
        pw.println();
    }

    /**
     * Extracts name from classifier.
     * 
     * @param qname
     * @return
     */
    private static String simpleName(String qname) {
        int lt = qname.lastIndexOf('.');
        return (lt >= 0) ? qname.substring(lt + 1) : qname;
    }

}