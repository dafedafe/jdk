/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_KLASS_HPP
#define SHARE_OOPS_KLASS_HPP

#include "oops/klassFlags.hpp"
#include "oops/markWord.hpp"
#include "oops/metadata.hpp"
#include "oops/oop.hpp"
#include "oops/oopHandle.hpp"
#include "utilities/accessFlags.hpp"
#include "utilities/macros.hpp"
#if INCLUDE_JFR
#include "jfr/support/jfrTraceIdExtension.hpp"
#endif

//
// A Klass provides:
//  1: language level class object (method dictionary etc.)
//  2: provide vm dispatch behavior for the object
// Both functions are combined into one C++ class.

// One reason for the oop/klass dichotomy in the implementation is
// that we don't want a C++ vtbl pointer in every object.  Thus,
// normal oops don't have any virtual functions.  Instead, they
// forward all "virtual" functions to their klass, which does have
// a vtbl and does the C++ dispatch depending on the object's
// actual type.  (See oop.inline.hpp for some of the forwarding code.)
// ALL FUNCTIONS IMPLEMENTING THIS DISPATCH ARE PREFIXED WITH "oop_"!

// Forward declarations.
template <class T> class Array;
template <class T> class GrowableArray;
class ClassLoaderData;
class fieldDescriptor;
class klassVtable;
class ModuleEntry;
class PackageEntry;
class vtableEntry;

class Klass : public Metadata {

  friend class VMStructs;
  friend class JVMCIVMStructs;
 public:
  // Klass Kinds for all subclasses of Klass
  enum KlassKind : u2 {
    InstanceKlassKind,
    InstanceRefKlassKind,
    InstanceMirrorKlassKind,
    InstanceClassLoaderKlassKind,
    InstanceStackChunkKlassKind,
    TypeArrayKlassKind,
    ObjArrayKlassKind,
    UnknownKlassKind
  };

  static const uint KLASS_KIND_COUNT = ObjArrayKlassKind + 1;
 protected:

  // If you add a new field that points to any metaspace object, you
  // must add this field to Klass::metaspace_pointers_do().

  // note: put frequently-used fields together at start of klass structure
  // for better cache behavior (may not make much of a difference but sure won't hurt)
  enum { _primary_super_limit = 8 };

  // The "layout helper" is a combined descriptor of object layout.
  // For klasses which are neither instance nor array, the value is zero.
  //
  // For instances, layout helper is a positive number, the instance size.
  // This size is already passed through align_object_size and scaled to bytes.
  // The low order bit is set if instances of this class cannot be
  // allocated using the fastpath.
  //
  // For arrays, layout helper is a negative number, containing four
  // distinct bytes, as follows:
  //    MSB:[tag, hsz, ebt, log2(esz)]:LSB
  // where:
  //    tag is 0x80 if the elements are oops, 0xC0 if non-oops
  //    hsz is array header size in bytes (i.e., offset of first element)
  //    ebt is the BasicType of the elements
  //    esz is the element size in bytes
  // This packed word is arranged so as to be quickly unpacked by the
  // various fast paths that use the various subfields.
  //
  // The esz bits can be used directly by a SLL instruction, without masking.
  //
  // Note that the array-kind tag looks like 0x00 for instance klasses,
  // since their length in bytes is always less than 24Mb.
  //
  // Final note:  This comes first, immediately after C++ vtable,
  // because it is frequently queried.
  jint _layout_helper;

  // Klass kind used to resolve the runtime type of the instance.
  //  - Used to implement devirtualized oop closure dispatching.
  //  - Various type checking in the JVM
  const KlassKind _kind;

  AccessFlags _access_flags;    // Access flags. The class/interface distinction is stored here.
                                // Some flags created by the JVM, not in the class file itself,
                                // are in _misc_flags below.
  KlassFlags  _misc_flags;

  // The fields _super_check_offset, _secondary_super_cache, _secondary_supers
  // and _primary_supers all help make fast subtype checks.  See big discussion
  // in doc/server_compiler/checktype.txt
  //
  // Where to look to observe a supertype (it is &_secondary_super_cache for
  // secondary supers, else is &_primary_supers[depth()].
  juint       _super_check_offset;

