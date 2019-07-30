/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2006 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package org.javolution.util.stripped;

import org.javolution.util.stripped.FastCollection.Record;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p> This class represents an iterator over a FastCollection
 *     Iterations are thread-safe if the collections records are not removed 
 *     or inserted at arbitrary position (appending/prepending is fine).</p>
 *     
 * <p> Iterators are allocated on the stack when executing in a 
 *
 * @author <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 3.7, March 17, 2005
 */
final class FastIterator implements Iterator {

    private FastCollection _collection;

    private Record _current;

    private Record _next;

    private Record _tail;

    public static FastIterator valueOf(FastCollection collection) {
        FastIterator iterator = new FastIterator();
        iterator._collection = collection;
        iterator._next = collection.head().getNext();
        iterator._tail = collection.tail();
        return iterator;
    }

    private FastIterator() {
    }

    public boolean hasNext() {
        return (_next != _tail);
    }

    public Object next() {
        if (_next == _tail)
            throw new NoSuchElementException();
        _current = _next;
        _next = _next.getNext();
        return _collection.valueOf(_current);
    }

    public void remove() {
        if (_current != null) {
            // Uses the previous record (not affected by the remove)
            // to set the next record.
            final Record previous = _current.getPrevious();
            _collection.delete(_current);
            _current = null;
            _next = previous.getNext();
        } else {
            throw new IllegalStateException();
        }
    }
}