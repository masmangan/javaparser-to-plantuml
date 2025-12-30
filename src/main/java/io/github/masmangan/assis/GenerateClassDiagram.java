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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.ParseResult;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.AccessSpecifier;

import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

/**
 * The {@code GenerateClassDiagram} class is a PlantUML Language class diagram
 * generator.
 */
public class GenerateClassDiagram {

	/**
	 * Default documentation path. We are borrowing from PlantUML folder convention.
	 */
	private static final String DOCS_UML_CLASS_DIAGRAM_PUML = "docs/diagrams/src/class-diagram.puml";

	/**
	 * Default source path. We are borrowing from Maven folder convention.
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
	 * 
	 */
	static class RecordComponentRef {
		  final Parameter param;
		  final List<String> stereotypes;
		  RecordComponentRef(Parameter p) {
		    this.param = p;
		    this.stereotypes = stereotypesOf(p);
		  }
		  String name() { return param.getNameAsString(); }
		  Type type() { return param.getType(); }
		}
	
	/**
	 * 
	 */
	static class EnumConstantRef {
		final EnumConstantDeclaration ecd;

		public EnumConstantRef(EnumConstantDeclaration ecd) {
			this.ecd = ecd;
		}
	}

	/**
	 * 
	 */
	static class FieldRef {
		final FieldDeclaration fd;
		final VariableDeclarator vd;
		final List<String> stereotypes;

		FieldRef(FieldDeclaration fd, VariableDeclarator vd) {
			this.fd = fd;
			this.vd = vd;
			this.stereotypes = stereotypesOf(fd);
		}
	}

	/**
	 * 
	 */
	static class ConstructorRef {
		final ConstructorDeclaration ctor;
		final List<String> stereotypes;

		public ConstructorRef(ConstructorDeclaration ctor) {
			this.ctor = ctor;
			this.stereotypes = stereotypesOf(ctor);
		}
	}

	/**
	 * 
	 */
	static class MethodRef {
		final MethodDeclaration method;
		final List<String> stereotypes;

		public MethodRef(MethodDeclaration method) {
			this.method = method;
			this.stereotypes = stereotypesOf(method);
		}
	}

	/**
	 * Symbol table entry
	 */
	static class TypeInfo {
		String pkg;
		String name;
		String fqn;
		Kind kind = Kind.CLASS;
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);

		Set<String> extendsTypes = new LinkedHashSet<>();
		Set<String> implementsTypes = new LinkedHashSet<>();
		Set<FieldRef> fields = new LinkedHashSet<>();
		Set<MethodRef> methods = new LinkedHashSet<>();
		Set<ConstructorRef> constructors = new LinkedHashSet<>();
		List<EnumConstantRef> enumConstants = new ArrayList<>();

		List<String> stereotypes = new ArrayList<>();

		TypeDeclaration<?> td;
		CompilationUnit cu;

		String ownerFqn; // null for top-level
		
		List<RecordComponentRef> recordComponents = new ArrayList<>();
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

		Map<String, TypeInfo> types = new LinkedHashMap<>();

		logger.log(Level.INFO, () -> "Scanning " + src);
		scanSources(src, types);

