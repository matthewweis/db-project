package edu.dbgroup.gui.components;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import edu.dbgroup.logic.database.County;
import edu.dbgroup.logic.database.GovernmentData;
import edu.dbgroup.logic.database.Precipitation;
import edu.dbgroup.logic.database.Temperature;
import edu.dbgroup.logic.models.KansasMapModel;
import edu.dbgroup.logic.ServiceProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.rxjavafx.sources.Change;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.davidmoten.rx.jdbc.tuple.Tuple2;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
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
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

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
    private static final Color SELECTED_COLOR = Color.YELLOW;
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
    @Nullable @UnknownInitialization private String selectedGeomName;

    /** holds last selected county geometry enum, see {@link GeomType}, {@link #setGeometry()} */
    @Nullable @UnknownInitialization private GeomType selectedGeomType = GeomType.POLYGON;

    // reusable coord objects
    private final Point2D localCoords = new Point2D.Double(0, 0);
    //    private final Point2D worldCoords = new Point2D.Double(0, 0);
    private final Rectangle clickRectBox = new Rectangle(0, 0, 0, 0);

    private Map<Integer, Integer> colorMap = new HashMap<>();

    @FXML
    private void initialize() {
        System.out.print("here");

        for (int i=0; i < colors.size(); i+=2) {
            colorMap.put(Integer.parseInt(colors.get(i)), Integer.parseInt(colors.get(i+1), 16));
            colorMap.put(Integer.parseInt(colors.get(i)) + 1, Integer.parseInt(colors.get(i+1), 16));
        }

//        Flowable.fromIterable(colors).window(2).map(flow -> flow.take(2).toList().blockingGet())
//                    .map(strings -> Lists.newArrayList())
//                    .blockingForEach(list -> colorMap.put(Integer.parseInt((String)list.get(0)), Integer.parseInt((String)list.get(1), 16)));

        fxg2d = new FXGraphics2D(canvas.getGraphicsContext2D());

        try {
            final FileDataStore dataStore = FileDataStoreFinder.getDataStore(
                this.getClass().getResource("/edu/dbgroup/gui/data/counties/tl_2018_us_county.shp")
            );

            featureSource = dataStore.getFeatureSource();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        selectedGeomName = featureSource.getSchema().getGeometryDescriptor().getLocalName();

        map = new MapContent();
        renderer = new StreamingRenderer();
        init();
        initPropertyBindings();

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
//        final Style style;
//        if (highlightedFeature != null) {
//            style = createSelectedStyle(highlightedFeature.getIdentifier());
//        } else {
//            style = createDefaultStyle();
//        }
//        ((FeatureLayer) map.layers().get(0)).setStyle(style);
        draw();
    }

    private Rectangle getCanvasDim() {
        return new Rectangle(new Rectangle((int) canvas.getWidth(), (int) canvas.getHeight()));
    }

    @Nullable private SimpleFeature lastSelectedFeature = null;

    private void initPropertyBindings() {
        final GeometryDescriptor geomDesc = featureSource.getSchema().getGeometryDescriptor();
//        final FeatureTypeStyle fts = sf.createFeatureTypeStyle();

        JavaFxObservable.changesOf(ServiceProvider.INSTANCE.MODELS.getHomeViewModel().selectedDateProperty())
                .toFlowable(BackpressureStrategy.BUFFER)
                .observeOn(Schedulers.io())
                .map(Change::getNewVal)
                .flatMap(ldate -> ServiceProvider.INSTANCE.getCountiesAsFlowable().map(county -> Tuple2.create(ldate, county)))
                .map(tuple -> Tuple2.create(tuple._2(), ServiceProvider.INSTANCE.QUERIES.averageAll(tuple._2(), Date.valueOf(tuple._1()), Date.valueOf(tuple._1().plusDays(1)))))
                .buffer(105)
                .map(list -> Flowable.fromIterable(list)
                .reduceWith(() -> sf.createFeatureTypeStyle(), (fts, t2) -> {


                    final String countyName = t2._1();
                    final double rainFall = identityOrNullEquals0(t2._2()._1());
                    final double snowFall = identityOrNullEquals0(t2._2()._2());
                    final int avgTemp = identityOrNullEquals0(t2._2()._3());
                    final int avgHiTemp = identityOrNullEquals0(t2._2()._4());
                    final int avgLoTemp = identityOrNullEquals0(t2._2()._5());
                    final int countyID = ServiceProvider.INSTANCE.getCountyIdByName(countyName);

//                    final Color color = new Color((float) Math.random(), (float) Math.random(), (float) Math.random());
                    final int diff = Math.abs(avgHiTemp - avgLoTemp); // abs protects from incorrect data
                    final int sum = avgHiTemp + avgLoTemp; // abs protects from incorrect data
                    final float nd = (diff / (sum / 2.0f));
                    final int mid = avgHiTemp; //avgLoTemp + (avgHiTemp - avgLoTemp);

                    final Color mainColor;// = new Color((float) Math.random(), (float) Math.random(), (float) Math.random());
                    if (nd > 0) {
//                        final float r = Color.blue.getRed() * nd + Color.red.getRed() * (1.0f - nd);
//                        final float g = Color.blue.getGreen() * nd + Color.red.getGreen() * (1.0f - nd);
//                        final float b = Color.blue.getBlue() * nd + Color.red.getBlue() * (1.0f - nd);

//                        mainColor = new Color(clamp0to1(nd), clamp0to1(0.0f/255.0f), clamp0to1(1.0f - nd));
                        mainColor = getColorFromTemp(mid);
                    } else {
//                        final float r = Color.blue.getRed() * nd + Color.red.getRed() * (1 - nd);
//                        final float g = Color.blue.getGreen() * nd + Color.red.getGreen() * (1 - nd);
//                        final float b = Color.blue.getBlue() * nd + Color.red.getBlue() * (1 - nd);

//                        mainColor = new Color(clamp0to1(nd), clamp0to1(0.0f/255.0f), clamp0to1(1.0f - nd));
//                        mainColor = new Color(clamp0to1(r), clamp0to1(g), clamp0to1(b));
//                        mainColor = new Color((1.0f - nd), nd, 60.0f/255.0f);
                        mainColor = getColorFromTemp(mid);
                    }
                    final FeatureId featureId = ff.featureId(getFeatureIdOfCounty(countyID));
                    final Color borderColor = Color.WHITE;
//                    if (lastSelectedFeature.getIdentifier().equals(featureId)) {
//                        borderColor = Color.YELLOW;
//                    } else {
//                        borderColor = Color.WHITE;
//                    }

                    final Rule selectedRule = createRule(borderColor, mainColor, GeomType.POLYGON, geomDesc.getLocalName());
                    logger.debug("mapped: " + countyID + " --> " + ff.id(featureId));
                    selectedRule.setFilter(ff.id(featureId));

                    fts.rules().add(selectedRule);
                    return fts;
                })
                .observeOn(JavaFxScheduler.platform())
                .subscribeOn(JavaFxScheduler.platform())
                .subscribe(fts -> {
                    System.out.println("here!");

                    // rule for anything not caught in previous rules (shouldn't occur?)
                    final Rule otherRule = createRule(LINE_COLOUR, FILL_COLOUR, GeomType.POLYGON, geomDesc.getLocalName());
                    otherRule.setElseFilter(true);
                    fts.rules().add(otherRule); // should only occur once per date change!

                    // add to style
                    final Style style = sf.createStyle();
                    style.featureTypeStyles().add(fts);
                    setStyleAndRedraw(style);
        }, Throwable::printStackTrace)).subscribe();

    }

    private int identityOrNullEquals0(@Nullable Integer intValue) {
        if (intValue != null) {
            return intValue;
        } else {
            return 0;
        }
    }

    private double identityOrNullEquals0(@Nullable Double doubleValue) {
        if (doubleValue != null) {
            return doubleValue;
        } else {
            return 0.0;
        }
    }

    private int getCountyGeocodeOfFeature(SimpleFeature feature) {
        return Integer.parseInt(feature.getAttribute(2).toString());
    }

    private void setStyleAndRedraw(Style style) {
        logger.info("Map's style has been reset");
        ((FeatureLayer) map.layers().get(0)).setStyle(style);
        draw();
    }

    private String getFeatureIdOfCounty(int countyID) throws IOException {
        final String idPrefix = "tl_2018_us_county.";
        return String.format("%s%d", idPrefix, (countyID / 2) + (countyID % 2));
    }

//    private Style createSelectedStyle(FeatureId IDs) {
//
//        Rule selectedRule = createRule(SELECTED_COLOR, SELECTED_COLOR);
//        selectedRule.setFilter(ff.id(IDs));
//
//        Rule otherRule = createRule(LINE_COLOUR, FILL_COLOUR);
//        otherRule.setElseFilter(true);
//
//        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
//        fts.rules().add(selectedRule);
//        fts.rules().add(otherRule);
//
//        Style style = sf.createStyle();
//        style.featureTypeStyles().add(fts);
//        return style;
//    }
//
//    private Style createDefaultStyle() {
//        Rule rule = createRule(LINE_COLOUR, FILL_COLOUR);
//
//        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
//        fts.rules().add(rule);
//
//        Style style = sf.createStyle();
//        style.featureTypeStyles().add(fts);
//        return style;
//    }

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
        return createRule(outlineColor, fillColor, selectedGeomType, selectedGeomName);
    }

    private Rule createRule(Color outlineColor, Color fillColor, GeomType geomType, String geomName) {
        Symbolizer symbolizer = null;
        Fill fill = null;
        Stroke stroke = sf.createStroke(ff.literal(outlineColor), ff.literal(LINE_WIDTH));

        switch (geomType) {
            case POLYGON:
                fill = sf.createFill(ff.literal(fillColor), ff.literal(OPACITY));
                symbolizer = sf.createPolygonSymbolizer(stroke, fill, geomName);
                break;

            case LINE:
                symbolizer = sf.createLineSymbolizer(stroke, geomName);
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

                symbolizer = sf.createPointSymbolizer(graphic, geomName);
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
                if (simpleFeature != null) {
                    if (simpleFeature.getID() != null) {
                        kansasMapModel.setSelectedCounty(Strings.nullToEmpty(simpleFeature.getAttribute(5).toString()));
                    }
                }

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

    private float clamp0to1(float f) {
        return (float) Math.max(0.0, Math.min(1.0, f));
    }

    private Color getColorFromTemp(int temp) {
        if (temp < 12) {
            return getColorFromTemp(12);
        } else if (temp > 190) {
            return getColorFromTemp(190);
        } else {
            // 178 lines
            int realColor = (temp / 2) * 2;
            int ret = colorMap.getOrDefault(realColor, 0);
            return new Color(ret);
        }
    }


    private static java.util.List<String> colors = Lists.newArrayList(
            "190", "FF0EF0",
            "188", "FF0DF0",
            "186", "FF0CF0",
            "184", "FF0BF0",
            "182", "FF0AF0",
            "180", "FF09F0",
            "178", "FF08F0",
            "176", "FF07F0",
            "174", "FF06F0",
            "172", "FF05F0",
            "170", "FF04F0",
            "168", "FF03F0",
            "166", "FF02F0",
            "164", "FF01F0",
            "162", "FF00F0",
            "160", "FF00E0",
            "158", "FF00D0",
            "156", "FF00C0",
            "154", "FF00B0",
            "152", "FF00A0",
            "150", "FF0090",
            "148", "FF0080",
            "146", "FF0070",
            "144", "FF0060",
            "142", "FF0050",
            "140", "FF0040",
            "138", "FF0030",
            "136", "FF0020",
            "134", "FF0010",
            "132", "FF0000",
            "130", "FF0a00",
            "128", "FF1400",
            "126", "FF1e00",
            "124", "FF2800",
            "122", "FF3200",
            "120", "FF3c00",
            "118", "FF4600",
            "116", "FF5000",
            "114", "FF5a00",
            "112", "FF6400",
            "110", "FF6e00",
            "108", "FF7800",
            "106", "FF8200",
            "104", "FF8c00",
            "102", "FF9600",
            "100", "FFa000",
            "98", "FFaa00",
            "96", "FFb400",
            "94", "FFbe00",
            "92", "FFc800",
            "90", "FFd200",
            "88", "FFdc00",
            "86", "FFe600",
            "84", "FFf000",
            "82", "FFfa00",
            "80", "fdff00",
            "78", "d7ff00",
            "76", "b0ff00",
            "74", "8aff00",
            "72", "65ff00",
            "70", "3eff00",
            "68", "17ff00",
            "66", "00ff10",
            "64", "00ff36",
            "62", "00ff5c",
            "60", "00ff83",
            "58", "00ffa8",
            "56", "00ffd0",
            "54", "00fff4",
            "52", "00e4ff",
            "50", "00d4ff",
            "48", "00c4ff",
            "46", "00b4ff",
            "44", "00a4ff",
            "42", "0094ff",
            "40", "0084ff",
            "38", "0074ff",
            "36", "0064ff",
            "34", "0054ff",
            "32", "0044ff",
            "30", "0032ff",
            "28", "0022ff",
            "26", "0012ff",
            "24", "0002ff",
            "22", "0000ff",
            "20", "0100ff",
            "18", "0200ff",
            "16", "0300ff",
            "14", "0400ff",
            "12", "0500ff"
    );

}
