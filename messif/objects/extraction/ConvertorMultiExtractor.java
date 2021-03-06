/*
 * This file is part of MESSIF library.
 *
 * MESSIF library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MESSIF library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package messif.objects.extraction;

import java.io.IOException;
import java.util.Iterator;
import messif.objects.LocalAbstractObject;
import messif.utility.Convertor;
import messif.utility.ConvertorIterator;

/**
 * Multi-extractor that applies a {@link Convertor} to all the objects extracted by the encapsulated {@link MultiExtractor}.
 *
 * @param <F> the source class of the conversion, i.e. the class of objects returned by the encapsulated extractor
 * @param <T> the destination class of the conversion, i.e. the class of objects returned by this extractor
 *
 * @author Michal Batko, Masaryk University, Brno, Czech Republic, batko@fi.muni.cz
 * @author Vlastislav Dohnal, Masaryk University, Brno, Czech Republic, dohnal@fi.muni.cz
 * @author David Novak, Masaryk University, Brno, Czech Republic, david.novak@fi.muni.cz
 */
public class ConvertorMultiExtractor<F extends LocalAbstractObject, T extends LocalAbstractObject> implements MultiExtractor<T> {
    /** Encapsulated multi-extractor that provides the objects to convert */
    private final MultiExtractor<? extends F> extractor;
    /** Convertor to apply to iterated items */
    private final Convertor<F, T> convertor;
    
    /**
     * Creates a new instance of multi-extractor that applies a {@link Convertor} to all the objects extracted by the encapsulated {@link MultiExtractor}.
     * @param extractor the encapsulated multi-extractor that provides the objects to convert
     * @param convertor the convertor to apply to iterated items
     * @throws NullPointerException if the given extractor or convertor is <tt>null</tt>
     */
    public ConvertorMultiExtractor(MultiExtractor<? extends F> extractor, Convertor<F, T> convertor) throws NullPointerException {
        if (extractor == null || convertor == null)
            throw new NullPointerException();
        this.extractor = extractor;
        this.convertor = convertor;
    }

    @Override
    public Iterator<T> extract(ExtractorDataSource dataSource) throws ExtractorException, IOException {
        try {
            return new ConvertorIterator<F, T>(extractor.extract(dataSource), convertor);
        } catch (ExtractorException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtractorException("Cannot convert object: " + e, e);
        }
    }

    @Override
    public Class<? extends T> getExtractedClass() {
        return convertor.getDestinationClass();
    }

}
