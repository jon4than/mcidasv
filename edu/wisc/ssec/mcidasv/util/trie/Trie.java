/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2021
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
package edu.wisc.ssec.mcidasv.util.trie;

import java.util.Map;
import java.util.SortedMap;

/**
 * Defines the interface for a prefix tree, an ordered tree data structure. For 
 * more information, see <a href= "http://en.wikipedia.org/wiki/Trie">Tries</a>.
 * 
 * @author Roger Kapsi
 * @author Sam Berlin
 */
public interface Trie<K, V> extends SortedMap<K, V> {
    
    /**
     * Returns a view of this Trie of all elements that are
     * prefixed by the given key.
     * <p>
     * In a fixed-keysize Trie, this is essentially a 'get' operation.
     * <p>
     * For example, if the Trie contains 'Lime', 'LimeWire', 
     * 'LimeRadio', 'Lax', 'Later', 'Lake', and 'Lovely', then
     * a lookup of 'Lime' would return 'Lime', 'LimeRadio', and 'LimeWire'.
     *
     * @param key Key to search for within the trie.
     *
     * @return {@code Map} containing keys matching the given prefix and their
     * values.
     */
    SortedMap<K, V> getPrefixedBy(K key);
    
    /**
     * Returns a view of this Trie of all elements that are
     * prefixed by the length of the key.
     * <p>
     * Fixed-keysize Tries will not support this operation
     * (because all keys will be the same length).
     * <p>
     * For example, if the Trie contains 'Lime', 'LimeWire', 
     * 'LimeRadio', 'Lax', 'Later', 'Lake', and 'Lovely', then
     * a lookup of 'LimePlastics' with a length of 4 would
     * return 'Lime', 'LimeRadio', and 'LimeWire'.
     *
     * @param key Key to search for within the trie.
     * @param length Length of {@code key}. Ignored for tries with a fixed key
     *               size.
     *
     * @return {@code Map} containing keys matching the given prefix and their
     * values.
     */
    SortedMap<K, V> getPrefixedBy(K key, int length);
    
    /**
     * Returns a view of this Trie of all elements that are prefixed
     * by the key, starting at the given offset and for the given length.
     * <p>
     * Fixed-keysize Tries will not support this operation
     * (because all keys are the same length).
     * <p>
     * For example, if the Trie contains 'Lime', 'LimeWire', 
     * 'LimeRadio', 'Lax', 'Later', 'Lake', and 'Lovely', then
     * a lookup of 'The Lime Plastics' with an offset of 4 and a 
     * length of 4 would return 'Lime', 'LimeRadio', and 'LimeWire'.
     *
     * @param key Key to search for within the trie.
     * @param offset Offset to begin search at.
     * @param length Length of {@code key}. Ignored for tries with a fixed key
     *               size.
     *
     * @return {@code Map} containing keys matching the given prefix and their
     * values.
     */
    SortedMap<K, V> getPrefixedBy(K key, int offset, int length);
    
    /**
     * Returns a view of this Trie of all elements that are prefixed
     * by the number of bits in the given Key.
     * <p>
     * Fixed-keysize Tries can support this operation as a way to do
     * lookups of partial keys.  That is, if the Trie is storing IP
     * addresses, you can lookup all addresses that begin with
     * '192.168' by providing the key '192.168.X.X' and a length of 16
     * would return all addresses that begin with '192.168'.
     *
     * @param key Key to search for within the trie.
     * @param bitLength Length of {@code key} in bits.
     *
     * @return {@code Map} containing keys matching the given prefix and their
     * values.
     */
    SortedMap<K, V> getPrefixedByBits(K key, int bitLength);
    
    /**
     * Returns the value for the entry whose key is closest in a bitwise
     * XOR metric to the given key.  This is NOT lexicographic closeness.
     * For example, given the keys:<br>
     *  D = 1000100 <br>
     *  H = 1001000 <br> 
     *  L = 1001100 <br>
     * <p>
     * If the Trie contained 'H' and 'L', a lookup of 'D' would return 'L',
     * because the XOR distance between D and L is smaller than the XOR distance
     * between D and H.
     *
     * @param key Key to search for within the trie.
     *
     * @return Value associated with {@code key}.
     */
    V select(K key);
    
