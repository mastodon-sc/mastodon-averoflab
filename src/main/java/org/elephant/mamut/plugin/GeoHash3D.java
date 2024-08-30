package org.elephant.mamut.plugin;

/* 
 * 3D Geohash modified from https://github.com/kungfoo/geohash-java/blob/master/src/main/java/ch/hsr/geohash/GeoHash.java
 * Paul Brebner, Instaclustr.com
 * 5 June 2019
 * Just a demonstration of how z can be used to encode a 3D geohash, for use with Anomalia Machina blog series.
 */

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class GeoHash3D implements Comparable< GeoHash3D >, Serializable
{
    private static final int MAX_BIT_PRECISION = 64;

    private static final int MAX_CHARACTER_PRECISION = 12;

    public static final long FIRST_BIT_FLAGGED = 0x8000000000000000l;

    private static final char[] base32 = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    private final static Map< Character, Integer > decodeMap = new HashMap<>();

    static
    {
        int sz = base32.length;
        for ( int i = 0; i < sz; i++ )
        {
            decodeMap.put( base32[ i ], i );
        }
    }

    protected long bits = 0;

    protected byte significantBits = 0;

    protected GeoHash3D()
    {}

    /**
     * This method uses the given number of characters as the desired precision
     * value. The hash can only be 64bits long, thus a maximum precision of 12
     * characters can be achieved.
     */
    public static GeoHash3D withCharacterPrecision( double x, double y, double z, int numberOfCharacters )
    {
        if ( numberOfCharacters > MAX_CHARACTER_PRECISION )
        {
            throw new IllegalArgumentException( "A geohash can only be " + MAX_CHARACTER_PRECISION + " character long." );
        }
        int desiredPrecision = ( numberOfCharacters * 5 <= 60 ) ? numberOfCharacters * 5 : 60;
        return new GeoHash3D( x, y, z, desiredPrecision );
    }

    /**
     * This method uses the given number of characters as the desired precision
     * value. The hash can only be 64bits long, thus a maximum precision of 12
     * characters can be achieved.
     * z is in units of km, - is below sea level, + is above sea level.
     */
    public static String geoHashStringWithCharacterPrecision( double x, double y, double z, int numberOfCharacters )
    {
        GeoHash3D hash = withCharacterPrecision( x, y, z, numberOfCharacters );
        return hash.toBase32();
    }

    private GeoHash3D( double x, double y, double z, int desiredPrecision )
    {
        desiredPrecision = Math.min( desiredPrecision, MAX_BIT_PRECISION );

        int bit = 1;
        double[] xRange = { -180, 180 };
        double[] yRange = { -180, 180 };
        // convert from km to degrees so z has the same range as x and y
        // radius of the earth is only 6356km
        // allowable z could be from -6356 to 35786km, sufficient range for zs from the centre of earth to geostationary orbit.
        // this is a bit more than the range of y (which is 2 * 180 * 100km = 36,000km).  Given that depths below 12.2km (deepest borehole)
        // aren't practically useful, could limit to this giving very close to same range as y.
        // Should check the z and throw exception if outside range.
        double[] zRange = { -180, 180 };

        while ( significantBits < desiredPrecision )
        {
            if ( bit == 1 )
            {
                divideRangeEncode( y, yRange );
            }
            else if ( bit == 2 )
            {
                divideRangeEncode( x, xRange );
            }
            else
            {
                divideRangeEncode( z, zRange );
            }
            if ( ++bit > 3 )
                bit = 1;
        }

        bits <<= ( MAX_BIT_PRECISION - desiredPrecision );
    }

    public long ord()
    {
        int insignificantBits = MAX_BIT_PRECISION - significantBits;
        return bits >>> insignificantBits;
    }

    /**
     * Returns the number of characters that represent this hash.
     * 
     * @throws IllegalStateException
     *             when the hash cannot be encoded in base32, i.e. when the
     *             precision is not a multiple of 5.
     */
    public int getCharacterPrecision()
    {
        if ( significantBits % 5 != 0 )
        {
            throw new IllegalStateException(
                    "precision of GeoHash is not divisble by 5: " + this );
        }
        return significantBits / 5;
    }

    private void divideRangeEncode( double value, double[] range )
    {
        double mid = ( range[ 0 ] + range[ 1 ] ) / 2;
        if ( value >= mid )
        {
            addOnBitToEnd();
            range[ 0 ] = mid;
        }
        else
        {
            addOffBitToEnd();
            range[ 1 ] = mid;
        }
    }

    /**
     * how many significant bits are there in this {@link GeoHash3D}?
     */
    public int significantBits()
    {
        return significantBits;
    }

    public long longValue()
    {
        return bits;
    }

    /**
     * get the base32 string for this {@link GeoHash3D}.<br>
     * this method only makes sense, if this hash has a multiple of 5
     * significant bits.
     * 
     * @throws IllegalStateException
     *             when the number of significant bits is not a multiple of 5.
     */
    public String toBase32()
    {
        if ( significantBits % 5 != 0 )
        {
            throw new IllegalStateException( "Cannot convert a geohash to base32 if the precision is not a multiple of 5." );
        }
        StringBuilder buf = new StringBuilder();

        long firstFiveBitsMask = 0xf800000000000000l;
        long bitsCopy = bits;
        int partialChunks = ( int ) Math.ceil( ( ( double ) significantBits / 5 ) );

        for ( int i = 0; i < partialChunks; i++ )
        {
            int pointer = ( int ) ( ( bitsCopy & firstFiveBitsMask ) >>> 59 );
            buf.append( base32[ pointer ] );
            bitsCopy <<= 5;
        }
        return buf.toString();
    }

    protected final void addOnBitToEnd()
    {
        significantBits++;
        bits <<= 1;
        bits = bits | 0x1;
    }

    protected final void addOffBitToEnd()
    {
        significantBits++;
        bits <<= 1;
    }

    public String toBinaryString()
    {
        StringBuilder bui = new StringBuilder();
        long bitsCopy = bits;
        for ( int i = 0; i < significantBits; i++ )
        {
            if ( ( bitsCopy & FIRST_BIT_FLAGGED ) == FIRST_BIT_FLAGGED )
            {
                bui.append( '1' );
            }
            else
            {
                bui.append( '0' );
            }
            bitsCopy <<= 1;
        }
        return bui.toString();
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }
        if ( obj instanceof GeoHash3D )
        {
            GeoHash3D other = ( GeoHash3D ) obj;
            if ( other.significantBits == significantBits && other.bits == bits )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int f = 17;
        f = 31 * f + ( int ) ( bits ^ ( bits >>> 32 ) );
        f = 31 * f + significantBits;
        return f;
    }

    @Override
    public int compareTo( GeoHash3D o )
    {
        int bitsCmp = Long.compare( bits ^ FIRST_BIT_FLAGGED, o.bits ^ FIRST_BIT_FLAGGED );
        if ( bitsCmp != 0 )
        {
            return bitsCmp;
        }
        else
        {
            return Integer.compare( significantBits, o.significantBits );
        }
    }
}
