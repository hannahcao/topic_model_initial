package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: Aug 25, 2007
 * Time: 6:36:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynchronizedRowCompressedColtMatrix3D implements IMatrix3D {
    private final RowCompressedColtMatrix3D rowCompressedColtMatrix3D;

    public SynchronizedRowCompressedColtMatrix3D(int dim0size, int dim1size, int dim2size) {
        rowCompressedColtMatrix3D = new RowCompressedColtMatrix3D(dim0size, dim1size, dim2size);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public synchronized void add(int coord0, int coord1, int coord2, double value) {
        rowCompressedColtMatrix3D.add(coord0, coord1, coord2, value);
    }

    public synchronized void set(int coord0, int coord1, int coord2, double value) {
        rowCompressedColtMatrix3D.set(coord0, coord1, coord2, value);
    }

    public synchronized double get(int coord0, int coord1, int coord2) {
        return rowCompressedColtMatrix3D.get(coord0, coord1, coord2);
    }

    public synchronized void doForAllNonZeros(INonZeroPerformer3D performer) {
        rowCompressedColtMatrix3D.doForAllNonZeros(performer);
    }

    public synchronized void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        rowCompressedColtMatrix3D.add(coordinates, val, position);
    }

    public synchronized void set(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        rowCompressedColtMatrix3D.set(coordinates, val, position);
    }

    public double get(int[] coordinates, int position) throws IndexOutOfBoundsException {
        return get(coordinates);

    }

    public synchronized IMatrixMin marginalizeMin(int[] margeDims) {
        return rowCompressedColtMatrix3D.marginalizeMin(margeDims);
    }

    public synchronized IMatrixMin transposeMin(int[] dimensions) {
        return rowCompressedColtMatrix3D.transposeMin(dimensions);
    }

    public synchronized IMatrix toMatrix(IMatrix instance) {
        return rowCompressedColtMatrix3D.toMatrix(instance);
    }

    public synchronized void assignZero() {
        rowCompressedColtMatrix3D.assignZero();
    }

    public synchronized int getDimensions() {
        return rowCompressedColtMatrix3D.getDimensions();
    }

    public synchronized int getDimensionSize(int dimension) {
        return rowCompressedColtMatrix3D.getDimensionSize(dimension);
    }

    public synchronized void trimToSize() {
        rowCompressedColtMatrix3D.trimToSize();
    }

    public synchronized IMatrix marginalize(int margeDimension) {
        return rowCompressedColtMatrix3D.marginalize(margeDimension);
    }

    public synchronized IMatrix marginalize(int[] margeDimensions) {
        return rowCompressedColtMatrix3D.marginalize(margeDimensions);
    }

    public synchronized IMatrix transpose(int[] dimensions) {
        return rowCompressedColtMatrix3D.transpose(dimensions);
    }

    public synchronized void set(int[] coordinates, double val) throws IndexOutOfBoundsException {
        rowCompressedColtMatrix3D.set(coordinates, val);
    }

    public synchronized void add(int[] coordinates, double val) throws IndexOutOfBoundsException {
        rowCompressedColtMatrix3D.add(coordinates, val);
    }

    public synchronized double get(int[] coordinates) throws IndexOutOfBoundsException {
        return rowCompressedColtMatrix3D.get(coordinates);
    }

    public synchronized void getNonZeros(List<IntArrayList> keys, DoubleArrayList values) {
        rowCompressedColtMatrix3D.getNonZeros(keys, values);
    }

    public synchronized void doForAllNonZeros(INonZeroPerformerGeneric performer) {
        rowCompressedColtMatrix3D.doForAllNonZeros(performer);
    }

    public synchronized void addQuick(int[] coords, double val) {
        rowCompressedColtMatrix3D.addQuick(coords, val);
    }

    public synchronized void setQuick(int[] coords, double value) {
        rowCompressedColtMatrix3D.setQuick(coords, value);
    }

    public synchronized double getQuick(int[] coords) {
        return rowCompressedColtMatrix3D.getQuick(coords);
    }

    public synchronized double sum() {
        return rowCompressedColtMatrix3D.sum();
    }

    public synchronized IMatrix marginalizeTo(int[] margeDimensions, IMatrix target) {
        return rowCompressedColtMatrix3D.marginalizeTo(margeDimensions, target);
    }

    public synchronized int size() {
        return rowCompressedColtMatrix3D.size();
    }
}
