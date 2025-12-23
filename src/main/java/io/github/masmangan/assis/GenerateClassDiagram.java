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
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.ParseResult;

/**
 * JLS Types.
 */
enum Kind {
    CLASS, INTERFACE, ENUM, RECORD, ANNOTATION
}

/**
 * Class modifiers.
 */
enum Modifier {
    ABSTRACT, FINAL
}

/**
 * The {@code GenerateClassDiagram} class is a PlantUML Language class diagram
 * generator.
 */
public class GenerateClassDiagram {

    /**
     * Default documentation path.
     * We are borrowing from PlantUML folder convention.
     */
    private static final String DOCS_UML_CLASS_DIAGRAM_PUML = "docs/diagrams/src/class-diagram.puml";

    /**
     * Default source path.
     * We are borrowing from Maven folder convention.
     */
    private static final String SRC_MAIN_JAVA = "src/main/java";

    /**
     * Logs info and warnings.
     */
    private static final Logger logger = Logger.getLogger(GenerateClassDiagram.class.getName());

    /**
     * No constructor available.
     */
    private GenerateClassDiagram() {
    }

    /**
     */
    static class FieldRef {
        final FieldDeclaration fd;
        final com.github.javaparser.ast.body.VariableDeclarator vd;

        FieldRef(FieldDeclaration fd, com.github.javaparser.ast.body.VariableDeclarator vd) {
            this.fd = fd;
            this.vd = vd;
        }
    }

    /**
     * Symbol table entry
     */
    static class TypeInfo {
        String pkg;
        String name;
        Kind kind = Kind.CLASS;
        EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        boolean jpaEntity = false;

        Set<String> extendsTypes = new LinkedHashSet<>();
        Set<String> implementsTypes = new LinkedHashSet<>();
        Set<FieldRef> fields = new LinkedHashSet<>();
        Set<String> methods = new LinkedHashSet<>();
        public TypeDeclaration<?> td;
        public CompilationUnit cu;
    }

    /**
     * Extracts package version information.
     * 
     * @return gets package information from Maven property, or dev otherwise.
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
        Path src = Paths.get(SRC_MAIN_JAVA);
        Path out = Paths.get(DOCS_UML_CLASS_DIAGRAM_PUML);
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
        logger.log(Level.INFO, () -> assisLine());

        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(config);

        Map<String, TypeInfo> types = new HashMap<>();

        logger.log(Level.INFO, () -> "Scanning " + src);
        SourceRoot root = scanSources(src, types);

        logger.log(Level.INFO, () -> "Writing " + out);
        writeDiagram(out, types, root);
    }

    /**
     * 
     * @return
     */
    private static String assisLine() {
        return "ASSIS " + versionOrDev() + " (Java -> UML)";
    }

    /**
     * Adds a header to the diagram.
     * 
     * @param pw
     */
    private static void addHeader(PrintWriter pw) {
        pw.println("@startuml class-diagram");
        pw.println();
        pw.println("hide empty members");
        pw.println("!theme blueprint");
        pw.println("!pragma useIntermediatePackages false");
        pw.println();
    }