    /**
     * Iterates through the Trie, starting with the entry whose bitwise
     * value is closest in an XOR metric to the given key.  After the closest
     * entry is found, the Trie will call select on that entry and continue
     * calling select for each entry (traversing in order of XOR closeness,
     * NOT lexicographically) until the cursor returns 
     * {@code Cursor.SelectStatus.EXIT}.<br>
     * The cursor can return {@code Cursor.SelectStatus.CONTINUE} to
     * continue traversing.<br>
     * {@code Cursor.SelectStatus.REMOVE_AND_EXIT} is used to remove the current element
     * and stop traversing.
     * <p>
     * Note: The {@link Cursor.SelectStatus#REMOVE} operation is not supported.
     *
     * @param key Key to match.
     * @param cursor Cursor being used to traverse the trie.
     *
     * @return The entry the cursor returned EXIT on, or null if it continued
     *         till the end.
     */
    Map.Entry<K,V> select(K key, Cursor<? super K, ? super V> cursor);
    
    /**
     * Traverses the Trie in lexicographical order. {@code Cursor.select}
     * will be called on each entry.<p>
     * The traversal will stop when the cursor returns {@code Cursor.SelectStatus.EXIT}.<br>
     * {@code Cursor.SelectStatus.CONTINUE} is used to continue traversing.<br>
     * {@code Cursor.SelectStatus.REMOVE} is used to remove the element that was
     * selected and continue traversing.<br>
     * {@code Cursor.SelectStatus.REMOVE_AND_EXIT} is used to remove the current element
     * and stop traversing.
     *
     * @param cursor Cursor being used to traverse the trie.
     *   
     * @return The entry the cursor returned EXIT on, or null if it continued
     *         till the end.
     */
    Map.Entry<K,V> traverse(Cursor<? super K, ? super V> cursor);
    
    /**
     * An interface used by a {@link Trie}. A {@link Trie} selects items by 
     * closeness and passes the items to the {@code Cursor}. You can then
     * decide what to do with the key-value pair and the return value 
     * from {@link #select(java.util.Map.Entry)} tells the {@code Trie}
     * what to do next.
     * <p>
     * {@code Cursor} returns status/selection status might be:
     * <table summary="The various Cursor status values">
     * <tr><td><b>Return Value</b></td><td><b>Status</b></td></tr>
     * <tr><td>EXIT</td><td>Finish the Trie operation</td></tr>
     * <tr><td>CONTINUE</td><td>Look at the next element in the traversal</td></tr>
     * <tr><td>REMOVE_AND_EXIT</td><td>Remove the entry and stop iterating</td></tr>
     * <tr><td>REMOVE</td><td>Remove the entry and continue iterating</td></tr>
     * </table>

     * Note: {@link Trie#select(Object, edu.wisc.ssec.mcidasv.util.trie.Trie.Cursor)}
     * does not support {@code REMOVE}.
     *
     * @param <K> Key Type
     * @param <V> Key Value
     */
    interface Cursor<K, V> {
        
        /**
         * Notification that the Trie is currently looking at the given entry.
         * Return {@code EXIT} to finish the Trie operation,
         * {@code CONTINUE} to look at the next entry, {@code REMOVE}
         * to remove the entry and continue iterating, or
         * {@code REMOVE_AND_EXIT} to remove the entry and stop iterating.
         * Not all operations support {@code REMOVE}.
         *
         * @param entry Entry that will be operated upon.
         *
         * @return Status of cursor after trie operation.
         */
        SelectStatus select(Map.Entry<? extends K, ? extends V> entry);
     
        /** The mode during selection. */
        enum SelectStatus { EXIT, CONTINUE, REMOVE, REMOVE_AND_EXIT }
    }
}

