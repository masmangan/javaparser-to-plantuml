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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
 * Generates a PlantUML class diagram from one or more Java source roots.
 *
 * <p>
 * This generator parses Java sources using JavaParser (configured for Java 17),
 * indexes declared types (including nested types), and writes a PlantUML
 * class-diagram file.
 *
 * <h2>Determinism</h2>
 * <p>
 * For reproducible output, callers should provide source roots in a stable
 * order. If {@code sourceRoots} is an unordered set, consider sorting it before
 * calling this method.
 *
 * <h2>Output</h2>
 * <p>
 * The diagram includes:
 * <ul>
 * <li>Type declarations grouped by package</li>
 * <li>Inheritance and implementation relationships</li>
 * <li>Nesting relationships for inner/nested types</li>
 * <li>Associations inferred from fields and record components</li>
 * </ul>
 *
 * @author Marco Mangan
 */
public class GenerateClassDiagram {

	/**
	 * Logger used by the generator to report progress and parse/write issues.
	 */
	static final Logger logger = Logger.getLogger(GenerateClassDiagram.class.getName());

	/**
	 * No constructor available.
	 */
	private GenerateClassDiagram() {
	}

	/**
	 * Generates a PlantUML class diagram from the given Java source roots.
	 *
	 * <p>
	 * Each path in {@code sourceRoots} should point to a directory that represents
	 * a Java source root (e.g., {@code src/main/java}). Non-existing roots are
	 * logged as warnings.
	 *
	 * <p>
	 * If {@code outDir} exists and is a directory, the output file name is fixed as
	 * {@code class-diagram.puml} within that directory. Otherwise, output directory
	 * is created; file paths rejected.
	 *
	 * @param sourceRoots one or more Java source roots; must not be {@code null}
	 * @param outDir      output directory; must not be {@code null}
	 * @throws NullPointerException if {@code sourceRoots} or {@code outDir} is
	 *                              {@code null}
	 * @throws IOException          if an I/O error occurs while reading sources or
	 *                              writing the output file
	 */
	public static void generate(final Set<Path> sourceRoots, final Path outDir) throws IOException {
		Objects.requireNonNull(sourceRoots, "sourceRoots");
		Objects.requireNonNull(outDir, "outDir");

		DeclaredIndex index = new DeclaredIndex();
		List<CompilationUnit> cus = new ArrayList<>();

		scanSourceRoots(sourceRoots, index, cus);

		logger.log(Level.FINE, () -> "**     byFqn    ** " + index.byFqn.toString());
		logger.log(Level.FINE, () -> "**   fqnsByPkg  ** " + index.fqnsByPkg.toString());
		logger.log(Level.FINE, () -> "**   pkgByFqn   ** " + index.pkgByFqn.toString());
		logger.log(Level.FINE, () -> "**uniqueBySimple** " + index.uniqueBySimple.toString());

		logger.log(Level.FINE, () -> "**      CUS     **" + cus.toString());

		Path dir = outDir.normalize();

		if (Files.exists(dir) && !Files.isDirectory(dir)) {
			throw new IllegalArgumentException("outDir must be a directory: " + dir.toAbsolutePath());
		}
		Files.createDirectories(dir);

		Path outputFile = dir.resolve("class-diagram.puml");

		logger.log(Level.INFO, () -> "Writing " + outputFile);

		writeDiagram(outputFile, index);

	}

	/**
	 * Scans directories on sourceRoots.
	 *
	 * <p>
	 * Best effort scanning will discard invalid and proceed with a simple warning.
	 * This behavior can scan a large mass of files even in the presence of partial
	 * failure.
	 *
	 * @param sourceRoots one or more Java source roots; must not be {@code null}
	 * @param index       index to be filled with compilation units scanned from
	 *                    sources roots
	 * @param cus         list of compilation units scanned
	 * @throws IOException if an I/O error occurs while reading sources
	 */
	private static void scanSourceRoots(final Set<Path> sourceRoots, final DeclaredIndex index,
			final List<CompilationUnit> cus) throws IOException {

		final ParserConfiguration cfg = new ParserConfiguration()
				.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
				// MEMORY: Stripping non structural data to save memory space.
				// Comments and tokens disabled by the following sets.
				.setAttributeComments(false).setStoreTokens(false);

		StaticJavaParser.setConfiguration(cfg);

		logger.log(Level.INFO, () -> "Scanning started");

		// Sorting files by path
		List<Path> roots = sourceRoots.stream().sorted(Path::compareTo).toList();

		for (Path src : roots) {

			logger.log(Level.INFO, () -> "Scanning " + src);

			if (!Files.exists(src)) {
				logger.log(Level.WARNING, () -> "@assis:bogus-src: Source folder does not exist: " + src);
				continue;
			}

			SourceRoot root = new SourceRoot(src);
			List<ParseResult<CompilationUnit>> results = root.tryToParse("");

			for (ParseResult<CompilationUnit> r : results) {

				r.getResult().ifPresent(cu -> {
					// MEMORY: Stripping no structural data to save memory space: method body
					cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).forEach(m -> m.removeBody());
					// MEMORY: Stripping no structural data to save memory space: constructor body
					cu.findAll(com.github.javaparser.ast.body.ConstructorDeclaration.class)
							.forEach(c -> c.getBody().getStatements().clear());

					cus.add(cu);
				});
			}

		}

		cus.sort(Comparator.comparing(cu -> cu.getStorage().map(s -> s.getPath().toString()).orElse("")));

