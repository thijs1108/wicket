/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.util.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.lang.Generics;


/**
 * The Wicket ObjectOutputStream to enable back-button support for the reloading mechanism, be sure
 * to put <tt>Objects.setObjectStreamFactory(new WicketObjectStreamFactory());</tt> in your
 * application's {@link WebApplication#init()} method.
 * 
 * @see org.apache.wicket.protocol.http.ReloadingWicketFilter
 * @author jcompagner
 * @deprecated in Wicket 1.5 the (de)serializator is a pluggable piece, if some customization is
 *             needed see the new framework settings
 */
@Deprecated
public final class WicketObjectOutputStream extends ObjectOutputStream
{
	/**
	 * Lightweight identity hash table which maps objects to integer handles, assigned in ascending
	 * order (comes from {@link ObjectOutputStream}).
	 */
	private static final class HandleTable
	{
		/* number of mappings in table/next available handle */
		private int size;
		/* size threshold determining when to expand hash spine */
		private int threshold;
		/* factor for computing size threshold */
		private final float loadFactor;
		/* maps hash value -> candidate handle value */
		private int[] spine;
		/* maps handle value -> next candidate handle value */
		private int[] next;
		/* maps handle value -> associated object */
		private Object[] objs;

		HandleTable()
		{
			this(16, 0.75f);
		}

		HandleTable(int initialCapacity, float loadFactor)
		{
			this.loadFactor = loadFactor;
			spine = new int[initialCapacity];
			next = new int[initialCapacity];
			objs = new Object[initialCapacity];
			threshold = (int)(initialCapacity * loadFactor);
			clear();
		}

		private void growEntries()
		{
			int newLength = (next.length << 1) + 1;
			int[] newNext = new int[newLength];
			System.arraycopy(next, 0, newNext, 0, size);
			next = newNext;

			Object[] newObjs = new Object[newLength];
			System.arraycopy(objs, 0, newObjs, 0, size);
			objs = newObjs;
		}

		private void growSpine()
		{
			spine = new int[(spine.length << 1) + 1];
			threshold = (int)(spine.length * loadFactor);
			Arrays.fill(spine, -1);
			for (int i = 0; i < size; i++)
			{
				insert(objs[i], i);
			}
		}

		private int hash(Object obj)
		{
			return System.identityHashCode(obj) & 0x7FFFFFFF;
		}

		private void insert(Object obj, int handle)
		{
			int index = hash(obj) % spine.length;
			objs[handle] = obj;
			next[handle] = spine[index];
			spine[index] = handle;
		}

		/**
		 * Assigns next available handle to given object, and returns handle value. Handles are
		 * assigned in ascending order starting at 0.
		 * 
		 * @param obj
		 * @return size
		 */
		int assign(Object obj)
		{
			if (size >= next.length)
			{
				growEntries();
			}
			if (size >= threshold)
			{
				growSpine();
			}
			insert(obj, size);
			return size++;
		}

		void clear()
		{
			Arrays.fill(spine, -1);
			Arrays.fill(objs, 0, size, null);
			size = 0;
		}

		boolean contains(Object obj)
		{
			return lookup(obj) != -1;
		}

		/**
		 * Looks up and returns handle associated with given object, or -1 if no mapping found.
		 * 
		 * @param obj
		 * @return position, or -1 if not found
		 */
		int lookup(Object obj)
		{
			if (size == 0)
			{
				return -1;
			}
			int index = hash(obj) % spine.length;
			for (int i = spine[index]; i >= 0; i = next[i])
			{
				if (objs[i] == obj)
				{
					return i;
				}
			}
			return -1;
		}

		int size()
		{
			return size;
		}
	}

