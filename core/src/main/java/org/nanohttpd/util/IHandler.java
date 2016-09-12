package org.nanohttpd.util;

/**
 * A generic handler to apply processing to data in a pluggable manner.
 * 
 * @author LordFokas
 *
 * @param <I> The input data type
 * @param <O> The output data type
 */
public interface IHandler<I, O>{
	/**
	 * @param input the data to be processed.
	 * @return The result of the processed data, if any.
	 */
	public O handle(I input);
}
