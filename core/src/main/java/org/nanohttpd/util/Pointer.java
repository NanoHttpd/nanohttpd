package org.nanohttpd.util;

/**
 * Used to pass values as arguments in methods
 * while allowing the method to modify them
 * effectively allowing multiple returns.
 * 
 * @author LordFokas
 * @param <T> The type of value to hold.
 */
public class Pointer<T>{
	private T value;
	
	/** Default constructor, with no value. */
	public Pointer(){}
	
	/**
	 * Equivalent to calling the empty constructor and then <code>set(T value)</code>
	 * @param value the value to set.
	 */
	public Pointer(T value){
		this.value = value;
	}
	
	/** @return the contained value. May be <code>null</code>. */
	public T get(){
		return value;
	}
	
	/**
	 * @param newValue the new value to set.
	 * @return the old value. May be <code>null</code>.
	 */
	public T set(T newValue){
		T oldValue = value;
		value = newValue;
		return oldValue;
	}
	
	/**
	 * Wraps a given value in a new parameterized pointer.
	 * @param value the value to initialize with.
	 * @return A parameterized pointer containing the given value.
	 */
	public static <T> Pointer<T> wrap(T value){
		return new Pointer<T>(value);
	}
}
