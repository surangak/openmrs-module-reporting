package org.openmrs.module.reporting.data.converter;

import junit.framework.Assert;

import org.junit.Test;
import org.openmrs.PersonName;

public class PersonNameConverterTest {
	
	/**
	 * @see PersonNameConverter#convert(Object)
	 * @verifies convert a Person name into a String using a format expression
	 */
	@Test
	public void convert_shouldConvertAPersonNameIntoAStringUsingAFormatExpression() throws Exception {
		PersonName personName = new PersonName();
		personName.setGivenName("John");
		personName.setMiddleName("T");
		personName.setFamilyName("Smith");
		Object result = (new ObjectFormatter("{familyName}, {givenName}")).convert(personName);
		Assert.assertEquals("Smith, John", result.toString());
	}
}