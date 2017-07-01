/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.syslogd;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.opennms.netmgt.config.SyslogdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

public class GenericParser extends SyslogParser {

    public enum Params {
        facilityCode, messageId, hostname, processName, processId, message, version, timestamp
    }
    
    private static final String COMMA=",";
    
    private static final String EMPTY_SPACE=" ";
    
    private static final Logger LOG = LoggerFactory.getLogger(GenericParser.class);

    protected GenericParser(SyslogdConfig config, String text) {
        super(config, text);
    }

    public SyslogMessage parse(Map<String, String> params)
            throws ParseException {
        SyslogMessage syslogMessage = new SyslogMessage();

        syslogMessage.setParserClass(getClass());
        try {
            int priorityField = parseInteger(getStringTokenValue(params,
                                                                     Params.facilityCode.toString(),
                                                                     "99"));
            syslogMessage.setFacility(SyslogFacility.getFacilityForCode(priorityField));
            syslogMessage.setSeverity(SyslogSeverity.getSeverityForCode(priorityField));
        } catch (final NumberFormatException e) {
            LOG.debug("Unable to parse priority field '{}'", e);
        }

        syslogMessage.setMessageID(getStringTokenValue(params,
                                                       Params.messageId.toString(),
                                                       null));
        syslogMessage.setHostName(getStringTokenValue(params,
                                                      Params.hostname.toString(),
                                                      null));
        syslogMessage.setProcessName(getStringTokenValue(params,
                                                         Params.processName.toString(),
                                                         null));
        syslogMessage.setProcessId(parseInteger(getStringTokenValue(params,
                                                                        Params.processId.toString(),
                                                                       null)));
        syslogMessage.setMessage(getStringTokenValue(params,
                                                     Params.message.toString(),
                                                     null));
        syslogMessage.setVersion(parseInteger(getStringTokenValue(params,
                                                                      Params.version.toString(),
                                                                      "0")));

        syslogMessage.setDate(getDateString(params));

        return syslogMessage;
    }
    
    private int parseInteger(String intValue) {
        try {
            return Integer.parseInt(intValue);
        } catch (NumberFormatException e) {
            return 0;
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * @param params
     * @param field
     * @param isNullValue
     * @return String token value for the fields like message by breaking from 
     * params
     */
    private String getStringTokenValue(Map<String, String> params,
            String field, String isNullValue) {
        String value;
        StringBuilder messageBuilder=new StringBuilder();
        
        if (null == params.get(field))
            return isNullValue;
        if (params.get(field).equals("-"))
            return isNullValue;

        value = params.get(field);
        if (value.startsWith("BOM")) {
            return value.replaceFirst("BOM", "");

        }
		// To Parse PA Firewall Messages and build a proper meaningfull message
		if (("message").equals(field) && params.get("firewallVersion") != null) {
			messageBuilder.append(params.get("firewallVersion"));
			messageBuilder.append(COMMA);
			messageBuilder.append(params.get("recievedate"));
			messageBuilder.append(EMPTY_SPACE);
			messageBuilder.append(params.get("recievetime"));
			messageBuilder.append(COMMA);
			messageBuilder.append(params.get("serialnumber"));
			messageBuilder.append(COMMA);
			messageBuilder.append(params.get("type"));
			messageBuilder.append(COMMA);
			messageBuilder.append(params.get("subtype"));
			messageBuilder.append(COMMA);
			messageBuilder.append(value);

			return messageBuilder.toString();
		}
        return value;
    }

    public Date getDateString(Map<String, String> dateParams)
            throws ParseException {

        return tokenizeRfcDate(getDateFromParams(dateParams));

    }

    private String getDateFromParams(Map<String, String> dateParams) {
        StringBuilder dateBuilder = new StringBuilder();
        String time, day, month, year, timeZone, DATE_SEPERATOR, WHITE_SPACE = " ",rfcTimeStamp;

        time = getTime(dateParams,"timestamp","");
        
        rfcTimeStamp=getStringTokenValue(dateParams, "isotimestamp", null);

        if(rfcTimeStamp!=null)
        return rfcTimeStamp;  
        
		if (dateParams.get("date") == null) {
			year = getStringTokenValue(dateParams, "year",
					String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
			day = getStringTokenValue(
					dateParams,
					"day",
					String.valueOf(Calendar.getInstance().get(
							Calendar.DAY_OF_MONTH)));
			month = getStringTokenValue(dateParams, "month",
					String.valueOf(Calendar.getInstance().get(Calendar.MONTH)));

			if (isInteger(month)) {
				DATE_SEPERATOR = "-";
			} else {
				DATE_SEPERATOR = " ";
			}

			dateBuilder.append(month);
			dateBuilder.append(DATE_SEPERATOR);
			dateBuilder.append(day);
			dateBuilder.append(DATE_SEPERATOR);
			dateBuilder.append(year);
			dateBuilder.append(WHITE_SPACE);
			dateBuilder.append(time);
			return dateBuilder.toString();
		}
        
        dateBuilder.append(dateParams.get("date"));
        dateBuilder.append(time);
        return dateBuilder.toString();
        
        // timeZone = getStringTokenValue(dateParams, "timeZone", "UTC");
        // Intentionally commented since adding timezone gives different time
        // which seems to be correct
        // but not matching the existing output
        // dateBuilder.append(WHITE_SPACE);
        // dateBuilder.append(timeZone);
    }

	private String getTime(Map<String, String> dateParams, String tokenValue,
			String isNullValue) {
		
		if (dateParams.get(tokenValue) != null) {
			return dateParams.get(tokenValue);
		}
		return isNullValue;
	}

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    private Date tokenizeRfcDate(String dateString) throws ParseException {
        Parser parser = new Parser();
        List<DateGroup> groups = parser.parse(dateString);
        for (DateGroup group : groups) {
            return group.getDates().get(0);
        }
        return Calendar.getInstance().getTime();

    }
}