	private class PutFieldImpl extends PutField
	{
		private HashMap<String, Byte> mapBytes;
		private HashMap<String, Character> mapChar;
		private HashMap<String, Double> mapDouble;
		private HashMap<String, Float> mapFloat;
		private HashMap<String, Integer> mapInt;
		private HashMap<String, Long> mapLong;
		private HashMap<String, Short> mapShort;
		private HashMap<String, Boolean> mapBoolean;
		private HashMap<String, Object> mapObject;

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, boolean)
		 */
		@Override
		public void put(String name, boolean val)
		{
			if (mapBoolean == null)
			{
				mapBoolean = Generics.newHashMap(4);
			}
			mapBoolean.put(name, val ? Boolean.TRUE : Boolean.FALSE);
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, byte)
		 */
		@Override
		public void put(String name, byte val)
		{
			if (mapBytes == null)
			{
				mapBytes = Generics.newHashMap(4);
			}
			mapBytes.put(name, new Byte(val));
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, char)
		 */
		@Override
		public void put(String name, char val)
		{
			if (mapChar == null)
			{
				mapChar = Generics.newHashMap(4);
			}
			mapChar.put(name, new Character(val));
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, double)
		 */
		@Override
		public void put(String name, double val)
		{
			if (mapDouble == null)
			{
				mapDouble = Generics.newHashMap(4);
			}
			mapDouble.put(name, new Double(val));
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, float)
		 */
		@Override
		public void put(String name, float val)
		{
			if (mapFloat == null)
			{
				mapFloat = Generics.newHashMap(4);
			}
			mapFloat.put(name, new Float(val));
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, int)
		 */
		@Override
		public void put(String name, int val)
		{
			if (mapInt == null)
			{
				mapInt = Generics.newHashMap(4);
			}
			mapInt.put(name, new Integer(val));
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, long)
		 */
		@Override
		public void put(String name, long val)
		{
			if (mapLong == null)
			{
				mapLong = Generics.newHashMap(4);
			}
			mapLong.put(name, new Long(val));
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, java.lang.Object)
		 */
		@Override
		public void put(String name, Object val)
		{
			if (mapObject == null)
			{
				mapObject = Generics.newHashMap(4);
			}
			mapObject.put(name, val);
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#put(java.lang.String, short)
		 */
		@Override
		public void put(String name, short val)
		{
			if (mapShort == null)
			{
				mapShort = Generics.newHashMap(4);
			}
			mapShort.put(name, new Short(val));
		}

		/**
		 * @see java.io.ObjectOutputStream.PutField#write(java.io.ObjectOutput)
		 */
		@Override
		public void write(ObjectOutput out) throws IOException
		{
			// i don't know if all the fields (names in the map)
			// are really also always real fields.. So i just
			// write them by name->value
			// maybe in the further we can really calculate an offset?
			if (mapBoolean != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(boolean.class);
				writeShort(lookup.getClassId());
				writeShort(mapBoolean.size());
				Iterator<Entry<String, Boolean>> it = mapBoolean.entrySet().iterator();
				while (it.hasNext())
				{
					Map.Entry<String, Boolean> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeBoolean((entry.getValue()).booleanValue());
				}
			}
			if (mapBytes != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(byte.class);
				writeShort(lookup.getClassId());
				writeShort(mapBytes.size());
				Iterator<Entry<String, Byte>> it = mapBytes.entrySet().iterator();
				while (it.hasNext())
				{
					Entry<String, Byte> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeByte((entry.getValue()).byteValue());
				}
			}
			if (mapShort != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(short.class);
				writeShort(lookup.getClassId());
				writeShort(mapShort.size());
				Iterator<Entry<String, Short>> it = mapShort.entrySet().iterator();
				while (it.hasNext())
				{
					Entry<String, Short> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeShort((entry.getValue()).shortValue());
				}
			}
			if (mapChar != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(char.class);
				writeShort(lookup.getClassId());
				writeShort(mapChar.size());
				Iterator<Entry<String, Character>> it = mapChar.entrySet().iterator();
				while (it.hasNext())
				{
					Entry<String, Character> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeChar((entry.getValue()).charValue());
				}
			}
			if (mapInt != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(int.class);
				writeShort(lookup.getClassId());
				writeShort(mapInt.size());
				Iterator<Entry<String, Integer>> it = mapInt.entrySet().iterator();
				while (it.hasNext())
				{
					Entry<String, Integer> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeInt((entry.getValue()).intValue());
				}
			}
			if (mapLong != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(long.class);
				writeShort(lookup.getClassId());
				writeShort(mapLong.size());
				Iterator<Entry<String, Long>> it = mapLong.entrySet().iterator();
				while (it.hasNext())
				{
					Entry<String, Long> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeLong((entry.getValue()).longValue());
				}
			}
			if (mapFloat != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(float.class);
				writeShort(lookup.getClassId());
				writeShort(mapFloat.size());
				Iterator<Entry<String, Float>> it = mapFloat.entrySet().iterator();
				while (it.hasNext())
				{
					Entry<String, Float> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeFloat((entry.getValue()).floatValue());
				}
			}
			if (mapDouble != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(double.class);
				writeShort(lookup.getClassId());
				writeShort(mapDouble.size());
				Iterator<Entry<String, Double>> it = mapDouble.entrySet().iterator();
				while (it.hasNext())
				{
					Entry<String, Double> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeDouble((entry.getValue()).doubleValue());
				}
			}
			if (mapObject != null)
			{
				ClassStreamHandler lookup = ClassStreamHandler.lookup(Serializable.class);
				writeShort(lookup.getClassId());
				writeShort(mapObject.size());
				Iterator<Entry<String, Object>> it = mapObject.entrySet().iterator();
				while (it.hasNext())
				{
					Entry<String, Object> entry = it.next();
					// write the key.
					writeObjectOverride(entry.getKey());
					writeObjectOverride(entry.getValue());
				}
			}
			// end byte.
			writeShort(ClassStreamHandler.NULL);
		}

	}

