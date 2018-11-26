package edu.dbgroup.gui.components;

import com.google.common.base.Strings;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import edu.dbgroup.logic.models.KansasMapModel;
import edu.dbgroup.logic.ServiceProvider;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.*;
import org.geotools.styling.Stroke;
import org.jfree.fx.FXGraphics2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

/**
 * Certain aspects of this class (especially the coloring and picking) are adapted from a official geo-tools tutorial:
 * http://docs.geotools.org/latest/userguide/tutorial/map/style.html
 *
 * See resource: /edu/dbgroup/gui/components/kansas_map.fxml
 */
public class KansasMap extends VBox { // todo make disposable for map

    private final static Logger logger = LoggerFactory.getLogger(KansasMap.class);

    private final KansasMapModel kansasMapModel = ServiceProvider.INSTANCE.MODELS.getKansasMapModel();

    // START OF VARS FROM TUTORIAL
    private StyleFactory sf = CommonFactoryFinder.getStyleFactory();
    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    private enum GeomType {
        POINT,
        LINE,
        POLYGON
    }

    private static final Color LINE_COLOUR = Color.BLUE;
    private static final Color FILL_COLOUR = Color.CYAN;
    private static final Color SELECTED_COLOUR = Color.YELLOW;
    private static final float OPACITY = 1.0f;
    private static final float LINE_WIDTH = 1.0f;
    private static final float POINT_SIZE = 10.0f;
    // END

    @FXML private Canvas canvas;

    /**  see {@link #initialize()} */
    @NonNull @Initialized private MapContent map;

    /** spatial rendering abstraction over {@link FXGraphics2D}, see {@link #initialize()} */
    @NonNull @Initialized private GTRenderer renderer;

    /** graphics2d targeting javafx wrapper of swing map container see {@link #initialize()} */
    @NonNull @Initialized private FXGraphics2D fxg2d;

    /** container of map layers, see {@link #initialize()} */
    @NonNull @Initialized private SimpleFeatureSource featureSource;

    /** viewport transform for full-view, unmodified map view, see {@link #initialize()}, {@link #init()} */
    @NonNull @Initialized private ReferencedEnvelope zoomedOutBounds;

    /** holds last clicked county name, see {@link #setGeometry()} */
    @Nullable @UnknownInitialization private String selectedGeomName = null;

    /** holds last selected county geometry enum, see {@link GeomType}, {@link #setGeometry()} */
    @Nullable @UnknownInitialization private GeomType selectedGeomType = null;

    // reusable coord objects
    private final Point2D localCoords = new Point2D.Double(0, 0);
    //    private final Point2D worldCoords = new Point2D.Double(0, 0);
    private final Rectangle clickRectBox = new Rectangle(0, 0, 0, 0);


