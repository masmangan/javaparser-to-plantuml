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
 * line. This class also provides helper methods to emit common block constructs
 * such as packages and type declarations.
 *
 * <p>
 * Blocks started with {@code begin*} methods must be closed with a
 * corresponding {@code end*} method. While inside a package or type block,
 * subsequent lines are indented by two spaces per indentation level.
 *
 * <p>
 * This class performs no validation of PlantUML syntax and does not attempt to
 * enforce balanced blocks.
 * <p>
 * For format integrity, names must be single-line and must not contain double
 * quotes ("). Violations cause IllegalArgumentException.
 * <p>
 * End a type block before beginning another type block.
 * <p>
 * PlantUML does not support true nested type declarations. To represent Java
 * inner or nested types, emit each type as a separate PlantUML type and encode
 * the nesting relationship in the type name using a separator.
 * <p>
 * Package qualification follows standard Java fully qualified naming, using the
 * {@code .} (dot) separator. Avoid using {@code .} to encode nesting; treat
 * {@code .} as package qualification.
 * <p>
 * Structural relationships between types (such as nesting, inheritance, and
 * interface implementation) are rendered separately using standard PlantUML
 * relationship syntax. For details on relationship notation, refer to the
 * PlantUML documentation.
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
 * pw.beginClass("p.A", null); // Names are provided unquoted; helpers quote as needed.
 * pw.endClass("p.A");
 *
 * pw.beginClass("p.A$B", null); // A different separator than ".".
 * pw.endClass("p.A$B");
 *
 * pw.println("\"p.A\" +-- \"p.A$B\""); // Raw PlantUML: caller provides correct quoting.
 * }</pre>
 * <p>
 * Closing this writer flushes its output but does not close the underlying
 * {@code PrintWriter}, since it is not owned by this instance.
 *
 * @since 0.9.1
 * @author Marco Mangan
 */
public final class PlantUMLWriter implements AutoCloseable {

	private static final String INDENT_UNIT = "  ";

	private final PrintWriter out;

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
		Objects.requireNonNull(line, "line");
		out.println(INDENT_UNIT.repeat(indentLevel) + line);
	}

	/**
	 * Writes an empty line.
	 */
	public void println() {
		println("");
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
		println("@startuml " + name);
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
		println("@enduml");
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
		println("package " + quote(name) + " {");
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
		println("} /' @assis:end package " + quote(name) + " '/");
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
	public void beginClass(final String name, final String stereotypes) {
		beginType("class", name, stereotypes);
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
	public void beginAbstractClass(final String name, final String stereotypes) {
		beginType("abstract class", name, stereotypes);
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
	public void beginInterface(final String name, final String stereotypes) {
		beginType("interface", name, stereotypes);
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
	public void beginRecord(final String name, final String stereotypes) {
		beginType("record", name, stereotypes);
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
	public void beginEnum(final String name, final String stereotypes) {
		beginType("enum", name, stereotypes);
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
	public void beginAnnotation(final String name, final String stereotypes) {
		beginType("annotation", name, stereotypes);
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

	private void beginType(final String keyword, final String name, final String stereotypes) {
		checkName(name);
		requireSingleLine(stereotypes, "stereotypes");
		requireNotContainsQuote(stereotypes, "stereotypes");
		println(keyword + " " + quote(name) + stereotypesSuffix(stereotypes) + " {");
		indent();
	}

	private void endType(final String keyword, final String name) {
		checkName(name);
		dedent();
		println("} /' @assis:end " + keyword + " " + quote(name) + "'/");
	}

	private static void checkName(final String name) {
		Objects.requireNonNull(name, "name");
		requireSingleLine(name, "name");
		requireNotContainsQuote(name, "name");
	}

	private static void requireNotContainsQuote(final String s, final String label) {
		if (s.contains("\"")) {
			throw new IllegalArgumentException("Quote (\") not allowed in " + label + ": " + s);
		}
	}

	private static void requireSingleLine(final String s, final String label) {
		if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
			throw new IllegalArgumentException(label + " must be a single line");
		}
	}

	private static String quote(final String name) {
		checkName(name);
		return "\"" + name.trim() + "\"";
	}

	private static String stereotypesSuffix(final String stereotypes) {
		if (stereotypes == null) {
			return "";
		}
		final String s = stereotypes.trim();
		return s.isEmpty() ? "" : " " + s;
	}

}