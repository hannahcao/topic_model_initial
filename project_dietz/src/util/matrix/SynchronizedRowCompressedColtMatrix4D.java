package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: Aug 25, 2007
 * Time: 6:34:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynchronizedRowCompressedColtMatrix4D implements IMatrix4D {
    private final RowCompressedColtMatrix4D rowCompressedColtMatrix4D;

    public SynchronizedRowCompressedColtMatrix4D(int dim0size, int dim1size, int dim2size, int dim3size) {
        rowCompressedColtMatrix4D = new RowCompressedColtMatrix4D(dim0size, dim1size, dim2size, dim3size);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public synchronized void add(int coord0, int coord1, int coord2, int coord3, double value) {
        rowCompressedColtMatrix4D.add(coord0, coord1, coord2, coord3, value);
    }

    public synchronized void set(int coord0, int coord1, int coord2, int coord3, double value) {
        rowCompressedColtMatrix4D.set(coord0, coord1, coord2, coord3, value);
    }

    public synchronized double get(int coord0, int coord1, int coord2, int coord3) {
        return rowCompressedColtMatrix4D.get(coord0, coord1, coord2, coord3);
    }

    public synchronized void doForAllNonZeros(INonZeroPerformer4D performer) {
        rowCompressedColtMatrix4D.doForAllNonZeros(performer);
    }

    public synchronized void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        rowCompressedColtMatrix4D.add(coordinates, val, position);
    }

    public synchronized void set(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        rowCompressedColtMatrix4D.set(coordinates, val, position);
    }

    public double get(int[] coordinates, int position) throws IndexOutOfBoundsException {

        return get(coordinates);

    }

    public synchronized IMatrixMin marginalizeMin(int[] margeDims) {
        return rowCompressedColtMatrix4D.marginalizeMin(margeDims);
    }

    public synchronized IMatrixMin transposeMin(int[] dimensions) {
        return rowCompressedColtMatrix4D.transposeMin(dimensions);
    }

    public synchronized IMatrix toMatrix(IMatrix instance) {
        return rowCompressedColtMatrix4D.toMatrix(instance);
    }

    public synchronized void assignZero() {
        rowCompressedColtMatrix4D.assignZero();
    }

    public synchronized int getDimensions() {
        return rowCompressedColtMatrix4D.getDimensions();
    }

    public synchronized int getDimensionSize(int dimension) {
        return rowCompressedColtMatrix4D.getDimensionSize(dimension);
    }

    public synchronized void trimToSize() {
        rowCompressedColtMatrix4D.trimToSize();
    }

    public synchronized IMatrix marginalize(int margeDimension) {
        return rowCompressedColtMatrix4D.marginalize(margeDimension);
    }

    public synchronized IMatrix marginalize(int[] margeDimensions) {
        return rowCompressedColtMatrix4D.marginalize(margeDimensions);
    }

    public synchronized IMatrix transpose(int[] dimensions) {
        return rowCompressedColtMatrix4D.transpose(dimensions);
    }

    public synchronized void set(int[] coordinates, double val) throws IndexOutOfBoundsException {
        rowCompressedColtMatrix4D.set(coordinates, val);
    }

    public synchronized void add(int[] coordinates, double val) throws IndexOutOfBoundsException {
        rowCompressedColtMatrix4D.add(coordinates, val);
    }

    public synchronized double get(int[] coordinates) throws IndexOutOfBoundsException {
        return rowCompressedColtMatrix4D.get(coordinates);
    }

    public synchronized void getNonZeros(List<IntArrayList> keys, DoubleArrayList values) {
        rowCompressedColtMatrix4D.getNonZeros(keys, values);
    }

    public synchronized void doForAllNonZeros(INonZeroPerformerGeneric performer) {
        rowCompressedColtMatrix4D.doForAllNonZeros(performer);
    }

    public synchronized void addQuick(int[] coords, double val) {
        rowCompressedColtMatrix4D.addQuick(coords, val);
    }

    public synchronized void setQuick(int[] coords, double value) {
        rowCompressedColtMatrix4D.setQuick(coords, value);
    }

    public synchronized double getQuick(int[] coords) {
        return rowCompressedColtMatrix4D.getQuick(coords);
    }

    public synchronized double sum() {
        return rowCompressedColtMatrix4D.sum();
    }

    public synchronized IMatrix marginalizeTo(int[] margeDimensions, IMatrix target) {
        return rowCompressedColtMatrix4D.marginalizeTo(margeDimensions, target);
    }

    public synchronized int size() {
        return rowCompressedColtMatrix4D.size();
    }
}