    @FXML
    private void initialize() {
        fxg2d = new FXGraphics2D(canvas.getGraphicsContext2D());

        try {
            final FileDataStore dataStore = FileDataStoreFinder.getDataStore(
                this.getClass().getResource("/edu/dbgroup/gui/data/counties/tl_2018_us_county.shp")
            );

            featureSource = dataStore.getFeatureSource();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        map = new MapContent();
        renderer = new StreamingRenderer();
        init();
        attachEventHandlers();

        draw();
    }


    private void init() {
        map.addLayer(new FeatureLayer(featureSource, SLD.createSimpleStyle(featureSource.getSchema())));
        map.getViewport().setScreenArea(getCanvasDim());
        renderer.setMapContent(map);
        zoomedOutBounds = map.getViewport().getBounds();
    }

    public void draw() {
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        renderer.paint(fxg2d, getCanvasDim(), map.getViewport().getBounds());
    }

    public void draw(SimpleFeature highlightedFeature) {
        Style style;
        if (highlightedFeature != null) {
            style = createSelectedStyle(highlightedFeature.getIdentifier());
        } else {
            style = createDefaultStyle();
        }
        ((FeatureLayer) map.layers().get(0)).setStyle(style);
        draw();
    }

    private Rectangle getCanvasDim() {
        return new Rectangle(new Rectangle((int) canvas.getWidth(), (int) canvas.getHeight()));
    }

    @Nullable private SimpleFeature lastSelectedFeature = null;

    private void attachEventHandlers() {

    }

    private Style createSelectedStyle(FeatureId IDs) {

        Rule selectedRule = createRule(SELECTED_COLOUR, SELECTED_COLOUR);
        selectedRule.setFilter(ff.id(IDs));

        Rule otherRule = createRule(LINE_COLOUR, FILL_COLOUR);
        otherRule.setElseFilter(true);

        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        fts.rules().add(selectedRule);
        fts.rules().add(otherRule);

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    private Style createDefaultStyle() {
        Rule rule = createRule(LINE_COLOUR, FILL_COLOUR);

        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        fts.rules().add(rule);

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    @Nullable
    private SimpleFeature clickIntersection(MouseEvent event) throws Exception {

        setGeometry();
        localCoords.setLocation(event.getX(), event.getY());
//        map.getViewport().getScreenToWorld().transform(localCoords, worldCoords);

        // get county at cords
        clickRectBox.setBounds((int)localCoords.getX() - 2, (int)localCoords.getY() - 2, 2, 2);
        final CoordinateReferenceSystem worldCRS =
                renderer.getMapContent().getCoordinateReferenceSystem();

        final Rectangle2D worldRect = map.getViewport().getScreenToWorld().createTransformedShape(clickRectBox).getBounds2D();
        final ReferencedEnvelope worldBBox = new ReferencedEnvelope(worldRect, worldCRS);

        final SimpleFeatureType schema = featureSource.getSchema();
        final CoordinateReferenceSystem targetCRS = schema.getCoordinateReferenceSystem();
        final String geometryAttributeName = schema.getGeometryDescriptor().getLocalName();

        final ReferencedEnvelope bbox;
        try {
            bbox = worldBBox.transform(targetCRS, true, 10);
        } catch (TransformException | FactoryException e) {
            e.printStackTrace();
            return null;
        }

        final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        // Option 1 BBOX
        final Filter filter = ff.bbox(ff.property(geometryAttributeName), bbox);

        // Option 2 Intersects
//        Filter filter = ff.intersects(ff.property(geometryAttributeName), ff.literal(bbox));

        final SimpleFeatureIterator features = featureSource.getFeatures(filter).features();
        try {
            if (features.hasNext()) {
                final SimpleFeature next = features.next();
                return next;
            } else {
                return null;
            }
        } finally {
            features.close();
        }

    }

    private Rule createRule(Color outlineColor, Color fillColor) {
        Symbolizer symbolizer = null;
        Fill fill = null;
        Stroke stroke = sf.createStroke(ff.literal(outlineColor), ff.literal(LINE_WIDTH));

        switch (selectedGeomType) {
            case POLYGON:
                fill = sf.createFill(ff.literal(fillColor), ff.literal(OPACITY));
                symbolizer = sf.createPolygonSymbolizer(stroke, fill, selectedGeomName);
                break;

            case LINE:
                symbolizer = sf.createLineSymbolizer(stroke, selectedGeomName);
                break;

            case POINT:
                fill = sf.createFill(ff.literal(fillColor), ff.literal(OPACITY));

                final Mark mark = sf.getCircleMark();
                mark.setFill(fill);
                mark.setStroke(stroke);

                final Graphic graphic = sf.createDefaultGraphic();
                graphic.graphicalSymbols().clear();
                graphic.graphicalSymbols().add(mark);
                graphic.setSize(ff.literal(POINT_SIZE));

                symbolizer = sf.createPointSymbolizer(graphic, selectedGeomName);
        }

        final Rule rule = sf.createRule();
        rule.symbolizers().add(symbolizer);
        return rule;
    }

    final Timer mouseMovementComputationLimiter = new Timer(0, new ActionListener() {

        private final float frequency = 0.2f;
        private float lastCatalogedComputationTime = 0.0f;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (System.currentTimeMillis() - lastCatalogedComputationTime > frequency) {

            }
        }
    });

    @FXML
    private void canvasMouseMoved(MouseEvent event) {
        try {
            if (!kansasMapModel.getIsZoomed()) {
                final SimpleFeature simpleFeature = clickIntersection(event);
                if (simpleFeature != null) {
                    if (lastSelectedFeature == null || simpleFeature != lastSelectedFeature ) {
                        lastSelectedFeature = simpleFeature;
                        draw(simpleFeature);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void canvasMouseClick(MouseEvent event) {
        try {

            final SimpleFeature simpleFeature = clickIntersection(event);
            final boolean clickedOnFeature = simpleFeature != null;

            map.getViewport().setMatchingAspectRatio(true);

            if (kansasMapModel.getIsZoomed()) { // zoom out
                map.getViewport().setBounds(zoomedOutBounds);
                kansasMapModel.isZoomedProperty().setValue(false);
            } else if (clickedOnFeature) { // zoom in
                final Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
                final ReferencedEnvelope refEnv = new ReferencedEnvelope(
                        geom.getEnvelopeInternal(),
                        renderer.getMapContent().getCoordinateReferenceSystem()
                );

                refEnv.expandBy(0.3);
                map.getViewport().setBounds(refEnv);
                kansasMapModel.isZoomedProperty().setValue(true);
            }

            draw(lastSelectedFeature);

            if (clickedOnFeature) {
                kansasMapModel.setSelectedCounty(Strings.nullToEmpty(simpleFeature.getID()));
                logger.info("selected " + simpleFeature.getID());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /** Retrieve information about the feature geometry */
    private void setGeometry() {
        final GeometryDescriptor geomDesc = featureSource.getSchema().getGeometryDescriptor();
        selectedGeomName = geomDesc.getLocalName();

        final Class<?> clazz = geomDesc.getType().getBinding();

        if (Polygon.class.isAssignableFrom(clazz) || MultiPolygon.class.isAssignableFrom(clazz)) {
            selectedGeomType = GeomType.POLYGON;
        } else if (LineString.class.isAssignableFrom(clazz) || MultiLineString.class.isAssignableFrom(clazz)) {
            selectedGeomType = GeomType.LINE;
        } else {
            selectedGeomType = GeomType.POINT;
        }
    }

}
