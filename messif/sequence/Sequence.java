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
package messif.sequence;

/**
 * Objects implementing this class provide generic access to their sequence data.
 * Sequences are usually static arrays of primitive types, however, this interface
 * can be used also with sets or other data types. The position of a particular
 * sequence value is references by non-negative intergers starting from zero.
 *
 * @param <T> the type of the sequence data, usually a static array of a primitive type
 *          or {@link java.util.List}
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public interface Sequence<T> {
    /**
     * Returns the data of this sequence.
     * Note that a copy of the stored data is returned.
     * @return the data of this sequence
     */
    public abstract T getSequenceData();

    /**
     * Returns the number of elements of this sequence.
     * @return the number of elements of this sequence
     */
    public abstract int getSequenceLength();

    /**
     * Returns the class of the data in this sequence.
     * This is usually a static array of a primitive type or {@link java.util.List}.
     * @return the number of elements of this sequence
     */
    public abstract Class<? extends T> getSequenceDataClass();

    /**
     * Returns a subsequence data from this sequence.
     * Note that a copy of the stored data is returned.
     * @param from the initial index of the subsequence element to be copied, inclusive
     * @param to the final index of the subsequence element to be copied, exclusive
     * @return a subsequence data from this sequence
     */
    public abstract T getSubsequenceData(int from, int to);
}