  // Class name.  Instance classes: java/lang/String, etc.  Array classes: [I,
  // [Ljava/lang/String;, etc.  Set to zero for all other kinds of classes.
  Symbol*     _name;

  // Cache of last observed secondary supertype
  Klass*      _secondary_super_cache;
  // Array of all secondary supertypes
  Array<Klass*>* _secondary_supers;
  // Ordered list of all primary supertypes
  Klass*      _primary_supers[_primary_super_limit];
  // java/lang/Class instance mirroring this class
  OopHandle   _java_mirror;
  // Superclass
  Klass*      _super;
  // First subclass (null if none); _subklass->next_sibling() is next one
  Klass* volatile _subklass;
  // Sibling link (or null); links all subklasses of a klass
  Klass* volatile _next_sibling;

  // All klasses loaded by a class loader are chained through these links
  Klass*      _next_link;

  // The VM's representation of the ClassLoader used to load this class.
  // Provide access the corresponding instance java.lang.ClassLoader.
  ClassLoaderData* _class_loader_data;

  markWord _prototype_header;   // Used to initialize objects' header

  // Bitmap and hash code used by hashed secondary supers.
  uintx    _secondary_supers_bitmap;
  uint8_t  _hash_slot;

private:
  // This is an index into AOTClassLocationConfig::class_locations(), to
  // indicate the AOTClassLocation where this class is loaded from during
  // dump time. If a class is not loaded from the AOT cache, this field is
  // -1.
  s2 _shared_class_path_index;

#if INCLUDE_CDS
  // Various attributes for shared classes. Should be zero for a non-shared class.
  u2     _shared_class_flags;
  enum CDSSharedClassFlags {
    _is_shared_class                       = 1 << 0,  // shadows MetaspaceObj::is_shared
    _archived_lambda_proxy_is_available    = 1 << 1,
    _has_value_based_class_annotation      = 1 << 2,
    _verified_at_dump_time                 = 1 << 3,
    _has_archived_enum_objs                = 1 << 4,
    // This class was not loaded from a classfile in the module image
    // or classpath.
    _is_generated_shared_class             = 1 << 5,
    // archived mirror already initialized by AOT-cache assembly: no further need to call <clinit>
    _has_aot_initialized_mirror            = 1 << 6,
  };
#endif

  int _vtable_len;              // vtable length. This field may be read very often when we
                                // have lots of itable dispatches (e.g., lambdas and streams).
                                // Keep it away from the beginning of a Klass to avoid cacheline
                                // contention that may happen when a nearby object is modified.

  CDS_JAVA_HEAP_ONLY(int _archived_mirror_index;)

public:

  JFR_ONLY(DEFINE_TRACE_ID_FIELD;)

protected:

  Klass(KlassKind kind);
  Klass();

 public:
  int kind() { return _kind; }

  enum class DefaultsLookupMode { find, skip };
  enum class OverpassLookupMode { find, skip };
  enum class StaticLookupMode   { find, skip };
  enum class PrivateLookupMode  { find, skip };

  virtual bool is_klass() const { return true; }

  // super() cannot be InstanceKlass* -- Java arrays are covariant, and _super is used
  // to implement that. NB: the _super of "[Ljava/lang/Integer;" is "[Ljava/lang/Number;"
  // If this is not what your code expects, you're probably looking for Klass::java_super().
  Klass* super() const               { return _super; }
  void set_super(Klass* k)           { _super = k; }

  // initializes _super link, _primary_supers & _secondary_supers arrays
  void initialize_supers(Klass* k, Array<InstanceKlass*>* transitive_interfaces, TRAPS);

  // klass-specific helper for initializing _secondary_supers
  virtual GrowableArray<Klass*>* compute_secondary_supers(int num_extra_slots,
                                                          Array<InstanceKlass*>* transitive_interfaces);

