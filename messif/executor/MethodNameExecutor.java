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
package messif.executor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import messif.utility.reflection.Instantiators;


/**
 *  This class allows to execute a methods on a specified object.
 *  First, methods must be registered. This is done through constructor, where 
 *  also an instance of the target object must be provided.
 *  The second parameter specify the required argument types
 *  that the method must have.
 *
 *  Then the method execute method can be called. This method invokes the method
 *  of the instance (provided in constructor), which is appropriate for the provided
 *  arguments.
 *
 *  Method backgroundExecute can be called to invoke the method in
 *  a new thread. A returned thread can be used for wait calls
 *  to test, whether the execution has finished and also to retrieve the data.
 *
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class MethodNameExecutor extends MethodExecutor {

    //****************** Attributes ******************//

    /** The table of found operation methods */
    private final Map<String, Method> registeredMethods;

    /** Index of an argument from methodPrototype, which will hold the method name */
    private final int nameArgIndex;


    //****************** Constructors ******************//

    /**
     * Create new instance of MethodNameExecutor and search for operation methods.
     * 
     * @param executionObject an instance of the object to execute the operations on
     * @param methodPrototype list of argument types for the registered methods
     * @param nameArgIndex the index of an argument from methodPrototype, which will hold the method name
     * @throws IllegalArgumentException if either the method prototype or named argument index is invalid or the executionObject is <tt>null</tt>
     */
    public MethodNameExecutor(Object executionObject, int nameArgIndex, Class<?>... methodPrototype) throws IllegalArgumentException {
        super(executionObject);
        
        // The method prototype must have at least one argument
        if (methodPrototype == null || methodPrototype.length == 0)
            throw new IllegalArgumentException("Method prototype must be specified.");

        // Validate the nameArgIndex argument
        if ((nameArgIndex < 0) && (nameArgIndex >= methodPrototype.length && methodPrototype[methodPrototype.length - 1].isArray()))
            throw new IllegalArgumentException("Index of method name argument is out of bounds");
        this.nameArgIndex = nameArgIndex;

        // Get the class where the methods are searched
        Class<?> executionClass;
        boolean staticOnly;
        if (executionObject instanceof Class) {
            executionClass = (Class)executionObject;
            staticOnly = true;
        } else {
            executionClass = executionObject.getClass();
            staticOnly = false;
        }

        // Search all methods of the execution object and register the matching ones
        this.registeredMethods = createRegisteredMethods(executionClass, staticOnly, methodPrototype);
    }

    /**
     * Create new instance of MethodNameExecutor and search for operation methods.
     * The {@link #getFirstStringClass(java.lang.Class[]) first string class}
     * in the given {@code methodPrototype} is expected to hold the method name.
     *
     * @param executionObject an instance of the object to execute the operations on
     * @param methodPrototype list of argument types for the registered methods
     * @throws IllegalArgumentException if either the method prototype or named argument index is invalid or the executionObject is <tt>null</tt>
     */
    public MethodNameExecutor(Object executionObject, Class<?>... methodPrototype) throws IllegalArgumentException {
        this(executionObject, getFirstStringClass(methodPrototype), methodPrototype);
    }


    //****************** Metho searching ******************//

    /**
     * Search the {@code classToSearch} for methods that are matching the given
     * {@code methodPrototype}. The search is recursive starting from the
     * top-level (excluding {@link Object}) class to {@code classToSearch}.
     * 
     * @param classToSearch the class to search
     * @param staticOnly the flag if only static methods are added
     * @param methodPrototype the prototype of the methods to search for
     * @return the map of methods using the respective method's name as a key
     */
    protected final Map<String, Method> createRegisteredMethods(Class<?> classToSearch, boolean staticOnly, Class<?>[] methodPrototype) {
        // Finish recursion
        if (classToSearch == null || classToSearch == Object.class)
            return new HashMap<String, Method>();

        // Recurse first
        Map<String, Method> ret = createRegisteredMethods(classToSearch.getSuperclass(), staticOnly, methodPrototype);

        // Fill methods for the class
        for (Method method : classToSearch.getDeclaredMethods()) {
            // Skip non static members on execution classes
            if (staticOnly && !Modifier.isStatic(method.getModifiers()))
                continue;

            // Check prototype and add method to the registry
            Class<?>[] methodArgTypes = method.getParameterTypes();
            if (Instantiators.isPrototypeMatching(methodArgTypes, methodPrototype))
                ret.put(method.getName(), method);
        }

        return ret;
    }

    /**
     * Search array for first String class. String[] class is allowed as the last item.
     * @param array the array to search
     * @return the index of the first String class in the array or -1 if it is not found
     */
    public static int getFirstStringClass(Class<?>[] array) {
        for (int i = 0; i < array.length; i++)
            if (String.class.equals(array[i]))
                return i;
        if (array.length > 0 && String[].class.equals(array[array.length - 1]))
            return array.length - 1;
        
        return -1;
    }

    /**
     * Get string from array at specified position.
     * This method handles the encapsulated string arrays of the varargs.
     * @param array the array of arguments
     * @param index the index of the argument to get
     * @return the string argument at the specified index of the array
     * @throws ClassCastException if the array item at the specified position is not a string
     * @throws IndexOutOfBoundsException if the specified position is invalid
     */
    public static String getStringObject(Object[] array, int index) throws ClassCastException, IndexOutOfBoundsException {
        // Index is last or beyond end and the last argument is array
        if (index >= array.length - 1 && array[array.length - 1].getClass().isArray())
            return ((String[])array[array.length - 1])[index - array.length + 1];
        
        // Index is classical
        return (String)array[index];
    }


    //****************** Implementation of necessary methods ******************//

    @Override
    protected Method getMethod(Object[] arguments) throws NoSuchMethodException {
        Method method = null;
        try {
            method = registeredMethods.get(getStringObject(arguments, nameArgIndex));
        } catch (ClassCastException ignore) {
        } catch (IndexOutOfBoundsException ignore) {
        }
        if (method == null)
            throw new NoSuchMethodException("Method for specified arguments not found");

        return method;
    }

    /**
     * Returns the list of method names that this executor supports and
     * that match the specified regular expression.
     * @param regexp the regular expression for matching method names
     * @return the list of method names
     */
    public List<String> getDifferentiatingNames(String regexp) {
        List<String> rtv = new ArrayList<String>();
        for (String key : registeredMethods.keySet())
            if (key.matches(regexp))
                rtv.add(key);
        return rtv;
    }

    @Override
    protected Collection<Method> getRegisteredMethods() {
        return Collections.unmodifiableCollection(registeredMethods.values());
    }

}