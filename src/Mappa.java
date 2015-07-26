import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.swing.styling.JSimpleStyleDialog;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
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
 * @author Descartes
 */
public class Mappa extends MapContent {
    static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);
    static HashMap<String, FeatureId> stateAbbrToId;

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

    private MapContent map;
    
    public Mappa(File shapeFile) {
        try {
            featureSource = FileDataStoreFinder.getDataStore(shapeFile).getFeatureSource();
            SimpleFeatureIterator sfi = featureSource/*.getFeatures(new Query("STATE_ABBR", new NationSearchFilter(null, states)))*/.getFeatures().features();

            stateAbbrToId = new HashMap<String, FeatureId>();

            try {
                while(sfi.hasNext()){
                    SimpleFeature feature = sfi.next();
                    stateAbbrToId.put(feature.getAttribute("STATE_ABBR").toString(), feature.getIdentifier());
                }
            }
            finally {
                sfi.close();
                System.out.println("Association: " + stateAbbrToId);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        setGeometry();

        // Create a map content and add our shapefile to it
//        map = new MapContent();

        Style style = createDefaultStyle(); //createStyle2(featureSource);
        //        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        this.addLayer(layer);

    }
    
    public void setColoredStates(List<String> states) {
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
     * Create a Style to display the features. If an SLD file is in the same
     * directory as the shapefile then we will create the Style by processing
     * this. Otherwise we display a JSimpleStyleDialog to prompt the user for
     * preferences.
     */
    private static Style createStyle(File file, FeatureSource featureSource) {
        File sld = toSLDFile(file);
        if (sld != null) {
            return createFromSLD(sld);
        }

        SimpleFeatureType schema = (SimpleFeatureType)featureSource.getSchema();
        return JSimpleStyleDialog.showDialog(null, schema);
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
     * Here is a programmatic alternative to using JSimpleStyleDialog to
     * get a Style. This methods works out what sort of feature geometry
     * we have in the shapefile and then delegates to an appropriate style
     * creating method.
     */
    private static Style createStyle2(FeatureSource featureSource) {
        SimpleFeatureType schema = (SimpleFeatureType)featureSource.getSchema();
        Class geomType = schema.getGeometryDescriptor().getType().getBinding();

        if (Polygon.class.isAssignableFrom(geomType)
                || MultiPolygon.class.isAssignableFrom(geomType)) {

            return createPolygonStyle();

        } else if (LineString.class.isAssignableFrom(geomType)
                || MultiLineString.class.isAssignableFrom(geomType)) {
            return createLineStyle();

        } else {
            return createPointStyle();
        }
    }

    /**
     * Create a Style to draw polygon features with a thin blue outline and
     * a cyan fill
     */
    private static Style createPolygonStyle() {
        // create a partially opaque outline stroke
        Stroke stroke = styleFactory.createStroke(
                filterFactory.literal(Color.BLUE),
                filterFactory.literal(1),
                filterFactory.literal(0.5));

        // create a partial opaque fill
        Fill fill = styleFactory.createFill(
                filterFactory.literal(Color.CYAN),
                filterFactory.literal(0.5));

        PolygonSymbolizer sym = styleFactory.createPolygonSymbolizer(stroke, fill, null);
        Rule rule = createRule(Color.DARK_GRAY, Color.GREEN);
        rule.symbolizers().add(sym);

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);

        return style;
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
     * Create a Style to draw line features as thin blue lines
     */
    private static Style createLineStyle() {
        Stroke stroke = styleFactory.createStroke(
                filterFactory.literal(Color.BLUE),
                filterFactory.literal(1));

        /*
         * Setting the geometryPropertyName arg to null signals that we want to
         * draw the default geomettry of features
         */
        LineSymbolizer sym = styleFactory.createLineSymbolizer(stroke, null);

        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(sym);
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);

        return style;
    }

    /**
     * Create a Style to draw point features as circles with blue outlines
     * and cyan fill
     */
    private static Style createPointStyle() {
        Graphic gr = styleFactory.createDefaultGraphic();

        Mark mark = styleFactory.getCircleMark();

        mark.setStroke(styleFactory.createStroke(
                filterFactory.literal(Color.BLUE), filterFactory.literal(1)));

        mark.setFill(styleFactory.createFill(filterFactory.literal(Color.CYAN)));

        gr.graphicalSymbols().clear();
        gr.graphicalSymbols().add(mark);
        gr.setSize(filterFactory.literal(5));

        /*
         * Setting the geometryPropertyName arg to null signals that we want to
         * draw the default geomettry of features
         */
        PointSymbolizer sym = styleFactory.createPointSymbolizer(gr, null);

        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(sym);
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
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