  // java_super is the Java-level super type as specified by Class.getSuperClass.
  virtual InstanceKlass* java_super() const  { return nullptr; }

  juint    super_check_offset() const  { return _super_check_offset; }
  void set_super_check_offset(juint o) { _super_check_offset = o; }

  Klass* secondary_super_cache() const     { return _secondary_super_cache; }
  void set_secondary_super_cache(Klass* k) { _secondary_super_cache = k; }

  Array<Klass*>* secondary_supers() const { return _secondary_supers; }
  void set_secondary_supers(Array<Klass*>* k, uintx bitmap);

  uint8_t hash_slot() const { return _hash_slot; }

  // Return the element of the _super chain of the given depth.
  // If there is no such element, return either null or this.
  Klass* primary_super_of_depth(juint i) const {
    assert(i < primary_super_limit(), "oob");
    Klass* super = _primary_supers[i];
    assert(super == nullptr || super->super_depth() == i, "correct display");
    return super;
  }

  // Can this klass be a primary super?  False for interfaces and arrays of
  // interfaces.  False also for arrays or classes with long super chains.
  bool can_be_primary_super() const {
    const juint secondary_offset = in_bytes(secondary_super_cache_offset());
    return super_check_offset() != secondary_offset;
  }
  virtual bool can_be_primary_super_slow() const;

  // Returns number of primary supers; may be a number in the inclusive range [0, primary_super_limit].
  juint super_depth() const {
    if (!can_be_primary_super()) {
      return primary_super_limit();
    } else {
      juint d = (super_check_offset() - in_bytes(primary_supers_offset())) / sizeof(Klass*);
      assert(d < primary_super_limit(), "oob");
      assert(_primary_supers[d] == this, "proper init");
      return d;
    }
  }

  // java mirror
  oop java_mirror() const;
  oop java_mirror_no_keepalive() const;
  void set_java_mirror(Handle m);

  oop archived_java_mirror() NOT_CDS_JAVA_HEAP_RETURN_(nullptr);

  // Temporary mirror switch used by RedefineClasses
  OopHandle java_mirror_handle() const { return _java_mirror; }
  void swap_java_mirror_handle(OopHandle& mirror) { _java_mirror.swap(mirror); }

  // Set java mirror OopHandle to null for CDS
  // This leaves the OopHandle in the CLD, but that's ok, you can't release them.
  void clear_java_mirror_handle() { _java_mirror = OopHandle(); }

  // size helper
  int layout_helper() const            { return _layout_helper; }
  void set_layout_helper(int lh)       { _layout_helper = lh; }

  // Note: for instances layout_helper() may include padding.
  // Use InstanceKlass::contains_field_offset to classify field offsets.

  // sub/superklass links
  Klass* subklass(bool log = false) const;
  Klass* next_sibling(bool log = false) const;

  InstanceKlass* superklass() const;
  void append_to_sibling_list();           // add newly created receiver to superklass' subklass list

  void set_next_link(Klass* k) { _next_link = k; }
  Klass* next_link() const { return _next_link; }   // The next klass defined by the class loader.
  Klass** next_link_addr() { return &_next_link; }

  // class loader data
  ClassLoaderData* class_loader_data() const               { return _class_loader_data; }
  void set_class_loader_data(ClassLoaderData* loader_data) {  _class_loader_data = loader_data; }

  s2 shared_classpath_index() const   {
    return _shared_class_path_index;
  };

  void set_shared_classpath_index(s2 index) {
    _shared_class_path_index = index;
  };

  bool has_archived_mirror_index() const {
    CDS_JAVA_HEAP_ONLY(return _archived_mirror_index >= 0;)
    NOT_CDS_JAVA_HEAP(return false);
  }

  void clear_archived_mirror_index() NOT_CDS_JAVA_HEAP_RETURN;

  void set_lambda_proxy_is_available() {
    CDS_ONLY(_shared_class_flags |= _archived_lambda_proxy_is_available;)
  }
  void clear_lambda_proxy_is_available() {
    CDS_ONLY(_shared_class_flags &= (u2)(~_archived_lambda_proxy_is_available);)
  }
  bool lambda_proxy_is_available() const {
    CDS_ONLY(return (_shared_class_flags & _archived_lambda_proxy_is_available) != 0;)
    NOT_CDS(return false;)
  }

