/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

import static org.opennms.core.utils.InetAddressUtils.str;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.SyslogdConfig;
import org.opennms.netmgt.config.syslogd.HideMatch;
import org.opennms.netmgt.config.syslogd.HideMessage;
import org.opennms.netmgt.config.syslogd.HostaddrMatch;
import org.opennms.netmgt.config.syslogd.HostnameMatch;
import org.opennms.netmgt.config.syslogd.ParameterAssignment;
import org.opennms.netmgt.config.syslogd.ProcessMatch;
import org.opennms.netmgt.config.syslogd.UeiList;
import org.opennms.netmgt.config.syslogd.UeiMatch;
import org.opennms.netmgt.dao.api.AbstractInterfaceToNodeCache;
import org.opennms.netmgt.dao.api.InterfaceToNodeCache;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * This routine does the majority of Syslogd's work.
 * Improvements are most likely to be made.
 * 
 * TODO: This class is sloooow. It needs to be sped up significantly
 * to handle increased syslog volume.
 *
 * @author <a href="mailto:joed@opennms.org">Johan Edstrom</a>
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 * @author <a href="mailto:mhuot@opennms.org">Mike Huot</a>
 * @author <a href="mailto:jeffg@opennms.org">Jeff Gehlbach</a>
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 */
public class ConvertToEvent {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertToEvent.class);

    /** Constant <code>HIDDEN_MESSAGE="The message logged has been removed due"{trunked}</code> */
    protected static final String HIDDEN_MESSAGE = "The message logged has been removed due to configuration of Syslogd; it may contain sensitive data.";

    private final Event m_event;

    private static final LoadingCache<String,Pattern> CACHED_PATTERNS = CacheBuilder.newBuilder().build(
        new CacheLoader<String,Pattern>() {
            public Pattern load(String expression) {
                try {
                    return Pattern.compile(expression, Pattern.MULTILINE);
                } catch(final PatternSyntaxException e) {
                    LOG.warn("Failed to compile regex pattern '{}'", expression, e);
                    return null;
                }
            }
        }
    );

    /**
     * Constructs a new event encapsulation instance based upon the
     * information passed to the method. The passed datagram data is decoded
     * into a string using the <tt>US-ASCII</tt> character encoding.
     *
     * @param packet The datagram received from the remote agent.
     * @throws java.io.UnsupportedEncodingException
     *          Thrown if the data buffer cannot be decoded using the
     *          US-ASCII encoding.
     * @throws MessageDiscardedException 
     * @throws ParseException 
     */
    public ConvertToEvent(
        final String systemId,
        final String location,
        final DatagramPacket packet,
        final SyslogdConfig config,
        final Map<String,String> params
    ) throws UnsupportedEncodingException, MessageDiscardedException, ParseException {
        this(systemId, location, packet.getAddress(), packet.getPort(), new String(packet.getData(), 0, packet.getLength(), "US-ASCII"), config,params);
    }

    /**
     * Constructs a new event encapsulation instance based upon the
     * information passed to the method. The passed byte array is decoded into
     * a string using the <tt>US-ASCII</tt> character encoding.
     *
     * @param addr The remote agent's address.
     * @param port The remote agent's port
     * @param data The XML data in US-ASCII encoding.
     * @param list 
     * @param len  The length of the XML data in the buffer.
     * @throws java.io.UnsupportedEncodingException
     *          Thrown if the data buffer cannot be decoded using the
     *          US-ASCII encoding.
     * @throws MessageDiscardedException 
     * @throws ParseException 
     */
    public ConvertToEvent(
        final String systemId,
        final String location,
        final InetAddress addr,
        final int port,
        final String data,
        final SyslogdConfig config, Map<String, String> params
    ) throws UnsupportedEncodingException, MessageDiscardedException, ParseException {

        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        final UeiList ueiList = config.getUeiList();
        final HideMessage hideMessage = config.getHideMessages();
        final String discardUei = config.getDiscardUei();

        String syslogString = data;
        // Trim trailing nulls from the string
        while (syslogString.endsWith("\0")) {
            syslogString = syslogString.substring(0, syslogString.length() - 1);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Converting to event: {}", this);
        }

        GenericParser parser = new GenericParser(config, syslogString);
        if (!parser.find()) {
            throw new MessageDiscardedException("Message does not match the grok : "+syslogString);
        }
        SyslogMessage message;
        try {
            message = parser.parse(params);
        } catch (final Exception ex) {
            LOG.debug("Unable to parse '{}'", syslogString, ex);
            throw new MessageDiscardedException(ex);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("got syslog message {}", message);
        }
        if (message == null) {
            throw new MessageDiscardedException(String.format("Unable to parse '%s'", syslogString));
        }
        // Build a basic event out of the syslog message
        final String priorityTxt = message.getSeverity().toString();
        final String facilityTxt = message.getFacility().toString();

        EventBuilder bldr = new EventBuilder("uei.opennms.org/syslogd/" + facilityTxt + "/" + priorityTxt, "syslogd");

        bldr.setDistPoller(systemId);

        bldr.setTime(message.getDate());

        // Set event host
        bldr.setHost(InetAddressUtils.getLocalHostName());

        //Getting the host address from syslog message,
        //If syslog message address is not null set the hostadress
        //Else adding the source address obtained from headers of syslog packets 
        final InetAddress hostAddress = (message.getHostAddress()!=null) ? message.getHostAddress():addr;
        
        //Updated the code from foundation-2017
        if (hostAddress != null) {
            // Set nodeId
            InterfaceToNodeCache cache = AbstractInterfaceToNodeCache.getInstance();
            if (cache != null) {
                int nodeId = cache.getNodeId(location, hostAddress);
                if (nodeId > 0) {
                    bldr.setNodeid(nodeId);
                }
            }

            bldr.setInterface(hostAddress);
        }
        
        bldr.setLogDest("logndisplay");


        // We will also here find out if, the host needs to
        // be replaced, the message matched to a UEI, and
        // last if we need to actually hide the message.
        // this being potentially helpful in avoiding showing
        // operator a password or other data that should be
        // confidential.

        /*
        * We matched on a regexp for host/message pair.
        * This can be a forwarded message as in BSD Style
        * or syslog-ng.
        * We assume that the host is given to us
        * as an IP/Hostname and that the resolver
        * on the ONMS host actually can resolve the
        * node to match against nodeId.
         */

        Pattern msgPat = null;
        Matcher msgMat = null;

        // Time to verify UEI matching.

        final String fullText = message.getFullText();
        final String matchedText = message.getMatchedMessage();

        final List<UeiMatch> ueiMatch = ueiList == null? null : ueiList.getUeiMatchCollection();
        if (ueiMatch == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No ueiList configured.");
            }
        } else {
            for (final UeiMatch uei : ueiMatch) {
                final boolean otherStuffMatches = containsIgnoreCase(uei.getFacilityCollection(), facilityTxt) &&
                                                  containsIgnoreCase(uei.getSeverityCollection(), priorityTxt) &&
                                                  matchProcess(uei.getProcessMatch(), message.getProcessName()) && 
                                                  matchHostname(uei.getHostnameMatch(), message.getHostName()) &&
                                                  matchHostAddr(uei.getHostaddrMatch(), str(hostAddress));

                // Single boolean check is added instead of performing multiple
                // boolean check for both if and else if which causes a extra time
                if (otherStuffMatches) {
                    if (uei.getMatch().getType().equals("substr")) {
                    	if (matchSubstring(message.getMessage(), uei, bldr, config.getDiscardUei())) {
                            break;
                        }
                    } else if ((uei.getMatch().getType().startsWith("regex"))) {
                    	 if (matchRegex(message.getMessage(), uei, bldr, config.getDiscardUei())) {
                            break;
                        }
                    }
                }
            }
        }

        // Time to verify if we need to hide the message
        boolean doHide = false;
        final List<HideMatch> hideMatch = hideMessage == null? null : hideMessage.getHideMatchCollection();
        if (hideMatch == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No hideMessage configured.");
            }
        } else {
            for (final HideMatch hide : hideMatch) {
                if (hide.getMatch().getType().equals("substr")) {
                    if (fullText.contains(hide.getMatch().getExpression())) {
                        // We should hide the message based on this match
                        doHide = true;
                    }
                } else if (hide.getMatch().getType().equals("regex")) {
                    try {
                        msgPat = Pattern.compile(hide.getMatch().getExpression(), Pattern.MULTILINE);
                        msgMat = msgPat.matcher(fullText);
                    } catch (PatternSyntaxException pse) {
                        LOG.warn("Failed to compile regex pattern '{}'", hide.getMatch().getExpression(), pse);
                        msgMat = null;
                    }
                    if ((msgMat != null) && (msgMat.find())) {
                        // We should hide the message based on this match
                        doHide = true;
                    }
                }
                if (doHide) {
                    LOG.debug("Hiding syslog message from Event - May contain sensitive data");
                    message.setMessage(HIDDEN_MESSAGE);
                    // We want to stop here, no point in checking further hideMatches
                    break;
                }
            }
        }

        // Using parms provides configurability.
        bldr.setLogMessage(message.getMessage());

        bldr.addParam("rawmessage", data);
        bldr.addParam("syslogmessage", message.getMessage());
        bldr.addParam("severity", "" + priorityTxt);
        bldr.addParam("timestamp", message.getDate().toString());
        
        if (message.getProcessName() != null) {
            bldr.addParam("process", message.getProcessName());
        }

        bldr.addParam("service", "" + facilityTxt);

        if (message.getProcessId() != null) {
            bldr.addParam("processid", message.getProcessId().toString());
        }

        m_event = bldr.getEvent();
    }

    private static boolean matchFind(final String expression, final String input, final String context) {
        final Pattern pat = getPattern(expression);
        if (pat == null) {
            LOG.debug("Unable to get pattern for expression '{}' in {} context", expression, context);
            return false;
        }
        final Matcher mat = pat.matcher(input);
        if (mat != null && mat.find()) return true;
        return false;
    }
    
    private static boolean matchHostAddr(final HostaddrMatch hostaddrMatch, final String hostAddress) {
        if (hostaddrMatch == null) return true;
        if (hostAddress == null) return false;
        
        final String expression = hostaddrMatch.getExpression();
        
        if (matchFind(expression, hostAddress, "hostaddr-match")) {
            LOG.trace("Successful regex hostaddr-match for input '{}' against expression '{}'", hostAddress, expression);
            return true;
        }
        return false;
    }
    
    private static boolean matchHostname(final HostnameMatch hostnameMatch, final String hostName) {
        if (hostnameMatch == null) return true;
        if (hostName == null) return false;
        
        final String expression = hostnameMatch.getExpression();
        
        if (matchFind(expression, hostName, "hostname-match")) {
            LOG.trace("Successful regex hostname-match for input '{}' against expression '{}'", hostName, expression);
            return true;
        }
        return false;
    }

    private static boolean matchProcess(final ProcessMatch processMatch, final String processName) {
        if (processMatch == null) return true;
        if (processName == null) return false;

        final String expression = processMatch.getExpression();

        if (matchFind(expression, processName, "process-match")) {
            LOG.trace("Successful regex process-match for input '{}' against expression '{}'", processName, expression);
            return true;
        }
        return false;
    }
    
    private static boolean containsIgnoreCase(List<String> collection, String match) {
         if (collection.size() == 0) return true;
         for (String string : collection) {
             if (string.equalsIgnoreCase(match)) return true;
         }
         return false;
    }

    private static Pattern getPattern(final String expression) {
        return CACHED_PATTERNS.getUnchecked(expression);
    }

    /**
     * Checks the message for substring matches to a {@link UeiMatch}. If the message
     * matches, then the UEI is updated (or the event is discarded if the discard
     * UEI is used). Parameter assignments are NOT performed for substring matches.
     * 
     * @param message
     * @param uei
     * @param bldr
     * @param discardUei
     * @return
     * @throws MessageDiscardedException
     */
    private static boolean matchSubstring(String message, final UeiMatch uei, final EventBuilder bldr, final String discardUei) throws MessageDiscardedException {
        final boolean traceEnabled = LOG.isTraceEnabled();
        if (message.contains(uei.getMatch().getExpression())) {
            if (discardUei.equals(uei.getUei())) {
                if (traceEnabled) LOG.trace("Specified UEI '{}' is same as discard-uei, discarding this message.", uei.getUei());
                throw new MessageDiscardedException();
            } else {
                // Update the UEI to the new value
                if (traceEnabled) LOG.trace("Changed the UEI of a Syslogd event, based on substring match, to : {}", uei.getUei());
                bldr.setUei(uei.getUei());
                return true;
            }
        } else {
            if (traceEnabled) LOG.trace("No substring match for text of a Syslogd event to : {}", uei.getMatch().getExpression());
            return false;
        }
    }

    /**
     * Checks the message for matches to a {@link UeiMatch}. If the message
     * matches, then the UEI is updated (or the event is discarded if the discard
     * UEI is used) and parameters are added to the event.
     * 
     * @param message
     * @param uei
     * @param bldr
     * @param discardUei
     * @return
     * @throws MessageDiscardedException
     */
    private static boolean matchRegex(final String message, final UeiMatch uei, final EventBuilder bldr, final String discardUei) throws MessageDiscardedException {
        final boolean traceEnabled = LOG.isTraceEnabled();
        final String expression = uei.getMatch().getExpression();
        final Pattern msgPat = getPattern(expression);
        if (msgPat == null) {
            LOG.debug("Unable to create pattern for expression '{}'", expression);
            return false;
        } 

        final Matcher msgMat = msgPat.matcher(message);

        // If the message matches the regex
        if ((msgMat != null) && (msgMat.find())) {
            // Discard the message if the UEI is set to the discard UEI
            if (discardUei.equals(uei.getUei())) {
                if (traceEnabled) LOG.trace("Specified UEI '{}' is same as discard-uei, discarding this message.", uei.getUei());
                throw new MessageDiscardedException();
            } else {
                // Update the UEI to the new value
                if (traceEnabled) LOG.trace("Changed the UEI of a Syslogd event, based on regex match, to : {}", uei.getUei());
                bldr.setUei(uei.getUei());
            }

            if (msgMat.groupCount() > 0) {
                // Perform default parameter mapping
                if (uei.getMatch().isDefaultParameterMapping()) {
                    if (traceEnabled) LOG.trace("Doing default parameter mappings for this regex match.");
                    for (int groupNum = 1; groupNum <= msgMat.groupCount(); groupNum++) {
                        if (traceEnabled) LOG.trace("Added parm 'group{}' with value '{}' to Syslogd event based on regex match group", groupNum, msgMat.group(groupNum));
                        bldr.addParam("group"+groupNum, msgMat.group(groupNum));
                    }
                }

                // If there are specific parameter mappings as well, perform those mappings
                if (uei.getParameterAssignmentCount() > 0) {
                    if (traceEnabled) LOG.trace("Doing user-specified parameter assignments for this regex match.");
                    for (ParameterAssignment assignment : uei.getParameterAssignmentCollection()) {
                        String parmName = assignment.getParameterName();
                        String parmValue = msgMat.group(assignment.getMatchingGroup());
                        parmValue = parmValue == null ? "" : parmValue;
                        bldr.addParam(parmName, parmValue);
                        if (traceEnabled) {
                            LOG.trace("Added parm '{}' with value '{}' to Syslogd event based on user-specified parameter assignment", parmName, parmValue);
                        }
                    }
                }
            }

            return true;
        }

        if (traceEnabled) LOG.trace("Message portion '{}' did not regex-match pattern '{}'", message, expression);
        return false;
    }

    /**
     * <p>getEvent</p>
     *
     * @return a {@link org.opennms.netmgt.xml.event.Event} object.
     */
    public Event getEvent() {
        return m_event;
    }

    /**
     * <p>toString</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("Event", m_event)
            .toString();
    }
}
