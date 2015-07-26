import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Wrapper class for MapContent in the Billualizer. This class provides methods which allow the
 * main class to update its map view(s) without having to manage the map content directly.
 */
public class Mappa extends MapContent {
    private static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    private static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);
    private static HashMap<String, FeatureId> stateAbbrToId;

    private enum GeomType { POINT, LINE, POLYGON };

    private static final Color LINE_COLOUR = Color.BLUE;
    private static final Color FILL_COLOUR = Color.CYAN;
    private static final Color SELECTED_COLOUR = Color.YELLOW;
    private static final float OPACITY = 1.0f;
    private static final float LINE_WIDTH = 1.0f;
    private static final float POINT_SIZE = 10.0f;

    private static String geometryAttributeName;
    private static GeomType geometryType;

    private static SimpleFeatureSource featureSource;

    public Mappa(File shapeFile) {
        //Set up dictionary:
        try {
            featureSource = FileDataStoreFinder.getDataStore(shapeFile).getFeatureSource();
            SimpleFeatureIterator sfi = featureSource.getFeatures().features();

            stateAbbrToId = new HashMap<String, FeatureId>();

            try {
                while(sfi.hasNext()){
                    SimpleFeature feature = sfi.next();
                    stateAbbrToId.put(feature.getAttribute("STATE_ABBR").toString(), feature.getIdentifier());
                }
            }
            finally {
                sfi.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        setGeometry();

        Layer layer = new FeatureLayer(featureSource, createDefaultStyle());
        this.addLayer(layer);

    }
    
    public void setColoredStates(List<String> states) {
        //Fetch subset of dictionary corresponding to the matching states
        Set<FeatureId> ids = new HashSet<FeatureId>();
        for(String state : states) 
            ids.add(stateAbbrToId.get(state));
        
         List<Layer> layers = this.layers();    
         for(Layer layer : layers) 
             this.removeLayer(layer);
         
//         this.addLayer(new FeatureLayer(featureSource, createDefaultStyle()));
         this.addLayer(new FeatureLayer(featureSource, createSelectedStyle(ids)));
    }
    
    /**
     * Create a Style where features with given IDs are painted
     * yellow, while others are painted with the default colors.
     */
    private static Style createSelectedStyle(Set<FeatureId> IDs) {
        Rule selectedRule = createRule(SELECTED_COLOUR, SELECTED_COLOUR);
        selectedRule.setFilter(filterFactory.id(IDs));

        Rule otherRule = createRule(LINE_COLOUR, FILL_COLOUR);
        otherRule.setElseFilter(true);

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(selectedRule);
        fts.rules().add(otherRule);

        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);
        
        return style;
    }

    /**
     * Figure out if a valid SLD file is available.
     */
    public static File toSLDFile(File file)  {
        String path = file.getAbsolutePath();
        String base = path.substring(0,path.length()-4);
        String newPath = base + ".sld";
        File sld = new File( newPath );
        if( sld.exists() ){
            return sld;
        }
        newPath = base + ".SLD";
        sld = new File( newPath );
        if( sld.exists() ){
            return sld;
        }
        return null;
    }

    /**
     * Create a Style object from a definition in a SLD document
     */
    @SuppressWarnings("unused")
    private static Style createFromSLD(File sld) {
        try {
            SLDParser stylereader = new SLDParser(styleFactory, sld.toURI().toURL());
            Style[] style = stylereader.readXML();
            return style[0];

        } catch (Exception e) {
            e.printStackTrace();
            //            ExceptionMonitor.show(null, e, "Problem creating style");
        }
        return null;
    }

    /**
     * Create a default Style for feature display
     */
    private static Style createDefaultStyle() {
        Rule rule = createRule(LINE_COLOUR, FILL_COLOUR);

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(rule);

        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    /**
     * Helper for createXXXStyle methods. Creates a new Rule containing
     * a Symbolizer tailored to the geometry type of the features that
     * we are displaying.
     */
    private static Rule createRule(Color outlineColor, Color fillColor) {
        Symbolizer symbolizer = null;
        Fill fill = null;
        Stroke stroke = styleFactory.createStroke(filterFactory.literal(outlineColor), filterFactory.literal(LINE_WIDTH));

        switch (geometryType) {
        case POLYGON:
            fill = styleFactory.createFill(filterFactory.literal(fillColor), filterFactory.literal(OPACITY));
            symbolizer = styleFactory.createPolygonSymbolizer(stroke, fill, geometryAttributeName);
            break;

        case LINE:
            symbolizer = styleFactory.createLineSymbolizer(stroke, geometryAttributeName);
            break;

        case POINT:
            fill = styleFactory.createFill(filterFactory.literal(fillColor), filterFactory.literal(OPACITY));

            Mark mark = styleFactory.getCircleMark();
            mark.setFill(fill);
            mark.setStroke(stroke);

            Graphic graphic = styleFactory.createDefaultGraphic();
            graphic.graphicalSymbols().clear();
            graphic.graphicalSymbols().add(mark);
            graphic.setSize(filterFactory.literal(POINT_SIZE));

            symbolizer = styleFactory.createPointSymbolizer(graphic, geometryAttributeName);
        }

        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(symbolizer);
        return rule;
    }

    /**
     * Retrieve information about the feature geometry
     */
    private static void setGeometry() {
        GeometryDescriptor geomDesc = featureSource.getSchema().getGeometryDescriptor();
        geometryAttributeName = geomDesc.getLocalName();

        Class<?> clazz = geomDesc.getType().getBinding();

        if (Polygon.class.isAssignableFrom(clazz) ||
                MultiPolygon.class.isAssignableFrom(clazz)) {
            geometryType = GeomType.POLYGON;

        } else if (LineString.class.isAssignableFrom(clazz) ||
                MultiLineString.class.isAssignableFrom(clazz)) {

            geometryType = GeomType.LINE;

        } else {
            geometryType = GeomType.POINT;
        }

    }
}