  void set_has_value_based_class_annotation() {
    CDS_ONLY(_shared_class_flags |= _has_value_based_class_annotation;)
  }
  void clear_has_value_based_class_annotation() {
    CDS_ONLY(_shared_class_flags &= (u2)(~_has_value_based_class_annotation);)
  }
  bool has_value_based_class_annotation() const {
    CDS_ONLY(return (_shared_class_flags & _has_value_based_class_annotation) != 0;)
    NOT_CDS(return false;)
  }

  void set_verified_at_dump_time() {
    CDS_ONLY(_shared_class_flags |= _verified_at_dump_time;)
  }
  bool verified_at_dump_time() const {
    CDS_ONLY(return (_shared_class_flags & _verified_at_dump_time) != 0;)
    NOT_CDS(return false;)
  }

  void set_has_archived_enum_objs() {
    CDS_ONLY(_shared_class_flags |= _has_archived_enum_objs;)
  }
  bool has_archived_enum_objs() const {
    CDS_ONLY(return (_shared_class_flags & _has_archived_enum_objs) != 0;)
    NOT_CDS(return false;)
  }

  void set_is_generated_shared_class() {
    CDS_ONLY(_shared_class_flags |= _is_generated_shared_class;)
  }
  bool is_generated_shared_class() const {
    CDS_ONLY(return (_shared_class_flags & _is_generated_shared_class) != 0;)
    NOT_CDS(return false;)
  }

  void set_has_aot_initialized_mirror() {
    CDS_ONLY(_shared_class_flags |= _has_aot_initialized_mirror;)
  }
  bool has_aot_initialized_mirror() const {
    CDS_ONLY(return (_shared_class_flags & _has_aot_initialized_mirror) != 0;)
    NOT_CDS(return false;)
  }

  bool is_shared() const                { // shadows MetaspaceObj::is_shared)()
    CDS_ONLY(return (_shared_class_flags & _is_shared_class) != 0;)
    NOT_CDS(return false;)
  }

  void set_is_shared() {
    CDS_ONLY(_shared_class_flags |= _is_shared_class;)
  }

  // Obtain the module or package for this class
  virtual ModuleEntry* module() const = 0;
  virtual PackageEntry* package() const = 0;

 protected:                                // internal accessors
  void     set_subklass(Klass* s);
  void     set_next_sibling(Klass* s);

 private:
  static uint8_t compute_hash_slot(Symbol* s);
  static void  hash_insert(Klass* klass, GrowableArray<Klass*>* secondaries, uintx& bitmap);
  static uintx hash_secondary_supers(Array<Klass*>* secondaries, bool rewrite);

  bool search_secondary_supers(Klass* k) const;
  bool lookup_secondary_supers_table(Klass *k) const;
  bool linear_search_secondary_supers(const Klass* k) const;
  bool fallback_search_secondary_supers(const Klass* k, int index, uintx rotated_bitmap) const;

 public:
  // Secondary supers table support
  static Array<Klass*>* pack_secondary_supers(ClassLoaderData* loader_data,
                                              GrowableArray<Klass*>* primaries,
                                              GrowableArray<Klass*>* secondaries,
                                              uintx& bitmap,
                                              TRAPS);

  static uintx   compute_secondary_supers_bitmap(Array<Klass*>* secondary_supers);
  static uint8_t compute_home_slot(Klass* k, uintx bitmap);

  static constexpr int SECONDARY_SUPERS_TABLE_SIZE = sizeof(_secondary_supers_bitmap) * 8;
  static constexpr int SECONDARY_SUPERS_TABLE_MASK = SECONDARY_SUPERS_TABLE_SIZE - 1;

  static constexpr uintx SECONDARY_SUPERS_BITMAP_EMPTY    = 0;
  static constexpr uintx SECONDARY_SUPERS_BITMAP_FULL     = ~(uintx)0;