    /**
     * Get classifier and modifiers.
     * 
     * Kind enumeration defines available classifiers.
     * Modifier enumeration defines available modifiers.
     * 
     * @param t
     * @return
     */
    private static String getClassifier(TypeInfo t) {
        String classifier = "**error at classifier**";
        if (t.kind == Kind.CLASS) {
            if (t.modifiers.contains(Modifier.ABSTRACT)) {
                classifier = "abstract class " + t.name;
            } else if (t.modifiers.contains(Modifier.FINAL)) {
                classifier = "class " + t.name + " <<final>>";
            } else {
                classifier = "class " + t.name;
                if (t.jpaEntity) {
                    classifier += " <<Entity>>";
                }
            }
            classifier += " {";
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
     * Writes package contents to the diagram.
     * 
     * @param pw
     * @param byPkg
     * @param root
     * @throws IOException
     */
    private static void writePackages(PrintWriter pw, Map<String, List<TypeInfo>> byPkg, Map<String, TypeInfo> types,
            SourceRoot root) {
        for (var entry : byPkg.entrySet()) {
            String pkg = entry.getKey();

            if (!pkg.isEmpty()) {
                pw.println("package \"" + pkg + "\" {");
            }

            for (TypeInfo t : entry.getValue()) {
                String classifier = getClassifier(t);
                pw.println(classifier);

                writeFields(pw, types, t);

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
     * Writes relationships to the diagram.
     * 
     * @param pw
     * @param types
     * @param root
     * @throws IOException
     */
    private static void writeRelationships(PrintWriter pw, Map<String, TypeInfo> types, SourceRoot root) {
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
     * Writes associations from fields.
     * 
     * @param pw
     * @param types
     * @param t
     */
    private static void writeAssociations(PrintWriter pw, Map<String, TypeInfo> types, TypeInfo t) {
        for (FieldRef fr : t.fields) {
            String assocType = assocTypeFrom(fr.fd, fr.vd);
            if (isAssociation(types, t, fr)) {
                pw.println(t.name + " --> " + assocType + " : " + fr.vd.getNameAsString());
            }
        }
    }

    private static String displayTypeFrom(com.github.javaparser.ast.body.VariableDeclarator vd) {
        return vd.getType().asString();
    }

    private static boolean isAssociation(Map<String, TypeInfo> types, TypeInfo t, FieldRef fr) {
        String assocType = assocTypeFrom(fr.fd, fr.vd);
        return types.containsKey(assocType) && !assocType.equals(t.name);
    }

    private static String assocTypeFrom(FieldDeclaration fd, com.github.javaparser.ast.body.VariableDeclarator vd) {
        String s = vd.getType().asString(); // better than fd.getElementType() for weird cases
        s = s.replaceAll("<.*>", "").replace("[]", "");
        return simpleName(s);
    }

    private static void writeFields(PrintWriter pw, Map<String, TypeInfo> types, TypeInfo t) {
        for (FieldRef fr : t.fields) {
            if (isAssociation(types, t, fr)) {
                continue; // keep current behavior: association instead of attribute
            }

            String name = fr.vd.getNameAsString();
            String type = displayTypeFrom(fr.vd);

            // PlantUML underline for static:
            String staticPrefix = fr.fd.isStatic() ? "{static} " : "";

            // visibility (optional now; easy to add)
             String vis = switch (fr.fd.getAccessSpecifier()) {
             case PUBLIC -> "+";
             case PROTECTED -> "#";
             case PRIVATE -> "-";
             default -> "~";
             };

            List<String> mods = new ArrayList<>();
            if (fr.fd.isFinal())
                mods.add("final");
            if (fr.fd.isTransient())
                mods.add("transient");
            if (fr.fd.isVolatile())
                mods.add("volatile");

            String modBlock = mods.isEmpty() ? "" : " {" + String.join(", ", mods) + "}";

            pw.println("  " + vis + " " + staticPrefix + name + " : " + type + modBlock);
        }
    }

    /**
     * Writes implements relationships.
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
     * Writes implements relationships.
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
     * Writes diagram contents.
     * 
     * @param out
     * @param types
     * @param root
     */
    private static void writeDiagram(Path out, Map<String, TypeInfo> types, SourceRoot root) {
        // Generate PlantUML
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
            addHeader(pw);

            // packages
            Map<String, List<TypeInfo>> byPkg = types.values().stream()
                    .collect(Collectors.groupingBy(t -> t.pkg == null ? "" : t.pkg));

            writePackages(pw, byPkg, types, root);

            writeRelationships(pw, types, root);

            pw.println();
            pw.println("left to right direction");

            addFooter(pw);

            pw.println("@enduml");
        } catch (IOException e) {
            logger.log(Level.WARNING, () -> "Error writing diagram file: " + e.getLocalizedMessage());
        }
    }

    private static SourceRoot scanSources(Path src, Map<String, TypeInfo> types) throws IOException {
        if (!Files.exists(src)) {
            logger.log(Level.WARNING, () -> "Source folder does not exist: " + src);
            return null;
        }

        SourceRoot root = new SourceRoot(src);
        root.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

        List<ParseResult<CompilationUnit>> results = root.tryToParse("");

        for (ParseResult<CompilationUnit> r : results) {
            r.getResult().ifPresent(cu -> {

                String pkg = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("");

                for (TypeDeclaration<?> td : cu.getTypes()) {
                    scanType(types, pkg, td); // <-- reuse your existing logic
                }
            });
        }

        return root;
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
            info.jpaEntity = cid.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().equals("Entity"));
        }

        // extends
        for (ClassOrInterfaceType ext : cid.getExtendedTypes()) {
            info.extendsTypes.add(simpleName(ext.getNameAsString()));
        }

        // implements
        for (ClassOrInterfaceType impl : cid.getImplementedTypes()) {
            info.implementsTypes.add(simpleName(impl.getNameAsString()));
        }

        for (FieldDeclaration fd : cid.getFields()) {
            for (var v : fd.getVariables()) {
                info.fields.add(new FieldRef(fd, v));
            }
        }

        // methods
        for (MethodDeclaration method : cid.getMethods()) {
            String signature = scanMethod(method);
            info.methods.add(signature);
        }
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
        pw.println("https://github.com/masmangan/assis");
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