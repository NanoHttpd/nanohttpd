package org.nanohttpd.util;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2017 nanohttpd
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

/**
 * Used to pass values as arguments in methods while allowing the method to
 * modify them effectively allowing multiple returns.
 * 
 * @author LordFokas
 * @param <T>
 *            The type of value to hold.
 */
public class Pointer<T> {

    private T value;

    /** Default constructor, with no value. */
    public Pointer() {
    }

    /**
     * Equivalent to calling the empty constructor and then
     * <code>set(T value)</code>
     * 
     * @param value
     *            the value to set.
     */
    public Pointer(T value) {
        this.value = value;
    }

    /** @return the contained value. May be <code>null</code>. */
    public T get() {
        return value;
    }

    /**
     * @param newValue
     *            the new value to set.
     * @return the old value. May be <code>null</code>.
     */
    public T set(T newValue) {
        T oldValue = value;
        value = newValue;
        return oldValue;
    }

    /**
     * Wraps a given value in a new parameterized pointer.
     * 
     * @param value
     *            the value to initialize with.
     * @return A parameterized pointer containing the given value.
     */
    public static <T> Pointer<T> wrap(T value) {
        return new Pointer<T>(value);
    }
}