  // Compiler support
  static ByteSize super_offset()                 { return byte_offset_of(Klass, _super); }
  static ByteSize super_check_offset_offset()    { return byte_offset_of(Klass, _super_check_offset); }
  static ByteSize primary_supers_offset()        { return byte_offset_of(Klass, _primary_supers); }
  static ByteSize secondary_super_cache_offset() { return byte_offset_of(Klass, _secondary_super_cache); }
  static ByteSize secondary_supers_offset()      { return byte_offset_of(Klass, _secondary_supers); }
  static ByteSize java_mirror_offset()           { return byte_offset_of(Klass, _java_mirror); }
  static ByteSize class_loader_data_offset()     { return byte_offset_of(Klass, _class_loader_data); }
  static ByteSize layout_helper_offset()         { return byte_offset_of(Klass, _layout_helper); }
  static ByteSize access_flags_offset()          { return byte_offset_of(Klass, _access_flags); }
#if INCLUDE_JVMCI
  static ByteSize subklass_offset()              { return byte_offset_of(Klass, _subklass); }
  static ByteSize next_sibling_offset()          { return byte_offset_of(Klass, _next_sibling); }
#endif
  static ByteSize secondary_supers_bitmap_offset()
                                                 { return byte_offset_of(Klass, _secondary_supers_bitmap); }
  static ByteSize hash_slot_offset()             { return byte_offset_of(Klass, _hash_slot); }
  static ByteSize misc_flags_offset()            { return byte_offset_of(Klass, _misc_flags._flags); }

  // Unpacking layout_helper:
  static const int _lh_neutral_value           = 0;  // neutral non-array non-instance value
  static const int _lh_instance_slow_path_bit  = 0x01;
  static const int _lh_log2_element_size_shift = BitsPerByte*0;
  static const int _lh_log2_element_size_mask  = BitsPerLong-1;
  static const int _lh_element_type_shift      = BitsPerByte*1;
  static const int _lh_element_type_mask       = right_n_bits(BitsPerByte);  // shifted mask
  static const int _lh_header_size_shift       = BitsPerByte*2;
  static const int _lh_header_size_mask        = right_n_bits(BitsPerByte);  // shifted mask
  static const int _lh_array_tag_bits          = 2;
  static const int _lh_array_tag_shift         = BitsPerInt - _lh_array_tag_bits;
  static const int _lh_array_tag_obj_value     = ~0x01;   // 0x80000000 >> 30

  static const unsigned int _lh_array_tag_type_value = 0Xffffffff; // ~0x00,  // 0xC0000000 >> 30

  static int layout_helper_size_in_bytes(jint lh) {
    assert(lh > (jint)_lh_neutral_value, "must be instance");
    return (int) lh & ~_lh_instance_slow_path_bit;
  }
  static bool layout_helper_needs_slow_path(jint lh) {
    assert(lh > (jint)_lh_neutral_value, "must be instance");
    return (lh & _lh_instance_slow_path_bit) != 0;
  }
  static bool layout_helper_is_instance(jint lh) {
    return (jint)lh > (jint)_lh_neutral_value;
  }
  static bool layout_helper_is_array(jint lh) {
    return (jint)lh < (jint)_lh_neutral_value;
  }
  static bool layout_helper_is_typeArray(jint lh) {
    // _lh_array_tag_type_value == (lh >> _lh_array_tag_shift);
    return (juint)lh >= (juint)(_lh_array_tag_type_value << _lh_array_tag_shift);
  }
  static bool layout_helper_is_objArray(jint lh) {
    // _lh_array_tag_obj_value == (lh >> _lh_array_tag_shift);
    return (jint)lh < (jint)(_lh_array_tag_type_value << _lh_array_tag_shift);
  }
  static int layout_helper_header_size(jint lh) {
    assert(lh < (jint)_lh_neutral_value, "must be array");
    int hsize = (lh >> _lh_header_size_shift) & _lh_header_size_mask;
    assert(hsize > 0 && hsize < (int)sizeof(oopDesc)*3, "sanity");
    return hsize;
  }
  static BasicType layout_helper_element_type(jint lh) {
    assert(lh < (jint)_lh_neutral_value, "must be array");
    int btvalue = (lh >> _lh_element_type_shift) & _lh_element_type_mask;
    assert(btvalue >= T_BOOLEAN && btvalue <= T_OBJECT, "sanity");
    return (BasicType) btvalue;
  }

