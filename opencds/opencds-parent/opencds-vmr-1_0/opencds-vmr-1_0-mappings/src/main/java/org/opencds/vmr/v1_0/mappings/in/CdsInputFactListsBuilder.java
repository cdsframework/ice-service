/**
 * Copyright 2011 - 2013 OpenCDS.org
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 *	
 */

package org.opencds.vmr.v1_0.mappings.in;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencds.common.exceptions.DataFormatException;
import org.opencds.common.exceptions.ImproperUsageException;
import org.opencds.common.exceptions.InvalidDataException;
import org.opencds.common.exceptions.InvalidDriDataFormatException;
import org.opencds.common.exceptions.OpenCDSRuntimeException;
import org.opencds.common.utilities.AbsoluteTimeDifference;
import org.opencds.common.utilities.DateUtility;
import org.opencds.config.api.FactListsBuilder;
import org.opencds.config.api.KnowledgeRepository;
import org.opencds.config.api.model.ConceptDeterminationMethod;
import org.opencds.config.api.model.KnowledgeModule;
import org.opencds.config.api.model.SupportMethod;
import org.opencds.config.api.service.ConceptService;
import org.opencds.vmr.v1_0.internal.AdministrableSubstance;
import org.opencds.vmr.v1_0.internal.AdverseEvent;
import org.opencds.vmr.v1_0.internal.AppointmentProposal;
import org.opencds.vmr.v1_0.internal.AppointmentRequest;
import org.opencds.vmr.v1_0.internal.CDSInput;
import org.opencds.vmr.v1_0.internal.DeniedAdverseEvent;
import org.opencds.vmr.v1_0.internal.DeniedProblem;
import org.opencds.vmr.v1_0.internal.EncounterEvent;
import org.opencds.vmr.v1_0.internal.Entity;
import org.opencds.vmr.v1_0.internal.EvalTime;
import org.opencds.vmr.v1_0.internal.EvaluatedPerson;
import org.opencds.vmr.v1_0.internal.EvaluatedPersonAgeAtEvalTime;
import org.opencds.vmr.v1_0.internal.EvaluatedPersonRelationship;
import org.opencds.vmr.v1_0.internal.Facility;
import org.opencds.vmr.v1_0.internal.FocalPersonId;
import org.opencds.vmr.v1_0.internal.Goal;
import org.opencds.vmr.v1_0.internal.GoalProposal;
import org.opencds.vmr.v1_0.internal.MissedAppointment;
import org.opencds.vmr.v1_0.internal.ObservationOrder;
import org.opencds.vmr.v1_0.internal.ObservationProposal;
import org.opencds.vmr.v1_0.internal.ObservationResult;
import org.opencds.vmr.v1_0.internal.Organization;
import org.opencds.vmr.v1_0.internal.Person;
import org.opencds.vmr.v1_0.internal.Problem;
import org.opencds.vmr.v1_0.internal.ProcedureEvent;
import org.opencds.vmr.v1_0.internal.ProcedureOrder;
import org.opencds.vmr.v1_0.internal.ProcedureProposal;
import org.opencds.vmr.v1_0.internal.ScheduledAppointment;
import org.opencds.vmr.v1_0.internal.ScheduledProcedure;
import org.opencds.vmr.v1_0.internal.Specimen;
import org.opencds.vmr.v1_0.internal.SubstanceAdministrationEvent;
import org.opencds.vmr.v1_0.internal.SubstanceAdministrationOrder;
import org.opencds.vmr.v1_0.internal.SubstanceAdministrationProposal;
import org.opencds.vmr.v1_0.internal.SubstanceDispensationEvent;
import org.opencds.vmr.v1_0.internal.SupplyEvent;
import org.opencds.vmr.v1_0.internal.SupplyOrder;
import org.opencds.vmr.v1_0.internal.SupplyProposal;
import org.opencds.vmr.v1_0.internal.UnconductedObservation;
import org.opencds.vmr.v1_0.internal.UndeliveredProcedure;
import org.opencds.vmr.v1_0.internal.UndeliveredSubstanceAdministration;
import org.opencds.vmr.v1_0.internal.UndeliveredSupply;
import org.opencds.vmr.v1_0.internal.VMR;
import org.opencds.vmr.v1_0.mappings.mappers.AdministrableSubstanceMapper;
import org.opencds.vmr.v1_0.mappings.mappers.CDSInputMapper;
import org.opencds.vmr.v1_0.mappings.mappers.ClinicalStatementRelationshipMapper;
import org.opencds.vmr.v1_0.mappings.mappers.EntityMapper;
import org.opencds.vmr.v1_0.mappings.mappers.EntityRelationshipMapper;
import org.opencds.vmr.v1_0.mappings.mappers.EvaluatedPersonMapper;
import org.opencds.vmr.v1_0.mappings.mappers.EvaluatedPersonRelationshipMapper;
import org.opencds.vmr.v1_0.mappings.mappers.FacilityMapper;
import org.opencds.vmr.v1_0.mappings.mappers.OneObjectMapper;
import org.opencds.vmr.v1_0.mappings.mappers.OrganizationMapper;
import org.opencds.vmr.v1_0.mappings.mappers.PersonMapper;
import org.opencds.vmr.v1_0.mappings.mappers.SpecimenMapper;
import org.opencds.vmr.v1_0.mappings.mappers.VMRMapper;
import org.opencds.vmr.v1_0.mappings.utilities.MappingUtility;


