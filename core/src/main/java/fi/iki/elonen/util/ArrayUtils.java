package fi.iki.elonen.util;

/*
 * #%L
 * NanoHttpd-Webserver
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

public final class ArrayUtils {

    private ArrayUtils() {
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static long[] clone(final long[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static int[] clone(final int[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static boolean[] clone(final boolean[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static short[] clone(final short[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static char[] clone(final char[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static double[] clone(final double[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static byte[] clone(final byte[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static float[] clone(final float[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }

    /**
     * <p>
     * Clones an array returning a typecast result and handling {@code null}.
     * </p>
     * <p/>
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     * 
     * @param array
     *            the array to clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
     */
    public static <T> T[] clone(final T[] array) {
        if (array == null) {
            return null;
        }
        return array.clone();
    }
}