  // Want a pattern to quickly diff against layout header in register
  // find something less clever!
  static int layout_helper_boolean_diffbit() {
    jint zlh = array_layout_helper(T_BOOLEAN);
    jint blh = array_layout_helper(T_BYTE);
    assert(zlh != blh, "array layout helpers must differ");
    int diffbit = 1;
    while ((diffbit & (zlh ^ blh)) == 0 && (diffbit & zlh) == 0) {
      diffbit <<= 1;
      assert(diffbit != 0, "make sure T_BOOLEAN has a different bit than T_BYTE");
    }
    return diffbit;
  }

  static int layout_helper_log2_element_size(jint lh) {
    assert(lh < (jint)_lh_neutral_value, "must be array");
    int l2esz = (lh >> _lh_log2_element_size_shift) & _lh_log2_element_size_mask;
    assert(l2esz <= LogBytesPerLong,
           "sanity. l2esz: 0x%x for lh: 0x%x", (uint)l2esz, (uint)lh);
    return l2esz;
  }
  static jint array_layout_helper(jint tag, int hsize, BasicType etype, int log2_esize) {
    return (tag        << _lh_array_tag_shift)
      |    (hsize      << _lh_header_size_shift)
      |    ((int)etype << _lh_element_type_shift)
      |    (log2_esize << _lh_log2_element_size_shift);
  }
  static jint instance_layout_helper(jint size, bool slow_path_flag) {
    return (size << LogBytesPerWord)
      |    (slow_path_flag ? _lh_instance_slow_path_bit : 0);
  }
  static int layout_helper_to_size_helper(jint lh) {
    assert(lh > (jint)_lh_neutral_value, "must be instance");
    // Note that the following expression discards _lh_instance_slow_path_bit.
    return lh >> LogBytesPerWord;
  }
  // Out-of-line version computes everything based on the etype:
  static jint array_layout_helper(BasicType etype);

  // What is the maximum number of primary superclasses any klass can have?
  static juint primary_super_limit()         { return _primary_super_limit; }

  // vtables
  klassVtable vtable() const;
  int vtable_length() const { return _vtable_len; }

  // subclass check
  bool is_subclass_of(const Klass* k) const;

  // subtype check: true if is_subclass_of, or if k is interface and receiver implements it
  bool is_subtype_of(Klass* k) const;

public:
  // Find LCA in class hierarchy
  Klass *LCA( Klass *k );

  // Check whether reflection/jni/jvm code is allowed to instantiate this class;
  // if not, throw either an Error or an Exception.
  virtual void check_valid_for_instantiation(bool throwError, TRAPS);

  // array copying
  virtual void  copy_array(arrayOop s, int src_pos, arrayOop d, int dst_pos, int length, TRAPS);

  // tells if the class should be initialized
  virtual bool should_be_initialized() const    { return false; }
  // initializes the klass
  virtual void initialize(TRAPS);
  virtual Klass* find_field(Symbol* name, Symbol* signature, fieldDescriptor* fd) const;
  virtual Method* uncached_lookup_method(const Symbol* name, const Symbol* signature,
                                         OverpassLookupMode overpass_mode,
                                         PrivateLookupMode = PrivateLookupMode::find) const;
 public:
  Method* lookup_method(const Symbol* name, const Symbol* signature) const {
    return uncached_lookup_method(name, signature, OverpassLookupMode::find);
  }

  // array class with specific rank
  virtual ArrayKlass* array_klass(int rank, TRAPS) = 0;

  // array class with this klass as element type
  virtual ArrayKlass* array_klass(TRAPS) = 0;