/**
 * <p>structural mapper to go from external XML data described by vmr.xsd in project opencds-vmr-v1_0-schema
 * to internal form based on javaBeans in the project opencds-vmr-v1_0-internal.
 * 
 * Note that this class also accomplishes the following:
 * - Based on DOB, populates EvaluatedPersonAgeAtEvalTime (this may be extended in the future to 
 * 		not rely on the presence of a DOB and accept, e.g., from Observations about age)
 * 
 * - Populates Concept lists
 * 
 * <p/>
 * <p>Copyright: Copyright (c) 2010</p>
 * <p>Company: OpenCDS</p>
 *
 * @authors David Shields, Kensaku Kawamoto, Daryl Chertcoff (developed mappers library)
 * @version 2.0
 * @date 09-11-2011
 * 
 */
public class CdsInputFactListsBuilder implements FactListsBuilder 
{
	private static final Logger log = LogManager.getLogger();
	
	private final BuildOpenCDSConceptLists buildOpenCDSConceptLists;

	public CdsInputFactListsBuilder() {
		buildOpenCDSConceptLists = new BuildOpenCDSConceptLists();
	}

	/**
	 * FIXME: allFactLists is mutable at this point
	 */
	@Override
    public Map<Class<?>, List<?>> buildFactLists(KnowledgeRepository knowledgeRepository, KnowledgeModule knowledgeModule, Object payload, Date evalTime) {
		MappingUtility 				mu					= new MappingUtility(); 
		log.debug("buildFactLists");
		long t0 = System.nanoTime();
		Map<Class<?>, List<?>> allFactLists = new ConcurrentHashMap<>();

		try {
			MappingUtility.initParsedDatesCache();
		    FactLists factLists = new FactLists();
		    
		    @SuppressWarnings("unchecked")
            JAXBElement<org.opencds.vmr.v1_0.schema.CDSInput> cdsInput = (JAXBElement<org.opencds.vmr.v1_0.schema.CDSInput>) payload; 
            
            EvalTime evalTimeFact = new EvalTime();
			evalTimeFact.setEvalTimeValue(evalTime);
			
			factLists.put(EvalTime.class, evalTimeFact);
			
			if ( (cdsInput.getValue() == null) ) {
				throw new InvalidDriDataFormatException( "Error: No payload within the CDSInput." ); 
			}
			
            // for later reference, never changes for the entire VMR...
            FocalPersonId focalPersonId = null;

            CDSInput internalCDSInput = new CDSInput();
            CDSInputMapper.pullIn(cdsInput.getValue(), internalCDSInput, mu);
            factLists.put(CDSInput.class, internalCDSInput);

            log.debug("CdsInputFactListsBuilder for focalPersonId=" + focalPersonId);
            focalPersonId = new FocalPersonId(internalCDSInput.getFocalPersonId());
            factLists.put(FocalPersonId.class, focalPersonId);

            // same as focal person initially, but will change for each
            // "otherEvaluatedPerson" entry
            String subjectPersonId = focalPersonId.getId();

            // TODO: How will we pass in the CDSResource (Context, Resource and
            // Template)? These are above when mapping to internalCDSInput, but
            // they aren't further broken down for Drools or anything else.
            // (BUG)
            org.opencds.vmr.v1_0.schema.VMR vmrInput = cdsInput.getValue().getVmrInput();

            VMR internalVMR = new VMR();
			VMRMapper.pullIn( vmrInput, internalVMR, mu);	 
			factLists.put(VMR.class, internalVMR);
			
			// ================= Patient =======================//
			if ( vmrInput.getPatient() != null ) {
				org.opencds.vmr.v1_0.schema.EvaluatedPerson inputPatient = vmrInput.getPatient();
				oneEvaluatedPerson(inputPatient, evalTime, subjectPersonId, focalPersonId.getId(), factLists);
			}
			
			// ================= OtherEvaluatedPersons =======================//
			if ( (vmrInput.getOtherEvaluatedPersons() != null) && (vmrInput.getOtherEvaluatedPersons().getEvaluatedPerson() != null) 
					&& (vmrInput.getOtherEvaluatedPersons().getEvaluatedPerson().size() > 0) ) {
				List<org.opencds.vmr.v1_0.schema.EvaluatedPerson> input = vmrInput.getOtherEvaluatedPersons().getEvaluatedPerson();
				for ( org.opencds.vmr.v1_0.schema.EvaluatedPerson  eachOtherEvaluatedPerson : input ) {
					subjectPersonId = MappingUtility.iI2FlatId(eachOtherEvaluatedPerson.getId());
					oneEvaluatedPerson(eachOtherEvaluatedPerson, evalTime, subjectPersonId, focalPersonId.getId(), factLists);
				}
			}

			// ================= EvaluatedPersonRelationships (for family history relationships, where relatedPersons also have their own vMR) =======================//
			if ( (vmrInput.getEvaluatedPersonRelationships() != null) && (vmrInput.getEvaluatedPersonRelationships().getEvaluatedPersonRelationship() != null) 
					&& (vmrInput.getEvaluatedPersonRelationships().getEvaluatedPersonRelationship().size() > 0) ) {
				List<org.opencds.vmr.v1_0.schema.EntityRelationship> input = vmrInput.getEvaluatedPersonRelationships().getEvaluatedPersonRelationship();
				for ( org.opencds.vmr.v1_0.schema.EntityRelationship each : input ) {
					EvaluatedPersonRelationship internalEvaluatedPersonRelationship = EvaluatedPersonRelationshipMapper.pullIn(each, mu);
					factLists.put(EvaluatedPersonRelationship.class, internalEvaluatedPersonRelationship);
				}
			}
			// ================= End of Building all Internal FactLists =======================//				
			
			// ================= Populate OpenCDS Concepts through Post-Processing of Internal VMR =======================//
			try {
				ConceptService conceptService = knowledgeRepository.getConceptService().byKM(knowledgeModule);
				buildOpenCDSConceptLists.buildConceptLists(conceptService, factLists, allFactLists);
			} catch (Exception e) {
				String err = e.getMessage();
				e.printStackTrace();
				throw new InvalidDriDataFormatException("BuildOpenCDSConceptLists threw error: " + err + "; " + e);
			}
			// ================= End Populating Code Concepts =======================//
			 
			// ================= Begin Populating AllFactLists object =======================//				 
			factLists.populateAllFactLists(allFactLists);
			// ================= End Populating AllFactLists object =======================//
			log.debug("buildFactLists completed for " + focalPersonId);
		} catch (ImproperUsageException | DataFormatException | InvalidDataException e) {
			e.printStackTrace();
			throw new InvalidDriDataFormatException(e.getClass().getSimpleName() + " error in CdsInputFactListsBuilder: " 
					+ e.getMessage() + ", therefore unable to complete unmarshalling input Semantic Payload: " + payload.toString() );
		} catch (OpenCDSRuntimeException e) {
		    throw new OpenCDSRuntimeException("RuntimeException in CdsInputFactListsBuilder: " + e.getMessage());
        } catch (Exception e) {
            String unknownError = e.getMessage();
            e.printStackTrace();
            throw new InvalidDriDataFormatException("Unknown error initializing CdsInputFactListsBuilder: " 
                    + unknownError + ", therefore unable to complete unmarshalling input Semantic Payload: " + payload.toString() );
		} finally {
			// Make sure to clear out the date cache for this request
			MappingUtility.clearParsedDatesCache();
		}
		/**
		 * The actual output of the mapping is in allFactLists, which is a live I-O input parameter
		 */
		log.debug("CdsInputFactListsBuilder time : " + (System.nanoTime() - t0)/1e6 + " ms");
		return allFactLists;
	}
	

