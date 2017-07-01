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

import java.util.regex.Pattern;

import org.opennms.netmgt.syslogd.BufferParser.BufferParserFactory;

public abstract class GrokParserFactory {
	
	private static final Pattern m_datePatternRFC=Pattern.compile("\\d{4}[-\\/]\\d{2}[-\\/]\\d{2}T(\\d{2})(:{1})(\\d{2})(:{1})(\\d{2}).(\\d|-|:)*Z*", Pattern.MULTILINE);
	private static final Pattern m_datePattern=Pattern.compile("\\d{4}[-\\/]\\d{2}[-\\/]\\d{2}", Pattern.MULTILINE);
	private static final Pattern m_timePattern=Pattern.compile("\\d{2}:\\d{2}:\\d{2}", Pattern.MULTILINE);

	private static enum GrokState {
		TEXT,
		ESCAPE_PATTERN,
		START_PATTERN,
		PATTERN,
		SEMANTIC,
		END_PATTERN
	}

	private static enum GrokPattern {
		STRING,
		INTEGER,
		MONTH,TIMESTAMP_ISO8601,DATE,TIMESTAMP,FIREWALLTIMPESTAMP
	}
	
	private static <E extends Exception> void throwCustomInterruptedException(
			Exception exception) throws E {
		throw (E) exception;
	}

	public static BufferParserFactory parseGrok(String grok) {
		GrokState state = GrokState.TEXT;
		BufferParserFactory factory = new BufferParserFactory();

		StringBuffer pattern = new StringBuffer();
		StringBuffer semantic = new StringBuffer();

		for (char c : grok.toCharArray()) {
			switch(state) {
			case TEXT:
				switch(c) {
				case '%':
					state = GrokState.START_PATTERN;
					continue;
				case '[':
				        state = GrokState.START_PATTERN;
				        continue;
				case '\\':
					state = GrokState.ESCAPE_PATTERN;
					continue;
				case ' ':
					factory = factory.whitespace();
					continue;
				default:
					factory = factory.character(c);
					continue;
				}
			case ESCAPE_PATTERN:
				switch(c) {
				default:
					factory = factory.character(c);
					state = GrokState.TEXT;
					continue;
				}
			case START_PATTERN:
			    switch(c) {
			    case '{':
			        state = GrokState.PATTERN;
			        continue;
			    case '%':
			        state = GrokState.START_PATTERN;
			        continue;
			    case '[':
			        state = GrokState.START_PATTERN;
			        continue;
				default:
					throw new IllegalStateException("Illegal character to start pattern");
				}
			case PATTERN:
				switch(c) {
				case ':':
					state = GrokState.SEMANTIC;
					continue;
				default:
					pattern.append(c);
					continue;
				}
			case SEMANTIC:
			    switch(c) {
			    case '}':
			        state = GrokState.END_PATTERN;
			        continue;
			    case ']':
			        state = GrokState.END_PATTERN;
			        continue;
				default:
					semantic.append(c);
					continue;
				}
			case END_PATTERN:
				final String patternString = pattern.toString();
				final String semanticString = semantic.toString();
				GrokPattern patternType = GrokPattern.valueOf(patternString);
				switch(c) {
				case ' ':
					switch(patternType) {
					case TIMESTAMP:
						factory.stringUntilWhitespace((s, v) -> {
							try {
								if (matchDateTimePattern(v)) {
									s.builder.addParam(semanticString, v);
								}
							} catch (Exception e) {
								throwCustomInterruptedException(e);
							}
						});
						factory.whitespace();
						break;
					case DATE:
						factory.stringUntilWhitespace((s, v) -> {
							try {
								if (matchDateTimePattern(v)) {
									s.builder.addParam(semanticString, v);
								}
							} catch (Exception e) {
								throwCustomInterruptedException(e);
							}
						});
						factory.whitespace();
						break;
					case STRING:
						factory.stringUntilWhitespace((s,v) -> {
							s.builder.addParam(semanticString, v);
						});
						factory.whitespace();
						break;
					case FIREWALLTIMPESTAMP:
						factory.stringUntilWhitespace((s,v) -> {
							if (v.matches("[0-9:]+[\\;\\:\\-\\.\\,\\?]$")) {
								v = v.substring(0, v.length() - 1);
							}
							s.builder.addParam(semanticString, v);
						});
						factory.whitespace();
						break;
					case INTEGER:
						factory.intUntilWhitespace((s,v) -> {
							s.builder.addParam(semanticString, v);
						});
						factory.whitespace();
						break;
					case TIMESTAMP_ISO8601:
						factory.stringUntilWhitespace((s, v) -> {
							try {
								if (matchDateTimePattern(v)) {
									s.builder.addParam(semanticString, v);
								}
							} catch (Exception e) {
								throwCustomInterruptedException(e);
							}
						});
						factory.whitespace();
						break;
					case MONTH:
						factory.month((s,v) -> {
							s.builder.addParam(semanticString, v);
						});
						factory.whitespace();
						break;
					}
					break;
				default:
					switch(patternType) {
					case STRING:
						factory.stringUntil(String.valueOf(c), (s,v) -> {
							s.builder.addParam(semanticString, v);
						});
						factory.character(c);
						break;
					case INTEGER:
						factory.integer((s,v) -> {
							s.builder.addParam(semanticString, v);
						});
						factory.character(c);
						break;
					case MONTH:
						factory.month((s,v) -> {
							s.builder.addParam(semanticString, v);
						});
						factory.character(c);
						break;
					}
				}
				pattern = new StringBuffer();
				semantic = new StringBuffer();
				state = GrokState.TEXT;
				continue;
			}
		}

		// If we are in the process of ending a pattern, then wrap it up with bow
		if (state == GrokState.END_PATTERN) {
			final String patternString = pattern.toString();
			final String semanticString = semantic.toString();
			GrokPattern patternType = GrokPattern.valueOf(patternString);

			switch(patternType) {
			case STRING:
				factory.terminal().string((s,v) -> {
					s.builder.addParam(semanticString, v);
				});
				break;
			case INTEGER:
				factory.terminal().integer((s,v) -> {
					s.builder.addParam(semanticString, v);
				});
				break;
			case MONTH:
				factory.terminal().month((s,v) -> {
					s.builder.addParam(semanticString, v);
				});
				break;
			}
		}
		return factory;
	}

	/*
	 * Method to pass and match date with formats "yyyy-MM-dd'T'HH:mm:ss'Z'" 
	 */
	private static boolean matchDateTimePattern(String date)
			throws InterruptedException {
		if (m_datePatternRFC.matcher(date).matches()) {
			return true;
		}
		else if (m_datePattern.matcher(date).matches()) {
			return true;
		}
		else if (m_timePattern.matcher(date).matches()) {
			return true;
		}
		throw new InterruptedException();
	}
}
