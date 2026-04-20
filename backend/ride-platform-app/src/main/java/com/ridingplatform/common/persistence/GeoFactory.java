package com.ridingplatform.common.persistence;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeoFactory {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoFactory() {
    }

    public static Point point(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}
