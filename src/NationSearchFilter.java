import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.AbstractFilter;
import org.geotools.filter.CompareFilterImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;

/**
 * @author Descartes
 *
 */
public class NationSearchFilter extends AbstractFilter {
    private ArrayList<String> matches;
    private Expression e1;
    private Expression e2;
    
    /**
     * Creates a new NationSearchFilter based on the provided matches
     * @param e1 the geometry's STATE_ABBR field, parsed as an expression
     * @param searchMatches a list of the state names that matched the search
     */
    public NationSearchFilter(Expression e1, ArrayList<String> searchMatches) {
        this.e1 = e1;
        matches = searchMatches;
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
        this.e2 = ff.property("STATE_ABBR");
    }

    /* (non-Javadoc)
     * @see org.opengis.filter.Filter#evaluate(java.lang.Object)
     */
    @Override
    public boolean evaluate(Object object) {
        System.out.println("Eval");
        if(object instanceof String) 
            return matches.contains((String) object);
        return false;
    }

    /* (non-Javadoc)
     * @see org.opengis.filter.Filter#accept(org.opengis.filter.FilterVisitor, java.lang.Object)
     */
    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        System.out.println(Arrays.toString((String[]) extraData));
        return visitor.visit(Filter.INCLUDE, extraData);
    }
}
