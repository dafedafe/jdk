/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @key randomness
 *
 * @library /test/lib
 * @modules jdk.incubator.vector
 * @run testng/othervm/timeout=300 -ea -esa -Xbatch -XX:-TieredCompilation DoubleMaxVectorTests
 */

// -- This file was mechanically generated: Do not edit! -- //

import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorShuffle;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.Vector;

import jdk.incubator.vector.DoubleVector;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.Integer;
import java.util.List;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Test
public class DoubleMaxVectorTests extends AbstractVectorTest {

    static final VectorSpecies<Double> SPECIES =
                DoubleVector.SPECIES_MAX;

    static final int INVOC_COUNT = Integer.getInteger("jdk.incubator.vector.test.loop-iterations", 100);

    static VectorShape getMaxBit() {
        return VectorShape.S_Max_BIT;
    }

    private static final int Max = 256;  // juts so we can do N/Max

    // for floating point addition reduction ops that may introduce rounding errors
    private static final double RELATIVE_ROUNDING_ERROR_FACTOR_ADD = (double)10.0;

    // for floating point multiplication reduction ops that may introduce rounding errors
    private static final double RELATIVE_ROUNDING_ERROR_FACTOR_MUL = (double)50.0;

    static final int BUFFER_REPS = Integer.getInteger("jdk.incubator.vector.test.buffer-vectors", 25000 / Max);

    static void assertArraysStrictlyEquals(double[] r, double[] a) {
        for (int i = 0; i < a.length; i++) {
            long ir = Double.doubleToRawLongBits(r[i]);
            long ia = Double.doubleToRawLongBits(a[i]);
            if (ir != ia) {
                Assert.fail(String.format("at index #%d, expected = %016X, actual = %016X", i, ia, ir));
            }
        }
    }

    interface FUnOp {
        double apply(double a);
    }

