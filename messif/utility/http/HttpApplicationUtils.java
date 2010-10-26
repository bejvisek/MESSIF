/*
 *  This file is part of MESSIF library.
 *
 *  MESSIF library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MESSIF library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MESSIF library.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.utility.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import messif.algorithms.Algorithm;
import messif.objects.LocalAbstractObject;
import messif.objects.util.AbstractObjectList;
import messif.operations.AbstractOperation;

/**
 * Utility methods for this package.
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public abstract class HttpApplicationUtils {
    /** Parser regular expression for HTTP request parameters */
    private static final Pattern paramParser = Pattern.compile("([^=]+)=([^&]*)(?:&|$)");

    //****************** Handler factory method ******************//

    /**
     * Factory method for creating {@link HttpHandler handlers}.
     * The first argument is expected to be a class according to which this factory
     * picks the handler.
     *
     * <p>
     * The following classes are supported:
     * <ul>
     * <li>{@link AbstractOperation} - the {@link OperationHandler} is created</li>
     * <li>anything else - the {@link SimpleHandler} is created</li>
     * </ul>
     * </p>
     *
     * @param log the logger to use for logging errors (if <tt>null</tt> is passed, no logging is done)
     * @param algorithm the algorithm on which the new handler operates
     * @param args arguments for the handler
     * @param offset the index into {@code args} where the first argument is
     * @param length the number of arguments to use
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @return a new handler instance
     * @throws IndexOutOfBoundsException if the {@code offset} or {@code length} are not valid for {@code args} array
     * @throws IllegalArgumentException if there was a problem creating the handler
     */
    @SuppressWarnings("unchecked")
    public static HttpHandler createHandler(Logger log, Algorithm algorithm, String args[], int offset, int length, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, IllegalArgumentException {
        try {
            Class<?> handlerClass = Class.forName(args[offset]);
            offset++;
            length--;
            if (AbstractOperation.class.isAssignableFrom(handlerClass))
                return new OperationHandler(log, algorithm, handlerClass, args, offset, length, namedInstances); // This IS checked on previous line
            else
                return new SimpleHandler<Object>(log, true, createProcessor(handlerClass, algorithm, args, offset, length, namedInstances));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown class: " + e.getMessage());
        }
    }

    //****************** Processor factory method ******************//

    /**
     * Factory method for creating {@link HttpApplicationProcessor processors}.
     * The first argument is expected to be a class according to which this factory
     * picks the processor.
     *
     * <p>
     * The following classes are supported:
     * <ul>
     * <li>quoted text - the {@link ParameterProcessor} is created</li>
     * <li>{@link LocalAbstractObject} - the {@link ExtractionProcessor} is created</li>
     * <li>{@link AbstractObjectList} - the {@link ExtractionListProcessor} is created</li>
     * <li>{@link AbstractOperation} - the {@link OperationProcessor} is created</li>
     * <li>text containing parenthesis - the {@link InstantiatorProcessor} is created</li>
     * <li>otherwise - the {@link ValueProcessor} is created</li>
     * </ul>
     * </p>
     *
     * @param <T> the class of the instances created by the processor
     * @param processorClass the class of the instances created by the processor
     * @param algorithm the algorithm on which the new processor operates
     * @param args arguments for the processor
     * @param offset the index into {@code args} where the first argument is
     * @param length the number of arguments to use
     * @param namedInstances collection of named instances that are used when converting string parameters
     * @return a new processor instance
     * @throws IndexOutOfBoundsException if the {@code offset} or {@code length} are not valid for {@code args} array
     * @throws IllegalArgumentException if there was a problem creating the processor
     */
    @SuppressWarnings("unchecked")
    public static <T> HttpApplicationProcessor<T> createProcessor(Class<? extends T> processorClass, Algorithm algorithm, String args[], int offset, int length, Map<String, Object> namedInstances) throws IndexOutOfBoundsException, IllegalArgumentException {
        if (isQuoted(args[offset], '"'))
            return new ParameterProcessor<T>(args[offset].substring(1, args[offset].length() - 1), processorClass, namedInstances);
        else if (LocalAbstractObject.class.isAssignableFrom(processorClass))
            return new ExtractionProcessor(args[offset], processorClass, namedInstances);
        else if (AbstractObjectList.class.isAssignableFrom(processorClass))
            return (HttpApplicationProcessor)new ExtractionListProcessor(args[offset], namedInstances);
        else if (AbstractOperation.class.isAssignableFrom(processorClass))
            return new OperationProcessor(algorithm, processorClass, args, offset, length, namedInstances);
        else
            return new ValueProcessor<T>(args[offset], processorClass, namedInstances);
    }


    //****************** Request helper methods ******************//

    /**
     * Parse query string parameters.
     * The query string has the following format:
     * <code>name1=value1&amp;name2=value2&amp;...</code>
     * Note that the query string can be get by
     * {@link HttpExchange#getRequestURI()}{@link java.net.URI#getQuery() .getQuery()}.
     *
     * @param query the string with URI-like query parameters
     * @return a hash map with parsed query string parameters (key represents the
     *          parameter name and value is the parameter value)
     */
    public static Map<String, String> parseParameters(String query) {
        if (query == null || query.isEmpty())
            return Collections.emptyMap();
        Matcher matcher = paramParser.matcher(query);
        Map<String, String> parameters = new HashMap<String, String>();
        while (matcher.find())
            try {
                parameters.put(matcher.group(1), URLDecoder.decode(matcher.group(2), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                throw new InternalError("Charset utf-8 should be always supported, but there was " + e);
            }
        return parameters;
    }

    /**
     * Returns <tt>true</tt> if the given {@code string} is surrounded by
     * {@code quote}. That is, if the first and last character of the
     * {@code string} is {@code quote}.
     * @param string the string to check
     * @param quote the quote character
     * @return <tt>true</tt> if the given {@code string} is surrounded by {@code quote} and <tt>false</tt> otherwise
     */
    public static boolean isQuoted(String string, char quote) {
        return string != null && string.length() >= 2 && string.charAt(0) == quote && string.charAt(string.length() - 1) == quote;
    }

}