	private final HandleTable handledObjects = new HandleTable();

	private final HandleArrayListStack<Object> defaultWrite = new HandleArrayListStack<Object>();
	private final DataOutputStream out;

	private final Stack<ClassStreamHandler> classHandlerStack = new Stack<ClassStreamHandler>();

	private PutField curPut;

	private Object curObject;

	/**
	 * Construct.
	 * 
	 * @param out
	 * @throws IOException
	 */
	public WicketObjectOutputStream(OutputStream out) throws IOException
	{
		super();
		this.out = new DataOutputStream(out);

	}

	/**
	 * @see java.io.ObjectOutputStream#close()
	 */
	@Override
	public void close() throws IOException
	{
		classHandlerStack.clear();
		curObject = null;
		curPut = null;
		handledObjects.clear();
		defaultWrite.clear();
		out.close();
	}

	/**
	 * @see java.io.ObjectOutputStream#defaultWriteObject()
	 */
	@Override
	public void defaultWriteObject() throws IOException
	{
		if (!defaultWrite.contains(curObject))
		{
			defaultWrite.add(curObject);
			classHandlerStack.peek().writeFields(this, curObject);
		}
	}

	/**
	 * @see java.io.ObjectOutputStream#putFields()
	 */
	@Override
	public PutField putFields() throws IOException
	{
		if (curPut == null)
		{
			try
			{
				curPut = new PutFieldImpl();
			}
			catch (Exception e)
			{
				throw new WicketSerializeableException("Error reading put fields", e);
			}
		}
		return curPut;
	}

	/**
	 * @see java.io.ObjectOutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] buf) throws IOException
	{
		out.write(buf);
	}

	/**
	 * @see java.io.ObjectOutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] buf, int off, int len) throws IOException
	{
		out.write(buf, off, len);
	}

	/**
	 * @see java.io.ObjectOutputStream#write(int)
	 */
	@Override
	public void write(int val) throws IOException
	{
		out.write(val);
	}

	/**
	 * Writes a boolean.
	 * 
	 * @param val
	 *            the boolean to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeBoolean(boolean val) throws IOException
	{
		out.writeBoolean(val);
	}

	/**
	 * Writes an 8 bit byte.
	 * 
	 * @param val
	 *            the byte value to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeByte(int val) throws IOException
	{
		out.writeByte(val);
	}

	/**
	 * Writes a String as a sequence of bytes.
	 * 
	 * @param str
	 *            the String of bytes to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeBytes(String str) throws IOException
	{
		out.writeBytes(str);
	}

	/**
	 * Writes a 16 bit char.
	 * 
	 * @param val
	 *            the char value to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeChar(int val) throws IOException
	{
		out.writeChar(val);
	}

	/**
	 * Writes a String as a sequence of chars.
	 * 
	 * @param str
	 *            the String of chars to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeChars(String str) throws IOException
	{
		out.writeChars(str);
	}

	/**
	 * Writes a 64 bit double.
	 * 
	 * @param val
	 *            the double value to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeDouble(double val) throws IOException
	{
		out.writeDouble(val);
	}

	/**
	 * @see java.io.ObjectOutputStream#writeFields()
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void writeFields() throws IOException
	{
		if (curPut != null)
		{
			try
			{
				curPut.write(this);
			}
			catch (Exception e)
			{
				throw new WicketSerializeableException("Error writing put fields", e);
			}
		}
	}

	/**
	 * Writes a 32 bit float.
	 * 
	 * @param val
	 *            the float value to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeFloat(float val) throws IOException
	{
		out.writeFloat(val);
	}

	/**
	 * Writes a 32 bit int.
	 * 
	 * @param val
	 *            the integer value to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeInt(int val) throws IOException
	{
		out.writeInt(val);
	}

	/**
	 * Writes a 64 bit long.
	 * 
	 * @param val
	 *            the long value to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeLong(long val) throws IOException
	{
		out.writeLong(val);
	}

	/**
	 * Writes a 16 bit short.
	 * 
	 * @param val
	 *            the short value to be written
	 * @throws IOException
	 *             if I/O errors occur while writing to the underlying stream
	 */
	@Override
	public void writeShort(int val) throws IOException
	{
		out.writeShort(val);
	}

