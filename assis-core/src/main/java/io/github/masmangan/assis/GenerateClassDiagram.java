/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.utils.SourceRoot;

/**
 * The {@code GenerateClassDiagram} class is a PlantUML Language class diagram
 * generator.
 */
public class GenerateClassDiagram {

	/**
	 * Logs info and warnings.
	 */
	static final Logger logger = Logger.getLogger(GenerateClassDiagram.class.getName());

	/**
	 * No constructor available.
	 */
	private GenerateClassDiagram() {
	}

	/**
	 * Extracts package version information.
	 * 
	 * @return gets package information from Maven property, or dev otherwise.
	 */
	public static String versionOrDev() {
		String v = GenerateClassDiagram.class.getPackage().getImplementationVersion();
		return (v == null || v.isBlank()) ? "dev" : v;
	}

	/**
	 * Generates code for a given source code and output path.
	 * 
	 * @param src source code path
	 * @param out output path
	 * @throws IOException
	 * @throws Exception
	 */
	public static void generate(Set<Path> sourceRoots, Path outDir) throws IOException {
		ParserConfiguration config = new ParserConfiguration();
		config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
		StaticJavaParser.setConfiguration(config);

		DeclaredIndex index = new DeclaredIndex();
		List<CompilationUnit> cus = new ArrayList<>();

		logger.log(Level.INFO, () -> "Scanning started" );

		for (Path src : sourceRoots) {

			logger.log(Level.INFO, () -> "Scanning " + src);

			if (!Files.exists(src)) {
				logger.log(Level.WARNING, () -> "Source folder does not exist: " + src);
			}

			SourceRoot root = new SourceRoot(src);
			List<ParseResult<CompilationUnit>> results = root.tryToParse("");

			for (ParseResult<CompilationUnit> r : results) {
				r.getResult().ifPresent(cus::add);
			}

		}
		DeclaredIndex.fill(index, cus);

		logger.log(Level.FINE, () -> "**     byFqn    ** "+ index.byFqn.toString());
		logger.log(Level.FINE, () -> "**   fqnsByPkg  ** "+ index.fqnsByPkg.toString());
		logger.log(Level.FINE, () -> "**   pkgByFqn   ** "+ index.pkgByFqn.toString());
		logger.log(Level.FINE, () -> "**uniqueBySimple** "+ index.uniqueBySimple.toString());

		
		
		logger.log(Level.FINE, () -> "  **CUS** "+cus.toString());

		logger.log(Level.INFO, () -> "Writing " + outDir);

		// Gambiarra para o ASSIS CLI
		if (Files.isDirectory(outDir)) {
			Path outputFile = outDir.resolve("class-diagram.puml");
			writeDiagram(outputFile, index);

		} else {
			writeDiagram(outDir, index);
		}

	}

	/**
	 * Adds a header to the diagram.
	 * 
	 * @param pw
	 */
	private static void addHeader(PlantUMLWriter pw) {
		pw.println();
		pw.println("hide empty members");
		pw.println();
		pw.println("!theme blueprint");
		pw.println("!pragma useIntermediatePackages false");
		pw.println();
	}

	/**
	 * 
	 * @param n
	 * @return
	 */
	static String visibility(NodeWithAccessModifiers<?> n) {
		AccessSpecifier a = n.getAccessSpecifier();
		return switch (a) {
		case PUBLIC -> "+";
		case PROTECTED -> "#";
		case PRIVATE -> "-";
		default -> "~";
		};
	}

	/**
	 * 
	 * @param out
	 * @param idx
	 */
	private static void writeDiagram(Path out, DeclaredIndex idx) {
		try (

				PlantUMLWriter pw = new PlantUMLWriter(
						new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8)));

		) {
			pw.beginDiagram("class-diagram");

			addHeader(pw);

			for (var entry : idx.fqnsByPkg.entrySet()) {
				String pkg = entry.getKey();
				List<String> fqns = entry.getValue();

				if (!pkg.isEmpty()) {
					pw.println();
					pw.beginPackage(pkg);
				}

				// FIXME: fqn can be restored from idx and td, no need to pass it as parameter!
				// FIXME: Use the same visitor, just emitType(td)
				// FIXME: if the same visitor, pkg would be a parameter, BUT if we can get the
				// same pkg from td, no need to pass it!

				for (String fqn : fqns) {
					TypeDeclaration<?> td = idx.byFqn.get(fqn);
					new CollectTypesVisitor(idx, pkg, pw).emitType(fqn, td);
				}

				if (!pkg.isEmpty()) {
					pw.println();
					pw.endPackage(pkg);
				}
			}

			new CollectRelationshipsVisitor(idx, pw).emitAll();

			pw.println();
			pw.println("left to right direction");

			pw.endDiagram("class-diagram");

		} catch (IOException e) {
			logger.log(Level.WARNING, () -> "Error writing diagram file: " + e.getLocalizedMessage());
		}
	}

	/**
	 * 
	 * @param n
	 * @return
	 */
	static List<String> stereotypesOf(NodeWithAnnotations<?> n) {
		return n.getAnnotations().stream().map(a -> a.getName().getIdentifier()) // simple name only
				.toList();
	}

	/**
	 * 
	 * @param ss
	 * @return
	 */
	static String renderStereotypes(List<String> ss) {
		if (ss == null || ss.isEmpty()) {
			return "";
		}
		return " " + ss.stream().map(s -> "<<" + s + ">>").collect(Collectors.joining(" "));
	}

	/**
	 * 
	 * @param method
	 * @param flags
	 * @return
	 */
	static String getFlags(MethodDeclaration method) {
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
	 * Extracts name from classifier.
	 * 
	 * @param qname
	 * @return
	 */
	static String simpleName(String qname) {
		String s = qname;
		int lt = s.lastIndexOf('.');
		if (lt >= 0) {
			s = s.substring(lt + 1);
		}
		lt = s.lastIndexOf('$');
		if (lt >= 0) {
			s = s.substring(lt + 1);
		}
		return s;
	}

}