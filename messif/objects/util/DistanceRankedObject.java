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
package messif.objects.util;

import java.io.Serializable;
import java.util.Iterator;
import messif.objects.DistanceFunction;

/**
 * Ranked object where the rank is based on distances.
 * 
 * @param <T> the encapsulated object class
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class DistanceRankedObject<T> implements Cloneable, Serializable, DistanceRanked, Comparable<DistanceRankedObject<?>> {

    /** Class serial id for serialization */
    private static final long serialVersionUID = 50201L;

    //****************** Attributes ******************//

    /** Encapsulated object */
    private final T object;

    /** Distance that represents the rank of this object */
    private float distance;


    //****************** Constructor ******************//

    /**
     * Creates a new instance of DistanceRankedObject that encapsulates a given object.
     * The specified distance is used as object's rank.
     * @param object the encapsulated object
     * @param distance the distance specifying object's rank
     */
    public DistanceRankedObject(T object, float distance) {
        this.object = object;
        this.distance = distance;
    }

    /**
     * Creates a new instance of DistanceRankedObject that encapsulates a given object
     * and measures the distance using {@code distanceFunction} from the given reference object.
     *
     * @param <O> the type of object used to measure the distance
     * @param object the encapsulated object
     * @param distanceFunction the distance function used for the measuring
     * @param referenceObject the reference object from which the distance is measured
     * @throws NullPointerException if the distance function is <tt>null</tt>
     */
    public <O extends T> DistanceRankedObject(O object, DistanceFunction<? super O> distanceFunction, O referenceObject) throws NullPointerException {
        this(object, distanceFunction.getDistance(referenceObject, object));
    }


    //****************** Attribute access ******************//

    /**
     * Returns the encapsulated object.
     * @return the encapsulated object
     */
    public T getObject() {
        return object;
    }

    /**
     * Returns the measured distance.
     * @return the measured distance
     */
    @Override
    public float getDistance() {
        return distance;
    }


    //****************** Overrides ******************//

    /**
     * Returns <tt>true</tt> if this encapsulated object is equal to the specified 
     * <code>DistanceRankedObject</code> encapsulated object. <tt>Null</tt> values
     * are handled correctly.
     * <p>
     * Note that the equality defined by this method is <b>inconsistent</b> with {@link #compareTo}.
     * </p>
     * @param obj the reference object with which to compare
     * @return <code>true</code> if this object is the same as the {@code obj} argument; <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DistanceRankedObject))
            return false;
        Object objEncap = ((DistanceRankedObject)obj).object;
        if (object == null || objEncap == null)
            return object == objEncap;
        else
            return object.equals(objEncap);
    }

    /**
     * Returns a hash code value for the stored distance.
     * @return a hash code value for the stored distance
     */
    @Override
    public int hashCode() {
        return (object == null)?0:object.hashCode();
    }

    /**
     * Compares this object with the specified object for order.
     * <p>
     * Note that the natural order defined by this method is <b>inconsistent</b> with {@link #equals}.
     * </p>
     * @param   o the object to be compared
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object
     */
    @Override
    public int compareTo(DistanceRankedObject<?> o) {
        return Float.compare(distance, o.distance);
    }

    /**
     * Clone this ranked object and change the distance of the cloned one
     * @param newDistance distance to be set for the cloned object
     * @return the clone with new distance
     */
    public DistanceRankedObject<T> clone(float newDistance) {
        try {
            @SuppressWarnings("unchecked")
            DistanceRankedObject<T> clone = (DistanceRankedObject<T>) super.clone(); // This IS checked (cloning)
            clone.distance = newDistance;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }


    //****************** Iterator support ******************//

    /**
     * Converts an iterator on {@link DistanceRankedObject distance-ranked objects}
     * to an iterator on the objects themselves.
     * Note that the remove method is passed on the original iterator, so
     * the returned iterator supports removal if and only if the given
     * {@code iterator} supports removal.
     *
     * @param <T> the type of objects the distance-ranked object encapsulates
     * @param iterator the original iterator on the distance-ranked objects
     * @return an iterator on the encapsulated objects
     */
    public static <T> Iterator<T> getObjectsIterator(final Iterator<? extends DistanceRankedObject<T>> iterator) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next().getObject();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }


    //****************** Textual representation ******************//

    @Override
    public String toString() {
        return "<" + distance + ": " + object + ">";
    }

}
