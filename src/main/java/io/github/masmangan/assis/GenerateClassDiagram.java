/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.io.IOException;
import java.io.PrintWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

enum Kind {
    CLASS, INTERFACE, ENUM, RECORD, ANNOTATION
}

enum Modifier {
    ABSTRACT, FINAL
}

/**
 * The {@code GenerateClassDiagram} class is a PlantUML class diagram generator.
 */
public class GenerateClassDiagram {

    /**
     * 
     */
    private static final Logger logger = Logger.getLogger(GenerateClassDiagram.class.getName());

    /**
     * 
     */
    private GenerateClassDiagram() {
    }

    /**
     * Symbol table entry
     */
    static class TypeInfo {
        String pkg;
        String name;
        Kind kind = Kind.CLASS;
        EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);

        Set<String> extendsTypes = new LinkedHashSet<>();
        Set<String> implementsTypes = new LinkedHashSet<>();

        Set<String> fieldsToTypes = new LinkedHashSet<>();
        Set<String> methods = new LinkedHashSet<>();
    }

    /**
     * Extracts package version information.
     * 
     * @return
     */
    private static String versionOrDev() {
        String v = GenerateClassDiagram.class.getPackage().getImplementationVersion();
        return (v == null || v.isBlank()) ? "dev" : v;
    }

    /**
     * Generates code for the current project.
     * 
     * @throws Exception
     */
    public static void generate() throws IOException {
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
    public static void generate(Path src, Path out) throws IOException {
        logger.log(Level.INFO, () -> "ASSIS " + versionOrDev() + " (Java -> UML)");

        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(config);

        Map<String, TypeInfo> types = new HashMap<>();

        logger.log(Level.INFO, () -> "Scanning " + src);
        scanSources(src, types);

        logger.log(Level.INFO, () -> "Writing " + out);
        writeDiagram(out, types);
    }

    /**
     * 
     * @param pw
     */
    private static void addHeader(PrintWriter pw) {
        pw.println("@startuml class-diagram");
        pw.println("hide empty members");
        pw.println("!theme blueprint");
    }

    /**
     * 
     * @param t
     * @return
     */
    private static String getClassifier(TypeInfo t) {
        String classifier = "**error at classifier**  {";
        if (t.kind == Kind.CLASS) {
            if (t.modifiers.contains(Modifier.ABSTRACT)) {
                classifier = "abstract class " + t.name + " {";
            } else if (t.modifiers.contains(Modifier.FINAL)) {
                classifier = "class " + t.name + " <<final>>  {";
            } else {
                classifier = "class " + t.name + " {";
            }
        } else if (t.kind == Kind.INTERFACE) {
            classifier = "interface " + t.name + " {";
        } else if (t.kind == Kind.RECORD) {
            classifier = "record " + t.name + " {";
        } else if (t.kind == Kind.ENUM) {
            classifier = "enum " + t.name + " {";
        } else if (t.kind == Kind.ANNOTATION) {
            classifier = "annotation " + t.name + " {";
        } else {
            logger.log(Level.WARNING, () -> "Unexpected type: " + t.toString());
        }

        return classifier;
    }

    /**
     * 
     * @param pw
     * @param byPkg
     * @throws IOException
     */
    private static void writePackages(PrintWriter pw, Map<String, List<TypeInfo>> byPkg) {
        for (var entry : byPkg.entrySet()) {
            String pkg = entry.getKey();

            if (!pkg.isEmpty()) {
                pw.println("package \"" + pkg + "\" {");
            }

            for (TypeInfo t : entry.getValue()) {
                String classifier = getClassifier(t);
                pw.println(classifier);
                for (String m : t.methods) {
                    pw.println("  " + m);
                }
                pw.println("}");
            }

            if (!pkg.isEmpty()) {
                pw.println("}");
            }
        }

    }

    /**
     * 
     * @param pw
     * @param types
     * @throws IOException
     */
    private static void writeRelationships(PrintWriter pw, Map<String, TypeInfo> types) {
        // Extends and implements
        for (TypeInfo t : types.values()) {
            writeExtends(pw, types, t);
            writeImplements(pw, types, t);
        }

        // Associations
        for (TypeInfo t : types.values()) {
            writeAssociations(pw, types, t);
        }
    }

    /**
     * 
     * @param pw
     * @param types
     * @param t
     */
    private static void writeAssociations(PrintWriter pw, Map<String, TypeInfo> types, TypeInfo t) {
        for (String f : t.fieldsToTypes) {
            if (types.containsKey(f) && !f.equals(t.name)) {
                pw.println(t.name + " --> " + f);
            }
        }
    }

    /**
     * 
     * @param pw
     * @param types
     * @param t
     */
    private static void writeImplements(PrintWriter pw, Map<String, TypeInfo> types, TypeInfo t) {
        for (String i : t.implementsTypes) {
            if (types.containsKey(i)) {
                pw.println(i + " <|.. " + t.name);
            }
        }
    }

    /**
     * 
     * @param pw
     * @param types
     * @param t
     */
    private static void writeExtends(PrintWriter pw, Map<String, TypeInfo> types, TypeInfo t) {
        for (String e : t.extendsTypes) {
            if (types.containsKey(e)) {
                pw.println(e + " <|-- " + t.name);
            }
        }
    }

    /**
     * 
     * @param out
     * @param types
     */
    private static void writeDiagram(Path out, Map<String, TypeInfo> types) {
        // Generate PlantUML
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
            addHeader(pw);

            // packages
            Map<String, List<TypeInfo>> byPkg = types.values().stream()
                    .collect(Collectors.groupingBy(t -> t.pkg == null ? "" : t.pkg));
            writePackages(pw, byPkg);

            writeRelationships(pw, types);

            pw.println();
            pw.println("left to right direction");

            addFooter(pw);

            pw.println("@enduml");
        } catch (IOException e) {
            logger.log(Level.WARNING, () -> "Error writing diagram file: " + e.getLocalizedMessage());
        }
    }

    /**
     * 
     * @param src
     * @param types
     * @throws IOException
     */
    private static void scanSources(Path src, Map<String, TypeInfo> types) throws IOException {
        List<Path> files = new ArrayList<>();
        scanJavaSources(src, files);

        for (Path p : files) {
            scanSource(types, p);
        }
    }

    /**
     * 
     * @param code
     * @return
     */
    private static CompilationUnit getCompilationUnit(String code) {
        try {
            return StaticJavaParser.parse(code);
        } catch (Exception e) {
            logger.log(Level.WARNING, () -> "Parser fail:  " + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * 
     * @param types
     * @param p
     * @throws IOException
     */
    private static void scanSource(Map<String, TypeInfo> types, Path p) throws IOException {
        String code = Files.readString(p);
        CompilationUnit cu = getCompilationUnit(code);

        String pkg = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");

        for (TypeDeclaration<?> td : cu.getTypes()) {
            scanType(types, pkg, td);
        }
    }

    /**
     * 
     * @param types
     * @param pkg
     * @param td
     */
    private static void scanType(Map<String, TypeInfo> types, String pkg, TypeDeclaration<?> td) {

        TypeInfo info = new TypeInfo();
        info.pkg = pkg;

        if (td instanceof com.github.javaparser.ast.body.EnumDeclaration ed) {
            info.name = ed.getNameAsString();
            info.kind = Kind.ENUM;
        } else if (td instanceof com.github.javaparser.ast.body.RecordDeclaration rd) {
            info.name = rd.getNameAsString();
            info.kind = Kind.RECORD;
        } else if (td instanceof com.github.javaparser.ast.body.AnnotationDeclaration ad) {
            info.name = ad.getNameAsString();
            info.kind = Kind.ANNOTATION;
        } else if (td instanceof ClassOrInterfaceDeclaration cid) {
            scanClassOrInterface(info, cid);
        } else {
            logger.log(Level.WARNING, () -> "Unexpected type: " + td.toString());
        }
        types.put(info.name, info);
    }

    /**
     * 
     * @param info
     * @param cid
     */
    private static void scanClassOrInterface(TypeInfo info, ClassOrInterfaceDeclaration cid) {
        info.name = cid.getNameAsString();
        if (cid.isInterface()) {
            info.kind = Kind.INTERFACE;
        } else {
            info.kind = Kind.CLASS;
            if (cid.isAbstract()) {
                info.modifiers.add(Modifier.ABSTRACT);
            } else if (cid.isFinal()) {
                info.modifiers.add(Modifier.FINAL);
            }
        }

        // extends
        for (ClassOrInterfaceType ext : cid.getExtendedTypes()) {
            info.extendsTypes.add(simpleName(ext.getNameAsString()));
        }

        // implements
        for (ClassOrInterfaceType impl : cid.getImplementedTypes()) {
            info.implementsTypes.add(simpleName(impl.getNameAsString()));
        }

        // fields -> association candidates
        for (FieldDeclaration fd : cid.getFields()) {
            String t = scanField(fd);
            info.fieldsToTypes.add(simpleName(t));
        }

        // methods
        for (MethodDeclaration method : cid.getMethods()) {
            if (!method.isPublic()) {
                continue;
            }

            String signature = scanMethod(method);
            info.methods.add(signature);
        }
    }

    private static String scanField(FieldDeclaration fd) {
        String t = fd.getElementType().asString();
        // tira generics e arrays simples
        t = t.replaceAll("<.*>", "").replace("[]", "");
        return t;
    }

    /**
     * 
     * @param method
     * @return
     */
    private static String scanMethod(MethodDeclaration method) {
        String returnType = method.getType().asString();
        String name = method.getNameAsString();

        String params = method.getParameters().stream()
                .map(param -> param.getNameAsString() + " : " + param.getType().asString())
                .collect(Collectors.joining(", "));

        String flags = getFlags(method);

        return "+ " + name + "(" + params + ") : " + returnType + flags;
    }

    /**
     * 
     * @param src
     * @param files
     */
    private static void scanJavaSources(Path src, List<Path> files) {
        if (Files.exists(src)) {
            try (var s = Files.walk(src)) {
                s.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            } catch (Exception e) {
                logger.log(Level.WARNING, () -> "Error walking source folder: " + e.getMessage());
            }
        }
    }

    /**
     * 
     * @param method
     * @param flags
     * @return
     */
    private static String getFlags(MethodDeclaration method) {
        String flags = "";

        if (method.isStatic()) {
            flags += " {static}";
        } 
        
        if (method.isAbstract()) {
            flags += " {abstract}";
        }

        if (method.isFinal()) {
            flags += " {final}";
        }
        return flags;
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
        pw.println("Generated with ASSIS (Java -> UML) at " + timestamp);
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