  // These will return null instead of allocating on the heap:
  virtual ArrayKlass* array_klass_or_null(int rank) = 0;
  virtual ArrayKlass* array_klass_or_null() = 0;

  virtual oop protection_domain() const = 0;

  oop class_loader() const;

  inline oop klass_holder() const;

  inline void keep_alive() const;

 protected:

  // Error handling when length > max_length or length < 0
  static void check_array_allocation_length(int length, int max_length, TRAPS);

  void set_vtable_length(int len) { _vtable_len= len; }

  vtableEntry* start_of_vtable() const;
#if INCLUDE_CDS
  void restore_unshareable_info(ClassLoaderData* loader_data, Handle protection_domain, TRAPS);
#endif
 public:
  Method* method_at_vtable(int index);

  static ByteSize vtable_start_offset();
  static ByteSize vtable_length_offset() {
    return byte_offset_of(Klass, _vtable_len);
  }

#if INCLUDE_CDS
  // CDS support - remove and restore oops from metadata. Oops are not shared.
  virtual void remove_unshareable_info();
  virtual void remove_java_mirror();

  bool is_unshareable_info_restored() const {
    assert(is_shared(), "use this for shared classes only");
    if (has_archived_mirror_index()) {
      // _java_mirror is not a valid OopHandle but rather an encoded reference in the shared heap
      return false;
    } else if (_java_mirror.is_empty()) {
      return false;
    } else {
      return true;
    }
  }
#endif // INCLUDE_CDS

 public:
  // ALL FUNCTIONS BELOW THIS POINT ARE DISPATCHED FROM AN OOP
  // These functions describe behavior for the oop not the KLASS.

  // actual oop size of obj in memory in word size.
  virtual size_t oop_size(oop obj) const = 0;

  // Size of klass in word size.
  virtual int size() const = 0;

  // Returns the Java name for a class (Resource allocated)
  // For arrays, this returns the name of the element with a leading '['.
  // For classes, this returns the name with the package separators
  //     turned into '.'s.
  const char* external_name() const;
  // Returns the name for a class (Resource allocated) as the class
  // would appear in a signature.
  // For arrays, this returns the name of the element with a leading '['.
  // For classes, this returns the name with a leading 'L' and a trailing ';'
  //     and the package separators as '/'.
  virtual const char* signature_name() const;

  const char* joint_in_module_of_loader(const Klass* class2, bool include_parent_loader = false) const;
  const char* class_in_module_of_loader(bool use_are = false, bool include_parent_loader = false) const;

  // Returns "interface", "abstract class" or "class".
  const char* external_kind() const;

  // type testing operations
#ifdef ASSERT
 protected:
  virtual bool is_instance_klass_slow()     const { return false; }
  virtual bool is_array_klass_slow()        const { return false; }
  virtual bool is_objArray_klass_slow()     const { return false; }
  virtual bool is_typeArray_klass_slow()    const { return false; }
#endif // ASSERT
 public:

  // Fast non-virtual versions
  #ifndef ASSERT
  #define assert_same_query(xval, xcheck) xval
  #else
 private:
  static bool assert_same_query(bool xval, bool xslow) {
    assert(xval == xslow, "slow and fast queries agree");
    return xval;
  }
 public:
  #endif

  bool is_instance_klass()              const { return assert_same_query(_kind <= InstanceStackChunkKlassKind, is_instance_klass_slow()); }
  // Other is anything that is not one of the more specialized kinds of InstanceKlass.
  bool is_other_instance_klass()        const { return _kind == InstanceKlassKind; }
  bool is_reference_instance_klass()    const { return _kind == InstanceRefKlassKind; }
  bool is_mirror_instance_klass()       const { return _kind == InstanceMirrorKlassKind; }
  bool is_class_loader_instance_klass() const { return _kind == InstanceClassLoaderKlassKind; }
  bool is_array_klass()                 const { return assert_same_query( _kind >= TypeArrayKlassKind, is_array_klass_slow()); }
  bool is_stack_chunk_instance_klass()  const { return _kind == InstanceStackChunkKlassKind; }
  bool is_objArray_klass()              const { return assert_same_query( _kind == ObjArrayKlassKind,  is_objArray_klass_slow()); }
  bool is_typeArray_klass()             const { return assert_same_query( _kind == TypeArrayKlassKind, is_typeArray_klass_slow()); }
  #undef assert_same_query

