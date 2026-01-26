/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsClass;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsName;
import static io.github.masmangan.assis.TestWorkbench.assertPumlNotContains;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramAssociationCoverageSamplesTest {

	@TempDir
	Path tempDir;

	@Test
	void generatesAssociationForSimpleFieldSamePackage() throws Exception {
		String puml = generatePumlFromSample("samples/associations/fields", tempDir, "fields");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.B");
		assertAnyLineContainsAll(puml, "p1.A", "-->", "b", "p1.B");
	}

	@Test
	void generatesAssociationForSimpleFieldAnotherPackageP1() throws Exception {
		String puml = generatePumlFromSample("samples/solver/pkgfieldsp1", tempDir, "pkgfieldsp1");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p2.B");
		assertAnyLineContainsAll(puml, "p2.B", "-->", "a", "p1.A");
	}

	@Test
	void generatesAssociationForSimpleFieldAnotherPackageP2() throws Exception {
		String puml = generatePumlFromSample("samples/solver/pkgfieldsp2", tempDir, "pkgfieldsp2");

		assertPumlContainsName(puml, "p2.A");
		assertPumlContainsName(puml, "p1.B");
		assertAnyLineContainsAll(puml, "p1.B", "-->", "a", "p2.A");
	}

	@Test
	void generatesAssociationForSimpleFieldAnotherPackageP3() throws Exception {
		String puml = generatePumlFromSample("samples/solver/pkgfieldsp3", tempDir, "pkgfieldsp3");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p2.A");
		assertPumlContainsName(puml, "p3.B");
		assertAnyLineContainsAll(puml, "p3.B", "-->", "a", "p1.A");
	}

	@Test
	void generatesAssociationsForArrays1dAnd2d() throws Exception {
		String puml = generatePumlFromSample("samples/associations/arrays", tempDir, "arrays");

		assertPumlContainsName(puml, "p1.ArraysSample");
		assertPumlContainsName(puml, "p1.C");

		assertAnyLineContainsAll(puml, "p1.ArraysSample", "-->", "p1.C", "cs");
		assertAnyLineContainsAll(puml, "p1.ArraysSample", "-->", "p1.C", "matrix");
	}

	@Test
	void rendersGenericFieldButDoesNotCreateAssociationYet() throws Exception {
		String puml = generatePumlFromSample("samples/associations/lists", tempDir, "lists");

		assertPumlContains(puml, "class \"p1.ListField\"");
		assertPumlContains(puml, "class \"p1.D\"");

		assertAnyLineContainsAll(puml, "ds", ":", "List<D>");

		assertPumlNotContains(puml, "-->");
	}

	@Test
	void rendersRecordComponentListAsFieldWithoutAssociationInterpretation() throws Exception {
		String puml = generatePumlFromSample("samples/associations/records", tempDir, "records");

		assertPumlContains(puml, "record \"p1.ListRecord\"");
		assertPumlContains(puml, "class \"p1.E\"");

		assertAnyLineContainsAll(puml, "es", ":", "List<E>");

		assertPumlNotContains(puml, "-->");

		assertAnyLineContainsAll(puml, "p1.ListRecord", "..>", "p1.E");
		assertAnyLineContainsAll(puml, "cherry-pick ghost", "p1.ListRecord", "..>", "java.util.List");

	}

	@Test
	void rendersOptionalFieldAndRecordComponentAsFieldsWithoutAssociationInterpretation() throws Exception {
		String puml = generatePumlFromSample("samples/associations/optionals", tempDir, "optionals");

		assertPumlContainsName(puml, "p1.OptionalField");
		assertPumlContainsName(puml, "p1.OptionalRecord");
		assertPumlContainsName(puml, "p1.F");

		assertAnyLineContainsAll(puml, "f", ":", "Optional<F>");

		assertPumlNotContains(puml, "-->");
	}

	@Test
	void generatesAssociationForCrossPackageTypeUsingFqn() throws Exception {
		String puml = generatePumlFromSample("samples/associations/cross", tempDir, "cross");

		assertPumlContainsName(puml, "p1.CrossPackage");
		assertPumlContainsName(puml, "p2.G");
		assertAnyLineContainsAll(puml, "p1.CrossPackage", "-->", "p2.G", "g");
	}

	@Test
	void generatesAssociationForEnumField() throws Exception {
		String puml = generatePumlFromSample("samples/associations/enum", tempDir, "enum");

		assertPumlContainsName(puml, "p1.EnumFieldSample");
		assertPumlContainsName(puml, "p1.H");
		assertAnyLineContainsAll(puml, "p1.EnumFieldSample", "-->", "p1.H", "h");
	}

	@Test
	void generatesAssociationForInterfaceConstantField() throws Exception {
		String puml = generatePumlFromSample("samples/associations/constant", tempDir, "constant");

		assertPumlContainsName(puml, "p1.InterfaceConstant");
		assertPumlContainsName(puml, "p1.I");
		assertAnyLineContainsAll(puml, "p1.InterfaceConstant", "-->", "p1.I", "DEFAULT");
	}

	@Test
	void generatesAssociationStereotypesFromAnnotations() throws Exception {
		String puml = generatePumlFromSample("samples/associations/stereotype", tempDir, "stereotype");

		assertPumlContainsName(puml, "p1.FieldStereotype");
		assertPumlContainsName(puml, "p1.RecordStereotype");
		assertPumlContainsName(puml, "p1.J");

		assertAnyLineContainsAll(puml, "p1.FieldStereotype", "-->", "p1.J", ":", "x", "<<AssocTag>>");
		assertAnyLineContainsAll(puml, "p1.RecordStereotype", "-->", "p1.J", ":", "y", "<<AssocTag>>");
	}

	@Test
	void generatesDiagramContainingAssociation() throws Exception {
		String puml = TestWorkbench.generatePumlFromSample("samples/associations/association", tempDir, "association");

		assertPumlContains(puml, "\"samples.association.Order\" ---> \"buyer\" \"samples.association.Customer\"");
		assertPumlContainsClass(puml, "samples.association.Order");
		assertPumlContainsClass(puml, "samples.association.Customer");

	}

}