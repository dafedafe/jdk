/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "logging/log.hpp"
#include "nmt/memTracker.hpp"
#include "runtime/globals.hpp"
#include "runtime/javaThread.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "runtime/osThread.hpp"
#include "runtime/safepointMechanism.inline.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "utilities/globalDefinitions.hpp"
#if INCLUDE_JFR
#include "jfr/jfr.inline.hpp"
#endif

uintptr_t SafepointMechanism::_poll_word_armed_value;
uintptr_t SafepointMechanism::_poll_word_disarmed_value;
uintptr_t SafepointMechanism::_poll_page_armed_value;
uintptr_t SafepointMechanism::_poll_page_disarmed_value;
address SafepointMechanism::_polling_page;

void SafepointMechanism::default_initialize() {
  // Poll bit values
  _poll_word_armed_value    = poll_bit();
  _poll_word_disarmed_value = ~_poll_word_armed_value;

  bool poll_bit_only = false;

#ifdef USE_POLL_BIT_ONLY
  poll_bit_only = USE_POLL_BIT_ONLY;
#endif

  if (poll_bit_only) {
    _poll_page_armed_value    = poll_bit();
    _poll_page_disarmed_value = 0;
  } else {
    // Polling page
    const size_t page_size = os::vm_page_size();
    const size_t allocation_size = 2 * page_size;
    char* polling_page = os::reserve_memory(allocation_size, mtSafepoint);
    os::commit_memory_or_exit(polling_page, allocation_size, !ExecMem, "Unable to commit Safepoint polling page");

    char* bad_page  = polling_page;
    char* good_page = polling_page + page_size;

    os::protect_memory(bad_page,  page_size, os::MEM_PROT_NONE);
    os::protect_memory(good_page, page_size, os::MEM_PROT_READ);

    log_info(os)("SafePoint Polling address, bad (protected) page:" INTPTR_FORMAT ", good (unprotected) page:" INTPTR_FORMAT, p2i(bad_page), p2i(good_page));

    // Poll address values
    _poll_page_armed_value    = reinterpret_cast<uintptr_t>(bad_page);
    _poll_page_disarmed_value = reinterpret_cast<uintptr_t>(good_page);
    _polling_page = (address)bad_page;
  }
}

uintptr_t SafepointMechanism::compute_poll_word(bool armed, uintptr_t stack_watermark) {
  if (armed) {
    log_debug(stackbarrier)("Computed armed for tid %d", Thread::current()->osthread()->thread_id());
    return _poll_word_armed_value;
  }
  if (stack_watermark == 0) {
    log_debug(stackbarrier)("Computed disarmed for tid %d", Thread::current()->osthread()->thread_id());
    return _poll_word_disarmed_value;
  }
  log_debug(stackbarrier)("Computed watermark for tid %d", Thread::current()->osthread()->thread_id());
  return stack_watermark;
}

void SafepointMechanism::update_poll_values(JavaThread* thread) {
  assert(thread == Thread::current(), "Must be");
  assert(thread->thread_state() != _thread_blocked, "Must not be");
  assert(thread->thread_state() != _thread_in_native, "Must not be");

  for (;;) {
    bool armed = has_pending_safepoint(thread);
    uintptr_t stack_watermark = StackWatermarkSet::lowest_watermark(thread);
    uintptr_t poll_page = armed ? _poll_page_armed_value
                                : _poll_page_disarmed_value;
    uintptr_t poll_word = compute_poll_word(armed, stack_watermark);
    uintptr_t prev_poll_word = thread->poll_data()->get_polling_word();

    if (prev_poll_word != poll_word ||
        prev_poll_word == _poll_word_armed_value) {
      // While updating the poll value, we allow entering new nmethods
      // through stack unwinding. The nmethods might have been processed in
      // a concurrent thread by the GC. So we need to run a cross modify
      // fence to ensure patching becomes visible. We may also wake up from
      // a safepoint that has patched code. This cross modify fence will
      // ensure such paths can observe patched code.
      // Note that while other threads may arm the thread-local poll of
      // a thread, only the thread itself has permission to disarm its own
      // poll value, in any way making it less restrictive. Therefore, whenever
      // the frontier of what the mutator allows itself to do is increased,
      // we will catch that here, and ensure a cross modifying fence is used.
      OrderAccess::cross_modify_fence();
    }

    thread->poll_data()->set_polling_page(poll_page);
    thread->poll_data()->set_polling_word(poll_word);
    OrderAccess::fence();
    if (!armed && has_pending_safepoint(thread)) {
      // We disarmed an old safepoint, but a new one is synchronizing.
      // We need to arm the poll for the subsequent safepoint poll.
      continue;
    }
    break;
  }
}

void SafepointMechanism::process(JavaThread *thread, bool allow_suspend, bool check_async_exception) {
  DEBUG_ONLY(intptr_t* sp_before = thread->last_Java_sp();)
  // Read global poll and has_handshake after local poll
  OrderAccess::loadload();

  // local poll already checked, if used.
  bool need_rechecking;
  do {
    JavaThreadState state = thread->thread_state();
    guarantee(state == _thread_in_vm, "Illegal threadstate encountered: %d", state);
    if (global_poll()) {
      // Any load in ::block() must not pass the global poll load.
      // Otherwise we might load an old safepoint counter (for example).
      OrderAccess::loadload();
      SafepointSynchronize::block(thread);
    }

    // The call to on_safepoint fixes the thread's oops and the first few frames.
    //
    // The call has been carefully placed here to cater to a few situations:
    // 1) After we exit from block after a global poll
    // 2) After a thread races with the disarming of the global poll and transitions from native/blocked
    // 3) Before the handshake code is run
    StackWatermarkSet::on_safepoint(thread);

    need_rechecking = thread->handshake_state()->has_operation() && thread->handshake_state()->process_by_self(allow_suspend, check_async_exception);
  } while (need_rechecking);

  JFR_ONLY(Jfr::check_and_process_sample_request(thread);)
  update_poll_values(thread);
  assert(sp_before == thread->last_Java_sp(), "Anchor has changed");
}

void SafepointMechanism::initialize_header(JavaThread* thread) {
  disarm_local_poll(thread);
}

void SafepointMechanism::initialize() {
  pd_initialize();
}
