package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.MedicareBeneficiaryIdHistory;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link SamhsaMatcherTest}. Integration with
 * {@link ExplanationOfBenefitResourceProvider} is covered by
 * {@link ExplanationOfBenefitResourceProviderIT#searchForEobsWithSamhsaFiltering()}
 * and related integration tests.
 */
public final class SamhsaMatcherTest {
	// TODO complete and verify that these exactly match real values in our DB
	static final String SAMPLE_SAMHSA_CPT_CODE = "4320F";
	static final String SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE = "29189";
	static final String SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE = "9445";
	static final String SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE = "F1010";
	static final String SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE = "HZ2ZZZZ";
	static final String SAMPLE_SAMHSA_DRG_CODE = "522";

	private static final String DRG = TransformerUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>false</code> for claims that have no SAMHSA-related codes.
	 */
	@Test
	public void nonSamhsaRelatedClaims() {
		SamhsaMatcher matcher = new SamhsaMatcher();

		// Note: none of our SAMPLE_A claims have SAMHSA-related codes (by default).
		List<Object> sampleRifRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		List<ExplanationOfBenefit> sampleEobs = sampleRifRecords.stream().map(r -> {
			// FIXME remove most `else if`s once filtering fully supports all claim types
			if (r instanceof Beneficiary)
				return null;
			else if (r instanceof BeneficiaryHistory)
				return null;
			else if (r instanceof MedicareBeneficiaryIdHistory)
				return null;

			return TransformerUtils.transformRifRecordToEob(new MetricRegistry(), r);
		}).filter(ExplanationOfBenefit.class::isInstance).collect(Collectors.toList());

		for (ExplanationOfBenefit sampleEob : sampleEobs)
			Assert.assertFalse("Unexpected SAMHSA filtering of EOB: " + sampleEob.getId(), matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchCarrierClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 10 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchCarrierClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related CPT procedure codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchCarrierClaimsByCptProcedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
		Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
		sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchDmeClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 10 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchDmeClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related CPT procedure codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchDmeClaimsByCptProcedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
		Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
		sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#INPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchInpatientClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#INPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis
	 * codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchInpatientClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#INPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchInpatientClaimsByIcd9Procedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
		Coding sampleEobDiagnosis = sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#INPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis
	 * codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchInpatientClaimsByIcd10Procedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
		Coding sampleEobDiagnosis = sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#INPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related drg codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchInpatientClaimsByDrg() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
		sampleEob.getDiagnosisFirstRep().getPackageCode().addCoding().setSystem(SamhsaMatcherTest.DRG)
				.setCode(SAMPLE_SAMHSA_DRG_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#OUTPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchOutpatientClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#OUTPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis
	 * codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchOutpatientClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#OUTPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchOutpatientClaimsByCptProcedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
		Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
		sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#OUTPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchOutpatientClaimsByIcd9Procedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
		Coding sampleEobDiagnosis = sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#OUTPATIENT}
	 * {@link ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis
	 * codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchOutpatientClaimsByIcd10Procedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
		Coding sampleEobDiagnosis = sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#HHA} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchHhaClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#HHA} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 10 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchHhaClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#HHA} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related CPT procedure codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchHhaClaimsByCptProcedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
		Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
		sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#HOSPICE} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchHospiceClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#HOSPICE} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 10 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchHospiceClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#HOSPICE} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related CPT procedure codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchHospiceClaimsByCptProcedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
		Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
		sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchSnfClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 10 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchSnfClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related CPT procedure codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchSnfClaimsByCptProcedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
		Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
		sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchSnfClaimsByIcd9Procedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
		Coding sampleEobDiagnosis = sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 10 diagnosis codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchSnfClaimsByIcd10Procedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
		Coding sampleEobDiagnosis = sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related drg codes.
	 * 
	 * @throws FHIRException (indicates problem with test data)
	 */
	@Test
	public void matchSnfClaimsByDrg() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
		sampleEob.getDiagnosisFirstRep().getPackageCode().addCoding().setSystem(SamhsaMatcherTest.DRG)
				.setCode(SAMPLE_SAMHSA_DRG_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * @param claimType the {@link ClaimType} to get a sample
	 *                  {@link ExplanationOfBenefit} for
	 * @return a sample {@link ExplanationOfBenefit} of the specified
	 *         {@link ClaimType} (derived from the
	 *         {@link StaticRifResourceGroup#SAMPLE_A} sample RIF records)
	 */
	private ExplanationOfBenefit getSampleAClaim(ClaimType claimType) {
		List<Object> sampleRifRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Object sampleRifRecordForClaimType = sampleRifRecords.stream().filter(claimType.getEntityClass()::isInstance)
				.findFirst().get();
		ExplanationOfBenefit sampleEobForClaimType = TransformerUtils.transformRifRecordToEob(new MetricRegistry(),
				sampleRifRecordForClaimType);

		return sampleEobForClaimType;
	}
}