		DeclaredIndex.fill(index, cus);
	}

	/**
	 * Writes common PlantUML directives used by ASSIS diagrams.
	 *
	 * <p>
	 * Current directives include:
	 * <ul>
	 * <li>{@code hide empty members}</li>
	 * <li>{@code !theme blueprint}</li>
	 * <li>{@code !pragma useIntermediatePackages false}</li>
	 * </ul>
	 *
	 * @param pw writer to receive directives; must not be {@code null}
	 * @throws NullPointerException if {@code pw} is {@code null}
	 */
	private static void addHeader(final PlantUMLWriter pw) {
		pw.println();
		pw.println("mainframe class diagram (cd)");
		pw.println();
		pw.println("hide empty members");
		pw.println();
		pw.println("!theme blueprint");
		pw.println("!pragma useIntermediatePackages false");
		pw.println();
		pw.println("left to right direction");
		pw.println();
		pw.println("' Diagram generated by ASSIS (%s).".formatted(versionOrDev()));
		pw.println("' https://github.com/masmangan/assis");
		pw.println();
	}

	/**
	 * Writes the PlantUML diagram to a file.
	 *
	 * <p>
	 * This method emits:
	 * <ol>
	 * <li>Diagram start and header</li>
	 * <li>Packages and types</li>
	 * <li>Relationships (inheritance, nesting, associations)</li>
	 * <li>Layout directives</li>
	 * <li>Diagram end</li>
	 * </ol>
	 *
	 * <p>
	 * The output is written using UTF-8.
	 *
	 * @param out output file path; must not be {@code null}
	 * @param idx index containing declared types and package grouping; must not be
	 *            {@code null}
	 * @throws NullPointerException if {@code out} or {@code idx} is {@code null}
	 */
	private static void writeDiagram(final Path out, final DeclaredIndex idx) {
		try (PlantUMLWriter pw = new PlantUMLWriter(
				new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8)));) {
			pw.beginDiagram("class-diagram");

			addHeader(pw);

			for (var entry : idx.fqnsByPkg.entrySet()) {
				String pkg = entry.getKey();
				List<String> fqns = entry.getValue();

				if (!pkg.isEmpty()) {
					pw.println();
					pw.beginPackage(pkg);
				}

				for (String fqn : fqns) {
					TypeDeclaration<?> td = idx.byFqn.get(fqn);
					new CollectTypesVisitor(idx, pkg, pw).emitType(fqn, td);
				}

				if (!pkg.isEmpty()) {
					pw.println();
					pw.endPackage(pkg);
				}
			}

			pw.println();
			pw.println();

			new CollectRelationshipsVisitor(idx, pw).emitAll();

			pw.println();

			pw.endDiagram("class-diagram");

		} catch (IOException e) {
			logger.log(Level.WARNING, () -> "Error writing diagram file: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Returns the implementation version of the running package, or {@code "dev"}
	 * when the version metadata is not available (e.g., during local development).
	 *
	 * @return the implementation version from the JAR manifest, or {@code "dev"}
	 */
	public static String versionOrDev() {
		String v = GenerateClassDiagram.class.getPackage().getImplementationVersion();
		return (v == null || v.isBlank()) ? "dev" : v;
	}

	/**
	 * Converts a JavaParser access specifier to PlantUML visibility notation.
	 *
	 * <p>
	 * Mapping:
	 * <ul>
	 * <li>{@code public} → {@code +}</li>
	 * <li>{@code protected} → {@code #}</li>
	 * <li>{@code private} → {@code -}</li>
	 * <li>package-private → {@code ~}</li>
	 * </ul>
	 *
	 * @param n node providing access modifiers; must not be {@code null}
	 * @return PlantUML visibility character
	 * @throws NullPointerException if {@code n} is {@code null}
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

	// Shared helpers for visitors and DeclaredIndex.

	/**
	 * Returns the stereotypes (annotation simple names) declared on a node.
	 *
	 * <p>
	 * This method returns annotation identifiers only (simple names), not fully
	 * qualified names.
	 *
	 * @param n annotated node; must not be {@code null}
	 * @return list of annotation identifiers (possibly empty)
	 * @throws NullPointerException if {@code n} is {@code null}
	 */
	static List<String> stereotypesOf(NodeWithAnnotations<?> n) {
		return n.getAnnotations().stream().map(a -> a.getName().getIdentifier()).sorted().toList();
	}

	/**
	 * Renders a list of stereotype names into PlantUML stereotype syntax.
	 *
	 * <p>
	 * Example: {@code ["Entity","Deprecated"]} becomes
	 * {@code " <<Entity>> <<Deprecated>>"}.
	 *
	 * @param ss stereotype names; may be {@code null} or empty
	 * @return a leading-space-prefixed stereotype block, or {@code ""} when none
	 */
	static String renderStereotypes(List<String> ss) {
		if (ss == null || ss.isEmpty()) {
			return "";
		}
		return " " + ss.stream().map(s -> "<<" + s + ">>").collect(Collectors.joining(" "));
	}

	/**
	 * Returns PlantUML method flags derived from Java modifiers.
	 *
	 * <p>
	 * Current flags:
	 * <ul>
	 * <li>{@code {static}}</li>
	 * <li>{@code {abstract}}</li>
	 * <li>{@code {final}}</li>
	 * </ul>
	 *
	 * @param method method declaration; must not be {@code null}
	 * @return flags string (possibly empty)
	 * @throws NullPointerException if {@code method} is {@code null}
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
	 * Returns the simple name of a type name that may be package-qualified and/or
	 * nested.
	 *
	 * <p>
	 * This method strips:
	 * <ul>
	 * <li>package qualification using {@code '.'}</li>
	 * <li>nesting qualification using {@code '$'}</li>
	 * </ul>
	 *
	 * <p>
	 * Example: {@code "p.Outer$Inner"} becomes {@code "Inner"}.
	 *
	 * @param qname fully-qualified and/or nested type name; must not be
	 *              {@code null}
	 * @return the simple name component
	 * @throws NullPointerException if {@code qname} is {@code null}
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