	/**
	 * @see java.io.ObjectOutputStream#writeUTF(java.lang.String)
	 */
	@Override
	public void writeUTF(String str) throws IOException
	{
		out.writeUTF(str);
	}

	/**
	 * @see java.io.ObjectOutputStream#writeObjectOverride(java.lang.Object)
	 */
	@Override
	protected final void writeObjectOverride(Object obj) throws IOException
	{
		if (obj == null)
		{
			out.write(ClassStreamHandler.NULL);
			return;
		}
		int handle = handledObjects.lookup(obj);
		if (handle != -1)
		{
			out.write(ClassStreamHandler.HANDLE);
			out.writeShort(handle);
		}
		else
		{
			if (obj instanceof Class)
			{
				ClassStreamHandler classHandler = ClassStreamHandler.lookup((Class<?>)obj);
				out.write(ClassStreamHandler.CLASS);
				out.writeShort(classHandler.getClassId());
			}
			else
			{
				Class<?> cls = obj.getClass();
				handledObjects.assign(obj);

				if (cls.isArray())
				{
					Class<?> componentType = cls.getComponentType();
					ClassStreamHandler classHandler = ClassStreamHandler.lookup(componentType);
					if (componentType.isPrimitive())
					{
						try
						{
							out.write(ClassStreamHandler.PRIMITIVE_ARRAY);
							out.writeShort(classHandler.getClassId());
							classHandler.writeArray(obj, this);
						}
						catch (WicketSerializeableException wse)
						{
							wse.addTrace(componentType.getName() + "[" + Array.getLength(obj) + "]");
							throw wse;
						}
						catch (Exception e)
						{
							throw new WicketSerializeableException(
								"Error writing primitive array of " + componentType.getName() +
									"[" + Array.getLength(obj) + "]", e);
						}
					}
					else
					{
						int length = Array.getLength(obj);
						try
						{
							out.write(ClassStreamHandler.ARRAY);
							out.writeShort(classHandler.getClassId());
							out.writeInt(length);
							for (int i = 0; i < length; i++)
							{
								writeObjectOverride(Array.get(obj, i));
							}
						}
						catch (WicketSerializeableException wse)
						{
							wse.addTrace(componentType.getName() + "[" + length + "]");
							throw wse;
						}
						catch (Exception e)
						{
							throw new WicketSerializeableException("Error writing array of " +
								componentType.getName() + "[" + length + "]", e);
						}
					}
					return;
				}
				else
				{
					Class<?> realClz = cls;
					ClassStreamHandler classHandler = ClassStreamHandler.lookup(realClz);

					Object object = classHandler.writeReplace(obj);
					if (object != null)
					{
						obj = object;
						realClz = obj.getClass();
						classHandler = ClassStreamHandler.lookup(realClz);
					}

					out.write(ClassStreamHandler.CLASS_DEF);
					out.writeShort(classHandler.getClassId());
					// handle strings directly.
					if (obj instanceof String)
					{
						out.writeUTF((String)obj);
					}
					else
					{
						PutField old = curPut;
						Object oldObject = curObject;
						curPut = null;
						curObject = obj;
						try
						{
							classHandlerStack.push(classHandler);
							if (!classHandler.invokeWriteMethod(this, obj))
							{
								classHandler.writeFields(this, obj);
							}
						}
						catch (WicketSerializeableException wse)
						{
							if (realClz != cls)
							{
								wse.addTrace(realClz.getName() + "(ReplaceOf:" + cls.getName() +
									")");
							}
							else
							{
								wse.addTrace(realClz.getName());
							}
							throw wse;
						}
						catch (Exception e)
						{
							if (realClz != cls)
							{
								throw new WicketSerializeableException("Error writing fields for " +
									realClz.getName() + "(ReplaceOf:" + cls.getName() + ")", e);

							}
							else
							{
								throw new WicketSerializeableException("Error writing fields for " +
									realClz.getName(), e);
							}
						}
						finally
						{
							classHandlerStack.pop();
							curObject = oldObject;
							curPut = old;
						}
					}
				}
			}
		}
	}
}