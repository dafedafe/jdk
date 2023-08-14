/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

package micro.org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 3)
@Measurement(iterations = 5, time = 3)
@Fork(3)
public class StringConstructor {

  @Param({"7", "64"})
  public int size;

  // Offset to use for ranged newStrings
  @Param("1")
  public int offset;
  private byte[] array;

  private char[] chars;
  private char[] charsMixedAtStart;
  private char[] charsMixedAtMiddle;
  private int[] codePointsMixedAtStart;
  private int[] codePointsMixedAtMiddle;

  private static char INTEROBANG = 0x2030;

  @Setup
  public void setup() {
      if (offset > size) {
        offset = size;
      }
      String s = "a".repeat(size);
      array = s.getBytes(StandardCharsets.UTF_8);
      chars = s.toCharArray();
      charsMixedAtStart = Arrays.copyOf(chars, array.length);
      charsMixedAtStart[0] = INTEROBANG;
      charsMixedAtMiddle = Arrays.copyOf(chars, array.length);
      charsMixedAtMiddle[charsMixedAtMiddle.length/2] = INTEROBANG;

      codePointsMixedAtStart = intCopyOfChars(chars, array.length);
      codePointsMixedAtStart[0] = INTEROBANG;
      codePointsMixedAtMiddle = intCopyOfChars(chars, array.length);
      codePointsMixedAtMiddle[codePointsMixedAtMiddle.length/2] = INTEROBANG;
  }

  private static int[] intCopyOfChars(char[] chars, int newLength) {
      int[] res = new int[newLength];
      for (int i = 0; i < Math.min(chars.length, newLength); i++)
          res[i] = (int)chars[i];
      return res;
  }

  @Benchmark
  public String newStringFromArray() {
      return new String(array);
  }

  @Benchmark
  public String newStringFromRangedArray() {
      return new String(array, offset, array.length - offset);
  }

  @Benchmark
  public String newStringFromArrayWithCharsetUTF8() {
      return new String(array, StandardCharsets.UTF_8);
  }

  @Benchmark
  public String newStringFromArrayWithCharsetNameUTF8() throws Exception {
      return new String(array, StandardCharsets.UTF_8.name());
  }

  @Benchmark
  public String newStringFromRangedArrayWithCharsetUTF8() {
      return new String(array, offset, array.length - offset, StandardCharsets.UTF_8);
  }

  @Benchmark
  public String newStringFromRangedCharLatin1() {
      return new String(chars, offset, chars.length - offset);
  }

  @Benchmark
  public String newStringFromRangedCharMixedAtStart() {
      return new String(charsMixedAtStart, 0, charsMixedAtStart.length);
  }

  @Benchmark
  public String newStringFromRangedCharMixedAtMiddle() {
      return new String(charsMixedAtMiddle, offset, charsMixedAtMiddle.length - offset);
  }

  @Benchmark
  public String newStringFromRangedCodePointMixedAtStart() {
      return new String(codePointsMixedAtStart, 0, codePointsMixedAtStart.length);
  }

  @Benchmark
  public String newStringFromRangedCodePointMixedAtMiddle() {
      return new String(codePointsMixedAtMiddle, offset, codePointsMixedAtMiddle.length - offset);
  }

}
