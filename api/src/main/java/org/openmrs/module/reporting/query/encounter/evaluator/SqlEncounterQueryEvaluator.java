/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.reporting.query.encounter.evaluator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.annotation.Handler;
import org.openmrs.module.reporting.IllegalDatabaseAccessException;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.context.EncounterEvaluationContext;
import org.openmrs.module.reporting.query.Query;
import org.openmrs.module.reporting.query.encounter.EncounterQueryResult;
import org.openmrs.module.reporting.query.encounter.definition.EncounterQuery;
import org.openmrs.module.reporting.query.encounter.definition.SqlEncounterQuery;
import org.openmrs.module.reporting.report.util.SqlUtils;
import org.openmrs.util.DatabaseUpdater;
import org.openmrs.util.OpenmrsUtil;

/**
 * The logic that evaluates a {@link SqlEncounterQuery} and produces an {@link Query}
 */
@Handler(supports=SqlEncounterQuery.class)
public class SqlEncounterQueryEvaluator implements EncounterQueryEvaluator {
	
	protected Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * Public constructor
	 */
	public SqlEncounterQueryEvaluator() { }
	
	/**
	 * @see EncounterQueryEvaluator#evaluate(EncounterQuery, EvaluationContext)
	 * @should evaluate a SQL query into an EncounterQuery
	 * @should filter results given a base Encounter Query Result in an EvaluationContext
	 * @should filter results given a base cohort in an EvaluationContext
	 */
	public EncounterQueryResult evaluate(EncounterQuery definition, EvaluationContext context) throws EvaluationException {
		
		context = ObjectUtil.nvl(context, new EvaluationContext());
		SqlEncounterQuery queryDefinition = (SqlEncounterQuery) definition;
		EncounterQueryResult queryResult = new EncounterQueryResult(queryDefinition, context);
		
		// TODO: Probably need to fix this
		StringBuilder sqlQuery = new StringBuilder(queryDefinition.getQuery());
		boolean whereFound = sqlQuery.indexOf("where") != -1;
		if (context.getBaseCohort() != null) {
			if (context.getBaseCohort().isEmpty()) {
				return queryResult;
			}
			whereFound = true;
			sqlQuery.append(whereFound ? " and " : " where ");
			sqlQuery.append("patient_id in (" + context.getBaseCohort().getCommaSeparatedPatientIds() + ")");
		}
		if (context instanceof EncounterEvaluationContext) {
			EncounterEvaluationContext eec = (EncounterEvaluationContext) context;
			if (eec.getBaseEncounters() != null) {
				if (eec.getBaseEncounters().isEmpty()) {
					return queryResult;
				}
				sqlQuery.append(whereFound ? " and " : " where ");
				sqlQuery.append("encounter_id in (" + OpenmrsUtil.join(eec.getBaseEncounters().getMemberIds(), ",") + ")");
			}
		}
		if (context.getLimit() != null) {
			sqlQuery.append(" limit " + context.getLimit());
		}
		
		// TODO: Consolidate this, the cohort, and the dataset implementations and improve them
		Connection connection = null;
		try {
			connection = DatabaseUpdater.getConnection();
			ResultSet resultSet = null;
			
			PreparedStatement statement = SqlUtils.prepareStatement(connection, sqlQuery.toString(), context.getParameterValues());
			boolean result = statement.execute();

			if (!result) {
				throw new EvaluationException("Unable to evaluate sql query");
			}
			resultSet = statement.getResultSet();
			while (resultSet.next()) {
				queryResult.add(resultSet.getInt(1));
			}
		}
		catch (IllegalDatabaseAccessException ie) {
			throw ie;
		}
		catch (Exception e) {
			throw new EvaluationException("Unable to evaluate sql query", e);
		}
		finally {
			try {
				if (connection != null) {
					connection.close();
				}
			}
			catch (Exception e) {
				log.error("Error while closing connection", e);
			}
		}
		return queryResult;
	}
}
