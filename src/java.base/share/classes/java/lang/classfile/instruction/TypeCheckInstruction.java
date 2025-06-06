/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package java.lang.classfile.instruction;

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;

import jdk.internal.classfile.impl.AbstractInstruction;
import jdk.internal.classfile.impl.TemporaryConstantPool;
import jdk.internal.classfile.impl.Util;

/**
 * Models an {@link Opcode#INSTANCEOF instanceof} or a {@link Opcode#CHECKCAST checkcast}
 * instruction in the {@code code} array of a {@code Code} attribute.  Corresponding
 * opcodes have a {@linkplain Opcode#kind() kind} of {@link Opcode.Kind#TYPE_CHECK}.
 * Delivered as a {@link CodeElement} when traversing the elements of a {@link CodeModel}.
 * <p>
 * An {@code instanceof} checks the type and pushes an integer to the operand stack.
 * A {@code checkcast} checks the type and throws a {@link ClassCastException} if
 * the check fails.  {@code instanceof} treat the {@code null} reference as a
 * failure, while {@code checkcast} treat the {@code null} reference as a success.
 * <p>
 * A type check instruction is composite:
 * {@snippet lang=text :
 * // @link substring="TypeCheckInstruction" target="#of(Opcode, ClassEntry)" :
 * TypeCheckInstruction(
 *     Opcode opcode, // @link substring="opcode" target="#opcode"
 *     ClassEntry type // @link substring="type" target="#type"
 * )
 * }
 *
 * @since 24
 */
public sealed interface TypeCheckInstruction extends Instruction
        permits AbstractInstruction.BoundTypeCheckInstruction,
                AbstractInstruction.UnboundTypeCheckInstruction {

    /**
     * {@return the type against which the instruction checks}
     */
    ClassEntry type();

    /**
     * {@return a type check instruction}
     *
     * @param op the opcode for the specific type of type check instruction,
     *           which must be of kind {@link Opcode.Kind#TYPE_CHECK}
     * @param type the type against which to check or cast
     * @throws IllegalArgumentException if the opcode kind is not
     *         {@link Opcode.Kind#TYPE_CHECK}
     */
    static TypeCheckInstruction of(Opcode op, ClassEntry type) {
        Util.checkKind(op, Opcode.Kind.TYPE_CHECK);
        return new AbstractInstruction.UnboundTypeCheckInstruction(op, type);
    }

    /**
     * {@return a type check instruction}
     *
     * @param op the opcode for the specific type of type check instruction,
     *           which must be of kind {@link Opcode.Kind#TYPE_CHECK}
     * @param type the type against which to check or cast
     * @throws IllegalArgumentException if the opcode kind is not
     *         {@link Opcode.Kind#TYPE_CHECK}, or if {@code type} is primitive
     */
    static TypeCheckInstruction of(Opcode op, ClassDesc type) {
        return of(op, TemporaryConstantPool.INSTANCE.classEntry(type));
    }
}