		logger.log(Level.INFO, () -> "Writing " + out);
		writeDiagram(out, types);
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
		pw.println();
		pw.println("!theme blueprint");
		pw.println("!pragma useIntermediatePackages false");
		pw.println();
	}

	/**
	 * 
	 * @param td
	 * @param info
	 */
	private static void collectAnnotations(TypeDeclaration<?> td, TypeInfo info) {
		td.getAnnotations().forEach(a -> info.stereotypes.add(a.getName().getIdentifier()));
	}

	/**
	 * Get classifier and modifiers.
	 * 
	 * Kind enumeration defines available classifiers. Modifier enumeration defines
	 * available modifiers.
	 * 
	 * @param t
	 * @return
	 */
	private static String getClassifier(TypeInfo t) {
		String classifier = "**error at classifier**";
		if (t.kind == Kind.CLASS) {
			if (t.modifiers.contains(Modifier.ABSTRACT)) {
				classifier = "abstract class " + fqn(t);
			} else if (t.modifiers.contains(Modifier.FINAL)) {
				classifier = "class " + fqn(t) + " <<final>>";
			} else {
				classifier = "class " + fqn(t);
			}
		} else if (t.kind == Kind.INTERFACE) {
			classifier = "interface " + fqn(t);
		} else if (t.kind == Kind.RECORD) {
			classifier = "record " + fqn(t);
		} else if (t.kind == Kind.ENUM) {
			classifier = "enum " + fqn(t);
		} else if (t.kind == Kind.ANNOTATION) {
			classifier = "annotation " + fqn(t);
		} else {
			logger.log(Level.WARNING, () -> "Unexpected type: " + t.toString());
		}
		
		classifier += renderStereotypes(t);
		classifier += " {";
		return classifier;
	}
	
	/**
	 * 
	 * @param pw
	 * @param types
	 * @param t
	 */
	private static void writeRecordComponents(PrintWriter pw, Map<String, TypeInfo> types, TypeInfo t) {
	    if (t.kind != Kind.RECORD) return;

	    for (RecordComponentRef rc : t.recordComponents) {
	        String raw = rc.type().asString().replaceAll("<.*>", "").replace("[]", "").trim();
	        String resolved = resolveTypeName(types, t, raw);

	        // If resolves to project type: skip body (association will show it)
	        if (resolved != null && !resolved.equals(t.fqn)) {
	            continue;
	        }

	        pw.println("  " + rc.name() + " : " + rc.type().asString()
	                + renderStereotypes(rc.stereotypes));
	    }
	}

	/**
	 * Writes package contents to the diagram.
	 * 
	 * @param pw
	 * @param byPkg
	 * @param root
	 * @throws IOException
	 */
	private static void writePackages(PrintWriter pw, Map<String, List<TypeInfo>> byPkg, Map<String, TypeInfo> types) {
		for (var entry : byPkg.entrySet()) {
			String pkg = entry.getKey();

			if (!pkg.isEmpty()) {
				pw.println("package \"" + pkg + "\" {");
			}

			for (TypeInfo t : entry.getValue()) {
				String classifier = getClassifier(t);
				pw.println(classifier);

				writeEnumConstants(pw, t);
				writeRecordComponents(pw, types, t);

				writeFields(pw, types, t);

				writeConstructors(pw, t);

				writeMethods(pw, t);

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
	 * @param t
	 */
	private static void writeEnumConstants(PrintWriter pw, TypeInfo t) {
		if (t.kind != Kind.ENUM)
			return;

		for (EnumConstantRef c : t.enumConstants) {
			pw.println("  " + c.ecd.getNameAsString());
		}
		
	}

	/**
	 * 
	 * @param pw
	 * @param t
	 */
	private static void writeConstructors(PrintWriter pw, TypeInfo t) {
		for (ConstructorRef c : t.constructors) {
			String name = c.ctor.getNameAsString(); // "Person" (valid Java)
			String params = c.ctor.getParameters().stream()
					.map(p -> p.getNameAsString() + " : " + p.getType().asString()).collect(Collectors.joining(", "));

			String vis = getVisibility(c);

			pw.println("  " + vis + " <<create>> " + name + "(" + params + ")" + renderStereotypes(c.stereotypes));
		}
	}

	/**
	 * 
	 * @param pw
	 * @param t
	 */
	private static void writeMethods(PrintWriter pw, TypeInfo t) {
		for (MethodRef m : t.methods) {
			String returnType = m.method.getType().asString();
			String name = m.method.getNameAsString();

			String params = m.method.getParameters().stream().map(p -> {
				String anns = renderStereotypes(stereotypesOf(p));
				return (anns + " " + p.getNameAsString() + " : " + p.getType().asString()).trim();
			}).collect(Collectors.joining(", "));
			String flags = getFlags(m.method);
			String vis = getVisibility(m);

			pw.println("  " + vis + " " + name + "(" + params + ") : " + returnType + flags
					+ renderStereotypes(m.stereotypes));
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
	private static void writeRelationships(PrintWriter pw, Map<String, TypeInfo> types) {
		for (TypeInfo t : types.values()) {
			writeExtends(pw, types, t);
			writeImplements(pw, types, t);
		}

		for (TypeInfo t : types.values()) {
		    if (t.ownerFqn != null && !t.ownerFqn.isBlank()) {
		        TypeInfo owner = types.get(t.ownerFqn);
		        if (owner != null) {
		            pw.println(fqn(owner) + " +-- " + fqn(t) );
		        }
		    }
		}		
		
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
	    // existing field-based associations
	    for (FieldRef fr : t.fields) {
	        String assocFqn = assocTypeFrom(types, t, fr.vd);
	        if (assocFqn != null) {
	            pw.println("\"" + t.fqn + "\" --> \"" + assocFqn + "\" : " + fr.vd.getNameAsString());
	        }
	    }

	    // NEW: record component associations
	    if (t.kind == Kind.RECORD) {
	        for (RecordComponentRef rc : t.recordComponents) {
	            String raw = rc.type().asString().replaceAll("<.*>", "").replace("[]", "").trim();
	            String target = resolveTypeName(types, t, raw);
	            if (target != null && !target.equals(t.fqn)) {
	                pw.println("\"" + t.fqn + "\" --> \"" + target + "\" : " + rc.name()
	                        + renderStereotypes(rc.stereotypes));
	            }
	        }
	    }
	}

	/**
	 * 
	 * @param vd
	 * @return
	 */
	private static String displayTypeFrom(VariableDeclarator vd) {
		return vd.getType().asString();
	}

	/**
	 * 
	 * @param types
	 * @param owner
	 * @param vd
	 * @return
	 */
	private static String assocTypeFrom(Map<String, TypeInfo> types, TypeInfo owner, VariableDeclarator vd) {
		String raw = vd.getType().asString();
		raw = raw.replaceAll("<.*>", "").replace("[]", "").trim();
		String resolved = resolveTypeName(types, owner, raw);
		if (resolved == null)
			return null;
		if (resolved.equals(owner.fqn))
			return null;
		return resolved;
	}

	/**
	 * 
	 * @param pw
	 * @param types
	 * @param t
	 */
	private static void writeFields(PrintWriter pw, Map<String, TypeInfo> types, TypeInfo t) {
		for (FieldRef fr : t.fields) {
			if (assocTypeFrom(types, t, fr.vd) != null) {
				continue;
			}

			String name = fr.vd.getNameAsString();
			String type = displayTypeFrom(fr.vd);

			// PlantUML underline for static:
			String staticPrefix = fr.fd.isStatic() ? "{static} " : "";

			// visibility (optional now; easy to add)
			String vis = getVisibility(fr);

			List<String> mods = new ArrayList<>();
			if (fr.fd.isFinal())
				mods.add("final");
			if (fr.fd.isTransient())
				mods.add("transient");
			if (fr.fd.isVolatile())
				mods.add("volatile");

			String modBlock = mods.isEmpty() ? "" : " {" + String.join(", ", mods) + "}";

			pw.println("  " + vis + " " + staticPrefix + name + " : " + type + modBlock
					+ renderStereotypes(fr.stereotypes));
		}
	}

	/**
	 * 
	 * @param fr
	 * @return
	 */
	private static String getVisibility(FieldRef fr) {
		return visibility(fr.fd);
	}

	/**
	 * 
	 * @param fr
	 * @return
	 */
	private static String getVisibility(MethodRef fr) {
		return visibility(fr.method);
	}

	/**
	 * 
	 * @param cr
	 * @return
	 */
	private static String getVisibility(ConstructorRef cr) {
		return visibility(cr.ctor);
	}

	/**
	 * 
	 * @param n
	 * @return
	 */
	private static String visibility(NodeWithAccessModifiers<?> n) {
		AccessSpecifier a = n.getAccessSpecifier();
		return switch (a) {
		case PUBLIC -> "+";
		case PROTECTED -> "#";
		case PRIVATE -> "-";
		default -> "~";
		};
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
			String target = resolveTypeName(types, t, i);
			if (target != null) {
				pw.println("\"" + target + "\" <|.. \"" + t.fqn + "\"");
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
			String target = resolveTypeName(types, t, e);
			if (target != null) {
				pw.println("\"" + target + "\" <|-- \"" + t.fqn + "\"");
			}
		}
	}

	/**
	 * 
	 * @param byPkg
	 * @return
	 */
	private static Map<String, List<TypeInfo>> sortPackagesByName(Map<String, List<TypeInfo>> byPkg) {
		return byPkg.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	/**
	 * Writes diagram contents.
	 * 
	 * @param out
	 * @param types
	 * @param root
	 */
	private static void writeDiagram(Path out, Map<String, TypeInfo> types) {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(out))) {
			addHeader(pw);

			Map<String, List<TypeInfo>> byPkg = types.values().stream().collect(
					Collectors.groupingBy(t -> t.pkg == null ? "" : t.pkg, LinkedHashMap::new, Collectors.toList()));
			byPkg = sortPackagesByName(byPkg);

			byPkg.values().forEach(list -> list.sort((a, b) -> a.fqn.compareTo(b.fqn)));
			writePackages(pw, byPkg, types);

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
		if (!Files.exists(src)) {
			logger.log(Level.WARNING, () -> "Source folder does not exist: " + src);
			return;
		}

		SourceRoot root = new SourceRoot(src);
		root.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

		List<ParseResult<CompilationUnit>> results = root.tryToParse("");

		for (ParseResult<CompilationUnit> r : results) {
			r.getResult().ifPresent(cu -> {

				String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

				for (TypeDeclaration<?> td : cu.getTypes()) {
					scanType(types, cu, pkg, td, null); 
				}
			});
		}

	}

	/**
	 * 
	 * @param n
	 * @return
	 */
	private static List<String> stereotypesOf(NodeWithAnnotations<?> n) {
		return n.getAnnotations().stream().map(a -> a.getName().getIdentifier()) // simple name only
				.toList();
	}

	/**
	 * 
	 * @param ss
	 * @return
	 */
	private static String renderStereotypes(List<String> ss) {
		if (ss == null || ss.isEmpty())
			return "";
		return " " + ss.stream().map(s -> "<<" + s + ">>").collect(Collectors.joining(" "));
	}

	/**
	 * 
	 * @param t
	 * @return
	 */
	private static String renderStereotypes(TypeInfo t) {
		if (t.stereotypes.isEmpty())
			return "";
		return " " + t.stereotypes.stream().map(s -> "<<" + s + ">>").collect(Collectors.joining(" "));
	}

	/**
	 * 
	 * @param types
	 * @param pkg
	 * @param td
	 */
	private static void scanType(Map<String, TypeInfo> types, CompilationUnit cu, String pkg, TypeDeclaration<?> td,
			TypeInfo owner) {
		
		TypeInfo info = new TypeInfo();
		info.pkg = pkg;
		info.cu = cu;
		info.td = td;
		
	    if (owner != null) {
	        info.ownerFqn = owner.fqn;
	    }
		
		if (td instanceof EnumDeclaration ed) {
			info.name = ed.getNameAsString();
			info.kind = Kind.ENUM;

			for (EnumConstantDeclaration c : ed.getEntries()) {
				info.enumConstants.add(new EnumConstantRef(c));
			}
		} else if (td instanceof RecordDeclaration rd) {
		    info.name = rd.getNameAsString();
		    info.kind = Kind.RECORD;

		    rd.getParameters().forEach(p ->
		        info.recordComponents.add(new RecordComponentRef(p))
		    );
		} else if (td instanceof AnnotationDeclaration ad) {
			info.name = ad.getNameAsString();
			info.kind = Kind.ANNOTATION;
		} else if (td instanceof ClassOrInterfaceDeclaration cid) {
			scanClassOrInterface(info, cid);
		} else {
			logger.log(Level.WARNING, () -> "Unexpected type: " + td.toString());
		}

		collectAnnotations(td, info);

	    if (owner == null) {
	        info.fqn = info.pkg == null || info.pkg.isEmpty()
	                ? info.name
	                : info.pkg + "." + info.name;
	    } else {
	        info.fqn = owner.fqn + "." + info.name;
	    }

	    types.put(info.fqn, info);

	    scanNestedTypes(types, cu, pkg, td, info);
	}

	/**
	 * 
	 * @param types
	 * @param cu
	 * @param pkg
	 * @param td
	 * @param owner
	 */
	private static void scanNestedTypes(
	        Map<String, TypeInfo> types,
	        CompilationUnit cu,
	        String pkg,
	        TypeDeclaration<?> td,
	        TypeInfo owner) {

	    // Only Class/Interface and Enum declarations can contain member types in practice
	    // (Record and Annotation types also can contain members in Java, but we can add later)
	    if (td instanceof ClassOrInterfaceDeclaration cid) {
	        cid.getMembers().forEach(m -> {
	            if (m instanceof TypeDeclaration<?> nested) {
	                scanType(types, cu, pkg, nested, owner);
	            }
	        });
	    } else if (td instanceof EnumDeclaration ed) {
	        ed.getMembers().forEach(m -> {
	            if (m instanceof TypeDeclaration<?> nested) {
	                scanType(types, cu, pkg, nested, owner);
	            }
	        });
	    }
	}	
	
	/**
	 * 
	 * @param t
	 * @return
	 */
	private static String pumlName(TypeInfo t) {
	    if (t.ownerFqn == null || t.ownerFqn.isBlank()) {
	        return t.fqn;
	    }
	    return t.ownerFqn + "_" + t.name; // exactly what the test expects
	}
	
	/**
	 * 
	 * @param t
	 * @return
	 */
	private static String fqn(TypeInfo t) {
	    return "\"" + pumlName(t) + "\"";
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

		for (ClassOrInterfaceType ext : cid.getExtendedTypes()) {
			info.extendsTypes.add(simpleName(ext.getNameAsString()));
		}

		for (ClassOrInterfaceType impl : cid.getImplementedTypes()) {
			info.implementsTypes.add(simpleName(impl.getNameAsString()));
		}

		for (FieldDeclaration fd : cid.getFields()) {
			for (var v : fd.getVariables()) {
				info.fields.add(new FieldRef(fd, v));
			}
		}

		for (ConstructorDeclaration ctor : cid.getConstructors()) {
			info.constructors.add(new ConstructorRef(ctor));
		}

		for (MethodDeclaration method : cid.getMethods()) {
			info.methods.add(new MethodRef(method));
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
		pw.println();
		pw.println("' Generated with ASSIS (Java -> UML)");
		pw.println("' https://github.com/masmangan/assis");
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

	/**
	 * Resolve a type name to an FQN key in {@code types}.
	 *
	 * Minimal resolver (good enough for shadowing + most small projects): 1) if
	 * name is already qualified and exists -> return it 2) try same package
	 * (owner.pkg + "." + simple) -> if exists, return it 3) try unique match by
	 * simple name across known project types -> if unique, return it otherwise
	 * return null (unresolved / external)
	 */
	private static String resolveTypeName(Map<String, TypeInfo> types, TypeInfo owner, String name) {
		if (name == null)
			return null;
		String raw = name.trim();
		if (raw.isEmpty())
			return null;

		if (raw.contains(".") && types.containsKey(raw)) {
			return raw;
		}

		String simple = simpleName(raw);

		String samePkg = (owner.pkg == null || owner.pkg.isEmpty()) ? simple : owner.pkg + "." + simple;
		if (types.containsKey(samePkg)) {
			return samePkg;
		}

		String suffix = "." + simple;
		String found = null;
		for (String key : types.keySet()) {
			if (key.equals(simple) || key.endsWith(suffix)) {
				if (found != null && !found.equals(key)) {
					return null; // ambiguous
				}
				found = key;
			}
		}
		return found;
	}
}