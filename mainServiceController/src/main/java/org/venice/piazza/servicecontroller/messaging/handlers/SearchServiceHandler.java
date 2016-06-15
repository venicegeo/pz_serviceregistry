package org.venice.piazza.servicecontroller.messaging.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.venice.piazza.servicecontroller.data.mongodb.accessors.MongoAccessor;
import org.venice.piazza.servicecontroller.elasticsearch.accessors.ElasticSearchAccessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.job.PiazzaJobType;
import model.job.type.SearchServiceJob;
import model.service.SearchCriteria;
import model.service.metadata.Service;

import util.PiazzaLogger;


/**
 * Handler for handling search requests.  Searches the databse for 
 * registered services using the name and/or ID provide
 * @author mlynum
 * @version 1.0
 *
 */
@Component
public class SearchServiceHandler implements PiazzaJobHandler {
	
	@Autowired
	private MongoAccessor accessor;

	@Autowired
	private PiazzaLogger coreLogger;

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchServiceHandler.class);

	/**
	 * Handler for the RegisterServiceJob that was submitted. Stores the metadata in MongoDB (non-Javadoc)
	 * 
	 * @see org.venice.piazza.servicecontroller.messaging.handlers.Handler#handle(model.job.PiazzaJobType)
	 */
	public ResponseEntity<String> handle(PiazzaJobType jobRequest) {
		LOGGER.debug("Search a service");
		SearchServiceJob job = (SearchServiceJob) jobRequest;

		// Get the criteria to use for the search
		SearchCriteria criteria = job.data;
		LOGGER.info("search " + " " + criteria.field + "->" + criteria.pattern);
		coreLogger.log("search " + " " + criteria.field + "->" + criteria.pattern, coreLogger.INFO);

		ResponseEntity<String> response = handle(criteria);
		return new ResponseEntity<String>(response.getBody(), response.getStatusCode());
	}

	/**
	 * 
	 * @param criteria
	 *            to search. field and regex expression
	 * @return a String of ResourceMetadata items that match the search
	 */
	public ResponseEntity<String> handle(SearchCriteria criteria) {
		ResponseEntity<String> responseEntity = null;
		String result = null;
		coreLogger.log("About to search using criteria" + criteria, PiazzaLogger.INFO);

		List<Service> results = accessor.search(criteria);
		if (results.size() <= 0) {
			coreLogger.log(
					"No results were returned searching for field " + criteria.getField() + " and search criteria " + criteria.getPattern(),
					PiazzaLogger.INFO);
			LOGGER.info("No results were returned searching for field " + criteria.getField() + " and search criteria "
					+ criteria.getPattern());
			responseEntity = new ResponseEntity<String>(result, HttpStatus.NO_CONTENT);
		} else {
			ObjectMapper mapper = new ObjectMapper();
			try {
				result = mapper.writeValueAsString(results);
				responseEntity = new ResponseEntity<String>(result, HttpStatus.OK);
			} catch (JsonProcessingException jpe) {
				// This should never happen, but still have to catch it
				coreLogger.log("There was a problem generating the Json response", PiazzaLogger.ERROR);
				LOGGER.error("There was a problem generating the Json response");
			}
		}

		return responseEntity;
	}
}
