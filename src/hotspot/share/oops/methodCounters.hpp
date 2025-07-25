/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#ifndef SHARE_OOPS_METHODCOUNTERS_HPP
#define SHARE_OOPS_METHODCOUNTERS_HPP

#include "compiler/compilerDefinitions.hpp"
#include "interpreter/invocationCounter.hpp"
#include "oops/metadata.hpp"
#include "utilities/align.hpp"

class MethodTrainingData;

class MethodCounters : public Metadata {
 friend class VMStructs;
 friend class JVMCIVMStructs;

 // Used by CDS. These classes need to access the private default constructor.
 template <class T> friend class CppVtableTesterA;
 template <class T> friend class CppVtableTesterB;
 template <class T> friend class CppVtableCloner;

 private:
  InvocationCounter _invocation_counter;         // Incremented before each activation of the method - used to trigger frequency-based optimizations
  InvocationCounter _backedge_counter;           // Incremented before each backedge taken - used to trigger frequency-based optimizations

  // Back pointer to the Method*
  Method* _method;

  Metadata*         _method_training_data;
  jlong             _prev_time;                   // Previous time the rate was acquired
  float             _rate;                        // Events (invocation and backedge counter increments) per millisecond
  int               _invoke_mask;                 // per-method Tier0InvokeNotifyFreqLog
  int               _backedge_mask;               // per-method Tier0BackedgeNotifyFreqLog
  int               _prev_event_count;            // Total number of events saved at previous callback
#if COMPILER2_OR_JVMCI
  u2                _interpreter_throwout_count; // Count of times method was exited via exception while interpreting
#endif
#if INCLUDE_JVMTI
  u2                _number_of_breakpoints;      // fullspeed debugging support
#endif
  u1                _highest_comp_level;          // Highest compile level this method has ever seen.
  u1                _highest_osr_comp_level;      // Same for OSR level

  MethodCounters(const methodHandle& mh);
  MethodCounters();

 public:
  virtual bool is_methodCounters() const { return true; }
  Method* method() const { return _method; }
  static MethodCounters* allocate_no_exception(const methodHandle& mh);
  static MethodCounters* allocate_with_exception(const methodHandle& mh, TRAPS);

  void deallocate_contents(ClassLoaderData* loader_data) {}

  static int method_counters_size() {
    return align_up((int)sizeof(MethodCounters), wordSize) / wordSize;
  }
  virtual int size() const {
    return method_counters_size();
  }

  MetaspaceObj::Type type() const { return MethodCountersType; }
  void metaspace_pointers_do(MetaspaceClosure* iter);

  void clear_counters();

#if COMPILER2_OR_JVMCI
  void interpreter_throwout_increment() {
    if (_interpreter_throwout_count < 65534) {
      _interpreter_throwout_count++;
    }
  }
  u2  interpreter_throwout_count() const {
    return _interpreter_throwout_count;
  }
  void set_interpreter_throwout_count(u2 count) {
    _interpreter_throwout_count = count;
  }
#else // COMPILER2_OR_JVMCI
  u2  interpreter_throwout_count() const {
    return 0;
  }
  void set_interpreter_throwout_count(u2 count) {
    assert(count == 0, "count must be 0");
  }
#endif // COMPILER2_OR_JVMCI

#if INCLUDE_JVMTI
  u2   number_of_breakpoints() const   { return _number_of_breakpoints; }
  void incr_number_of_breakpoints()    { ++_number_of_breakpoints; }
  void decr_number_of_breakpoints()    { --_number_of_breakpoints; }
  void clear_number_of_breakpoints()   { _number_of_breakpoints = 0; }
#endif

  int prev_event_count() const                   { return _prev_event_count;  }
  void set_prev_event_count(int count)           { _prev_event_count = count; }
  jlong prev_time() const                        { return _prev_time; }
  void set_prev_time(jlong time)                 { _prev_time = time; }
  float rate() const                             { return _rate; }
  void set_rate(float rate)                      { _rate = rate; }

  int highest_comp_level() const                 { return _highest_comp_level;  }
  void set_highest_comp_level(int level)         { _highest_comp_level = (u1)level; }
  int highest_osr_comp_level() const             { return _highest_osr_comp_level;  }
  void set_highest_osr_comp_level(int level)     { _highest_osr_comp_level = (u1)level; }

  // invocation counter
  InvocationCounter* invocation_counter() { return &_invocation_counter; }
  InvocationCounter* backedge_counter()   { return &_backedge_counter; }

  static ByteSize invocation_counter_offset()    {
    return byte_offset_of(MethodCounters, _invocation_counter);
  }

  static ByteSize backedge_counter_offset()      {
    return byte_offset_of(MethodCounters, _backedge_counter);
  }

  static ByteSize invoke_mask_offset() {
    return byte_offset_of(MethodCounters, _invoke_mask);
  }

  static ByteSize backedge_mask_offset() {
    return byte_offset_of(MethodCounters, _backedge_mask);
  }

  virtual const char* internal_name() const { return "{method counters}"; }

  Metadata* method_training_data_sentinel() {
    return this;
  }
  MethodTrainingData* method_training_data() const {
    return reinterpret_cast<MethodTrainingData*>(_method_training_data);
  }
  bool init_method_training_data(MethodTrainingData* td) {
    MethodTrainingData* cur = method_training_data();
    if (cur == td) {
      return true;
    }
    if (cur == nullptr || cur == reinterpret_cast<MethodTrainingData*>(method_training_data_sentinel())) {
      return Atomic::cmpxchg(reinterpret_cast<MethodTrainingData**>(&_method_training_data), cur, td) == cur;
    }
    return false;
  }

#if INCLUDE_CDS
  void remove_unshareable_info();
  void restore_unshareable_info(TRAPS);
#endif

  // Printing
  void print_on      (outputStream* st) const;
  void print_value_on(outputStream* st) const;
  void print_data_on(outputStream* st) const;
};
#endif // SHARE_OOPS_METHODCOUNTERS_HPP