    static void assertArraysEquals(double[] r, double[] a, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i]), "at index #" + i + ", input = " + a[i]);
        }
    }

    interface FUnArrayOp {
        double[] apply(double a);
    }

    static void assertArraysEquals(double[] r, double[] a, FUnArrayOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a[i]));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a[i]);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref, "(ref: " + Arrays.toString(ref)
              + ", res: " + Arrays.toString(res)
              + "), at index #" + i);
        }
    }

    static void assertArraysEquals(double[] r, double[] a, boolean[] mask, FUnOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], mask[i % SPECIES.length()] ? f.apply(a[i]) : a[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], mask[i % SPECIES.length()] ? f.apply(a[i]) : a[i], "at index #" + i + ", input = " + a[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    interface FReductionOp {
        double apply(double[] a, int idx);
    }

    interface FReductionAllOp {
        double apply(double[] a);
    }

    static void assertReductionArraysEquals(double[] r, double rc, double[] a,
                                            FReductionOp f, FReductionAllOp fa) {
        assertReductionArraysEquals(r, rc, a, f, fa, (double)0.0);
    }

    static void assertReductionArraysEquals(double[] r, double rc, double[] a,
                                            FReductionOp f, FReductionAllOp fa,
                                            double relativeErrorFactor) {
        int i = 0;
        try {
            Assert.assertEquals(rc, fa.apply(a), Math.ulp(rc) * relativeErrorFactor);
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(r[i], f.apply(a, i), Math.ulp(r[i]) * relativeErrorFactor);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(rc, fa.apply(a), Math.ulp(rc) * relativeErrorFactor, "Final result is incorrect!");
            Assert.assertEquals(r[i], f.apply(a, i), Math.ulp(r[i]) * relativeErrorFactor, "at index #" + i);
        }
    }

    interface FReductionMaskedOp {
        double apply(double[] a, int idx, boolean[] mask);
    }

    interface FReductionAllMaskedOp {
        double apply(double[] a, boolean[] mask);
    }

    static void assertReductionArraysEqualsMasked(double[] r, double rc, double[] a, boolean[] mask,
                                            FReductionMaskedOp f, FReductionAllMaskedOp fa) {
        assertReductionArraysEqualsMasked(r, rc, a, mask, f, fa, (double)0.0);
    }

    static void assertReductionArraysEqualsMasked(double[] r, double rc, double[] a, boolean[] mask,
                                            FReductionMaskedOp f, FReductionAllMaskedOp fa,
                                            double relativeError) {
        int i = 0;
        try {
            Assert.assertEquals(rc, fa.apply(a, mask), Math.abs(rc * relativeError));
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(r[i], f.apply(a, i, mask), Math.abs(r[i] *
relativeError));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(rc, fa.apply(a, mask), Math.abs(rc * relativeError), "Final result is incorrect!");
            Assert.assertEquals(r[i], f.apply(a, i, mask), Math.abs(r[i] * relativeError), "at index #" + i);
        }
    }

    interface FReductionOpLong {
        long apply(double[] a, int idx);
    }

    interface FReductionAllOpLong {
        long apply(double[] a);
    }

    static void assertReductionLongArraysEquals(long[] r, long rc, double[] a,
                                            FReductionOpLong f, FReductionAllOpLong fa) {
        int i = 0;
        try {
            Assert.assertEquals(rc, fa.apply(a));
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(r[i], f.apply(a, i));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(rc, fa.apply(a), "Final result is incorrect!");
            Assert.assertEquals(r[i], f.apply(a, i), "at index #" + i);
        }
    }

    interface FReductionMaskedOpLong {
        long apply(double[] a, int idx, boolean[] mask);
    }

    interface FReductionAllMaskedOpLong {
        long apply(double[] a, boolean[] mask);
    }

    static void assertReductionLongArraysEqualsMasked(long[] r, long rc, double[] a, boolean[] mask,
                                            FReductionMaskedOpLong f, FReductionAllMaskedOpLong fa) {
        int i = 0;
        try {
            Assert.assertEquals(rc, fa.apply(a, mask));
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(r[i], f.apply(a, i, mask));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(rc, fa.apply(a, mask), "Final result is incorrect!");
            Assert.assertEquals(r[i], f.apply(a, i, mask), "at index #" + i);
        }
    }

    interface FBoolReductionOp {
        boolean apply(boolean[] a, int idx);
    }

    static void assertReductionBoolArraysEquals(boolean[] r, boolean[] a, FBoolReductionOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(r[i], f.apply(a, i));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a, i), "at index #" + i);
        }
    }

    interface FMaskReductionOp {
        int apply(boolean[] a, int idx);
    }

    static void assertMaskReductionArraysEquals(int[] r, boolean[] a, FMaskReductionOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(r[i], f.apply(a, i));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a, i), "at index #" + i);
        }
    }

    static void assertRearrangeArraysEquals(double[] r, double[] a, int[] order, int vector_len) {
        int i = 0, j = 0;
        try {
            for (; i < a.length; i += vector_len) {
                for (j = 0; j < vector_len; j++) {
                    Assert.assertEquals(r[i+j], a[i+order[i+j]]);
                }
            }
        } catch (AssertionError e) {
            int idx = i + j;
            Assert.assertEquals(r[i+j], a[i+order[i+j]], "at index #" + idx + ", input = " + a[i+order[i+j]]);
        }
    }

    static void assertcompressArraysEquals(double[] r, double[] a, boolean[] m, int vector_len) {
        int i = 0, j = 0, k = 0;
        try {
            for (; i < a.length; i += vector_len) {
                k = 0;
                for (j = 0; j < vector_len; j++) {
                    if (m[(i + j) % SPECIES.length()]) {
                        Assert.assertEquals(r[i + k], a[i + j]);
                        k++;
                    }
                }
                for (; k < vector_len; k++) {
                    Assert.assertEquals(r[i + k], (double)0);
                }
            }
        } catch (AssertionError e) {
            int idx = i + k;
            if (m[(i + j) % SPECIES.length()]) {
                Assert.assertEquals(r[idx], a[i + j], "at index #" + idx);
            } else {
                Assert.assertEquals(r[idx], (double)0, "at index #" + idx);
            }
        }
    }

    static void assertexpandArraysEquals(double[] r, double[] a, boolean[] m, int vector_len) {
        int i = 0, j = 0, k = 0;
        try {
            for (; i < a.length; i += vector_len) {
                k = 0;
                for (j = 0; j < vector_len; j++) {
                    if (m[(i + j) % SPECIES.length()]) {
                        Assert.assertEquals(r[i + j], a[i + k]);
                        k++;
                    } else {
                        Assert.assertEquals(r[i + j], (double)0);
                    }
                }
            }
        } catch (AssertionError e) {
            int idx = i + j;
            if (m[idx % SPECIES.length()]) {
                Assert.assertEquals(r[idx], a[i + k], "at index #" + idx);
            } else {
                Assert.assertEquals(r[idx], (double)0, "at index #" + idx);
            }
        }
    }

    static void assertSelectFromTwoVectorEquals(double[] r, double[] order, double[] a, double[] b, int vector_len) {
        int i = 0, j = 0;
        boolean is_exceptional_idx = false;
        int idx = 0, wrapped_index = 0, oidx = 0;
        try {
            for (; i < a.length; i += vector_len) {
                for (j = 0; j < vector_len; j++) {
                    idx = i + j;
                    wrapped_index = Math.floorMod((int)order[idx], 2 * vector_len);
                    is_exceptional_idx = wrapped_index >= vector_len;
                    oidx = is_exceptional_idx ? (wrapped_index - vector_len) : wrapped_index;
                    Assert.assertEquals(r[idx], (is_exceptional_idx ? b[i + oidx] : a[i + oidx]));
                }
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[idx], (is_exceptional_idx ? b[i + oidx] : a[i + oidx]), "at index #" + idx + ", order = " + order[idx] + ", a = " + a[i + oidx] + ", b = " + b[i + oidx]);
        }
    }

    static void assertSelectFromArraysEquals(double[] r, double[] a, double[] order, int vector_len) {
        int i = 0, j = 0;
        try {
            for (; i < a.length; i += vector_len) {
                for (j = 0; j < vector_len; j++) {
                    Assert.assertEquals(r[i+j], a[i+(int)order[i+j]]);
                }
            }
        } catch (AssertionError e) {
            int idx = i + j;
            Assert.assertEquals(r[i+j], a[i+(int)order[i+j]], "at index #" + idx + ", input = " + a[i+(int)order[i+j]]);
        }
    }

    static void assertRearrangeArraysEquals(double[] r, double[] a, int[] order, boolean[] mask, int vector_len) {
        int i = 0, j = 0;
        try {
            for (; i < a.length; i += vector_len) {
                for (j = 0; j < vector_len; j++) {
                    if (mask[j % SPECIES.length()])
                         Assert.assertEquals(r[i+j], a[i+order[i+j]]);
                    else
                         Assert.assertEquals(r[i+j], (double)0);
                }
            }
        } catch (AssertionError e) {
            int idx = i + j;
            if (mask[j % SPECIES.length()])
                Assert.assertEquals(r[i+j], a[i+order[i+j]], "at index #" + idx + ", input = " + a[i+order[i+j]] + ", mask = " + mask[j % SPECIES.length()]);
            else
                Assert.assertEquals(r[i+j], (double)0, "at index #" + idx + ", input = " + a[i+order[i+j]] + ", mask = " + mask[j % SPECIES.length()]);
        }
    }

    static void assertSelectFromArraysEquals(double[] r, double[] a, double[] order, boolean[] mask, int vector_len) {
        int i = 0, j = 0;
        try {
            for (; i < a.length; i += vector_len) {
                for (j = 0; j < vector_len; j++) {
                    if (mask[j % SPECIES.length()])
                         Assert.assertEquals(r[i+j], a[i+(int)order[i+j]]);
                    else
                         Assert.assertEquals(r[i+j], (double)0);
                }
            }
        } catch (AssertionError e) {
            int idx = i + j;
            if (mask[j % SPECIES.length()])
                Assert.assertEquals(r[i+j], a[i+(int)order[i+j]], "at index #" + idx + ", input = " + a[i+(int)order[i+j]] + ", mask = " + mask[j % SPECIES.length()]);
            else
                Assert.assertEquals(r[i+j], (double)0, "at index #" + idx + ", input = " + a[i+(int)order[i+j]] + ", mask = " + mask[j % SPECIES.length()]);
        }
    }

    static void assertBroadcastArraysEquals(double[] r, double[] a) {
        int i = 0;
        for (; i < a.length; i += SPECIES.length()) {
            int idx = i;
            for (int j = idx; j < (idx + SPECIES.length()); j++)
                a[j]=a[idx];
        }

        try {
            for (i = 0; i < a.length; i++) {
                Assert.assertEquals(r[i], a[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], a[i], "at index #" + i + ", input = " + a[i]);
        }
    }

    interface FBinOp {
        double apply(double a, double b);
    }

    interface FBinMaskOp {
        double apply(double a, double b, boolean m);

        static FBinMaskOp lift(FBinOp f) {
            return (a, b, m) -> m ? f.apply(a, b) : a;
        }
    }

    static void assertArraysEqualsAssociative(double[] rl, double[] rr, double[] a, double[] b, double[] c, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                //Left associative
                Assert.assertEquals(rl[i], f.apply(f.apply(a[i], b[i]), c[i]));

                //Right associative
                Assert.assertEquals(rr[i], f.apply(a[i], f.apply(b[i], c[i])));

                //Results equal sanity check
                Assert.assertEquals(rl[i], rr[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(rl[i], f.apply(f.apply(a[i], b[i]), c[i]), "left associative test at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", input3 = " + c[i]);
            Assert.assertEquals(rr[i], f.apply(a[i], f.apply(b[i], c[i])), "right associative test at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", input3 = " + c[i]);
            Assert.assertEquals(rl[i], rr[i], "Result checks not equal at index #" + i + "leftRes = " + rl[i] + ", rightRes = " + rr[i]);
        }
    }

   static void assertArraysEqualsAssociative(double[] rl, double[] rr, double[] a, double[] b, double[] c, boolean[] mask, FBinOp f) {
       assertArraysEqualsAssociative(rl, rr, a, b, c, mask, FBinMaskOp.lift(f));
   }

    static void assertArraysEqualsAssociative(double[] rl, double[] rr, double[] a, double[] b, double[] c, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        boolean mask_bit = false;
        try {
            for (; i < a.length; i++) {
                mask_bit = mask[i % SPECIES.length()];
                //Left associative
                Assert.assertEquals(rl[i], f.apply(f.apply(a[i], b[i], mask_bit), c[i], mask_bit));

                //Right associative
                Assert.assertEquals(rr[i], f.apply(a[i], f.apply(b[i], c[i], mask_bit), mask_bit));

                //Results equal sanity check
                Assert.assertEquals(rl[i], rr[i]);
            }
        } catch (AssertionError e) {
            Assert.assertEquals(rl[i], f.apply(f.apply(a[i], b[i], mask_bit), c[i], mask_bit), "left associative masked test at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", input3 = " + c[i] + ", mask = " + mask_bit);
            Assert.assertEquals(rr[i], f.apply(a[i], f.apply(b[i], c[i], mask_bit), mask_bit), "right associative masked test at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", input3 = " + c[i] + ", mask = " + mask_bit);
            Assert.assertEquals(rl[i], rr[i], "Result checks not equal at index #" + i + "leftRes = " + rl[i] + ", rightRes = " + rr[i]);
        }
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i]), "(" + a[i] + ", " + b[i] + ") at index #" + i);
        }
    }

    static void assertArraysEquals(double[] r, double[] a, double b, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b), "(" + a[i] + ", " + b + ") at index #" + i);
        }
    }

    static void assertBroadcastArraysEquals(double[] r, double[] a, double[] b, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()]),
                                "(" + a[i] + ", " + b[(i / SPECIES.length()) * SPECIES.length()] + ") at index #" + i);
        }
    }

    static void assertBroadcastLongArraysEquals(double[] r, double[] a, double[] b, FBinOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], (double)((long)b[(i / SPECIES.length()) * SPECIES.length()])));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], (double)((long)b[(i / SPECIES.length()) * SPECIES.length()])),
                                "(" + a[i] + ", " + b[(i / SPECIES.length()) * SPECIES.length()] + ") at index #" + i);
        }
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, boolean[] mask, FBinOp f) {
        assertArraysEquals(r, a, b, mask, FBinMaskOp.lift(f));
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static void assertArraysEquals(double[] r, double[] a, double b, boolean[] mask, FBinOp f) {
        assertArraysEquals(r, a, b, mask, FBinMaskOp.lift(f));
    }

    static void assertArraysEquals(double[] r, double[] a, double b, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b, mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b, mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static void assertBroadcastArraysEquals(double[] r, double[] a, double[] b, boolean[] mask, FBinOp f) {
        assertBroadcastArraysEquals(r, a, b, mask, FBinMaskOp.lift(f));
    }

    static void assertBroadcastArraysEquals(double[] r, double[] a, double[] b, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()],
                                mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] +
                                ", input2 = " + b[(i / SPECIES.length()) * SPECIES.length()] + ", mask = " +
                                mask[i % SPECIES.length()]);
        }
    }

    static void assertBroadcastLongArraysEquals(double[] r, double[] a, double[] b, boolean[] mask, FBinOp f) {
        assertBroadcastLongArraysEquals(r, a, b, mask, FBinMaskOp.lift(f));
    }

    static void assertBroadcastLongArraysEquals(double[] r, double[] a, double[] b, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], (double)((long)b[(i / SPECIES.length()) * SPECIES.length()]), mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], (double)((long)b[(i / SPECIES.length()) * SPECIES.length()]),
                                mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] +
                                ", input2 = " + b[(i / SPECIES.length()) * SPECIES.length()] + ", mask = " +
                                mask[i % SPECIES.length()]);
        }
    }

    static void assertShiftArraysEquals(double[] r, double[] a, double[] b, FBinOp f) {
        int i = 0;
        int j = 0;
        try {
            for (; j < a.length; j += SPECIES.length()) {
                for (i = 0; i < SPECIES.length(); i++) {
                    Assert.assertEquals(r[i+j], f.apply(a[i+j], b[j]));
                }
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i+j], f.apply(a[i+j], b[j]), "at index #" + i + ", " + j);
        }
    }

    static void assertShiftArraysEquals(double[] r, double[] a, double[] b, boolean[] mask, FBinOp f) {
        assertShiftArraysEquals(r, a, b, mask, FBinMaskOp.lift(f));
    }

    static void assertShiftArraysEquals(double[] r, double[] a, double[] b, boolean[] mask, FBinMaskOp f) {
        int i = 0;
        int j = 0;
        try {
            for (; j < a.length; j += SPECIES.length()) {
                for (i = 0; i < SPECIES.length(); i++) {
                    Assert.assertEquals(r[i+j], f.apply(a[i+j], b[j], mask[i]));
                }
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i+j], f.apply(a[i+j], b[j], mask[i]), "at index #" + i + ", input1 = " + a[i+j] + ", input2 = " + b[j] + ", mask = " + mask[i]);
        }
    }

    interface FBinConstOp {
        double apply(double a);
    }

    interface FBinConstMaskOp {
        double apply(double a, boolean m);

        static FBinConstMaskOp lift(FBinConstOp f) {
            return (a, m) -> m ? f.apply(a) : a;
        }
    }

    static void assertShiftConstEquals(double[] r, double[] a, FBinConstOp f) {
        int i = 0;
        int j = 0;
        try {
            for (; j < a.length; j += SPECIES.length()) {
                for (i = 0; i < SPECIES.length(); i++) {
                    Assert.assertEquals(r[i+j], f.apply(a[i+j]));
                }
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i+j], f.apply(a[i+j]), "at index #" + i + ", " + j);
        }
    }

    static void assertShiftConstEquals(double[] r, double[] a, boolean[] mask, FBinConstOp f) {
        assertShiftConstEquals(r, a, mask, FBinConstMaskOp.lift(f));
    }

    static void assertShiftConstEquals(double[] r, double[] a, boolean[] mask, FBinConstMaskOp f) {
        int i = 0;
        int j = 0;
        try {
            for (; j < a.length; j += SPECIES.length()) {
                for (i = 0; i < SPECIES.length(); i++) {
                    Assert.assertEquals(r[i+j], f.apply(a[i+j], mask[i]));
                }
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i+j], f.apply(a[i+j], mask[i]), "at index #" + i + ", input1 = " + a[i+j] + ", mask = " + mask[i]);
        }
    }

    interface FTernOp {
        double apply(double a, double b, double c);
    }

    interface FTernMaskOp {
        double apply(double a, double b, double c, boolean m);

        static FTernMaskOp lift(FTernOp f) {
            return (a, b, c, m) -> m ? f.apply(a, b, c) : a;
        }
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, double[] c, FTernOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", input3 = " + c[i]);
        }
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, double[] c, boolean[] mask, FTernOp f) {
        assertArraysEquals(r, a, b, c, mask, FTernMaskOp.lift(f));
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, double[] c, boolean[] mask, FTernMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], c[i], mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = "
              + b[i] + ", input3 = " + c[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static void assertBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, FTernOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], c[(i / SPECIES.length()) * SPECIES.length()]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], c[(i / SPECIES.length()) * SPECIES.length()]), "at index #" +
                                i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", input3 = " +
                                c[(i / SPECIES.length()) * SPECIES.length()]);
        }
    }

    static void assertAltBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, FTernOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()], c[i]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()], c[i]), "at index #" +
                                i + ", input1 = " + a[i] + ", input2 = " +
                                b[(i / SPECIES.length()) * SPECIES.length()] + ",  input3 = " + c[i]);
        }
    }

    static void assertBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, boolean[] mask,
                                            FTernOp f) {
        assertBroadcastArraysEquals(r, a, b, c, mask, FTernMaskOp.lift(f));
    }

    static void assertBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, boolean[] mask,
                                            FTernMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[i], c[(i / SPECIES.length()) * SPECIES.length()],
                                    mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[i], c[(i / SPECIES.length()) * SPECIES.length()],
                                mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] + ", input2 = " +
                                b[i] + ", input3 = " + c[(i / SPECIES.length()) * SPECIES.length()] + ", mask = " +
                                mask[i % SPECIES.length()]);
        }
    }

    static void assertAltBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, boolean[] mask,
                                            FTernOp f) {
        assertAltBroadcastArraysEquals(r, a, b, c, mask, FTernMaskOp.lift(f));
    }

    static void assertAltBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, boolean[] mask,
                                            FTernMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()], c[i],
                                    mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()], c[i],
                                mask[i % SPECIES.length()]), "at index #" + i + ", input1 = " + a[i] +
                                ", input2 = " + b[(i / SPECIES.length()) * SPECIES.length()] +
                                ", input3 = " + c[i] + ", mask = " + mask[i % SPECIES.length()]);
        }
    }

    static void assertDoubleBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, FTernOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()],
                                    c[(i / SPECIES.length()) * SPECIES.length()]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()],
                                c[(i / SPECIES.length()) * SPECIES.length()]), "at index #" + i + ", input1 = " + a[i]
                                + ", input2 = " + b[(i / SPECIES.length()) * SPECIES.length()] + ", input3 = " +
                                c[(i / SPECIES.length()) * SPECIES.length()]);
        }
    }

    static void assertDoubleBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, boolean[] mask,
                                                  FTernOp f) {
        assertDoubleBroadcastArraysEquals(r, a, b, c, mask, FTernMaskOp.lift(f));
    }

    static void assertDoubleBroadcastArraysEquals(double[] r, double[] a, double[] b, double[] c, boolean[] mask,
                                                  FTernMaskOp f) {
        int i = 0;
        try {
            for (; i < a.length; i++) {
                Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()],
                                    c[(i / SPECIES.length()) * SPECIES.length()], mask[i % SPECIES.length()]));
            }
        } catch (AssertionError err) {
            Assert.assertEquals(r[i], f.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()],
                                c[(i / SPECIES.length()) * SPECIES.length()], mask[i % SPECIES.length()]), "at index #"
                                + i + ", input1 = " + a[i] + ", input2 = " + b[(i / SPECIES.length()) * SPECIES.length()] +
                                ", input3 = " + c[(i / SPECIES.length()) * SPECIES.length()] + ", mask = " +
                                mask[i % SPECIES.length()]);
        }
    }


    static boolean isWithin1Ulp(double actual, double expected) {
        if (Double.isNaN(expected) && !Double.isNaN(actual)) {
            return false;
        } else if (!Double.isNaN(expected) && Double.isNaN(actual)) {
            return false;
        }

        double low = Math.nextDown(expected);
        double high = Math.nextUp(expected);

        if (Double.compare(low, expected) > 0) {
            return false;
        }

        if (Double.compare(high, expected) < 0) {
            return false;
        }

        return true;
    }

    static void assertArraysEqualsWithinOneUlp(double[] r, double[] a, FUnOp mathf, FUnOp strictmathf) {
        int i = 0;
        try {
            // Check that result is within 1 ulp of strict math or equivalent to math implementation.
            for (; i < a.length; i++) {
                Assert.assertTrue(Double.compare(r[i], mathf.apply(a[i])) == 0 ||
                                    isWithin1Ulp(r[i], strictmathf.apply(a[i])));
            }
        } catch (AssertionError e) {
            Assert.assertTrue(Double.compare(r[i], mathf.apply(a[i])) == 0, "at index #" + i + ", input = " + a[i] + ", actual = " + r[i] + ", expected = " + mathf.apply(a[i]));
            Assert.assertTrue(isWithin1Ulp(r[i], strictmathf.apply(a[i])), "at index #" + i + ", input = " + a[i] + ", actual = " + r[i] + ", expected (within 1 ulp) = " + strictmathf.apply(a[i]));
        }
    }

    static void assertArraysEqualsWithinOneUlp(double[] r, double[] a, double[] b, FBinOp mathf, FBinOp strictmathf) {
        int i = 0;
        try {
            // Check that result is within 1 ulp of strict math or equivalent to math implementation.
            for (; i < a.length; i++) {
                Assert.assertTrue(Double.compare(r[i], mathf.apply(a[i], b[i])) == 0 ||
                                    isWithin1Ulp(r[i], strictmathf.apply(a[i], b[i])));
            }
        } catch (AssertionError e) {
            Assert.assertTrue(Double.compare(r[i], mathf.apply(a[i], b[i])) == 0, "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", actual = " + r[i] + ", expected = " + mathf.apply(a[i], b[i]));
            Assert.assertTrue(isWithin1Ulp(r[i], strictmathf.apply(a[i], b[i])), "at index #" + i + ", input1 = " + a[i] + ", input2 = " + b[i] + ", actual = " + r[i] + ", expected (within 1 ulp) = " + strictmathf.apply(a[i], b[i]));
        }
    }

    static void assertBroadcastArraysEqualsWithinOneUlp(double[] r, double[] a, double[] b,
                                                        FBinOp mathf, FBinOp strictmathf) {
        int i = 0;
        try {
            // Check that result is within 1 ulp of strict math or equivalent to math implementation.
            for (; i < a.length; i++) {
                Assert.assertTrue(Double.compare(r[i],
                                  mathf.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()])) == 0 ||
                                  isWithin1Ulp(r[i],
                                  strictmathf.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()])));
            }
        } catch (AssertionError e) {
            Assert.assertTrue(Double.compare(r[i],
                              mathf.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()])) == 0,
                              "at index #" + i + ", input1 = " + a[i] + ", input2 = " +
                              b[(i / SPECIES.length()) * SPECIES.length()] + ", actual = " + r[i] +
                              ", expected = " + mathf.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()]));
            Assert.assertTrue(isWithin1Ulp(r[i],
                              strictmathf.apply(a[i], b[(i / SPECIES.length()) * SPECIES.length()])),
                             "at index #" + i + ", input1 = " + a[i] + ", input2 = " +
                             b[(i / SPECIES.length()) * SPECIES.length()] + ", actual = " + r[i] +
                             ", expected (within 1 ulp) = " + strictmathf.apply(a[i],
                             b[(i / SPECIES.length()) * SPECIES.length()]));
        }
    }

    interface FGatherScatterOp {
        double[] apply(double[] a, int ix, int[] b, int iy);
    }

    static void assertArraysEquals(double[] r, double[] a, int[] b, FGatherScatterOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, i, b, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a, i, b, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref,
              "(ref: " + Arrays.toString(ref) + ", res: " + Arrays.toString(res) + ", a: "
              + Arrays.toString(Arrays.copyOfRange(a, i, i+SPECIES.length()))
              + ", b: "
              + Arrays.toString(Arrays.copyOfRange(b, i, i+SPECIES.length()))
              + " at index #" + i);
        }
    }

    interface FGatherMaskedOp {
        double[] apply(double[] a, int ix, boolean[] mask, int[] b, int iy);
    }

    interface FScatterMaskedOp {
        double[] apply(double[] r, double[] a, int ix, boolean[] mask, int[] b, int iy);
    }

    static void assertArraysEquals(double[] r, double[] a, int[] b, boolean[] mask, FGatherMaskedOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, i, mask, b, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a, i, mask, b, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref,
              "(ref: " + Arrays.toString(ref) + ", res: " + Arrays.toString(res) + ", a: "
              + Arrays.toString(Arrays.copyOfRange(a, i, i+SPECIES.length()))
              + ", b: "
              + Arrays.toString(Arrays.copyOfRange(b, i, i+SPECIES.length()))
              + ", mask: "
              + Arrays.toString(mask)
              + " at index #" + i);
        }
    }

    static void assertArraysEquals(double[] r, double[] a, int[] b, boolean[] mask, FScatterMaskedOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(r, a, i, mask, b, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(r, a, i, mask, b, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref,
              "(ref: " + Arrays.toString(ref) + ", res: " + Arrays.toString(res) + ", a: "
              + Arrays.toString(Arrays.copyOfRange(a, i, i+SPECIES.length()))
              + ", b: "
              + Arrays.toString(Arrays.copyOfRange(b, i, i+SPECIES.length()))
              + ", r: "
              + Arrays.toString(Arrays.copyOfRange(r, i, i+SPECIES.length()))
              + ", mask: "
              + Arrays.toString(mask)
              + " at index #" + i);
        }
    }

    interface FLaneOp {
        double[] apply(double[] a, int origin, int idx);
    }

    static void assertArraysEquals(double[] r, double[] a, int origin, FLaneOp f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, origin, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a, origin, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref, "(ref: " + Arrays.toString(ref)
              + ", res: " + Arrays.toString(res)
              + "), at index #" + i);
        }
    }

    interface FLaneBop {
        double[] apply(double[] a, double[] b, int origin, int idx);
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, int origin, FLaneBop f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, b, origin, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a, b, origin, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref, "(ref: " + Arrays.toString(ref)
              + ", res: " + Arrays.toString(res)
              + "), at index #" + i
              + ", at origin #" + origin);
        }
    }

    interface FLaneMaskedBop {
        double[] apply(double[] a, double[] b, int origin, boolean[] mask, int idx);
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, int origin, boolean[] mask, FLaneMaskedBop f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, b, origin, mask, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a, b, origin, mask, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref, "(ref: " + Arrays.toString(ref)
              + ", res: " + Arrays.toString(res)
              + "), at index #" + i
              + ", at origin #" + origin);
        }
    }

    interface FLanePartBop {
        double[] apply(double[] a, double[] b, int origin, int part, int idx);
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, int origin, int part, FLanePartBop f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, b, origin, part, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a, b, origin, part, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref, "(ref: " + Arrays.toString(ref)
              + ", res: " + Arrays.toString(res)
              + "), at index #" + i
              + ", at origin #" + origin
              + ", with part #" + part);
        }
    }

    interface FLanePartMaskedBop {
        double[] apply(double[] a, double[] b, int origin, int part, boolean[] mask, int idx);
    }

    static void assertArraysEquals(double[] r, double[] a, double[] b, int origin, int part, boolean[] mask, FLanePartMaskedBop f) {
        int i = 0;
        try {
            for (; i < a.length; i += SPECIES.length()) {
                Assert.assertEquals(Arrays.copyOfRange(r, i, i+SPECIES.length()),
                  f.apply(a, b, origin, part, mask, i));
            }
        } catch (AssertionError e) {
            double[] ref = f.apply(a, b, origin, part, mask, i);
            double[] res = Arrays.copyOfRange(r, i, i+SPECIES.length());
            Assert.assertEquals(res, ref, "(ref: " + Arrays.toString(ref)
              + ", res: " + Arrays.toString(res)
              + "), at index #" + i
              + ", at origin #" + origin
              + ", with part #" + part);
        }
    }

    static int intCornerCaseValue(int i) {
        switch(i % 5) {
            case 0:
                return Integer.MAX_VALUE;
            case 1:
                return Integer.MIN_VALUE;
            case 2:
                return Integer.MIN_VALUE;
            case 3:
                return Integer.MAX_VALUE;
            default:
                return (int)0;
        }
    }

    static final List<IntFunction<double[]>> INT_DOUBLE_GENERATORS = List.of(
            withToString("double[-i * 5]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(-i * 5));
            }),
            withToString("double[i * 5]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(i * 5));
            }),
            withToString("double[i + 1]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (((double)(i + 1) == 0) ? 1 : (double)(i + 1)));
            }),
            withToString("double[intCornerCaseValue(i)]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)intCornerCaseValue(i));
            })
    );

    static void assertArraysEquals(int[] r, double[] a, int offs) {
        int i = 0;
        try {
            for (; i < r.length; i++) {
                Assert.assertEquals(r[i], (int)(a[i+offs]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], (int)(a[i+offs]), "at index #" + i + ", input = " + a[i+offs]);
        }
    }

    static long longCornerCaseValue(int i) {
        switch(i % 5) {
            case 0:
                return Long.MAX_VALUE;
            case 1:
                return Long.MIN_VALUE;
            case 2:
                return Long.MIN_VALUE;
            case 3:
                return Long.MAX_VALUE;
            default:
                return (long)0;
        }
    }

    static final List<IntFunction<double[]>> LONG_DOUBLE_GENERATORS = List.of(
            withToString("double[-i * 5]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(-i * 5));
            }),
            withToString("double[i * 5]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(i * 5));
            }),
            withToString("double[i + 1]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (((double)(i + 1) == 0) ? 1 : (double)(i + 1)));
            }),
            withToString("double[cornerCaseValue(i)]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)longCornerCaseValue(i));
            })
    );


    static void assertArraysEquals(long[] r, double[] a, int offs) {
        int i = 0;
        try {
            for (; i < r.length; i++) {
                Assert.assertEquals(r[i], (long)(a[i+offs]));
            }
        } catch (AssertionError e) {
            Assert.assertEquals(r[i], (long)(a[i+offs]), "at index #" + i + ", input = " + a[i+offs]);
        }
    }

    static long bits(double e) {
        return  Double.doubleToLongBits(e);
    }

    static final List<IntFunction<double[]>> DOUBLE_GENERATORS = List.of(
            withToString("double[-i * 5]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(-i * 5));
            }),
            withToString("double[i * 5]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(i * 5));
            }),
            withToString("double[i + 1]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (((double)(i + 1) == 0) ? 1 : (double)(i + 1)));
            }),
            withToString("double[0.01 + (i / (i + 1))]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)0.01 + ((double)i / (i + 1)));
            }),
            withToString("double[i -> i % 17 == 0 ? cornerCaseValue(i) : 0.01 + (i / (i + 1))]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> i % 17 == 0 ? cornerCaseValue(i) : (double)0.01 + ((double)i / (i + 1)));
            }),
            withToString("double[cornerCaseValue(i)]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> cornerCaseValue(i));
            })
    );

    // Create combinations of pairs
    // @@@ Might be sensitive to order e.g. div by 0
    static final List<List<IntFunction<double[]>>> DOUBLE_GENERATOR_PAIRS =
        Stream.of(DOUBLE_GENERATORS.get(0)).
                flatMap(fa -> DOUBLE_GENERATORS.stream().skip(1).map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] boolUnaryOpProvider() {
        return BOOL_ARRAY_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static final List<List<IntFunction<double[]>>> DOUBLE_GENERATOR_TRIPLES =
        DOUBLE_GENERATOR_PAIRS.stream().
                flatMap(pair -> DOUBLE_GENERATORS.stream().map(f -> List.of(pair.get(0), pair.get(1), f))).
                collect(Collectors.toList());

    static final List<IntFunction<double[]>> SELECT_FROM_INDEX_GENERATORS = List.of(
            withToString("double[0..VECLEN*2)", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(RAND.nextInt()));
            })
    );

    static final List<List<IntFunction<double[]>>> DOUBLE_GENERATOR_SELECT_FROM_TRIPLES =
        DOUBLE_GENERATOR_PAIRS.stream().
                flatMap(pair -> SELECT_FROM_INDEX_GENERATORS.stream().map(f -> List.of(pair.get(0), pair.get(1), f))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] doubleBinaryOpProvider() {
        return DOUBLE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleIndexedOpProvider() {
        return DOUBLE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleBinaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATOR_PAIRS.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleTernaryOpProvider() {
        return DOUBLE_GENERATOR_TRIPLES.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleSelectFromTwoVectorOpProvider() {
        return DOUBLE_GENERATOR_SELECT_FROM_TRIPLES.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleTernaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATOR_TRIPLES.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpProvider() {
        return DOUBLE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fm};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubletoIntUnaryOpProvider() {
        return INT_DOUBLE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubletoLongUnaryOpProvider() {
        return LONG_DOUBLE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] maskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] maskCompareOpProvider() {
        return BOOLEAN_MASK_COMPARE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] shuffleProvider() {
        return INT_SHUFFLE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] shuffleCompareOpProvider() {
        return INT_SHUFFLE_COMPARE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpShuffleProvider() {
        return INT_SHUFFLE_GENERATORS.stream().
                flatMap(fs -> DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpShuffleMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> INT_SHUFFLE_GENERATORS.stream().
                    flatMap(fs -> DOUBLE_GENERATORS.stream().map(fa -> {
                        return new Object[] {fa, fs, fm};
                }))).
                toArray(Object[][]::new);
    }

    static final List<BiFunction<Integer,Integer,double[]>> DOUBLE_SHUFFLE_GENERATORS = List.of(
            withToStringBi("shuffle[random]", (Integer l, Integer m) -> {
                double[] a = new double[l];
                int upper = m;
                for (int i = 0; i < 1; i++) {
                    a[i] = (double)RAND.nextInt(upper);
                }
                return a;
            })
    );

    @DataProvider
    public Object[][] doubleUnaryOpSelectFromProvider() {
        return DOUBLE_SHUFFLE_GENERATORS.stream().
                flatMap(fs -> DOUBLE_GENERATORS.stream().map(fa -> {
                    return new Object[] {fa, fs};
                })).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleUnaryOpSelectFromMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_SHUFFLE_GENERATORS.stream().
                    flatMap(fs -> DOUBLE_GENERATORS.stream().map(fa -> {
                        return new Object[] {fa, fs, fm};
                }))).
                toArray(Object[][]::new);
    }

    static final List<IntFunction<double[]>> DOUBLE_COMPARE_GENERATORS = List.of(
            withToString("double[i]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)i);
            }),
            withToString("double[i - length / 2]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(i - (s * BUFFER_REPS / 2)));
            }),
            withToString("double[i + 1]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(i + 1));
            }),
            withToString("double[i - 2]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> (double)(i - 2));
            }),
            withToString("double[zigZag(i)]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> i%3 == 0 ? (double)i : (i%3 == 1 ? (double)(i + 1) : (double)(i - 2)));
            }),
            withToString("double[cornerCaseValue(i)]", (int s) -> {
                return fill(s * BUFFER_REPS,
                            i -> cornerCaseValue(i));
            })
    );

    static final List<List<IntFunction<double[]>>> DOUBLE_TEST_GENERATOR_ARGS =
        DOUBLE_COMPARE_GENERATORS.stream().
                map(fa -> List.of(fa)).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] doubleTestOpProvider() {
        return DOUBLE_TEST_GENERATOR_ARGS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleTestOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_TEST_GENERATOR_ARGS.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    static final List<List<IntFunction<double[]>>> DOUBLE_COMPARE_GENERATOR_PAIRS =
        DOUBLE_COMPARE_GENERATORS.stream().
                flatMap(fa -> DOUBLE_COMPARE_GENERATORS.stream().map(fb -> List.of(fa, fb))).
                collect(Collectors.toList());

    @DataProvider
    public Object[][] doubleCompareOpProvider() {
        return DOUBLE_COMPARE_GENERATOR_PAIRS.stream().map(List::toArray).
                toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] doubleCompareOpMaskProvider() {
        return BOOLEAN_MASK_GENERATORS.stream().
                flatMap(fm -> DOUBLE_COMPARE_GENERATOR_PAIRS.stream().map(lfa -> {
                    return Stream.concat(lfa.stream(), Stream.of(fm)).toArray();
                })).
                toArray(Object[][]::new);
    }

    interface ToDoubleF {
        double apply(int i);
    }

    static double[] fill(int s , ToDoubleF f) {
        return fill(new double[s], f);
    }

    static double[] fill(double[] a, ToDoubleF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    static double cornerCaseValue(int i) {
        return switch(i % 8) {
            case 0  -> Double.MAX_VALUE;
            case 1  -> Double.MIN_VALUE;
            case 2  -> Double.NEGATIVE_INFINITY;
            case 3  -> Double.POSITIVE_INFINITY;
            case 4  -> Double.NaN;
            case 5  -> Double.longBitsToDouble(0x7FF123456789ABCDL);
            case 6  -> (double)0.0;
            default -> (double)-0.0;
        };
    }

    static final IntFunction<double[]> fr = (vl) -> {
        int length = BUFFER_REPS * vl;
        return new double[length];
    };

    static final IntFunction<boolean[]> fmr = (vl) -> {
        int length = BUFFER_REPS * vl;
        return new boolean[length];
    };

    static final IntFunction<long[]> lfr = (vl) -> {
        int length = BUFFER_REPS * vl;
        return new long[length];
    };

    static boolean eq(double a, double b) {
        return a == b;
    }

    static boolean neq(double a, double b) {
        return a != b;
    }

    static boolean lt(double a, double b) {
        return a < b;
    }

    static boolean le(double a, double b) {
        return a <= b;
    }

    static boolean gt(double a, double b) {
        return a > b;
    }

    static boolean ge(double a, double b) {
        return a >= b;
    }

    static double firstNonZero(double a, double b) {
        return Double.compare(a, (double) 0) != 0 ? a : b;
    }

    @Test
    static void smokeTest1() {
        DoubleVector three = DoubleVector.broadcast(SPECIES, (byte)-3);
        DoubleVector three2 = (DoubleVector) SPECIES.broadcast(-3);
        assert(three.eq(three2).allTrue());
        DoubleVector three3 = three2.broadcast(1).broadcast(-3);
        assert(three.eq(three3).allTrue());
        int scale = 2;
        Class<?> ETYPE = double.class;
        if (ETYPE == double.class || ETYPE == long.class)
            scale = 1000000;
        else if (ETYPE == byte.class && SPECIES.length() >= 64)
            scale = 1;
        DoubleVector higher = three.addIndex(scale);
        VectorMask<Double> m = three.compare(VectorOperators.LE, higher);
        assert(m.allTrue());
        m = higher.min((double)-1).test(VectorOperators.IS_NEGATIVE);
        assert(m.allTrue());
        m = higher.test(VectorOperators.IS_FINITE);
        assert(m.allTrue());
        double max = higher.reduceLanes(VectorOperators.MAX);
        assert(max == -3 + scale * (SPECIES.length()-1));
    }

    private static double[]
    bothToArray(DoubleVector a, DoubleVector b) {
        double[] r = new double[a.length() + b.length()];
        a.intoArray(r, 0);
        b.intoArray(r, a.length());
        return r;
    }

    @Test
    static void smokeTest2() {
        // Do some zipping and shuffling.
        DoubleVector io = (DoubleVector) SPECIES.broadcast(0).addIndex(1);
        DoubleVector io2 = (DoubleVector) VectorShuffle.iota(SPECIES,0,1,false).toVector();
        Assert.assertEquals(io, io2);
        DoubleVector a = io.add((double)1); //[1,2]
        DoubleVector b = a.neg();  //[-1,-2]
        double[] abValues = bothToArray(a,b); //[1,2,-1,-2]
        VectorShuffle<Double> zip0 = VectorShuffle.makeZip(SPECIES, 0);
        VectorShuffle<Double> zip1 = VectorShuffle.makeZip(SPECIES, 1);
        DoubleVector zab0 = a.rearrange(zip0,b); //[1,-1]
        DoubleVector zab1 = a.rearrange(zip1,b); //[2,-2]
        double[] zabValues = bothToArray(zab0, zab1); //[1,-1,2,-2]
        // manually zip
        double[] manual = new double[zabValues.length];
        for (int i = 0; i < manual.length; i += 2) {
            manual[i+0] = abValues[i/2];
            manual[i+1] = abValues[a.length() + i/2];
        }
        Assert.assertEquals(Arrays.toString(zabValues), Arrays.toString(manual));
        VectorShuffle<Double> unz0 = VectorShuffle.makeUnzip(SPECIES, 0);
        VectorShuffle<Double> unz1 = VectorShuffle.makeUnzip(SPECIES, 1);
        DoubleVector uab0 = zab0.rearrange(unz0,zab1);
        DoubleVector uab1 = zab0.rearrange(unz1,zab1);
        double[] abValues1 = bothToArray(uab0, uab1);
        Assert.assertEquals(Arrays.toString(abValues), Arrays.toString(abValues1));
    }

    static void iotaShuffle() {
        DoubleVector io = (DoubleVector) SPECIES.broadcast(0).addIndex(1);
        DoubleVector io2 = (DoubleVector) VectorShuffle.iota(SPECIES, 0 , 1, false).toVector();
        Assert.assertEquals(io, io2);
    }

    @Test
    // Test all shuffle related operations.
    static void shuffleTest() {
        // To test backend instructions, make sure that C2 is used.
        for (int loop = 0; loop < INVOC_COUNT * INVOC_COUNT; loop++) {
            iotaShuffle();
        }
    }

    @Test
    void viewAsIntegeralLanesTest() {
        Vector<?> asIntegral = SPECIES.zero().viewAsIntegralLanes();
        VectorSpecies<?> asIntegralSpecies = asIntegral.species();
        Assert.assertNotEquals(asIntegralSpecies.elementType(), SPECIES.elementType());
        Assert.assertEquals(asIntegralSpecies.vectorShape(), SPECIES.vectorShape());
        Assert.assertEquals(asIntegralSpecies.length(), SPECIES.length());
        Assert.assertEquals(asIntegral.viewAsFloatingLanes().species(), SPECIES);
    }

    @Test
    void viewAsFloatingLanesTest() {
        Vector<?> asFloating = SPECIES.zero().viewAsFloatingLanes();
        Assert.assertEquals(asFloating.species(), SPECIES);
    }

    static double ADD(double a, double b) {
        return (double)(a + b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void ADDDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.ADD, bv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::ADD);
    }

    static double add(double a, double b) {
        return (double)(a + b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void addDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.add(bv).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::add);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void ADDDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.ADD, bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::ADD);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void addDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.add(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::add);
    }

    static double SUB(double a, double b) {
        return (double)(a - b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void SUBDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.SUB, bv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::SUB);
    }

    static double sub(double a, double b) {
        return (double)(a - b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void subDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.sub(bv).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::sub);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void SUBDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.SUB, bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::SUB);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void subDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.sub(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::sub);
    }

    static double MUL(double a, double b) {
        return (double)(a * b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void MULDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.MUL, bv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::MUL);
    }

    static double mul(double a, double b) {
        return (double)(a * b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void mulDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.mul(bv).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::mul);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void MULDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.MUL, bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::MUL);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void mulDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.mul(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::mul);
    }

    static double DIV(double a, double b) {
        return (double)(a / b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void DIVDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.DIV, bv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::DIV);
    }

    static double div(double a, double b) {
        return (double)(a / b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void divDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.div(bv).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::div);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void DIVDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.DIV, bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::DIV);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void divDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.div(bv, vmask).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::div);
    }

    static double FIRST_NONZERO(double a, double b) {
        return (double)(Double.doubleToLongBits(a)!=0?a:b);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void FIRST_NONZERODoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.FIRST_NONZERO, bv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::FIRST_NONZERO);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void FIRST_NONZERODoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.FIRST_NONZERO, bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::FIRST_NONZERO);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void addDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.add(b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, DoubleMaxVectorTests::add);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void addDoubleMaxVectorTestsBroadcastMaskedSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.add(b[i], vmask).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, mask, DoubleMaxVectorTests::add);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void subDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.sub(b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, DoubleMaxVectorTests::sub);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void subDoubleMaxVectorTestsBroadcastMaskedSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.sub(b[i], vmask).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, mask, DoubleMaxVectorTests::sub);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void mulDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.mul(b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, DoubleMaxVectorTests::mul);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void mulDoubleMaxVectorTestsBroadcastMaskedSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.mul(b[i], vmask).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, mask, DoubleMaxVectorTests::mul);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void divDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.div(b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, DoubleMaxVectorTests::div);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void divDoubleMaxVectorTestsBroadcastMaskedSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.div(b[i], vmask).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, mask, DoubleMaxVectorTests::div);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void ADDDoubleMaxVectorTestsBroadcastLongSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.lanewise(VectorOperators.ADD, (long)b[i]).intoArray(r, i);
        }

        assertBroadcastLongArraysEquals(r, a, b, DoubleMaxVectorTests::ADD);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void ADDDoubleMaxVectorTestsBroadcastMaskedLongSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.lanewise(VectorOperators.ADD, (long)b[i], vmask).intoArray(r, i);
        }

        assertBroadcastLongArraysEquals(r, a, b, mask, DoubleMaxVectorTests::ADD);
    }

    static DoubleVector bv_MIN = DoubleVector.broadcast(SPECIES, (double)10);

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void MINDoubleMaxVectorTestsWithMemOp(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.MIN, bv_MIN).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, (double)10, DoubleMaxVectorTests::MIN);
    }

    static DoubleVector bv_min = DoubleVector.broadcast(SPECIES, (double)10);

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void minDoubleMaxVectorTestsWithMemOp(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.min(bv_min).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, (double)10, DoubleMaxVectorTests::min);
    }

    static DoubleVector bv_MIN_M = DoubleVector.broadcast(SPECIES, (double)10);

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void MINDoubleMaxVectorTestsMaskedWithMemOp(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.MIN, bv_MIN_M, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, (double)10, mask, DoubleMaxVectorTests::MIN);
    }

    static DoubleVector bv_MAX = DoubleVector.broadcast(SPECIES, (double)10);

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void MAXDoubleMaxVectorTestsWithMemOp(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.MAX, bv_MAX).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, (double)10, DoubleMaxVectorTests::MAX);
    }

    static DoubleVector bv_max = DoubleVector.broadcast(SPECIES, (double)10);

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void maxDoubleMaxVectorTestsWithMemOp(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.max(bv_max).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, (double)10, DoubleMaxVectorTests::max);
    }

    static DoubleVector bv_MAX_M = DoubleVector.broadcast(SPECIES, (double)10);

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void MAXDoubleMaxVectorTestsMaskedWithMemOp(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.MAX, bv_MAX_M, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, (double)10, mask, DoubleMaxVectorTests::MAX);
    }

    static double MIN(double a, double b) {
        return (double)(Math.min(a, b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void MINDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.MIN, bv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::MIN);
    }

    static double min(double a, double b) {
        return (double)(Math.min(a, b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void minDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.min(bv).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::min);
    }

    static double MAX(double a, double b) {
        return (double)(Math.max(a, b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void MAXDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.MAX, bv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::MAX);
    }

    static double max(double a, double b) {
        return (double)(Math.max(a, b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void maxDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.max(bv).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, DoubleMaxVectorTests::max);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void MINDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.lanewise(VectorOperators.MIN, b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, DoubleMaxVectorTests::MIN);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void minDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.min(b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, DoubleMaxVectorTests::min);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void MAXDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.lanewise(VectorOperators.MAX, b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, DoubleMaxVectorTests::MAX);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void maxDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.max(b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, DoubleMaxVectorTests::max);
    }

    static double ADDReduce(double[] a, int idx) {
        double res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res += a[i];
        }

        return res;
    }

    static double ADDReduceAll(double[] a) {
        double res = 0;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res += ADDReduce(a, i);
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void ADDReduceDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = 0;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.ADD);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = 0;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra += av.reduceLanes(VectorOperators.ADD);
            }
        }

        assertReductionArraysEquals(r, ra, a,
                DoubleMaxVectorTests::ADDReduce, DoubleMaxVectorTests::ADDReduceAll, RELATIVE_ROUNDING_ERROR_FACTOR_ADD);
    }

    static double ADDReduceMasked(double[] a, int idx, boolean[] mask) {
        double res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if (mask[i % SPECIES.length()])
                res += a[i];
        }

        return res;
    }

    static double ADDReduceAllMasked(double[] a, boolean[] mask) {
        double res = 0;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res += ADDReduceMasked(a, i, mask);
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void ADDReduceDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);
        double ra = 0;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.ADD, vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = 0;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra += av.reduceLanes(VectorOperators.ADD, vmask);
            }
        }

        assertReductionArraysEqualsMasked(r, ra, a, mask,
                DoubleMaxVectorTests::ADDReduceMasked, DoubleMaxVectorTests::ADDReduceAllMasked, RELATIVE_ROUNDING_ERROR_FACTOR_ADD);
    }

    static double MULReduce(double[] a, int idx) {
        double res = 1;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res *= a[i];
        }

        return res;
    }

    static double MULReduceAll(double[] a) {
        double res = 1;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res *= MULReduce(a, i);
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void MULReduceDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = 1;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.MUL);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = 1;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra *= av.reduceLanes(VectorOperators.MUL);
            }
        }

        assertReductionArraysEquals(r, ra, a,
                DoubleMaxVectorTests::MULReduce, DoubleMaxVectorTests::MULReduceAll, RELATIVE_ROUNDING_ERROR_FACTOR_MUL);
    }

    static double MULReduceMasked(double[] a, int idx, boolean[] mask) {
        double res = 1;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if (mask[i % SPECIES.length()])
                res *= a[i];
        }

        return res;
    }

    static double MULReduceAllMasked(double[] a, boolean[] mask) {
        double res = 1;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res *= MULReduceMasked(a, i, mask);
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void MULReduceDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);
        double ra = 1;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.MUL, vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = 1;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra *= av.reduceLanes(VectorOperators.MUL, vmask);
            }
        }

        assertReductionArraysEqualsMasked(r, ra, a, mask,
                DoubleMaxVectorTests::MULReduceMasked, DoubleMaxVectorTests::MULReduceAllMasked, RELATIVE_ROUNDING_ERROR_FACTOR_MUL);
    }

    static double MINReduce(double[] a, int idx) {
        double res = Double.POSITIVE_INFINITY;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res = (double) Math.min(res, a[i]);
        }

        return res;
    }

    static double MINReduceAll(double[] a) {
        double res = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res = (double) Math.min(res, MINReduce(a, i));
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void MINReduceDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = Double.POSITIVE_INFINITY;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.MIN);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = Double.POSITIVE_INFINITY;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = (double) Math.min(ra, av.reduceLanes(VectorOperators.MIN));
            }
        }

        assertReductionArraysEquals(r, ra, a,
                DoubleMaxVectorTests::MINReduce, DoubleMaxVectorTests::MINReduceAll);
    }

    static double MINReduceMasked(double[] a, int idx, boolean[] mask) {
        double res = Double.POSITIVE_INFINITY;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if (mask[i % SPECIES.length()])
                res = (double) Math.min(res, a[i]);
        }

        return res;
    }

    static double MINReduceAllMasked(double[] a, boolean[] mask) {
        double res = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res = (double) Math.min(res, MINReduceMasked(a, i, mask));
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void MINReduceDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);
        double ra = Double.POSITIVE_INFINITY;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.MIN, vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = Double.POSITIVE_INFINITY;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = (double) Math.min(ra, av.reduceLanes(VectorOperators.MIN, vmask));
            }
        }

        assertReductionArraysEqualsMasked(r, ra, a, mask,
                DoubleMaxVectorTests::MINReduceMasked, DoubleMaxVectorTests::MINReduceAllMasked);
    }

    static double MAXReduce(double[] a, int idx) {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res = (double) Math.max(res, a[i]);
        }

        return res;
    }

    static double MAXReduceAll(double[] a) {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res = (double) Math.max(res, MAXReduce(a, i));
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void MAXReduceDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = Double.NEGATIVE_INFINITY;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.MAX);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = (double) Math.max(ra, av.reduceLanes(VectorOperators.MAX));
            }
        }

        assertReductionArraysEquals(r, ra, a,
                DoubleMaxVectorTests::MAXReduce, DoubleMaxVectorTests::MAXReduceAll);
    }

    static double MAXReduceMasked(double[] a, int idx, boolean[] mask) {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if (mask[i % SPECIES.length()])
                res = (double) Math.max(res, a[i]);
        }

        return res;
    }

    static double MAXReduceAllMasked(double[] a, boolean[] mask) {
        double res = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res = (double) Math.max(res, MAXReduceMasked(a, i, mask));
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void MAXReduceDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);
        double ra = Double.NEGATIVE_INFINITY;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.MAX, vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = (double) Math.max(ra, av.reduceLanes(VectorOperators.MAX, vmask));
            }
        }

        assertReductionArraysEqualsMasked(r, ra, a, mask,
                DoubleMaxVectorTests::MAXReduceMasked, DoubleMaxVectorTests::MAXReduceAllMasked);
    }

    static double FIRST_NONZEROReduce(double[] a, int idx) {
        double res = (double) 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res = firstNonZero(res, a[i]);
        }

        return res;
    }

    static double FIRST_NONZEROReduceAll(double[] a) {
        double res = (double) 0;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res = firstNonZero(res, FIRST_NONZEROReduce(a, i));
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void FIRST_NONZEROReduceDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        double ra = (double) 0;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.FIRST_NONZERO);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = (double) 0;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = firstNonZero(ra, av.reduceLanes(VectorOperators.FIRST_NONZERO));
            }
        }

        assertReductionArraysEquals(r, ra, a,
                DoubleMaxVectorTests::FIRST_NONZEROReduce, DoubleMaxVectorTests::FIRST_NONZEROReduceAll);
    }

    static double FIRST_NONZEROReduceMasked(double[] a, int idx, boolean[] mask) {
        double res = (double) 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if (mask[i % SPECIES.length()])
                res = firstNonZero(res, a[i]);
        }

        return res;
    }

    static double FIRST_NONZEROReduceAllMasked(double[] a, boolean[] mask) {
        double res = (double) 0;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res = firstNonZero(res, FIRST_NONZEROReduceMasked(a, i, mask));
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void FIRST_NONZEROReduceDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);
        double ra = (double) 0;

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                r[i] = av.reduceLanes(VectorOperators.FIRST_NONZERO, vmask);
            }
        }

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            ra = (double) 0;
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                ra = firstNonZero(ra, av.reduceLanes(VectorOperators.FIRST_NONZERO, vmask));
            }
        }

        assertReductionArraysEqualsMasked(r, ra, a, mask,
                DoubleMaxVectorTests::FIRST_NONZEROReduceMasked, DoubleMaxVectorTests::FIRST_NONZEROReduceAllMasked);
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void withDoubleMaxVectorTests(IntFunction<double []> fa, IntFunction<double []> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0, j = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.withLane(j, b[i + j]).intoArray(r, i);
                a[i + j] = b[i + j];
                j = (j + 1) & (SPECIES.length() - 1);
            }
        }


        assertArraysStrictlyEquals(r, a);
    }

    static boolean testIS_DEFAULT(double a) {
        return bits(a)==0;
    }

    @Test(dataProvider = "doubleTestOpProvider")
    static void IS_DEFAULTDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_DEFAULT);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), testIS_DEFAULT(a[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleTestOpMaskProvider")
    static void IS_DEFAULTMaskedDoubleMaxVectorTests(IntFunction<double[]> fa,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_DEFAULT, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j),  vmask.laneIsSet(j) && testIS_DEFAULT(a[i + j]));
                }
            }
        }
    }

    static boolean testIS_NEGATIVE(double a) {
        return bits(a)<0;
    }

    @Test(dataProvider = "doubleTestOpProvider")
    static void IS_NEGATIVEDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_NEGATIVE);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), testIS_NEGATIVE(a[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleTestOpMaskProvider")
    static void IS_NEGATIVEMaskedDoubleMaxVectorTests(IntFunction<double[]> fa,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_NEGATIVE, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j),  vmask.laneIsSet(j) && testIS_NEGATIVE(a[i + j]));
                }
            }
        }
    }

    static boolean testIS_FINITE(double a) {
        return Double.isFinite(a);
    }

    @Test(dataProvider = "doubleTestOpProvider")
    static void IS_FINITEDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_FINITE);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), testIS_FINITE(a[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleTestOpMaskProvider")
    static void IS_FINITEMaskedDoubleMaxVectorTests(IntFunction<double[]> fa,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_FINITE, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j),  vmask.laneIsSet(j) && testIS_FINITE(a[i + j]));
                }
            }
        }
    }

    static boolean testIS_NAN(double a) {
        return Double.isNaN(a);
    }

    @Test(dataProvider = "doubleTestOpProvider")
    static void IS_NANDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_NAN);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), testIS_NAN(a[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleTestOpMaskProvider")
    static void IS_NANMaskedDoubleMaxVectorTests(IntFunction<double[]> fa,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_NAN, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j),  vmask.laneIsSet(j) && testIS_NAN(a[i + j]));
                }
            }
        }
    }

    static boolean testIS_INFINITE(double a) {
        return Double.isInfinite(a);
    }

    @Test(dataProvider = "doubleTestOpProvider")
    static void IS_INFINITEDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_INFINITE);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), testIS_INFINITE(a[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleTestOpMaskProvider")
    static void IS_INFINITEMaskedDoubleMaxVectorTests(IntFunction<double[]> fa,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                VectorMask<Double> mv = av.test(VectorOperators.IS_INFINITE, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j),  vmask.laneIsSet(j) && testIS_INFINITE(a[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void LTDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.LT, bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), lt(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void ltDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.lt(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), lt(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void LTDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.LT, bv, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), mask[j] && lt(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void GTDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.GT, bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), gt(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void GTDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.GT, bv, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), mask[j] && gt(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void EQDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.EQ, bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), eq(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void eqDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.eq(bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), eq(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void EQDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.EQ, bv, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), mask[j] && eq(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void NEDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.NE, bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), neq(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void NEDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.NE, bv, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), mask[j] && neq(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void LEDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.LE, bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), le(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void LEDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.LE, bv, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), mask[j] && le(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void GEDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.GE, bv);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), ge(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void GEDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                VectorMask<Double> mv = av.compare(VectorOperators.GE, bv, vmask);

                // Check results as part of computation.
                for (int j = 0; j < SPECIES.length(); j++) {
                    Assert.assertEquals(mv.laneIsSet(j), mask[j] && ge(a[i + j], b[i + j]));
                }
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void LTDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.compare(VectorOperators.LT, b[i]);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), a[i + j] < b[i]);
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void LTDoubleMaxVectorTestsBroadcastMaskedSmokeTest(IntFunction<double[]> fa,
                                IntFunction<double[]> fb, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.compare(VectorOperators.LT, b[i], vmask);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), mask[j] && (a[i + j] < b[i]));
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void LTDoubleMaxVectorTestsBroadcastLongSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.compare(VectorOperators.LT, (long)b[i]);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), a[i + j] < (double)((long)b[i]));
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void LTDoubleMaxVectorTestsBroadcastLongMaskedSmokeTest(IntFunction<double[]> fa,
                                IntFunction<double[]> fb, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.compare(VectorOperators.LT, (long)b[i], vmask);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), mask[j] && (a[i + j] < (double)((long)b[i])));
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void EQDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.compare(VectorOperators.EQ, b[i]);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), a[i + j] == b[i]);
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void EQDoubleMaxVectorTestsBroadcastMaskedSmokeTest(IntFunction<double[]> fa,
                                IntFunction<double[]> fb, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.compare(VectorOperators.EQ, b[i], vmask);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), mask[j] && (a[i + j] == b[i]));
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void EQDoubleMaxVectorTestsBroadcastLongSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.compare(VectorOperators.EQ, (long)b[i]);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), a[i + j] == (double)((long)b[i]));
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpMaskProvider")
    static void EQDoubleMaxVectorTestsBroadcastLongMaskedSmokeTest(IntFunction<double[]> fa,
                                IntFunction<double[]> fb, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());

        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.compare(VectorOperators.EQ, (long)b[i], vmask);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), mask[j] && (a[i + j] == (double)((long)b[i])));
            }
        }
    }

    static double blend(double a, double b, boolean mask) {
        return mask ? b : a;
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void blendDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.blend(bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, mask, DoubleMaxVectorTests::blend);
    }

    @Test(dataProvider = "doubleUnaryOpShuffleProvider")
    static void RearrangeDoubleMaxVectorTests(IntFunction<double[]> fa,
                                           BiFunction<Integer,Integer,int[]> fs) {
        double[] a = fa.apply(SPECIES.length());
        int[] order = fs.apply(a.length, SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.rearrange(VectorShuffle.fromArray(SPECIES, order, i)).intoArray(r, i);
            }
        }

        assertRearrangeArraysEquals(r, a, order, SPECIES.length());
    }

    @Test(dataProvider = "doubleUnaryOpShuffleMaskProvider")
    static void RearrangeDoubleMaxVectorTestsMaskedSmokeTest(IntFunction<double[]> fa,
                                                          BiFunction<Integer,Integer,int[]> fs,
                                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        int[] order = fs.apply(a.length, SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.rearrange(VectorShuffle.fromArray(SPECIES, order, i), vmask).intoArray(r, i);
        }

        assertRearrangeArraysEquals(r, a, order, mask, SPECIES.length());
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void compressDoubleMaxVectorTests(IntFunction<double[]> fa,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.compress(vmask).intoArray(r, i);
            }
        }

        assertcompressArraysEquals(r, a, mask, SPECIES.length());
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void expandDoubleMaxVectorTests(IntFunction<double[]> fa,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.expand(vmask).intoArray(r, i);
            }
        }

        assertexpandArraysEquals(r, a, mask, SPECIES.length());
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void getDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                int num_lanes = SPECIES.length();
                // Manually unroll because full unroll happens after intrinsification.
                // Unroll is needed because get intrinsic requires for index to be a known constant.
                if (num_lanes == 1) {
                    r[i]=av.lane(0);
                } else if (num_lanes == 2) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                } else if (num_lanes == 4) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                } else if (num_lanes == 8) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                    r[i+4]=av.lane(4);
                    r[i+5]=av.lane(5);
                    r[i+6]=av.lane(6);
                    r[i+7]=av.lane(7);
                } else if (num_lanes == 16) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                    r[i+4]=av.lane(4);
                    r[i+5]=av.lane(5);
                    r[i+6]=av.lane(6);
                    r[i+7]=av.lane(7);
                    r[i+8]=av.lane(8);
                    r[i+9]=av.lane(9);
                    r[i+10]=av.lane(10);
                    r[i+11]=av.lane(11);
                    r[i+12]=av.lane(12);
                    r[i+13]=av.lane(13);
                    r[i+14]=av.lane(14);
                    r[i+15]=av.lane(15);
                } else if (num_lanes == 32) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                    r[i+4]=av.lane(4);
                    r[i+5]=av.lane(5);
                    r[i+6]=av.lane(6);
                    r[i+7]=av.lane(7);
                    r[i+8]=av.lane(8);
                    r[i+9]=av.lane(9);
                    r[i+10]=av.lane(10);
                    r[i+11]=av.lane(11);
                    r[i+12]=av.lane(12);
                    r[i+13]=av.lane(13);
                    r[i+14]=av.lane(14);
                    r[i+15]=av.lane(15);
                    r[i+16]=av.lane(16);
                    r[i+17]=av.lane(17);
                    r[i+18]=av.lane(18);
                    r[i+19]=av.lane(19);
                    r[i+20]=av.lane(20);
                    r[i+21]=av.lane(21);
                    r[i+22]=av.lane(22);
                    r[i+23]=av.lane(23);
                    r[i+24]=av.lane(24);
                    r[i+25]=av.lane(25);
                    r[i+26]=av.lane(26);
                    r[i+27]=av.lane(27);
                    r[i+28]=av.lane(28);
                    r[i+29]=av.lane(29);
                    r[i+30]=av.lane(30);
                    r[i+31]=av.lane(31);
                } else if (num_lanes == 64) {
                    r[i]=av.lane(0);
                    r[i+1]=av.lane(1);
                    r[i+2]=av.lane(2);
                    r[i+3]=av.lane(3);
                    r[i+4]=av.lane(4);
                    r[i+5]=av.lane(5);
                    r[i+6]=av.lane(6);
                    r[i+7]=av.lane(7);
                    r[i+8]=av.lane(8);
                    r[i+9]=av.lane(9);
                    r[i+10]=av.lane(10);
                    r[i+11]=av.lane(11);
                    r[i+12]=av.lane(12);
                    r[i+13]=av.lane(13);
                    r[i+14]=av.lane(14);
                    r[i+15]=av.lane(15);
                    r[i+16]=av.lane(16);
                    r[i+17]=av.lane(17);
                    r[i+18]=av.lane(18);
                    r[i+19]=av.lane(19);
                    r[i+20]=av.lane(20);
                    r[i+21]=av.lane(21);
                    r[i+22]=av.lane(22);
                    r[i+23]=av.lane(23);
                    r[i+24]=av.lane(24);
                    r[i+25]=av.lane(25);
                    r[i+26]=av.lane(26);
                    r[i+27]=av.lane(27);
                    r[i+28]=av.lane(28);
                    r[i+29]=av.lane(29);
                    r[i+30]=av.lane(30);
                    r[i+31]=av.lane(31);
                    r[i+32]=av.lane(32);
                    r[i+33]=av.lane(33);
                    r[i+34]=av.lane(34);
                    r[i+35]=av.lane(35);
                    r[i+36]=av.lane(36);
                    r[i+37]=av.lane(37);
                    r[i+38]=av.lane(38);
                    r[i+39]=av.lane(39);
                    r[i+40]=av.lane(40);
                    r[i+41]=av.lane(41);
                    r[i+42]=av.lane(42);
                    r[i+43]=av.lane(43);
                    r[i+44]=av.lane(44);
                    r[i+45]=av.lane(45);
                    r[i+46]=av.lane(46);
                    r[i+47]=av.lane(47);
                    r[i+48]=av.lane(48);
                    r[i+49]=av.lane(49);
                    r[i+50]=av.lane(50);
                    r[i+51]=av.lane(51);
                    r[i+52]=av.lane(52);
                    r[i+53]=av.lane(53);
                    r[i+54]=av.lane(54);
                    r[i+55]=av.lane(55);
                    r[i+56]=av.lane(56);
                    r[i+57]=av.lane(57);
                    r[i+58]=av.lane(58);
                    r[i+59]=av.lane(59);
                    r[i+60]=av.lane(60);
                    r[i+61]=av.lane(61);
                    r[i+62]=av.lane(62);
                    r[i+63]=av.lane(63);
                } else {
                    for (int j = 0; j < SPECIES.length(); j++) {
                        r[i+j]=av.lane(j);
                    }
                }
            }
        }

        assertArraysStrictlyEquals(r, a);
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void BroadcastDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector.broadcast(SPECIES, a[i]).intoArray(r, i);
            }
        }

        assertBroadcastArraysEquals(r, a);
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void ZeroDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector.zero(SPECIES).intoArray(a, i);
            }
        }

        Assert.assertEquals(a, r);
    }

    static double[] sliceUnary(double[] a, int origin, int idx) {
        double[] res = new double[SPECIES.length()];
        for (int i = 0; i < SPECIES.length(); i++){
            if(i+origin < SPECIES.length())
                res[i] = a[idx+i+origin];
            else
                res[i] = (double)0;
        }
        return res;
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void sliceUnaryDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];
        int origin = RAND.nextInt(SPECIES.length());
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.slice(origin).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, origin, DoubleMaxVectorTests::sliceUnary);
    }

    static double[] sliceBinary(double[] a, double[] b, int origin, int idx) {
        double[] res = new double[SPECIES.length()];
        for (int i = 0, j = 0; i < SPECIES.length(); i++){
            if(i+origin < SPECIES.length())
                res[i] = a[idx+i+origin];
            else {
                res[i] = b[idx+j];
                j++;
            }
        }
        return res;
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void sliceBinaryDoubleMaxVectorTestsBinary(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = new double[a.length];
        int origin = RAND.nextInt(SPECIES.length());
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.slice(origin, bv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, origin, DoubleMaxVectorTests::sliceBinary);
    }

    static double[] slice(double[] a, double[] b, int origin, boolean[] mask, int idx) {
        double[] res = new double[SPECIES.length()];
        for (int i = 0, j = 0; i < SPECIES.length(); i++){
            if(i+origin < SPECIES.length())
                res[i] = mask[i] ? a[idx+i+origin] : (double)0;
            else {
                res[i] = mask[i] ? b[idx+j] : (double)0;
                j++;
            }
        }
        return res;
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void sliceDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
    IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        double[] r = new double[a.length];
        int origin = RAND.nextInt(SPECIES.length());
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.slice(origin, bv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, origin, mask, DoubleMaxVectorTests::slice);
    }

    static double[] unsliceUnary(double[] a, int origin, int idx) {
        double[] res = new double[SPECIES.length()];
        for (int i = 0, j = 0; i < SPECIES.length(); i++){
            if(i < origin)
                res[i] = (double)0;
            else {
                res[i] = a[idx+j];
                j++;
            }
        }
        return res;
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void unsliceUnaryDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];
        int origin = RAND.nextInt(SPECIES.length());
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.unslice(origin).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, origin, DoubleMaxVectorTests::unsliceUnary);
    }

    static double[] unsliceBinary(double[] a, double[] b, int origin, int part, int idx) {
        double[] res = new double[SPECIES.length()];
        for (int i = 0, j = 0; i < SPECIES.length(); i++){
            if (part == 0) {
                if (i < origin)
                    res[i] = b[idx+i];
                else {
                    res[i] = a[idx+j];
                    j++;
                }
            } else if (part == 1) {
                if (i < origin)
                    res[i] = a[idx+SPECIES.length()-origin+i];
                else {
                    res[i] = b[idx+origin+j];
                    j++;
                }
            }
        }
        return res;
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void unsliceBinaryDoubleMaxVectorTestsBinary(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = new double[a.length];
        int origin = RAND.nextInt(SPECIES.length());
        int part = RAND.nextInt(2);
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.unslice(origin, bv, part).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, origin, part, DoubleMaxVectorTests::unsliceBinary);
    }

    static double[] unslice(double[] a, double[] b, int origin, int part, boolean[] mask, int idx) {
        double[] res = new double[SPECIES.length()];
        for (int i = 0, j = 0; i < SPECIES.length(); i++){
            if(i+origin < SPECIES.length())
                res[i] = b[idx+i+origin];
            else {
                res[i] = b[idx+j];
                j++;
            }
        }
        for (int i = 0; i < SPECIES.length(); i++){
            res[i] = mask[i] ? a[idx+i] : res[i];
        }
        double[] res1 = new double[SPECIES.length()];
        if (part == 0) {
            for (int i = 0, j = 0; i < SPECIES.length(); i++){
                if (i < origin)
                    res1[i] = b[idx+i];
                else {
                   res1[i] = res[j];
                   j++;
                }
            }
        } else if (part == 1) {
            for (int i = 0, j = 0; i < SPECIES.length(); i++){
                if (i < origin)
                    res1[i] = res[SPECIES.length()-origin+i];
                else {
                    res1[i] = b[idx+origin+j];
                    j++;
                }
            }
        }
        return res1;
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void unsliceDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
    IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);
        double[] r = new double[a.length];
        int origin = RAND.nextInt(SPECIES.length());
        int part = RAND.nextInt(2);
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.unslice(origin, bv, part, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, origin, part, mask, DoubleMaxVectorTests::unslice);
    }

    static double SIN(double a) {
        return (double)(Math.sin((double)a));
    }

    static double strictSIN(double a) {
        return (double)(StrictMath.sin((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void SINDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.SIN).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::SIN, DoubleMaxVectorTests::strictSIN);
    }

    static double EXP(double a) {
        return (double)(Math.exp((double)a));
    }

    static double strictEXP(double a) {
        return (double)(StrictMath.exp((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void EXPDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.EXP).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::EXP, DoubleMaxVectorTests::strictEXP);
    }

    static double LOG1P(double a) {
        return (double)(Math.log1p((double)a));
    }

    static double strictLOG1P(double a) {
        return (double)(StrictMath.log1p((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void LOG1PDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.LOG1P).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::LOG1P, DoubleMaxVectorTests::strictLOG1P);
    }

    static double LOG(double a) {
        return (double)(Math.log((double)a));
    }

    static double strictLOG(double a) {
        return (double)(StrictMath.log((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void LOGDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.LOG).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::LOG, DoubleMaxVectorTests::strictLOG);
    }

    static double LOG10(double a) {
        return (double)(Math.log10((double)a));
    }

    static double strictLOG10(double a) {
        return (double)(StrictMath.log10((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void LOG10DoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.LOG10).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::LOG10, DoubleMaxVectorTests::strictLOG10);
    }

    static double EXPM1(double a) {
        return (double)(Math.expm1((double)a));
    }

    static double strictEXPM1(double a) {
        return (double)(StrictMath.expm1((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void EXPM1DoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.EXPM1).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::EXPM1, DoubleMaxVectorTests::strictEXPM1);
    }

    static double COS(double a) {
        return (double)(Math.cos((double)a));
    }

    static double strictCOS(double a) {
        return (double)(StrictMath.cos((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void COSDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.COS).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::COS, DoubleMaxVectorTests::strictCOS);
    }

    static double TAN(double a) {
        return (double)(Math.tan((double)a));
    }

    static double strictTAN(double a) {
        return (double)(StrictMath.tan((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void TANDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.TAN).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::TAN, DoubleMaxVectorTests::strictTAN);
    }

    static double SINH(double a) {
        return (double)(Math.sinh((double)a));
    }

    static double strictSINH(double a) {
        return (double)(StrictMath.sinh((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void SINHDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.SINH).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::SINH, DoubleMaxVectorTests::strictSINH);
    }

    static double COSH(double a) {
        return (double)(Math.cosh((double)a));
    }

    static double strictCOSH(double a) {
        return (double)(StrictMath.cosh((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void COSHDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.COSH).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::COSH, DoubleMaxVectorTests::strictCOSH);
    }

    static double TANH(double a) {
        return (double)(Math.tanh((double)a));
    }

    static double strictTANH(double a) {
        return (double)(StrictMath.tanh((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void TANHDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.TANH).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::TANH, DoubleMaxVectorTests::strictTANH);
    }

    static double ASIN(double a) {
        return (double)(Math.asin((double)a));
    }

    static double strictASIN(double a) {
        return (double)(StrictMath.asin((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void ASINDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.ASIN).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::ASIN, DoubleMaxVectorTests::strictASIN);
    }

    static double ACOS(double a) {
        return (double)(Math.acos((double)a));
    }

    static double strictACOS(double a) {
        return (double)(StrictMath.acos((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void ACOSDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.ACOS).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::ACOS, DoubleMaxVectorTests::strictACOS);
    }

    static double ATAN(double a) {
        return (double)(Math.atan((double)a));
    }

    static double strictATAN(double a) {
        return (double)(StrictMath.atan((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void ATANDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.ATAN).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::ATAN, DoubleMaxVectorTests::strictATAN);
    }

    static double CBRT(double a) {
        return (double)(Math.cbrt((double)a));
    }

    static double strictCBRT(double a) {
        return (double)(StrictMath.cbrt((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void CBRTDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.CBRT).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, DoubleMaxVectorTests::CBRT, DoubleMaxVectorTests::strictCBRT);
    }

    static double HYPOT(double a, double b) {
        return (double)(Math.hypot((double)a, (double)b));
    }

    static double strictHYPOT(double a, double b) {
        return (double)(StrictMath.hypot((double)a, (double)b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void HYPOTDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.HYPOT, bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, b, DoubleMaxVectorTests::HYPOT, DoubleMaxVectorTests::strictHYPOT);
    }


    static double POW(double a, double b) {
        return (double)(Math.pow((double)a, (double)b));
    }

    static double strictPOW(double a, double b) {
        return (double)(StrictMath.pow((double)a, (double)b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void POWDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.POW, bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, b, DoubleMaxVectorTests::POW, DoubleMaxVectorTests::strictPOW);
    }


    static double pow(double a, double b) {
        return (double)(Math.pow((double)a, (double)b));
    }

    static double strictpow(double a, double b) {
        return (double)(StrictMath.pow((double)a, (double)b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void powDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.pow(bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, b, DoubleMaxVectorTests::pow, DoubleMaxVectorTests::strictpow);
    }


    static double ATAN2(double a, double b) {
        return (double)(Math.atan2((double)a, (double)b));
    }

    static double strictATAN2(double a, double b) {
        return (double)(StrictMath.atan2((double)a, (double)b));
    }

    @Test(dataProvider = "doubleBinaryOpProvider")
    static void ATAN2DoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                av.lanewise(VectorOperators.ATAN2, bv).intoArray(r, i);
            }
        }

        assertArraysEqualsWithinOneUlp(r, a, b, DoubleMaxVectorTests::ATAN2, DoubleMaxVectorTests::strictATAN2);
    }


    @Test(dataProvider = "doubleBinaryOpProvider")
    static void POWDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.lanewise(VectorOperators.POW, b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEqualsWithinOneUlp(r, a, b, DoubleMaxVectorTests::POW, DoubleMaxVectorTests::strictPOW);
    }


    @Test(dataProvider = "doubleBinaryOpProvider")
    static void powDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.pow(b[i]).intoArray(r, i);
        }

        assertBroadcastArraysEqualsWithinOneUlp(r, a, b, DoubleMaxVectorTests::pow, DoubleMaxVectorTests::strictpow);
    }


    static double FMA(double a, double b, double c) {
        return (double)(Math.fma(a, b, c));
    }

    static double fma(double a, double b, double c) {
        return (double)(Math.fma(a, b, c));
    }

    @Test(dataProvider = "doubleTernaryOpProvider")
    static void FMADoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb, IntFunction<double[]> fc) {
        int count = INVOC_COUNT;
        switch ("FMA") {
        case "fma": case "lanewise_FMA":
           // Math.fma uses BigDecimal
           count = Math.max(5, count/20); break;
        }
        final int INVOC_COUNT = count;
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                DoubleVector cv = DoubleVector.fromArray(SPECIES, c, i);
                av.lanewise(VectorOperators.FMA, bv, cv).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, c, DoubleMaxVectorTests::FMA);
    }

    @Test(dataProvider = "doubleTernaryOpProvider")
    static void fmaDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb, IntFunction<double[]> fc) {
        int count = INVOC_COUNT;
        switch ("fma") {
        case "fma": case "lanewise_FMA":
           // Math.fma uses BigDecimal
           count = Math.max(5, count/20); break;
        }
        final int INVOC_COUNT = count;
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            DoubleVector cv = DoubleVector.fromArray(SPECIES, c, i);
            av.fma(bv, cv).intoArray(r, i);
        }

        assertArraysEquals(r, a, b, c, DoubleMaxVectorTests::fma);
    }

    @Test(dataProvider = "doubleTernaryOpMaskProvider")
    static void FMADoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<double[]> fc, IntFunction<boolean[]> fm) {
        int count = INVOC_COUNT;
        switch ("FMA") {
        case "fma": case "lanewise_FMA":
           // Math.fma uses BigDecimal
           count = Math.max(5, count/20); break;
        }
        final int INVOC_COUNT = count;
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                DoubleVector cv = DoubleVector.fromArray(SPECIES, c, i);
                av.lanewise(VectorOperators.FMA, bv, cv, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, b, c, mask, DoubleMaxVectorTests::FMA);
    }

    @Test(dataProvider = "doubleTernaryOpProvider")
    static void FMADoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb, IntFunction<double[]> fc) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.lanewise(VectorOperators.FMA, bv, c[i]).intoArray(r, i);
        }
        assertBroadcastArraysEquals(r, a, b, c, DoubleMaxVectorTests::FMA);
    }

    @Test(dataProvider = "doubleTernaryOpProvider")
    static void FMADoubleMaxVectorTestsAltBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb, IntFunction<double[]> fc) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector cv = DoubleVector.fromArray(SPECIES, c, i);
            av.lanewise(VectorOperators.FMA, b[i], cv).intoArray(r, i);
        }
        assertAltBroadcastArraysEquals(r, a, b, c, DoubleMaxVectorTests::FMA);
    }

    @Test(dataProvider = "doubleTernaryOpMaskProvider")
    static void FMADoubleMaxVectorTestsBroadcastMaskedSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<double[]> fc, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
            av.lanewise(VectorOperators.FMA, bv, c[i], vmask).intoArray(r, i);
        }

        assertBroadcastArraysEquals(r, a, b, c, mask, DoubleMaxVectorTests::FMA);
    }

    @Test(dataProvider = "doubleTernaryOpMaskProvider")
    static void FMADoubleMaxVectorTestsAltBroadcastMaskedSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<double[]> fc, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector cv = DoubleVector.fromArray(SPECIES, c, i);
            av.lanewise(VectorOperators.FMA, b[i], cv, vmask).intoArray(r, i);
        }

        assertAltBroadcastArraysEquals(r, a, b, c, mask, DoubleMaxVectorTests::FMA);
    }

    @Test(dataProvider = "doubleTernaryOpProvider")
    static void FMADoubleMaxVectorTestsDoubleBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb, IntFunction<double[]> fc) {
        int count = INVOC_COUNT;
        switch ("FMA") {
        case "fma": case "lanewise_FMA":
           // Math.fma uses BigDecimal
           count = Math.max(5, count/20); break;
        }
        final int INVOC_COUNT = count;
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.lanewise(VectorOperators.FMA, b[i], c[i]).intoArray(r, i);
        }

        assertDoubleBroadcastArraysEquals(r, a, b, c, DoubleMaxVectorTests::FMA);
    }

    @Test(dataProvider = "doubleTernaryOpProvider")
    static void fmaDoubleMaxVectorTestsDoubleBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb, IntFunction<double[]> fc) {
        int count = INVOC_COUNT;
        switch ("fma") {
        case "fma": case "lanewise_FMA":
           // Math.fma uses BigDecimal
           count = Math.max(5, count/20); break;
        }
        final int INVOC_COUNT = count;
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.fma(b[i], c[i]).intoArray(r, i);
        }

        assertDoubleBroadcastArraysEquals(r, a, b, c, DoubleMaxVectorTests::fma);
    }

    @Test(dataProvider = "doubleTernaryOpMaskProvider")
    static void FMADoubleMaxVectorTestsDoubleBroadcastMaskedSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<double[]> fc, IntFunction<boolean[]> fm) {
        int count = INVOC_COUNT;
        switch ("FMA") {
        case "fma": case "lanewise_FMA":
           // Math.fma uses BigDecimal
           count = Math.max(5, count/20); break;
        }
        final int INVOC_COUNT = count;
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] c = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            av.lanewise(VectorOperators.FMA, b[i], c[i], vmask).intoArray(r, i);
        }

        assertDoubleBroadcastArraysEquals(r, a, b, c, mask, DoubleMaxVectorTests::FMA);
    }

    static double NEG(double a) {
        return (double)(-((double)a));
    }

    static double neg(double a) {
        return (double)(-((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void NEGDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.NEG).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, DoubleMaxVectorTests::NEG);
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void negDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.neg().intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, DoubleMaxVectorTests::neg);
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void NEGMaskedDoubleMaxVectorTests(IntFunction<double[]> fa,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.NEG, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, mask, DoubleMaxVectorTests::NEG);
    }

    static double ABS(double a) {
        return (double)(Math.abs((double)a));
    }

    static double abs(double a) {
        return (double)(Math.abs((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void ABSDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.ABS).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, DoubleMaxVectorTests::ABS);
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void absDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.abs().intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, DoubleMaxVectorTests::abs);
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void ABSMaskedDoubleMaxVectorTests(IntFunction<double[]> fa,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.ABS, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, mask, DoubleMaxVectorTests::ABS);
    }

    static double SQRT(double a) {
        return (double)(Math.sqrt((double)a));
    }

    static double sqrt(double a) {
        return (double)(Math.sqrt((double)a));
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void SQRTDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.SQRT).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, DoubleMaxVectorTests::SQRT);
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void sqrtDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.sqrt().intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, DoubleMaxVectorTests::sqrt);
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void SQRTMaskedDoubleMaxVectorTests(IntFunction<double[]> fa,
                                                IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.lanewise(VectorOperators.SQRT, vmask).intoArray(r, i);
            }
        }

        assertArraysEquals(r, a, mask, DoubleMaxVectorTests::SQRT);
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void ltDoubleMaxVectorTestsBroadcastSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.lt(b[i]);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), a[i + j] < b[i]);
            }
        }
    }

    @Test(dataProvider = "doubleCompareOpProvider")
    static void eqDoubleMaxVectorTestsBroadcastMaskedSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            VectorMask<Double> mv = av.eq(b[i]);

            // Check results as part of computation.
            for (int j = 0; j < SPECIES.length(); j++) {
                Assert.assertEquals(mv.laneIsSet(j), a[i + j] == b[i]);
            }
        }
    }

    @Test(dataProvider = "doubletoIntUnaryOpProvider")
    static void toIntArrayDoubleMaxVectorTestsSmokeTest(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            int[] r = av.toIntArray();
            assertArraysEquals(r, a, i);
        }
    }

    @Test(dataProvider = "doubletoLongUnaryOpProvider")
    static void toLongArrayDoubleMaxVectorTestsSmokeTest(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            long[] r = av.toLongArray();
            assertArraysEquals(r, a, i);
        }
    }


    @Test(dataProvider = "doubleUnaryOpProvider")
    static void toStringDoubleMaxVectorTestsSmokeTest(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            String str = av.toString();

            double subarr[] = Arrays.copyOfRange(a, i, i + SPECIES.length());
            Assert.assertTrue(str.equals(Arrays.toString(subarr)), "at index " + i + ", string should be = " + Arrays.toString(subarr) + ", but is = " + str);
        }
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void hashCodeDoubleMaxVectorTestsSmokeTest(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            int hash = av.hashCode();

            double subarr[] = Arrays.copyOfRange(a, i, i + SPECIES.length());
            int expectedHash = Objects.hash(SPECIES, Arrays.hashCode(subarr));
            Assert.assertTrue(hash == expectedHash, "at index " + i + ", hash should be = " + expectedHash + ", but is = " + hash);
        }
    }


    static long ADDReduceLong(double[] a, int idx) {
        double res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            res += a[i];
        }

        return (long)res;
    }

    static long ADDReduceAllLong(double[] a) {
        long res = 0;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res += ADDReduceLong(a, i);
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpProvider")
    static void ADDReduceLongDoubleMaxVectorTests(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        long[] r = lfr.apply(SPECIES.length());
        long ra = 0;

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            r[i] = av.reduceLanesToLong(VectorOperators.ADD);
        }

        ra = 0;
        for (int i = 0; i < a.length; i ++) {
            ra += r[i];
        }

        assertReductionLongArraysEquals(r, ra, a,
                DoubleMaxVectorTests::ADDReduceLong, DoubleMaxVectorTests::ADDReduceAllLong);
    }

    static long ADDReduceLongMasked(double[] a, int idx, boolean[] mask) {
        double res = 0;
        for (int i = idx; i < (idx + SPECIES.length()); i++) {
            if(mask[i % SPECIES.length()])
                res += a[i];
        }

        return (long)res;
    }

    static long ADDReduceAllLongMasked(double[] a, boolean[] mask) {
        long res = 0;
        for (int i = 0; i < a.length; i += SPECIES.length()) {
            res += ADDReduceLongMasked(a, i, mask);
        }

        return res;
    }

    @Test(dataProvider = "doubleUnaryOpMaskProvider")
    static void ADDReduceLongDoubleMaxVectorTestsMasked(IntFunction<double[]> fa, IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        long[] r = lfr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);
        long ra = 0;

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            r[i] = av.reduceLanesToLong(VectorOperators.ADD, vmask);
        }

        ra = 0;
        for (int i = 0; i < a.length; i ++) {
            ra += r[i];
        }

        assertReductionLongArraysEqualsMasked(r, ra, a, mask,
                DoubleMaxVectorTests::ADDReduceLongMasked, DoubleMaxVectorTests::ADDReduceAllLongMasked);
    }

    @Test(dataProvider = "doubletoLongUnaryOpProvider")
    static void BroadcastLongDoubleMaxVectorTestsSmokeTest(IntFunction<double[]> fa) {
        double[] a = fa.apply(SPECIES.length());
        double[] r = new double[a.length];

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector.broadcast(SPECIES, (long)a[i]).intoArray(r, i);
        }
        assertBroadcastArraysEquals(r, a);
    }

    @Test(dataProvider = "doubleBinaryOpMaskProvider")
    static void blendDoubleMaxVectorTestsBroadcastLongSmokeTest(IntFunction<double[]> fa, IntFunction<double[]> fb,
                                          IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                av.blend((long)b[i], vmask).intoArray(r, i);
            }
        }
        assertBroadcastLongArraysEquals(r, a, b, mask, DoubleMaxVectorTests::blend);
    }


    @Test(dataProvider = "doubleUnaryOpSelectFromProvider")
    static void SelectFromDoubleMaxVectorTests(IntFunction<double[]> fa,
                                           BiFunction<Integer,Integer,double[]> fs) {
        double[] a = fa.apply(SPECIES.length());
        double[] order = fs.apply(a.length, SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, order, i);
            bv.selectFrom(av).intoArray(r, i);
        }

        assertSelectFromArraysEquals(r, a, order, SPECIES.length());
    }

    @Test(dataProvider = "doubleSelectFromTwoVectorOpProvider")
    static void SelectFromTwoVectorDoubleMaxVectorTests(IntFunction<double[]> fa, IntFunction<double[]> fb, IntFunction<double[]> fc) {
        double[] a = fa.apply(SPECIES.length());
        double[] b = fb.apply(SPECIES.length());
        double[] idx = fc.apply(SPECIES.length());
        double[] r = fr.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT; ic++) {
            for (int i = 0; i < idx.length; i += SPECIES.length()) {
                DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
                DoubleVector bv = DoubleVector.fromArray(SPECIES, b, i);
                DoubleVector idxv = DoubleVector.fromArray(SPECIES, idx, i);
                idxv.selectFrom(av, bv).intoArray(r, i);
            }
        }
        assertSelectFromTwoVectorEquals(r, idx, a, b, SPECIES.length());
    }

    @Test(dataProvider = "doubleUnaryOpSelectFromMaskProvider")
    static void SelectFromDoubleMaxVectorTestsMaskedSmokeTest(IntFunction<double[]> fa,
                                                           BiFunction<Integer,Integer,double[]> fs,
                                                           IntFunction<boolean[]> fm) {
        double[] a = fa.apply(SPECIES.length());
        double[] order = fs.apply(a.length, SPECIES.length());
        double[] r = fr.apply(SPECIES.length());
        boolean[] mask = fm.apply(SPECIES.length());
        VectorMask<Double> vmask = VectorMask.fromArray(SPECIES, mask, 0);

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            DoubleVector av = DoubleVector.fromArray(SPECIES, a, i);
            DoubleVector bv = DoubleVector.fromArray(SPECIES, order, i);
            bv.selectFrom(av, vmask).intoArray(r, i);
        }

        assertSelectFromArraysEquals(r, a, order, mask, SPECIES.length());
    }

    @Test(dataProvider = "shuffleProvider")
    static void shuffleMiscellaneousDoubleMaxVectorTestsSmokeTest(BiFunction<Integer,Integer,int[]> fs) {
        int[] a = fs.apply(SPECIES.length() * BUFFER_REPS, SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var shuffle = VectorShuffle.fromArray(SPECIES, a, i);
            int hash = shuffle.hashCode();
            int length = shuffle.length();

            int subarr[] = Arrays.copyOfRange(a, i, i + SPECIES.length());
            int expectedHash = Objects.hash(SPECIES, Arrays.hashCode(subarr));
            Assert.assertTrue(hash == expectedHash, "at index " + i + ", hash should be = " + expectedHash + ", but is = " + hash);
            Assert.assertEquals(length, SPECIES.length());
        }
    }

    @Test(dataProvider = "shuffleProvider")
    static void shuffleToStringDoubleMaxVectorTestsSmokeTest(BiFunction<Integer,Integer,int[]> fs) {
        int[] a = fs.apply(SPECIES.length() * BUFFER_REPS, SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var shuffle = VectorShuffle.fromArray(SPECIES, a, i);
            String str = shuffle.toString();

            int subarr[] = Arrays.copyOfRange(a, i, i + SPECIES.length());
            Assert.assertTrue(str.equals("Shuffle" + Arrays.toString(subarr)), "at index " +
                i + ", string should be = " + Arrays.toString(subarr) + ", but is = " + str);
        }
    }

    @Test(dataProvider = "shuffleCompareOpProvider")
    static void shuffleEqualsDoubleMaxVectorTestsSmokeTest(BiFunction<Integer,Integer,int[]> fa, BiFunction<Integer,Integer,int[]> fb) {
        int[] a = fa.apply(SPECIES.length() * BUFFER_REPS, SPECIES.length());
        int[] b = fb.apply(SPECIES.length() * BUFFER_REPS, SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var av = VectorShuffle.fromArray(SPECIES, a, i);
            var bv = VectorShuffle.fromArray(SPECIES, b, i);
            boolean eq = av.equals(bv);
            int to = i + SPECIES.length();
            Assert.assertEquals(eq, Arrays.equals(a, i, to, b, i, to));
        }
    }

    @Test(dataProvider = "maskCompareOpProvider")
    static void maskEqualsDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa, IntFunction<boolean[]> fb) {
        boolean[] a = fa.apply(SPECIES.length());
        boolean[] b = fb.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var av = SPECIES.loadMask(a, i);
            var bv = SPECIES.loadMask(b, i);
            boolean equals = av.equals(bv);
            int to = i + SPECIES.length();
            Assert.assertEquals(equals, Arrays.equals(a, i, to, b, i, to));
        }
    }

    static boolean band(boolean a, boolean b) {
        return a & b;
    }

    @Test(dataProvider = "maskCompareOpProvider")
    static void maskAndDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa, IntFunction<boolean[]> fb) {
        boolean[] a = fa.apply(SPECIES.length());
        boolean[] b = fb.apply(SPECIES.length());
        boolean[] r = new boolean[a.length];

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var av = SPECIES.loadMask(a, i);
            var bv = SPECIES.loadMask(b, i);
            var cv = av.and(bv);
            cv.intoArray(r, i);
        }
        assertArraysEquals(r, a, b, DoubleMaxVectorTests::band);
    }

    static boolean bor(boolean a, boolean b) {
        return a | b;
    }

    @Test(dataProvider = "maskCompareOpProvider")
    static void maskOrDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa, IntFunction<boolean[]> fb) {
        boolean[] a = fa.apply(SPECIES.length());
        boolean[] b = fb.apply(SPECIES.length());
        boolean[] r = new boolean[a.length];

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var av = SPECIES.loadMask(a, i);
            var bv = SPECIES.loadMask(b, i);
            var cv = av.or(bv);
            cv.intoArray(r, i);
        }
        assertArraysEquals(r, a, b, DoubleMaxVectorTests::bor);
    }

    static boolean bxor(boolean a, boolean b) {
        return a != b;
    }

    @Test(dataProvider = "maskCompareOpProvider")
    static void maskXorDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa, IntFunction<boolean[]> fb) {
        boolean[] a = fa.apply(SPECIES.length());
        boolean[] b = fb.apply(SPECIES.length());
        boolean[] r = new boolean[a.length];

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var av = SPECIES.loadMask(a, i);
            var bv = SPECIES.loadMask(b, i);
            var cv = av.xor(bv);
            cv.intoArray(r, i);
        }
        assertArraysEquals(r, a, b, DoubleMaxVectorTests::bxor);
    }

    static boolean bandNot(boolean a, boolean b) {
        return a & !b;
    }

    @Test(dataProvider = "maskCompareOpProvider")
    static void maskAndNotDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa, IntFunction<boolean[]> fb) {
        boolean[] a = fa.apply(SPECIES.length());
        boolean[] b = fb.apply(SPECIES.length());
        boolean[] r = new boolean[a.length];

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var av = SPECIES.loadMask(a, i);
            var bv = SPECIES.loadMask(b, i);
            var cv = av.andNot(bv);
            cv.intoArray(r, i);
        }
        assertArraysEquals(r, a, b, DoubleMaxVectorTests::bandNot);
    }

    static boolean beq(boolean a, boolean b) {
        return (a == b);
    }

    @Test(dataProvider = "maskCompareOpProvider")
    static void maskEqDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa, IntFunction<boolean[]> fb) {
        boolean[] a = fa.apply(SPECIES.length());
        boolean[] b = fb.apply(SPECIES.length());
        boolean[] r = new boolean[a.length];

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var av = SPECIES.loadMask(a, i);
            var bv = SPECIES.loadMask(b, i);
            var cv = av.eq(bv);
            cv.intoArray(r, i);
        }
        assertArraysEquals(r, a, b, DoubleMaxVectorTests::beq);
    }

    @Test(dataProvider = "maskProvider")
    static void maskHashCodeDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa) {
        boolean[] a = fa.apply(SPECIES.length());

        for (int i = 0; i < a.length; i += SPECIES.length()) {
            var vmask = SPECIES.loadMask(a, i);
            int hash = vmask.hashCode();

            boolean subarr[] = Arrays.copyOfRange(a, i, i + SPECIES.length());
            int expectedHash = Objects.hash(SPECIES, Arrays.hashCode(subarr));
            Assert.assertTrue(hash == expectedHash, "at index " + i + ", hash should be = " + expectedHash + ", but is = " + hash);
        }
    }

    static int maskTrueCount(boolean[] a, int idx) {
        int trueCount = 0;
        for (int i = idx; i < idx + SPECIES.length(); i++) {
            trueCount += a[i] ? 1 : 0;
        }
        return trueCount;
    }

    @Test(dataProvider = "maskProvider")
    static void maskTrueCountDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa) {
        boolean[] a = fa.apply(SPECIES.length());
        int[] r = new int[a.length];

        for (int ic = 0; ic < INVOC_COUNT * INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                var vmask = SPECIES.loadMask(a, i);
                r[i] = vmask.trueCount();
            }
        }

        assertMaskReductionArraysEquals(r, a, DoubleMaxVectorTests::maskTrueCount);
    }

    static int maskLastTrue(boolean[] a, int idx) {
        int i = idx + SPECIES.length() - 1;
        for (; i >= idx; i--) {
            if (a[i]) {
                break;
            }
        }
        return i - idx;
    }

    @Test(dataProvider = "maskProvider")
    static void maskLastTrueDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa) {
        boolean[] a = fa.apply(SPECIES.length());
        int[] r = new int[a.length];

        for (int ic = 0; ic < INVOC_COUNT * INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                var vmask = SPECIES.loadMask(a, i);
                r[i] = vmask.lastTrue();
            }
        }

        assertMaskReductionArraysEquals(r, a, DoubleMaxVectorTests::maskLastTrue);
    }

    static int maskFirstTrue(boolean[] a, int idx) {
        int i = idx;
        for (; i < idx + SPECIES.length(); i++) {
            if (a[i]) {
                break;
            }
        }
        return i - idx;
    }

    @Test(dataProvider = "maskProvider")
    static void maskFirstTrueDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa) {
        boolean[] a = fa.apply(SPECIES.length());
        int[] r = new int[a.length];

        for (int ic = 0; ic < INVOC_COUNT * INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                var vmask = SPECIES.loadMask(a, i);
                r[i] = vmask.firstTrue();
            }
        }

        assertMaskReductionArraysEquals(r, a, DoubleMaxVectorTests::maskFirstTrue);
    }

    @Test(dataProvider = "maskProvider")
    static void maskCompressDoubleMaxVectorTestsSmokeTest(IntFunction<boolean[]> fa) {
        int trueCount = 0;
        boolean[] a = fa.apply(SPECIES.length());

        for (int ic = 0; ic < INVOC_COUNT * INVOC_COUNT; ic++) {
            for (int i = 0; i < a.length; i += SPECIES.length()) {
                var vmask = SPECIES.loadMask(a, i);
                trueCount = vmask.trueCount();
                var rmask = vmask.compress();
                for (int j = 0; j < SPECIES.length(); j++)  {
                    Assert.assertEquals(rmask.laneIsSet(j), j < trueCount);
                }
            }
        }
    }


    @DataProvider
    public static Object[][] offsetProvider() {
        return new Object[][]{
                {0},
                {-1},
                {+1},
                {+2},
                {-2},
        };
    }

    @Test(dataProvider = "offsetProvider")
    static void indexInRangeDoubleMaxVectorTestsSmokeTest(int offset) {
        int limit = SPECIES.length() * BUFFER_REPS;
        for (int i = 0; i < limit; i += SPECIES.length()) {
            var actualMask = SPECIES.indexInRange(i + offset, limit);
            var expectedMask = SPECIES.maskAll(true).indexInRange(i + offset, limit);
            assert(actualMask.equals(expectedMask));
            for (int j = 0; j < SPECIES.length(); j++)  {
                int index = i + j + offset;
                Assert.assertEquals(actualMask.laneIsSet(j), index >= 0 && index < limit);
            }
        }
    }

    @Test(dataProvider = "offsetProvider")
    static void indexInRangeLongDoubleMaxVectorTestsSmokeTest(int offset) {
        long limit = SPECIES.length() * BUFFER_REPS;
        for (long i = 0; i < limit; i += SPECIES.length()) {
            var actualMask = SPECIES.indexInRange(i + offset, limit);
            var expectedMask = SPECIES.maskAll(true).indexInRange(i + offset, limit);
            assert(actualMask.equals(expectedMask));
            for (int j = 0; j < SPECIES.length(); j++)  {
                long index = i + j + offset;
                Assert.assertEquals(actualMask.laneIsSet(j), index >= 0 && index < limit);
            }
        }
    }

    @DataProvider
    public static Object[][] lengthProvider() {
        return new Object[][]{
                {0},
                {1},
                {32},
                {37},
                {1024},
                {1024+1},
                {1024+5},
        };
    }

    @Test(dataProvider = "lengthProvider")
    static void loopBoundDoubleMaxVectorTestsSmokeTest(int length) {
        int actualLoopBound = SPECIES.loopBound(length);
        int expectedLoopBound = length - Math.floorMod(length, SPECIES.length());
        Assert.assertEquals(actualLoopBound, expectedLoopBound);
    }

    @Test(dataProvider = "lengthProvider")
    static void loopBoundLongDoubleMaxVectorTestsSmokeTest(int _length) {
        long length = _length;
        long actualLoopBound = SPECIES.loopBound(length);
        long expectedLoopBound = length - Math.floorMod(length, SPECIES.length());
        Assert.assertEquals(actualLoopBound, expectedLoopBound);
    }

    @Test
    static void ElementSizeDoubleMaxVectorTestsSmokeTest() {
        DoubleVector av = DoubleVector.zero(SPECIES);
        int elsize = av.elementSize();
        Assert.assertEquals(elsize, Double.SIZE);
    }

    @Test
    static void VectorShapeDoubleMaxVectorTestsSmokeTest() {
        DoubleVector av = DoubleVector.zero(SPECIES);
        VectorShape vsh = av.shape();
        assert(vsh.equals(VectorShape.S_Max_BIT));
    }

    @Test
    static void ShapeWithLanesDoubleMaxVectorTestsSmokeTest() {
        DoubleVector av = DoubleVector.zero(SPECIES);
        VectorShape vsh = av.shape();
        VectorSpecies species = vsh.withLanes(double.class);
        assert(species.equals(SPECIES));
    }

    @Test
    static void ElementTypeDoubleMaxVectorTestsSmokeTest() {
        DoubleVector av = DoubleVector.zero(SPECIES);
        assert(av.species().elementType() == double.class);
    }

    @Test
    static void SpeciesElementSizeDoubleMaxVectorTestsSmokeTest() {
        DoubleVector av = DoubleVector.zero(SPECIES);
        assert(av.species().elementSize() == Double.SIZE);
    }

    @Test
    static void VectorTypeDoubleMaxVectorTestsSmokeTest() {
        DoubleVector av = DoubleVector.zero(SPECIES);
        assert(av.species().vectorType() == av.getClass());
    }

    @Test
    static void WithLanesDoubleMaxVectorTestsSmokeTest() {
        DoubleVector av = DoubleVector.zero(SPECIES);
        VectorSpecies species = av.species().withLanes(double.class);
        assert(species.equals(SPECIES));
    }

    @Test
    static void WithShapeDoubleMaxVectorTestsSmokeTest() {
        DoubleVector av = DoubleVector.zero(SPECIES);
        VectorShape vsh = av.shape();
        VectorSpecies species = av.species().withShape(vsh);
        assert(species.equals(SPECIES));
    }

    @Test
    static void MaskAllTrueDoubleMaxVectorTestsSmokeTest() {
        for (int ic = 0; ic < INVOC_COUNT; ic++) {
          Assert.assertEquals(SPECIES.maskAll(true).toLong(), -1L >>> (64 - SPECIES.length()));
        }
    }
}