  // Access flags
  AccessFlags access_flags() const         { return _access_flags;  }
  void set_access_flags(AccessFlags flags) { _access_flags = flags; }

  bool is_public() const                { return _access_flags.is_public(); }
  bool is_final() const                 { return _access_flags.is_final(); }
  bool is_interface() const             { return _access_flags.is_interface(); }
  bool is_abstract() const              { return _access_flags.is_abstract(); }
  bool is_super() const                 { return _access_flags.is_super(); }
  bool is_synthetic() const             { return _access_flags.is_synthetic(); }
  void set_is_synthetic()               { _access_flags.set_is_synthetic(); }
  bool has_finalizer() const            { return _misc_flags.has_finalizer(); }
  void set_has_finalizer()              { _misc_flags.set_has_finalizer(true); }
  bool is_hidden() const                { return _misc_flags.is_hidden_class(); }
  void set_is_hidden()                  { _misc_flags.set_is_hidden_class(true); }
  bool is_value_based() const           { return _misc_flags.is_value_based_class(); }
  void set_is_value_based()             { _misc_flags.set_is_value_based_class(true); }

  klass_flags_t misc_flags() const      { return _misc_flags.value(); }

  inline bool is_non_strong_hidden() const;

  bool is_cloneable() const;
  void set_is_cloneable();

  inline markWord prototype_header() const;
  inline void set_prototype_header(markWord header);
  static ByteSize prototype_header_offset() { return in_ByteSize(offset_of(Klass, _prototype_header)); }

  JFR_ONLY(DEFINE_TRACE_ID_METHODS;)

  virtual void metaspace_pointers_do(MetaspaceClosure* iter);
  virtual MetaspaceObj::Type type() const { return ClassType; }

  inline bool is_loader_alive() const;
  inline bool is_loader_present_and_alive() const;

  void clean_subklass();

  // Clean out unnecessary weak klass links from the whole klass hierarchy.
  static void clean_weak_klass_links(bool unloading_occurred, bool clean_alive_klasses = true);
  // Clean out unnecessary weak klass links from the given InstanceKlass.
  static void clean_weak_instanceklass_links(InstanceKlass* ik);

  // Return self, except for abstract classes with exactly 1
  // implementor.  Then return the 1 concrete implementation.
  Klass *up_cast_abstract();

  // klass name
  Symbol* name() const                   { return _name; }
  void set_name(Symbol* n);

  virtual void release_C_heap_structures(bool release_constant_pool = true);

 public:
  // Get modifier flags from Java mirror cache.
  int modifier_flags() const;

  // Compute modifier flags from the original data. This also allows
  // accessing flags when Java mirror is already dead, e.g. during class
  // unloading.
  virtual u2 compute_modifier_flags() const = 0;

  // JVMTI support
  virtual jint jvmti_class_status() const;

  // Printing
  virtual void print_on(outputStream* st) const;

  virtual void oop_print_value_on(oop obj, outputStream* st);
  virtual void oop_print_on      (oop obj, outputStream* st);

  void print_secondary_supers_on(outputStream* st) const;

  virtual const char* internal_name() const = 0;

  // Verification
  virtual void verify_on(outputStream* st);
  void verify() { verify_on(tty); }

#ifndef PRODUCT
  bool verify_vtable_index(int index);
#endif

  virtual void oop_verify_on(oop obj, outputStream* st);

  // for error reporting
  static bool is_valid(Klass* k);

  static void on_secondary_supers_verification_failure(Klass* super, Klass* sub, bool linear_result, bool table_result, const char* msg);

  // Returns true if this Klass needs to be addressable via narrow Klass ID.
  inline bool needs_narrow_id() const;

};

#endif // SHARE_OOPS_KLASS_HPP
