/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * A writer that emits PlantUML statements to a {@link PrintWriter}.
 *
 * <p>
 * Output is line-based: each call to {@link #println(String)} writes a single
 * line. This class provides helper methods to emit common block constructs such
 * as packages and type declarations. This class also provides methods to add
 * relationships.
 *
 * <p>
 * Blocks started with {@code begin*} methods must be closed with a
 * corresponding {@code end*} method. While inside a package or type block,
 * subsequent lines are indented by two spaces per indentation level.
 *
 * <p>
 * End a type block before beginning another type block.
 *
 * <p>
 * This class performs no validation of PlantUML syntax and does not attempt to
 * enforce balanced blocks.
 *
 * <p>
 * For format integrity, names must be fully qualified, single-lined and must
 * not contain double quotes ("). Violations cause IllegalArgumentException.
 *
 * <p>
 * PlantUML does not support true nested type declarations. To represent Java
 * inner or nested types, emit each type as a separate PlantUML type and encode
 * the nesting relationship in the type name using a separator.
 *
 * <p>
 * Package qualification follows standard Java fully qualified naming, using the
 * {@code .} (dot) separator. Avoid using {@code .} to encode nesting; treat
 * {@code .} as package qualification.
 *
 * <p>
 * Structural relationships between types (such as nesting, inheritance, and
 * interface implementation) are rendered separately using standard PlantUML
 * relationship syntax. For details on relationship notation, refer to the
 * PlantUML documentation.
 *
 * <p>
 * For example, the Java source:
 *
 * <pre>{@code
 * package p;
 *
 * class A {
 * 	class B {
 * 	}
 * }
 * }</pre>
 *
 * can be emitted as two separate type blocks:
 *
 * <pre>{@code
 * pw.beginClass("p.A", ""); // Names are provided unquoted; helpers quote as needed.
 * pw.endClass("p.A");
 *
 * pw.beginClass("p.A$B", ""); // A different separator than ".".
 * pw.endClass("p.A$B");
 *
 * pw.println("\"p.A\" +-- \"p.A$B\""); // Raw PlantUML: caller provides correct quoting.
 * }</pre>
 * <p>
 * Or using a convenience method:
 *
 * <pre>{@code
 * pw.connectInnerType("p.A", "p.A$B"); // Convenience: quoting is automatically provided.
 * }</pre>
 * <p>
 * Closing this writer flushes its output but does not close the underlying
 * {@code PrintWriter}, since it is not owned by this instance.
 *
 * @since 0.9.1
 * @author Marco Mangan
 */
public final class PlantUMLWriter implements AutoCloseable {

	/**
	 *
	 */
	private static final String COLON_SEPARATOR = ":";

	/**
	 *
	 */
	private static final String SPACE_STRING = " ";

	/**
	 *
	 */
	private static final String EMPTY_STRING = "";

	/**
	 *
	 */
	private static final String INDENT_UNIT = "  ";

	/**
	 *
	 */
	private static final String IS_A_IMPLEMENTS = "..|>";

	/**
	 *
	 */
	private static final String IS_A_EXTENDS = "--|>";

	/**
	 *
	 */
	private static final String HAS_A_INNER = "+--";

	/**
	 *
	 */
	private static final String HAS_A = "--->"; // longer line because of role and stereotype label

	/**
	 *
	 */
	private static final String USES = "..>";

	/**
	 *
	 */
	private final PrintWriter out;

	/**
	 *
	 */
	private int indentLevel;

	/**
	 * Creates a new writer that sends its output to the given {@code PrintWriter}.
	 *
	 * @param out destination writer; must not be {@code null}
	 * @throws NullPointerException if {@code out} is {@code null}
	 */
	public PlantUMLWriter(final PrintWriter out) {
		this.out = Objects.requireNonNull(out, "out");
	}

	/**
	 * Writes a line, prefixed with the current indentation.
	 *
	 * @param line the line to write (without a line terminator); must not be
	 *             {@code null}
	 * @throws NullPointerException if {@code line} is {@code null}
	 */
	public void println(final String line) {
		if (activeTag != null) {
			printlnInternal("/' " + activeTag + EMPTY_STRING + line + " '/");
		} else {
			printlnInternal(line);
		}
	}

	/**
	 *
	 * @param line
	 */
	private void printlnInternal(final String line) {
		Objects.requireNonNull(line, "line");
		out.println(INDENT_UNIT.repeat(indentLevel) + line);
	}

	/**
	 * Writes an empty line.
	 */
	public void println() {
		println(EMPTY_STRING);
	}

	/**
	 * Increases the indentation level for subsequently written lines.
	 */
	public void indent() {
		indentLevel++;
	}

	/**
	 * Decreases the indentation level for subsequently written lines. If the
	 * indentation level is already zero, this method has no effect.
	 */
	public void dedent() {
		if (indentLevel > 0) {
			indentLevel--;
		}
	}

	/**
	 * Flushes the underlying {@code PrintWriter}.
	 */
	public void flush() {
		out.flush();
	}

	/**
	 * Flushes this writer.
	 *
	 * <p>
	 * This method does not close the underlying {@code PrintWriter}.
	 */
	@Override
	public void close() {
		flush();
	}

	/**
	 * Begins a diagram.
	 *
	 * @param name diagram name in the emitted statement; must not be {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void beginDiagram(final String name) {
		checkName(name);
		out.print("@startuml");
		out.print(SPACE_STRING);
		out.print(quote(name));
		out.println();
	}

	/**
	 * Ends a diagram.
	 *
	 * <p>
	 * The caller is responsible for matching each {@link #beginDiagram(String)}
	 * with exactly one call to this method.
	 *
	 * <p>
	 * NOTE: the name will not be printed because PlantUML grammar does not allow
	 * text after @enduml.
	 *
	 * @param name diagram name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"} *
	 */
	public void endDiagram(final String name) {
		checkName(name);
		out.print("@enduml");
		out.println();
	}

	/**
	 * Begins a {@code package} block and increases indentation.
	 *
	 * @param name package name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void beginPackage(final String name) {
		checkName(name);
		println("package \"%s\" { /' @assis:begin package \"%s\" '/".formatted(name.strip(), name.strip()));
		indent();
	}

	/**
	 * Ends a {@code package} block.
	 *
	 * <p>
	 * This method decreases indentation by one level (if greater than zero) and
	 * emits {@code }}. The caller is responsible for matching each
	 * {@link #beginPackage(String)} with exactly one call to this method.
	 *
	 * @param name package name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void endPackage(final String name) {
		checkName(name);
		dedent();
		println("} /' @assis:end package \"%s\" '/".formatted(name.strip()));
	}

	/**
	 * Begins a {@code class} block and increases indentation.
	 *
	 * @param name        type name to quote in the emitted statement; must not be
	 *                    {@code null}
	 * @param stereotypes optional stereotypes (e.g., {@code <<Entity>>}); may be
	 *                    {@code null} or blank
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void beginClass(final String name, final String visibility, final String stereotypes) {
		beginType("class", name, visibility, stereotypes);
	}

	/**
	 * Ends a {@code class} block and decreases indentation.
	 * <p>
	 * This method decreases indentation by one level (if greater than zero) and
	 * emits {@code }}. The caller is responsible for matching each
	 * {@link #beginClass(String)} with exactly one call to this method.
	 *
	 * @param name type name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void endClass(final String name) {
		endType("class", name);
	}

	/**
	 * Begins a {@code abstract class} block and increases indentation.
	 *
	 * @param name        type name to quote in the emitted statement; must not be
	 *                    {@code null}
	 * @param stereotypes optional stereotypes (e.g., {@code <<Entity>>}); may be
	 *                    {@code null} or blank
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void beginAbstractClass(final String name, String visibility, final String stereotypes) {
		beginType("abstract class", name, visibility, stereotypes);
	}

	/**
	 * Ends a {@code abstract class} block and decreases indentation.
	 * <p>
	 * This method decreases indentation by one level (if greater than zero) and
	 * emits {@code }}. The caller is responsible for matching each
	 * {@link #beginAbstractClass(String)} with exactly one call to this method.
	 *
	 * @param name type name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void endAbstractClass(final String name) {
		endType("abstract class", name);
	}

	/**
	 * Begins a {@code interface} block and increases indentation.
	 *
	 * @param name        type name to quote in the emitted statement; must not be
	 *                    {@code null}
	 * @param stereotypes optional stereotypes (e.g.,
	 *                    {@code <<FunctionalInterface>>}); may be {@code null} or
	 *                    blank
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void beginInterface(final String name, final String visibility, final String stereotypes) {
		beginType("interface", name, visibility, stereotypes);
	}

	/**
	 * Ends a {@code interface} block and decreases indentation.
	 * <p>
	 * This method decreases indentation by one level (if greater than zero) and
	 * emits {@code }}. The caller is responsible for matching each
	 * {@link #beginInterface(String)} with exactly one call to this method.
	 *
	 * @param name type name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void endInterface(final String name) {
		endType("interface", name);
	}

	/**
	 * Begins a {@code record} block and increases indentation.
	 *
	 * @param name        type name to quote in the emitted statement; must not be
	 *                    {@code null}
	 * @param stereotypes optional stereotypes; may be {@code null} or blank
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void beginRecord(final String name, final String visibility, final String stereotypes) {
		beginType("record", name, visibility, stereotypes);
	}

	/**
	 * Ends a {@code record} block and decreases indentation.
	 * <p>
	 * This method decreases indentation by one level (if greater than zero) and
	 * emits {@code }}. The caller is responsible for matching each
	 * {@link #beginRecord(String)} with exactly one call to this method.
	 *
	 * @param name type name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void endRecord(final String name) {
		endType("record", name);
	}

	/**
	 * Begins a PlantUML {@code enum} block and increases indentation.
	 *
	 * @param name        type name to quote in the emitted statement; must not be
	 *                    {@code null}
	 * @param stereotypes optional stereotypes; may be {@code null} or blank
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void beginEnum(final String name, final String visibility, final String stereotypes) {
		beginType("enum", name, visibility, stereotypes);
	}

	/**
	 * Ends a {@code enum} block and decreases indentation.
	 * <p>
	 * This method decreases indentation by one level (if greater than zero) and
	 * emits {@code }}. The caller is responsible for matching each
	 * {@link #beginEnum(String)} with exactly one call to this method.
	 *
	 * @param name type name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void endEnum(final String name) {
		endType("enum", name);
	}

	/**
	 * Begins a PlantUML {@code annotation} block and increases indentation.
	 *
	 * @param name        type name to quote in the emitted statement; must not be
	 *                    {@code null}
	 * @param stereotypes optional stereotypes; may be {@code null} or blank
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void beginAnnotation(final String name, final String visibility, final String stereotypes) {
		beginType("annotation", name, visibility, stereotypes);
	}

	/**
	 * Ends a {@code annotation} block and decreases indentation.
	 * <p>
	 * This method decreases indentation by one level (if greater than zero) and
	 * emits {@code }}. The caller is responsible for matching each
	 * {@link #beginAnnotation(String)} with exactly one call to this method.
	 *
	 * @param name type name to quote in the emitted statement; must not be
	 *             {@code null}
	 * @throws NullPointerException     if {@code name} is {@code null}
	 * @throws IllegalArgumentException if {@code name} contains {@code "},
	 *                                  {@code "\n"}, or {@code "\r"}
	 */
	public void endAnnotation(final String name) {
		endType("annotation", name);
	}

	/**
	 * Connects owned as a inner type of owner.
	 *
	 * @param owner owner to quote in the emitted statement; must not be
	 *              {@code null}
	 * @param owned owned to quote in the emitted statement; must not be
	 *              {@code null} * @throws NullPointerException if {@code owner} is
	 *              {@code null}, {@code owned} is {@code null}
	 * @throws IllegalArgumentException if {@code owner} or {@code owned} contains
	 *                                  {@code "}, {@code "\n"}, or {@code "\r"}
	 */
	public void connectInnerType(String outerType, String innerType) {
		checkName(outerType);
		checkName(innerType);
		println("\"%s\" %s \"%s\"".formatted(outerType, HAS_A_INNER, innerType));
	}

	/**
	 *
	 * @param source
	 * @param target
	 */
	public void connectDepends(String source, String target) {
		checkName(source);
		checkName(target);

		if (activeTag != null) {
			out.print("/'");
			out.print(SPACE_STRING);
			out.print(activeTag);
			out.print(SPACE_STRING);
		}

		out.print(quote(source));
		out.print(SPACE_STRING);
		out.print(USES);
		out.print(SPACE_STRING);
		out.print(quote(target));

		if (activeTag != null) {
			out.print(SPACE_STRING);
			out.print("'/");
		}

		out.println();
	}

	/**
	 *
	 * @param subType
	 * @param superType
	 */
	public void connectImplements(String subType, String superType) {
		checkName(subType);
		checkName(superType);

		if (activeTag != null) {
			out.print("/'");
			out.print(SPACE_STRING);
			out.print(activeTag);
			out.print(SPACE_STRING);
		}
		out.print(quote(subType));

		out.print(SPACE_STRING);
		out.print(IS_A_IMPLEMENTS);

		out.print(SPACE_STRING);
		out.print(quote(superType));

		if (activeTag != null) {
			out.print(SPACE_STRING);
			out.print("'/");
		}

		out.println();
	}

	/**
	 *
	 * @param subType
	 * @param superType
	 */
	public void connectExtends(String subType, String superType) {
		checkName(subType);
		checkName(superType);

		if (activeTag != null) {
			out.print("/'");
			out.print(SPACE_STRING);
			out.print(activeTag);
			out.print(SPACE_STRING);
		}
		out.print(quote(subType));

		out.print(SPACE_STRING);
		out.print(IS_A_EXTENDS);

		out.print(SPACE_STRING);
		out.print(quote(superType));

		if (activeTag != null) {
			out.print(SPACE_STRING);
			out.print("'/");
		}

		out.println();
	}

	/**
	 *
	 * @param sourceType
	 * @param targetType
	 * @param role
	 * @param stereotypes
	 */
	public void connectAssociation(String sourceType, String targetType, String role, String stereotypes) {
		checkName(sourceType);
		checkName(targetType);
		checkName(role);
		checkStereotypes(stereotypes);

		if (activeTag != null) {
			out.print("/'");
			out.print(SPACE_STRING);
			out.print(activeTag);
			out.print(SPACE_STRING);
		}

		out.print(quote(sourceType));

		out.print(SPACE_STRING);
		out.print(HAS_A);

		if (!role.isBlank()) {
			out.print(SPACE_STRING);
			out.print(quote(role));
		}

		out.print(SPACE_STRING);
		out.print(quote(targetType));

		if (!stereotypes.isBlank()) {
			out.print(SPACE_STRING);
			out.print(COLON_SEPARATOR);
			out.print(SPACE_STRING);
			out.print(stereotypes.strip());
		}

		if (activeTag != null) {
			out.print(SPACE_STRING);
			out.print("'/");
		}

		out.println();
	}

	/**
	 *
	 * @param s
	 * @return
	 */
	private static String quote(String s) {
		return "\"" + s.strip() + "\"";
	}

	/**
	 *
	 */
	private String activeTag;

	/**
	 *
	 * @param tag
	 * @param action
	 */
	public void withBeforeTag(String tag, Runnable action) {
		checkTag(tag);
		if (activeTag != null) {
			throw new IllegalStateException("Nested withTag() is not allowed");
		}
		activeTag = tag;
		try {
			action.run();
		} finally {
			activeTag = null;
		}
	}

	/**
	 *
	 * @param tag
	 */
	private void checkTag(String tag) {
		if (tag == null || tag.isBlank()) {
			throw new IllegalArgumentException("Tag must be non-empty");
		}
	}

	// block helpers

	/**
	 *
	 * @param keyword
	 * @param name
	 * @param stereotypes
	 */
	private void beginType(final String keyword, final String name, final String visibility, final String stereotypes) {
		checkName(name);
		checkStereotypes(stereotypes);
		println("%s%s \"%s\"%s { /' @assis:begin %s \"%s\" '/".formatted(visibility, keyword, name.strip(),
				prefixIfPresent(" ", stereotypes.strip()), keyword, name.strip()));
		indent();
	}

	/**
	 *
	 * @param keyword
	 * @param name
	 */
	private void endType(final String keyword, final String name) {
		checkName(name);
		dedent();
		println("} /' @assis:end %s \"%s\" '/".formatted(keyword, name.strip()));
	}

	// static helpers

	/**
	 *
	 * @param stereotypes
	 */
	private static void checkStereotypes(final String stereotypes) {
		requireSingleLine(stereotypes, "stereotypes");
		requireNotContainsQuote(stereotypes, "stereotypes");
	}

	/**
	 *
	 * @param name
	 */
	private static void checkName(final String name) {
		Objects.requireNonNull(name, "name");
		requireSingleLine(name, "name");
		requireNotContainsQuote(name, "name");
	}

	/**
	 *
	 * @param s
	 * @param label
	 */
	private static void requireNotContainsQuote(final String s, final String label) {
		if (s.contains("\"")) {
			throw new IllegalArgumentException("Quote (\") not allowed in " + label + ": " + s);
		}
	}

	/**
	 *
	 * @param s
	 * @param label
	 */
	private static void requireSingleLine(final String s, final String label) {
		if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
			throw new IllegalArgumentException(label + " must be a single line");
		}
	}

	/**
	 *
	 * @param prefix
	 * @param s
	 * @return
	 */
	private static String prefixIfPresent(String prefix, String s) {
		if (s == null || s.isBlank()) {
			return "";
		}
		return prefix + s;
	}

}