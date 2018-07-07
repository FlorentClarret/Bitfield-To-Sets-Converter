package io.github.florentclarret.jbitfield;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a immutable bit field of <T> elements. This class aims to ease conversion between EnumSet and bit field
 * for database storage for instance. However, if not needed, never prefer using a bit field over an EnumSet. See Item
 * 36, Effective Java (Third Edition) by Joshua Bloch for further information. Warning : the representation of the bit
 * field is stored in a long primitive type. It means that you can not have more than 64 different values in this
 * implementation
 *
 * @param <T> The enum which represents the value in the bit field.
 * @author Florent Clarret
 */
public final class BitField<T extends Enum<T> & BitFieldElement> {

    /**
     * Binary representation of the bit field.
     */
    private final long bitField;

    /**
     * Set representation of the bit field.
     */
    private final Set<T> set;

    /**
     * The class represented in the bit field
     */
    private final Class<T> enumClass;

    /**
     * Creates a bit field initialized to zero for the given class.
     * @throws IllegalArgumentException if the enumClass is null
     * @throws IllegalArgumentException if the enumClass is not a valid BtFieldElement
     */
    public BitField(final Class<T> enumClass) {
        this(enumClass, Collections.emptySet());
    }

    /**
     * Generates a new instance of the bit field using the given set of BitFieldElement. Duplicated values are used only
     * once and automatically removed.
     *
     * @param set The set containing the element to place in the bit field.
     * @throws IllegalArgumentException if the set is null
     * @throws IllegalArgumentException if the enumClass is null
     * @throws IllegalArgumentException if the enumClass is not a valid BtFieldElement
     */
    public BitField(final Class<T> enumClass, final Set<T> set) {
        if (set == null) {
            throw new IllegalArgumentException("set can not be null");
        } else if (enumClass == null) {
            throw new IllegalArgumentException("enumClass can not be null");
        } else if (!BitFieldHelper.isValidEnum(enumClass)) {
            throw new IllegalArgumentException("the class [" + enumClass.getName() + "] is not a valid " +
                    "BitFieldElement");
        }

        // Not using guava immutable set (instead of unmodifiable set) to avoid relying on external libraries
        this.set = (set.isEmpty()) ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(set));
        this.bitField = this.set.stream().mapToLong(element -> (1 << element.getBitFieldPosition())).sum();
        this.enumClass = enumClass;
    }

    /**
     * Generates an instance of BitField from the binary bit field value.
     *
     * @param enumClass the Class represented in the bit field
     * @param bitField  The binary representation of the bit field.
     * @throws IllegalArgumentException if any value in the field are not present in the <T> enum position's or if the
     *                                  enumClass is null.
     * @throws IllegalArgumentException if the enumClass is null
     * @throws IllegalArgumentException if the enumClass is not a valid BtFieldElement
     */
    public BitField(final Class<T> enumClass, final long bitField) {
        if (enumClass == null) {
            throw new IllegalArgumentException("enumClass must not be null");
        } else if (!BitFieldHelper.isValidEnum(enumClass)) {
            throw new IllegalArgumentException("the class [" + enumClass.getName() + "] is not a valid " +
                    "BitFieldElement");
        }

        long localBitField = bitField;
        this.set = EnumSet.noneOf(enumClass);
        this.bitField = localBitField;
        this.enumClass = enumClass;

        if (localBitField != 0) {
            for (final T element : EnumSet.allOf(enumClass)) {
                if (((1 << element.getBitFieldPosition()) & localBitField) != 0) {
                    this.set.add(element);
                    // Remove the current bit to ease final comparison
                    localBitField = localBitField ^ (1 << element.getBitFieldPosition());
                }
            }

            // If the local bit field is not 0, it means that we have missing position values in the <T> enum
            if (localBitField != 0) {
                throw new IllegalArgumentException("Invalid value found in bit field [" + bitField + "] for enum [" +
                        enumClass.getName());
            }
        }
    }

    /**
     * Return a new instance of a bit field using the given value. Duplicated values are used only once and
     * automatically removed.
     *
     * @param set The set to define in the bit field.
     * @return The newly generated bit field.
     * @throws IllegalArgumentException if the set is null
     */
    public BitField<T> setValue(final Set<T> set) {
        return new BitField<>(enumClass, set);
    }

    /**
     * Create a new instance of the current bit field with the given extra value. If the element is already in the bit
     * field, nothing will be done. Null values from the elements parameter are ignored.
     *
     * @return The newly generated bit field with the extra value.
     * @throws IllegalArgumentException if element is null
     */
    public BitField<T> addValue(final Set<T> elements) {
        if (elements == null) {
            throw new IllegalArgumentException("elements can not be null");
        }

        final Set<T> copy = set.isEmpty() ? new HashSet<>() : EnumSet.copyOf(set);

        if (!elements.isEmpty()) {
            copy.addAll(elements);
        }

        return new BitField<>(enumClass, copy);
    }

    /**
     * Return the internal value of the bit field.
     *
     * @return A long representing the current bit field.
     */
    public long getBitFieldValue() {
        return bitField;
    }

    /**
     * Return the Set containing all the value from the current bit field.
     *
     * @return A unmodifiable set of the value stored in the current bit field.
     */
    public Set<T> getSet() {
        return set;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BitField<?> bitField1 = (BitField<?>) o;

        return bitField == bitField1.bitField;
    }

    @Override
    public int hashCode() {
        return (int) (bitField ^ (bitField >>> 32));
    }

    @Override
    public String toString() {
        return "BitField{" + "bitField=" + bitField + ", set=" + set + '}';
    }
}