	/**
	 * Process one evaluated person, either the primary patient, or other evaluated persons
	 * 
	 * @param inputPatient
	 * @param evalTime
	 * @param subjectPersonId
	 * @param focalPersonId
	 * @param factLists
	 * @throws ImproperUsageException
	 * @throws DataFormatException
	 * @throws InvalidDataException
	 */
	private void oneEvaluatedPerson (
			org.opencds.vmr.v1_0.schema.EvaluatedPerson	inputPatient, 
			Date										evalTime,
			String										subjectPersonId,
			String										focalPersonId,
			FactLists 									factLists
		) throws ImproperUsageException, DataFormatException, InvalidDataException
	{

		EvaluatedPerson internalPatient = new EvaluatedPerson();
		EvaluatedPersonMapper.pullIn(inputPatient, internalPatient, null, null, subjectPersonId, focalPersonId, factLists);
		
		// ================= Demographic Data =======================//
		if ( inputPatient.getDemographics() != null ) {
			// Note that the EvaluatedPersonMapper has already populated all the basic Demographic data
			// if Birth Time present, populate EvaluatedPersonAgeAtEvalTime entries
			Date birthTime = null;
			if (inputPatient.getDemographics().getBirthTime() != null) {
				birthTime = MappingUtility.tS2DateInternal(inputPatient.getDemographics().getBirthTime());
				populateEvaluatedPersonAgeAtEvalTime(birthTime, evalTime, focalPersonId, factLists);
			} 
	
		}
		factLists.put(EvaluatedPerson.class, internalPatient);
		// ================= End Demographic Data =======================//
		
		// ================= Clinical Statement Relationships =======================//
		//Load these first, so that we can check them when we are loading clinical statements
		if (inputPatient.getClinicalStatementRelationships() != null)
		{							
			org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatementRelationships inputClinicalStatementRelationships = inputPatient.getClinicalStatementRelationships();
			for ( int i = 0; ((inputClinicalStatementRelationships.getClinicalStatementRelationship() != null) 
					&& (i < inputClinicalStatementRelationships.getClinicalStatementRelationship().size() )); i++  ) {
				ClinicalStatementRelationshipMapper.pullIn(inputClinicalStatementRelationships.getClinicalStatementRelationship().get(i), factLists);				
			}
		}

		// ================= End Clinical Statement Relationships =======================//
		
		// ================= Clinical Statements =======================//
		org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatements inputClinicalStatements = inputPatient.getClinicalStatements();
		//following "for" statements must be in same order as given in the schema
		
		if (inputClinicalStatements != null) {
    		for ( int i = 0; ((inputClinicalStatements.getAdverseEvents() != null) 
    				&& (i < inputClinicalStatements.getAdverseEvents().getAdverseEvent().size() )); i++  ) 
    		{
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getAdverseEvents().getAdverseEvent().get(i), new AdverseEvent(), subjectPersonId, focalPersonId, factLists);
    		}
    
    		for ( int i = 0; ((inputClinicalStatements.getDeniedAdverseEvents() != null) 
    				&& (i < inputClinicalStatements.getDeniedAdverseEvents().getDeniedAdverseEvent().size() )); i++  ) 
    		{
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getDeniedAdverseEvents().getDeniedAdverseEvent().get(i), new DeniedAdverseEvent(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getAppointmentProposals() != null) 
    				&& (i < inputClinicalStatements.getAppointmentProposals().getAppointmentProposal().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getAppointmentProposals().getAppointmentProposal().get(i), new AppointmentProposal(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getAppointmentRequests() != null) 
    				&& (i < inputClinicalStatements.getAppointmentRequests().getAppointmentRequest().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getAppointmentRequests().getAppointmentRequest().get(i), new AppointmentRequest(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getEncounterEvents() != null) 
    				&& (i < inputClinicalStatements.getEncounterEvents().getEncounterEvent().size() )); i++  ) 
    		{
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getEncounterEvents().getEncounterEvent().get(i), new EncounterEvent(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getMissedAppointments() != null) 
    				&& (i < inputClinicalStatements.getMissedAppointments().getMissedAppointment().size() )); i++  ) 
    		{
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getMissedAppointments().getMissedAppointment().get(i), new MissedAppointment(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getScheduledAppointments() != null) 
    				&& (i < inputClinicalStatements.getScheduledAppointments().getScheduledAppointment().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getScheduledAppointments().getScheduledAppointment().get(i), new ScheduledAppointment(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getGoals() != null) 
    				&& (i < inputClinicalStatements.getGoals().getGoal().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getGoals().getGoal().get(i), new Goal(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getGoalProposals() != null) 
    				&& (i < inputClinicalStatements.getGoalProposals().getGoalProposal().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getGoalProposals().getGoalProposal().get(i), new GoalProposal(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getObservationOrders() != null) 
    				&& (i < inputClinicalStatements.getObservationOrders().getObservationOrder().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getObservationOrders().getObservationOrder().get(i), new ObservationOrder(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getObservationProposals() != null) 
    				&& (i < inputClinicalStatements.getObservationProposals().getObservationProposal().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getObservationProposals().getObservationProposal().get(i), new ObservationProposal(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getObservationResults() != null) 
    				&& (i < inputClinicalStatements.getObservationResults().getObservationResult().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getObservationResults().getObservationResult().get(i), new ObservationResult(), subjectPersonId, focalPersonId, factLists);
    		}
    				
    		for ( int i = 0; ((inputClinicalStatements.getUnconductedObservations() != null) 
    				&& (i < inputClinicalStatements.getUnconductedObservations().getUnconductedObservation().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getUnconductedObservations().getUnconductedObservation().get(i), new UnconductedObservation(), subjectPersonId, focalPersonId, factLists);
    		}
    					
    		for ( int i = 0; ((inputClinicalStatements.getDeniedProblems() != null) 
    				&& (i < inputClinicalStatements.getDeniedProblems().getDeniedProblem().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getDeniedProblems().getDeniedProblem().get(i), new DeniedProblem(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getProblems() != null) 
    				&& (i < inputClinicalStatements.getProblems().getProblem().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getProblems().getProblem().get(i), new Problem(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getProcedureEvents() != null) 
    				&& (i < inputClinicalStatements.getProcedureEvents().getProcedureEvent().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getProcedureEvents().getProcedureEvent().get(i), new ProcedureEvent(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getProcedureOrders() != null) 
    				&& (i < inputClinicalStatements.getProcedureOrders().getProcedureOrder().size() )); i++  ) 
    		{
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getProcedureOrders().getProcedureOrder().get(i), new ProcedureOrder(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getProcedureProposals() != null) 
    				&& (i < inputClinicalStatements.getProcedureProposals().getProcedureProposal().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getProcedureProposals().getProcedureProposal().get(i), new ProcedureProposal(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getScheduledProcedures() != null) 
    				&& (i < inputClinicalStatements.getScheduledProcedures().getScheduledProcedure().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getScheduledProcedures().getScheduledProcedure().get(i), new ScheduledProcedure(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getUndeliveredProcedures() != null) 
    				&& (i < inputClinicalStatements.getUndeliveredProcedures().getUndeliveredProcedure().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getUndeliveredProcedures().getUndeliveredProcedure().get(i), new UndeliveredProcedure(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getSubstanceAdministrationEvents() != null) 
    				&& (i < inputClinicalStatements.getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().size() )); i++  ) 
    		{
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getSubstanceAdministrationEvents().getSubstanceAdministrationEvent().get(i), new SubstanceAdministrationEvent(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getSubstanceAdministrationOrders() != null) 
    				&& (i < inputClinicalStatements.getSubstanceAdministrationOrders().getSubstanceAdministrationOrder().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getSubstanceAdministrationOrders().getSubstanceAdministrationOrder().get(i), new SubstanceAdministrationOrder(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getSubstanceAdministrationProposals() != null) 
    				&& (i < inputClinicalStatements.getSubstanceAdministrationProposals().getSubstanceAdministrationProposal().size() )); i++  ) 
    		{
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getSubstanceAdministrationProposals().getSubstanceAdministrationProposal().get(i), new SubstanceAdministrationProposal(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getSubstanceDispensationEvents() != null) 
    				&& (i < inputClinicalStatements.getSubstanceDispensationEvents().getSubstanceDispensationEvent().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getSubstanceDispensationEvents().getSubstanceDispensationEvent().get(i), new SubstanceDispensationEvent(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getUndeliveredSubstanceAdministrations() != null) 
    				&& (i < inputClinicalStatements.getUndeliveredSubstanceAdministrations().getUndeliveredSubstanceAdministration().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getUndeliveredSubstanceAdministrations().getUndeliveredSubstanceAdministration().get(i), new UndeliveredSubstanceAdministration(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getSupplyEvents() != null) 
    				&& (i < inputClinicalStatements.getSupplyEvents().getSupplyEvent().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getSupplyEvents().getSupplyEvent().get(i), new SupplyEvent(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getSupplyOrders() != null) 
    				&& (i < inputClinicalStatements.getSupplyOrders().getSupplyOrder().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getSupplyOrders().getSupplyOrder().get(i), new SupplyOrder(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getSupplyProposals() != null) 
    				&& (i < inputClinicalStatements.getSupplyProposals().getSupplyProposal().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getSupplyProposals().getSupplyProposal().get(i), new SupplyProposal(), subjectPersonId, focalPersonId, factLists);
    		}
    		
    		for ( int i = 0; ((inputClinicalStatements.getUndeliveredSupplies() != null) 
    				&& (i < inputClinicalStatements.getUndeliveredSupplies().getUndeliveredSupply().size() )); i++  ) {
    			OneObjectMapper.pullInClinicalStatement(inputClinicalStatements.getUndeliveredSupplies().getUndeliveredSupply().get(i), new UndeliveredSupply(), subjectPersonId, focalPersonId, factLists);
    		}	 							
		}
		// ================= End Clinical Statements =======================//
		
		// ================= Entity Lists (related to person's clinical statements) =======================//
		if (inputPatient.getEntityLists() != null)
		{
			org.opencds.vmr.v1_0.schema.EvaluatedPerson.EntityLists inputEntityLists = inputPatient.getEntityLists();
			if (inputEntityLists.getAdministrableSubstances() != null)
			{
				org.opencds.vmr.v1_0.schema.EvaluatedPerson.EntityLists.AdministrableSubstances administrableSubstances = inputEntityLists.getAdministrableSubstances();				
				for ( int i = 0; ((administrableSubstances.getAdministrableSubstance() != null) 
						&& (i < administrableSubstances.getAdministrableSubstance().size() )); i++  ) {
					AdministrableSubstanceMapper.pullIn(administrableSubstances.getAdministrableSubstance().get(i), new AdministrableSubstance(), null, null, subjectPersonId, focalPersonId, factLists);
				}
			}
			if (inputEntityLists.getEntities() != null)
			{
				org.opencds.vmr.v1_0.schema.EvaluatedPerson.EntityLists.Entities oneGroup = inputEntityLists.getEntities();				
				for ( int i = 0; ((oneGroup.getEntity() != null) 
						&& (i < oneGroup.getEntity().size() )); i++  ) {
					EntityMapper.pullIn(oneGroup.getEntity().get(i), new Entity(), null, null, subjectPersonId, focalPersonId, factLists);
				}
			}
			if (inputEntityLists.getFacilities() != null)
			{
				org.opencds.vmr.v1_0.schema.EvaluatedPerson.EntityLists.Facilities oneGroup = inputEntityLists.getFacilities();				
				for ( int i = 0; ((oneGroup.getFacility() != null) 
						&& (i < oneGroup.getFacility().size() )); i++  ) {
					FacilityMapper.pullIn(oneGroup.getFacility().get(i), new Facility(), null, null, subjectPersonId, focalPersonId, factLists);
				}
			}
			if (inputEntityLists.getOrganizations() != null)
			{
				org.opencds.vmr.v1_0.schema.EvaluatedPerson.EntityLists.Organizations oneGroup = inputEntityLists.getOrganizations();				
				for ( int i = 0; ((oneGroup.getOrganization() != null) 
						&& (i < oneGroup.getOrganization().size() )); i++  ) {
					OrganizationMapper.pullIn(oneGroup.getOrganization().get(i), new Organization(), null, null, subjectPersonId, focalPersonId, factLists);
				}
			}
			if (inputEntityLists.getPersons() != null)
			{
				org.opencds.vmr.v1_0.schema.EvaluatedPerson.EntityLists.Persons oneGroup = inputEntityLists.getPersons();				
				for ( int i = 0; ((oneGroup.getPerson() != null) 
						&& (i < oneGroup.getPerson().size() )); i++  ) {
					PersonMapper.pullIn(oneGroup.getPerson().get(i), new Person(), null, null, subjectPersonId, focalPersonId, factLists);
				}
			}
			if (inputEntityLists.getSpecimens() != null)
			{
				org.opencds.vmr.v1_0.schema.EvaluatedPerson.EntityLists.Specimens oneGroup = inputEntityLists.getSpecimens();				
				for ( int i = 0; ((oneGroup.getSpecimen() != null) 
						&& (i < oneGroup.getSpecimen().size() )); i++  ) {
					SpecimenMapper.pullIn(oneGroup.getSpecimen().get(i), new Specimen(), null, null, subjectPersonId, focalPersonId, factLists);
				}
			}
		}
		// ================= End Entities =======================//

		// ================= Entity to Entity Relationships =======================//
		if (inputPatient.getEntityRelationships() != null)
		{
			org.opencds.vmr.v1_0.schema.EvaluatedPerson.EntityRelationships inputEntityRelationships = inputPatient.getEntityRelationships();
			for ( int i = 0; ((inputEntityRelationships.getEntityRelationship() != null) 
						&& (i < inputEntityRelationships.getEntityRelationship().size() )); i++  ) {
				org.opencds.vmr.v1_0.schema.EntityRelationship inputEntityRelationship = inputEntityRelationships.getEntityRelationship().get(i);
				EntityRelationshipMapper.pullIn(inputEntityRelationship.getSourceId(), inputEntityRelationship.getTargetEntityId(), inputEntityRelationship.getTargetRole(), inputEntityRelationship.getRelationshipTimeInterval(), factLists);
			}
		}
		// ================= End Entity to Entity Relationships =======================//									

		// ================= Clinical Statement Entity In Role Relationships =======================//
		if (inputPatient.getClinicalStatementEntityInRoleRelationships() != null)
		{
			org.opencds.vmr.v1_0.schema.EvaluatedPerson.ClinicalStatementEntityInRoleRelationships inputClinicalStatementEntityInRoleRelationships = inputPatient.getClinicalStatementEntityInRoleRelationships();
			for ( int i = 0; ((inputClinicalStatementEntityInRoleRelationships.getClinicalStatementEntityInRoleRelationship() != null) 
					&& (i < inputClinicalStatementEntityInRoleRelationships.getClinicalStatementEntityInRoleRelationship().size() )); i++  ) {
				org.opencds.vmr.v1_0.schema.EntityRelationship inputEntityRelationship = inputClinicalStatementEntityInRoleRelationships.getClinicalStatementEntityInRoleRelationship().get(i);
//				ClinicalStatementEntityInRoleRelationshipMapper has been deprecated in favor of EntityRelationshipMapper des 2012-02-12
				//				ClinicalStatementEntityInRoleRelationshipMapper.pullIn(inputClinicalStatementEntityInRoleRelationship, subjectPersonId, focalPersonId, factLists);
				EntityRelationshipMapper.pullIn(inputEntityRelationship.getSourceId(), inputEntityRelationship.getTargetEntityId(), inputEntityRelationship.getTargetRole(), inputEntityRelationship.getRelationshipTimeInterval(), factLists);
			}
		}
		// ================= End Clinical Statement Entity Relationships =======================//
		
		// ================= End of Building FactLists from One Evaluated Person =======================//
	}
	

	/**
	 * Populates internal PersonAgeAtEvalTimeList if all input values are non-null and evalTime after birthTime.
	 * 
	 * NOTE:  precision of calculation of age is determined by the last parameter of the call to AbsoluteTimeDifference.
	 * 		The following ages are calculated based on whole dates, ignoring all submitted time values (hours and lower): 
	 * 			year, month, week, day
	 * 
	 * 		The following ages are calculated with the full submitted precision of the dateTime value:
	 * 			hour, minute, second
	 * 
	 * @param birthTime
	 * @param evalTime
	 * @param internalSubjectPersonId
	 */
	private void populateEvaluatedPersonAgeAtEvalTime(Date birthTime, Date evalTime, String internalSubjectPersonId, FactLists factLists)
	{
//	    List<EvaluatedPersonAgeAtEvalTime> internalPersonAgeAtEvalTimeList = factLists.get(EvaluatedPersonAgeAtEvalTime.class);
		if ((birthTime != null) && (evalTime != null) && (internalSubjectPersonId != null) &&  (evalTime.after(birthTime)))
		{
			DateUtility dateUtility = DateUtility.getInstance();
			AbsoluteTimeDifference tdYear = dateUtility.getAbsoluteTimeDifference(evalTime, birthTime, Calendar.YEAR, true, Calendar.HOUR);
			AbsoluteTimeDifference tdMonth = dateUtility.getAbsoluteTimeDifference(evalTime, birthTime, Calendar.MONTH, true, Calendar.HOUR);
			// NOTE: using time difference in days to calculate time difference in weeks as days / 7			
//			AbsoluteTimeDifference tdWeekAsDay = dateUtility.getAbsoluteTimeDifference(evalTime, birthTime, Calendar.DAY_OF_YEAR, true, Calendar.HOUR);
			AbsoluteTimeDifference tdDay = dateUtility.getAbsoluteTimeDifference(evalTime, birthTime, Calendar.DAY_OF_YEAR, true, Calendar.HOUR);
			AbsoluteTimeDifference tdHour = dateUtility.getAbsoluteTimeDifference(evalTime, birthTime, Calendar.HOUR, false, -1);
			AbsoluteTimeDifference tdMinute = dateUtility.getAbsoluteTimeDifference(evalTime, birthTime, Calendar.MINUTE, false, -1);
			AbsoluteTimeDifference tdSecond = dateUtility.getAbsoluteTimeDifference(evalTime, birthTime, Calendar.SECOND, false, -1);
	        
			EvaluatedPersonAgeAtEvalTime personAgeInYears = new EvaluatedPersonAgeAtEvalTime();
			EvaluatedPersonAgeAtEvalTime personAgeInMonths = new EvaluatedPersonAgeAtEvalTime();
			EvaluatedPersonAgeAtEvalTime personAgeInWeeks = new EvaluatedPersonAgeAtEvalTime();
			EvaluatedPersonAgeAtEvalTime personAgeInDays = new EvaluatedPersonAgeAtEvalTime();
			EvaluatedPersonAgeAtEvalTime personAgeInHours = new EvaluatedPersonAgeAtEvalTime();
			EvaluatedPersonAgeAtEvalTime personAgeInMinutes = new EvaluatedPersonAgeAtEvalTime();
			EvaluatedPersonAgeAtEvalTime personAgeInSeconds = new EvaluatedPersonAgeAtEvalTime();
			
			personAgeInYears.setAge(new Integer((int) tdYear.getYearDifference()));
			personAgeInMonths.setAge(new Integer((int) tdMonth.getMonthDifference()));
			personAgeInWeeks.setAge(new Integer((int) (tdDay.getDayDifference() / 7) )); //NOTE: week is expressed as days / 7
			personAgeInDays.setAge(new Integer((int) tdDay.getDayDifference()));
			personAgeInHours.setAge(new Integer((int) tdHour.getHourDifference()));
			personAgeInMinutes.setAge(new Integer((int) tdMinute.getMinuteDifference()));
			personAgeInSeconds.setAge(new Integer((int) tdSecond.getSecondDifference()));
			
			personAgeInYears.setAgeUnit(EvaluatedPersonAgeAtEvalTime.AGE_UNIT_YEAR);
			personAgeInMonths.setAgeUnit(EvaluatedPersonAgeAtEvalTime.AGE_UNIT_MONTH);
			personAgeInWeeks.setAgeUnit(EvaluatedPersonAgeAtEvalTime.AGE_UNIT_WEEK);
			personAgeInDays.setAgeUnit(EvaluatedPersonAgeAtEvalTime.AGE_UNIT_DAY);
			personAgeInHours.setAgeUnit(EvaluatedPersonAgeAtEvalTime.AGE_UNIT_HOUR);
			personAgeInMinutes.setAgeUnit(EvaluatedPersonAgeAtEvalTime.AGE_UNIT_MINUTE);
			personAgeInSeconds.setAgeUnit(EvaluatedPersonAgeAtEvalTime.AGE_UNIT_SECOND);
			
			personAgeInYears.setPersonId(internalSubjectPersonId);
			personAgeInMonths.setPersonId(internalSubjectPersonId);
			personAgeInWeeks.setPersonId(internalSubjectPersonId);
			personAgeInDays.setPersonId(internalSubjectPersonId);
			personAgeInHours.setPersonId(internalSubjectPersonId);
			personAgeInMinutes.setPersonId(internalSubjectPersonId);
			personAgeInSeconds.setPersonId(internalSubjectPersonId);
			
			factLists.put(EvaluatedPersonAgeAtEvalTime.class, personAgeInYears);
			factLists.put(EvaluatedPersonAgeAtEvalTime.class, personAgeInMonths);
			factLists.put(EvaluatedPersonAgeAtEvalTime.class, personAgeInWeeks);
			factLists.put(EvaluatedPersonAgeAtEvalTime.class, personAgeInDays);
			factLists.put(EvaluatedPersonAgeAtEvalTime.class, personAgeInHours);
			factLists.put(EvaluatedPersonAgeAtEvalTime.class, personAgeInMinutes);
			factLists.put(EvaluatedPersonAgeAtEvalTime.class, personAgeInSeconds);
		}
	}
}


			
		


